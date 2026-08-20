package com.beninho.fidelya.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Couche composants du design system « Modernist ».
 *
 * `Color.kt`, `Type.kt` et `Shape.kt` portent les *tokens* ; ce fichier porte
 * les *classes* de `styles.css`, celles que Material 3 ne rend pas telles
 * quelles : `.hr`, `.input` + `.field > label`, `.btn-block`.
 */

/** `--space-1` … `--space-8`, l'échelle 4px à densité 1. */
object ModernistSpace {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s6 = 24.dp
    val s8 = 32.dp
}

/**
 * `--shadow-sm/md/lg`, ramenés à l'élévation Compose par leur décalage
 * vertical (1 / 3 / 12 px). L'équivalence est approximative : une ombre CSS
 * porte aussi un flou et une teinte, que `Modifier.shadow` ne reproduit pas.
 */
object ModernistElevation {
    val sm = 1.dp
    val md = 3.dp
    val lg = 12.dp
}

/**
 * Les opacités d'encre du design system, là où il pose
 * `color-mix(in srgb, var(--color-text) N%, transparent)`.
 */
object ModernistAlpha {
    /** `.field > label` */
    const val Label = 0.70f
    /** `.table th` */
    const val Heading = 0.60f
    /** `.text-muted`, `figcaption` */
    const val Muted = 0.55f
    /** `.card-meta` */
    const val Meta = 0.50f
    /** `--color-divider`, thème clair */
    const val Divider = 0.40f
    /** `--color-divider`, thème Encre : le fond foncé demande un filet plus discret. */
    const val InkDivider = 0.28f
    /** Le filet entre deux lignes de liste, plus doux que les séparateurs de section. */
    const val RowDivider = 0.16f
    const val InkRowDivider = 0.14f
}

/**
 * `--color-divider` : l'encre à 40 %, et non un pas de la rampe neutre. Aplatie
 * sur `--color-bg` elle donnerait #9F9D9D, mais la garder en alpha la laisse
 * fonctionner sur n'importe quel fond.
 *
 * Le thème Encre descend à 28 % — la maquette y pose `rgba(243,242,242,.28)` :
 * sur un fond foncé, le même rapport ferait un filet criard.
 */
@Composable
fun modernistDividerColor(): Color = MaterialTheme.colorScheme.onSurface.copy(
    alpha = if (LocalModernistInk.current) ModernistAlpha.InkDivider else ModernistAlpha.Divider
)

/** Le filet entre deux lignes de liste : 16 % en clair, 14 % en encre. */
@Composable
fun modernistRowDividerColor(): Color = MaterialTheme.colorScheme.onSurface.copy(
    alpha = if (LocalModernistInk.current) ModernistAlpha.InkRowDivider else ModernistAlpha.RowDivider
)

/**
 * Le fond d'une ligne de liste, légèrement détaché du fond de l'écran : blanc en
 * clair, la surface Encre en sombre. C'est ce qui rend la liste dense balayable.
 */
@Composable
fun modernistRowSurface(): Color =
    if (LocalModernistInk.current) ModernistInkRowSurface else ModernistRowSurface

/**
 * Les cadres qui portent un code restent blancs dans les deux thèmes.
 *
 * « Deux surfaces restent blanches par nécessité : le mode caisse et les cadres
 * qui portent un code, parce qu'un lecteur optique a besoin de barres noires sur
 * blanc. » Le texte posé dessus doit donc être encré explicitement, quel que
 * soit le thème — d'où [ModernistCodeInk].
 */
val ModernistCodeSurface = Color(0xFFFFFFFF)
val ModernistCodeInk = ModernistInkBg

/**
 * `.hr` — `dividers: "strong"` dans `theme.json`, soit un filet de 2px là où
 * Material en pose un de 1dp. Sert aussi de bordure basse à `.nav`.
 */
@Composable
fun ModernistDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 2.dp, color = modernistDividerColor())
}

/** `.field > label` : au-dessus du champ, 12px à 70 % d'encre. */
@Composable
fun ModernistFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(bottom = 5.dp),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = ModernistAlpha.Label)
    )
}

/**
 * `h6` : 13px en capitales, interlettrage 0.08em. Le style `labelSmall` porte
 * déjà la taille et le tracking ; Compose n'ayant pas de `text-transform`, les
 * capitales sont explicites.
 */
@Composable
fun ModernistSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color ?: MaterialTheme.colorScheme.onSurface.copy(alpha = ModernistAlpha.Heading)
    )
}

@Composable
private fun fieldBorderColor(focused: Boolean, isError: Boolean): Color = when {
    isError -> MaterialTheme.colorScheme.error
    // `.input:focus-visible { border-color: var(--color-accent) }`
    focused -> MaterialTheme.colorScheme.primary
    else -> modernistDividerColor()
}

private val FieldPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
private val FieldMinHeight = 36.dp

