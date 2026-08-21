package com.beninho.fidelya.ui.cardedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.beninho.fidelya.data.brand.Brand
import com.beninho.fidelya.data.brand.brandSuggestions
import com.beninho.fidelya.data.logo.LogoStore
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.domain.color.nearestModernistColor
import com.beninho.fidelya.domain.model.LoyaltyCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class CardEditViewModel(
    private val repository: CardRepository,
    private val logoStore: LogoStore,
    private val cardId: Long,
    prefilledCardNumber: String? = null,
    prefilledFormat: String? = null
) : ViewModel() {
    private val isSaving = AtomicBoolean(false)

    /**
     * Le logo que la carte porte en base. Son fichier ne part qu'une fois
     * l'enregistrement réussi : un logo remplacé puis abandonné par un retour
     * arrière doit rester lisible, la carte enregistrée le désigne encore.
     */
    private var savedLogoPath: String? = null

    /**
     * Les champs que l'utilisateur a touchés. La lecture en base arrive après
     * l'ouverture du formulaire : sans ce garde-fou, une enseigne choisie dans
     * la seconde qui suit se ferait écraser par la carte chargée.
     */
    private val touched = mutableSetOf<Field>()

    private enum class Field { NAME, NUMBER, FORMAT, COLOR, EMOJI, LOGO }

    /**
     * La recopie de logo en cours. Les recopies s'enchaînent au lieu de courir
     * en parallèle, et l'enregistrement l'attend : sinon deux enseignes
     * choisies coup sur coup s'appliqueraient dans l'ordre d'arrivée des
     * copies, et un enregistrement rapide partirait sans le logo en laissant le
     * fichier recopié derrière lui.
     */
    private var logoCopy: Job? = null

    private val _uiState = MutableStateFlow(
        CardEditUiState(
            cardNumber = prefilledCardNumber ?: "",
            barcodeFormat = prefilledFormat ?: "QR_CODE"
        )
    )
    val uiState: StateFlow<CardEditUiState> = _uiState

    init {
        // Le formulaire ouvert depuis un scan arrive avec le numéro déjà lu : on
        // signale le doublon tout de suite plutôt qu'après la saisie du nom.
        if (!prefilledCardNumber.isNullOrBlank()) {
            touched += Field.NUMBER
            if (prefilledFormat != null) touched += Field.FORMAT
            viewModelScope.launch {
                repository.findDuplicate(prefilledCardNumber.trim(), excludeId())?.let { dup ->
                    _uiState.update { it.copy(duplicate = dup) }
                }
            }
        }

        if (cardId > 0) {
            viewModelScope.launch {
                repository.getById(cardId)?.let { card ->
                    savedLogoPath = card.logoUri
                    _uiState.update { s ->
                        s.copy(
                            storeName = if (Field.NAME in touched) s.storeName else card.storeName,
                            cardNumber = if (Field.NUMBER in touched) s.cardNumber else card.cardNumber,
                            barcodeFormat =
                                if (Field.FORMAT in touched) s.barcodeFormat else card.barcodeFormat,
                            backgroundColor =
                                if (Field.COLOR in touched) s.backgroundColor else card.backgroundColor,
                            logoEmoji =
                                if (Field.EMOJI in touched) s.logoEmoji else card.logoEmoji ?: "",
                            logoPath = if (Field.LOGO in touched) s.logoPath else card.logoUri
                        )
                    }
                }
            }
        }
    }

    /**
     * La saisie du nom alimente les suggestions d'enseignes : c'est le seul
     * endroit d'où l'utilisateur peut atteindre un logo embarqué, et il tape ce
     * nom de toute façon.
     */
    fun onStoreNameChange(value: String) {
        touched += Field.NAME
        _uiState.update {
            it.copy(
                storeName = value,
                storeNameError = null,
                brandSuggestions = brandSuggestions(value)
            )
        }
    }

    /**
     * Une enseigne choisie dans les suggestions : son nom, son logo embarqué et
     * sa couleur d'un coup — c'est le formulaire déjà rempli, ce que promet
     * l'issue #5.
     *
     * Le fond reprend la couleur du logo ramenée sur un pas de la palette :
     * l'écran de détail et la liste peignent la carte avec, et une teinte
     * arbitraire y sortirait du jeu de couleurs.
     */
    fun onBrandSelected(brand: Brand) {
        touched += setOf(Field.NAME, Field.COLOR)
        _uiState.update {
            it.copy(
                storeName = brand.name,
                storeNameError = null,
                brandSuggestions = emptyList(),
                backgroundColor = nearestModernistColor(brand.color)
            )
        }
        copyLogo { logoStore.storeResource(brand.logo) }
    }
    // Un numéro retouché invalide l'acceptation précédente : sans ce reset, on
    // pourrait accepter un doublon puis coller un autre numéro déjà pris.
    fun onCardNumberChange(value: String) {
        touched += Field.NUMBER
        _uiState.update {
            it.copy(
                cardNumber = value,
                cardNumberError = null,
                duplicate = null,
                duplicateAccepted = false
            )
        }
    }
    fun onFormatChange(value: String) {
        touched += Field.FORMAT
        _uiState.update { it.copy(barcodeFormat = value) }
    }
    fun onColorChange(value: String) {
        touched += Field.COLOR
        _uiState.update { it.copy(backgroundColor = value) }
    }
    fun onEmojiChange(value: String) {
        touched += Field.EMOJI
        _uiState.update { it.copy(logoEmoji = value) }
    }

    /**
     * Recopie l'image choisie dans le stockage interne. Le logo remplacé part
     * dans la foulée s'il n'avait jamais été enregistré — sans ça, changer de
     * logo laisserait un orphelin — et à l'enregistrement sinon.
     */
    fun onLogoPicked(source: Uri) {
        copyLogo { logoStore.store(source) }
    }

    /**
     * Enchaîne une recopie derrière la précédente et applique son résultat.
     * L'ordre des choix est ainsi celui des applications, et [save] n'a qu'un
     * seul travail à attendre.
     */
    private fun copyLogo(copy: suspend () -> String?) {
        val previous = logoCopy
        logoCopy = viewModelScope.launch {
            previous?.join()
            copy()?.let { replaceLogo(it) }
        }
    }

    /**
     * Le nouveau logo prend la place de l'ancien. Le fichier de l'ancien part
     * tout de suite s'il n'était qu'un brouillon jamais enregistré ; celui de
     * la carte en base attend l'enregistrement, sinon un retour arrière
     * laisserait la carte enregistrée sans son logo.
     */
    private fun replaceLogo(path: String?) {
        val previous = _uiState.value.logoPath
        touched += Field.LOGO
        _uiState.update { it.copy(logoPath = path) }
        if (previous != path && previous != savedLogoPath) logoStore.delete(previous)
    }

    fun onLogoCleared() = replaceLogo(null)

    fun save() {
        val entered = _uiState.value
        val storeNameError = if (entered.storeName.isBlank()) "Le nom est obligatoire" else null
        val cardNumberError = if (entered.cardNumber.isBlank()) "Le numéro est obligatoire" else null

        if (storeNameError != null || cardNumberError != null) {
            _uiState.update { it.copy(storeNameError = storeNameError, cardNumberError = cardNumberError) }
            return
        }

        if (!isSaving.compareAndSet(false, true)) return

        viewModelScope.launch {
            try {
                // La recopie du logo peut être encore en vol : sans cette
                // attente, la carte s'enregistrerait sans son logo et le
                // fichier recopié resterait sans rien qui le désigne.
                logoCopy?.join()
                val s = _uiState.value

                if (!s.duplicateAccepted) {
                    // La recherche est dans la coroutine d'enregistrement, pas avant :
                    // ça ferme la fenêtre entre le contrôle et l'insert.
                    val dup = repository.findDuplicate(s.cardNumber.trim(), excludeId())
                    if (dup != null) {
                        _uiState.update { it.copy(duplicate = dup, duplicateFromSave = true) }
                        return@launch
                    }
                }

                repository.save(
                    LoyaltyCard(
                        id = if (cardId > 0) cardId else 0,
                        storeName = s.storeName.trim(),
                        cardNumber = s.cardNumber.trim(),
                        barcodeFormat = s.barcodeFormat,
                        backgroundColor = s.backgroundColor,
                        logoUri = s.logoPath,
                        logoEmoji = s.logoEmoji.ifBlank { null }
                    )
                )
                // Le fichier du logo remplacé ne part qu'ici : jusqu'à
                // l'enregistrement, c'est lui que la carte en base désigne.
                val replaced = savedLogoPath
                savedLogoPath = s.logoPath
                if (replaced != null && replaced != s.logoPath) logoStore.delete(replaced)

                _uiState.update { it.copy(isSaved = true) }
            } finally {
                isSaving.set(false)
            }
        }
    }

    /** « Enregistrer quand même » : on ferme l'alerte et on relance l'enregistrement. */
    fun onDuplicateAccepted() {
        _uiState.update { it.copy(duplicate = null, duplicateAccepted = true) }
        save()
    }

    /** « Annuler » : l'alerte se ferme, le formulaire reste ouvert pour corriger. */
    fun onDuplicateDismissed() {
        _uiState.update { it.copy(duplicate = null, duplicateFromSave = false) }
    }

    /**
     * « Supprimer l'autre carte » : c'est celle-ci qu'on garde. Si l'alerte venait
     * d'un enregistrement, il reprend tout seul — le doublon n'existe plus.
     */
    fun onDeleteDuplicate() {
        val state = _uiState.value
        val dup = state.duplicate ?: return
        val resumeSave = state.duplicateFromSave
        _uiState.update { it.copy(duplicate = null, duplicateFromSave = false) }
        viewModelScope.launch {
            repository.delete(dup)
            logoStore.delete(dup.logoUri)
            if (resumeSave) save()
        }
    }

    /**
     * « Supprimer cette carte » : l'autre fait déjà le travail. Réservé à
     * l'édition — en création, il n'y a rien en base à supprimer, `Annuler` suffit.
     */
    fun onDeleteSelf() {
        if (cardId <= 0) return
        _uiState.update { it.copy(duplicate = null, duplicateFromSave = false) }
        viewModelScope.launch {
            val card = repository.getById(cardId) ?: return@launch
            repository.delete(card)
            // Deux chemins possibles : celui en base, et celui d'un logo choisi
            // dans le formulaire mais pas encore enregistré.
            logoStore.delete(card.logoUri)
            val pending = _uiState.value.logoPath
            if (pending != card.logoUri) logoStore.delete(pending)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    fun onDeletedConsumed() {
        _uiState.update { it.copy(isDeleted = false) }
    }

    fun onSavedConsumed() {
        _uiState.update { it.copy(isSaved = false) }
    }

    /** En édition, la carte modifiée ne doit pas se signaler comme son propre doublon. */
    private fun excludeId(): Long = if (cardId > 0) cardId else 0L
}

fun cardEditViewModelFactory(
    repository: CardRepository,
    logoStore: LogoStore,
    cardId: Long,
    prefilledCardNumber: String?,
    prefilledFormat: String?
) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CardEditViewModel(
            repository, logoStore, cardId, prefilledCardNumber, prefilledFormat
        ) as T
    }
}