/**
 * `.input` : fond `--color-surface`, filet de 1px en `--color-divider`, angles
 * vifs, texte de 14px, caret à l'accent — et le libellé *au-dessus*, pas
 * flottant dans la bordure comme le fait `OutlinedTextField`. Modernist n'a pas
 * cet idiome Material.
 */
@Composable
fun ModernistTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    error: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Column(modifier) {
        ModernistFieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, fieldBorderColor(focused, error != null), RectangleShape)
                .defaultMinSize(minHeight = FieldMinHeight)
                .padding(FieldPadding),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = Archivo,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = singleLine,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            interactionSource = interactionSource
        )
        if (error != null) {
            Text(
                text = error,
                modifier = Modifier.padding(top = ModernistSpace.s1),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Le même `.input`, en lecture seule et sans champ de saisie : c'est l'ancre
 * d'un `ExposedDropdownMenuBox`. Un `BasicTextField` y capterait le tap au lieu
 * de laisser `menuAnchor` ouvrir le menu.
 */
@Composable
fun ModernistSelectField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Column(modifier) {
        ModernistFieldLabel(label)
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, modernistDividerColor(), RectangleShape)
                .defaultMinSize(minHeight = FieldMinHeight)
                .padding(FieldPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                modifier = Modifier.fillMaxWidth(if (trailing != null) 0.9f else 1f),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (trailing != null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { trailing() }
            }
        }
    }
}

/**
 * `.btn-block` : pleine largeur, fond accent, texte en `--color-bg` — et
 * libellé **aligné à gauche**, `buttonAlign: "left"` dans `theme.json`. Le
 * bouton Material centre le sien.
 */
@Composable
fun ModernistBlockButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModernistButtonSurface(
        text = text,
        onClick = onClick,
        background = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Le filet de 2px en `--color-text` que la maquette pose sous le titre d'un
 * écran. Plus appuyé que `.hr`, qui reste en `--color-divider`.
 */
@Composable
fun ModernistStrongRule(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface)
}

/**
 * L'en-tête d'écran de la maquette : un retour en capitales, le titre en 26px,
 * puis le filet appuyé.
 */
@Composable
fun ModernistScreenHeader(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Les `TopAppBar` de Material écartent elles-mêmes la barre d'état ; un
    // en-tête maison doit le faire, sinon il passe sous l'horloge.
    Column(modifier.statusBarsPadding()) {
        Row(
            Modifier
                .clickable(onClick = onBack)
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Retour",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = backLabel.uppercase(),
                modifier = Modifier.padding(start = ModernistSpace.s2),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = title,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
            fontFamily = Archivo,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp
        )
        ModernistStrongRule()
    }
}

/**
 * La ligne de liste de la maquette : fond de surface, filet bas de 1px, titre en
 * 15px gras et sous-titre en 12.5px atténué. Sert aux réglages comme à la
 * réorganisation.
 */
@Composable
fun ModernistListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(modernistRowSurface())
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(ModernistSpace.s3))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = Archivo,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(ModernistSpace.s3))
                trailing()
            }
        }
        HorizontalDivider(thickness = 1.dp, color = modernistDividerColor())
    }
}

/**
 * `.seg` / `.seg-opt` : un contrôle segmenté encadré, séparateurs internes de
 * 1px, option retenue en accent plein sur texte `--color-bg`. C'est la façon
 * dont le design system rend un choix à plus de deux états.
 */
@Composable
fun ModernistSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.border(1.dp, modernistDividerColor(), RectangleShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, option ->
            if (index > 0) {
                // `.seg-opt + .seg-opt { border-left: 1px solid var(--color-divider) }`
                Box(
                    Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(modernistDividerColor())
                )
            }
            val selected = index == selectedIndex
            Text(
                text = option,
                modifier = Modifier
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                fontSize = 13.sp,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * L'interrupteur carré de la maquette : 46×26, bordure de 2px en encre, pastille
 * de 18px. `Switch` de Material est une capsule, ce que Modernist n'admet pas —
 * `--radius-*` valent tous 0.
 */
@Composable
fun ModernistSquareToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Box(
        modifier
            .width(46.dp)
            .height(26.dp)
            .border(2.dp, MaterialTheme.colorScheme.onSurface, RectangleShape)
            .background(
                if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClickLabel = contentDescription) { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier
                .size(18.dp)
                .background(
                    if (checked) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
        )
    }
}

/**
 * Le bouton carré de 34px à filet fin de la maquette — les flèches ↑ ↓ de
 * l'écran de réorganisation.
 */
@Composable
fun ModernistSquareIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier
            .size(34.dp)
            .border(1.dp, modernistDividerColor(), RectangleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(15.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        )
    }
}

/**
 * Le bouton texte des en-têtes de la maquette : capitales, 11px, interlettrage
 * 0.08em, encre atténuée. « Ordre », « Réglages », « Modifier », « Partager ».
 */
@Composable
fun ModernistTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    Text(
        text = text.uppercase(),
        modifier = modifier.clickable(onClick = onClick),
        style = MaterialTheme.typography.labelSmall,
        color = color ?: MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** `.tag-outline` : un filet fin, 10px en capitales. Porte le format du code. */
@Composable
fun ModernistTag(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier
            .border(1.dp, modernistDividerColor(), RectangleShape)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Le padding commun aux deux boutons — `.btn-block` et son pendant sobre.
 *
 * Ils se posent côte à côte dans les barres basses, donc ils doivent faire
 * exactement la même hauteur. C'est pour ça qu'ils ne passent pas par le
 * `Button` de Material : celui-ci impose une hauteur minimale de 40dp, qui ne
 * correspond ni au padding de la maquette ni à celle du bouton à filet.
 */
private val ButtonPadding = PaddingValues(horizontal = 15.dp, vertical = 14.dp)

@Composable
private fun ModernistButtonSurface(
    text: String,
    onClick: () -> Unit,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    border: Color? = null
) {
    Text(
        text = text,
        modifier = modifier
            .background(background)
            .then(if (border != null) Modifier.border(1.dp, border, RectangleShape) else Modifier)
            .clickable(onClick = onClick)
            .padding(ButtonPadding),
        fontFamily = Archivo,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        textAlign = TextAlign.Start,
        color = contentColor
    )
}

/** Le pendant sobre de `.btn-block` : `.btn-secondary`, filet et fond transparent. */
@Composable
fun ModernistOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    ModernistButtonSurface(
        text = text,
        onClick = onClick,
        background = Color.Transparent,
        contentColor = color ?: MaterialTheme.colorScheme.onSurface,
        // Une action destructrice porte son filet en `error` : le filet neutre la
        // rendrait indistinguable de « Voir la carte existante ».
        border = color ?: modernistDividerColor(),
        modifier = modifier
    )
}

/**
 * Une action secondaire de `ModernistDialog`, rendue en bouton à filet.
 *
 * `destructive` la passe en `error` : une suppression ne doit pas se lire comme
 * une navigation.
 */
data class ModernistDialogAction(
    val label: String,
    val onClick: () -> Unit,
    val destructive: Boolean = false
)

/**
 * La modale de la maquette : angles droits, bordure de 2px en encre — le même
 * appui que `ModernistStrongRule` — et les actions empilées pleine largeur.
 *
 * Material n'a pas d'équivalent : son `AlertDialog` impose des angles arrondis,
 * une élévation teintée et des boutons alignés à droite, trois choses que le
 * design system refuse. `usePlatformDefaultWidth = false` pour que la boîte
 * suive la largeur de l'écran moins ses marges, comme les écrans pleins.
 */
@Composable
fun ModernistDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    secondaryActions: List<ModernistDialogAction> = emptyList()
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, MaterialTheme.colorScheme.onSurface, RectangleShape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(ModernistSpace.s3)
        ) {
            Text(
                text = title,
                fontFamily = Archivo,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                lineHeight = 24.sp
            )
            Text(
                text = body,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(ModernistSpace.s2)) {
                ModernistBlockButton(text = confirmLabel, onClick = onConfirm)
                secondaryActions.forEach { action ->
                    ModernistOutlinedButton(
                        text = action.label,
                        onClick = action.onClick,
                        modifier = Modifier.fillMaxWidth(),
                        color = if (action.destructive) MaterialTheme.colorScheme.error else null
                    )
                }
                ModernistOutlinedButton(
                    text = dismissLabel,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Les lignes libellé / valeur du bas des écrans de détail et de partage :
 * filet haut de 1px, 13px, libellé atténué et valeur en demi-gras.
 */
@Composable
fun ModernistKeyValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    lastInGroup: Boolean = false
) {
    Column(modifier) {
        HorizontalDivider(thickness = 1.dp, color = modernistDividerColor())
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        if (lastInGroup) {
            HorizontalDivider(thickness = 1.dp, color = modernistDividerColor())
        }
    }
}

/**
 * Le champ de recherche de l'écran 03 : filet de 2px en encre — plus appuyé que
 * `.input`, parce qu'il est toujours visible et structure le haut de l'écran.
 */
@Composable
fun ModernistSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    /** Ce que TalkBack annonce : un placeholder visuel ne lui parvient pas. */
    contentDescription: String = placeholder,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, MaterialTheme.colorScheme.onSurface, RectangleShape)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Box(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = Archivo,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { this.contentDescription = contentDescription }
            )
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    // Le placeholder est un ornement : le laisser dans l'arbre
                    // sémantique le ferait passer pour le champ lui-même.
                    modifier = Modifier.clearAndSetSemantics {},
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = ModernistAlpha.Muted)
                )
            }
        }
        if (trailing != null) trailing()
    }
}

/** Le libellé de dénombrement en capitales, à droite de la recherche. */
@Composable
fun ModernistCountLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.7.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
