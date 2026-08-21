package com.beninho.fidelya.data.logo

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID

/**
 * Stockage des logos d'enseigne.
 *
 * L'URI rendue par le sélecteur de photos n'est pas durable : la permission de
 * lecture tombe à la fin du processus. On recopie donc l'image dans le stockage
 * interne et c'est ce chemin que porte `LoyaltyCard.logoUri`. C'est aussi ce que
 * promet la maquette — « le logo n'est jamais téléversé : il est stocké avec la
 * carte sur le téléphone ».
 */
interface LogoStore {
    /** Recopie l'image désignée. Renvoie le chemin local, ou `null` si la lecture échoue. */
    suspend fun store(source: Uri): String?

    /**
     * Recopie un logo embarqué dans l'application — celui d'une enseigne du
     * catalogue. Le fichier suit le même chemin qu'une image choisie par
     * l'utilisateur : partage, sauvegarde et suppression n'ont pas à distinguer
     * les deux, et un logo remplacé ne laisse rien derrière lui.
     */
    suspend fun storeResource(@DrawableRes resId: Int): String?

    /** Supprime le fichier local. Sans effet sur un chemin vide ou étranger. */
    fun delete(path: String?)

    /** Le fichier local d'un logo, ou `null` s'il n'est pas exploitable. */
    fun file(path: String?): File?
}

/**
 * Résolution d'un chemin de logo sans passer par le store — la couche UI en a
 * besoin pour afficher, et n'a pas à porter une dépendance injectée pour ça.
 * Source unique du garde-fou : [LogoStoreImpl] s'en sert aussi.
 */
object Logos {
    internal const val DIR_NAME = "logos"

    fun dir(context: Context): File = File(context.applicationContext.filesDir, DIR_NAME)

    /**
     * Un backup importé depuis un autre téléphone porte des chemins qui n'ont
     * aucun sens ici — et un chemin arbitraire ne doit surtout pas être lu ni
     * supprimé. On n'accepte donc que ce qui est dans notre propre dossier.
     */
    fun resolve(context: Context, path: String?): File? {
        if (path.isNullOrBlank()) return null
        val candidate = File(path)
        if (candidate.parentFile?.absolutePath != dir(context).absolutePath) return null
        return candidate.takeIf { it.isFile }
    }
}

class LogoStoreImpl(context: Context) : LogoStore {
    private val appContext = context.applicationContext

    private val dir: File
        get() = Logos.dir(appContext).apply { mkdirs() }

    override suspend fun store(source: Uri): String? = withContext(Dispatchers.IO) {
        copyToStorage { appContext.contentResolver.openInputStream(source)!! }
    }

    override suspend fun storeResource(@DrawableRes resId: Int): String? = withContext(Dispatchers.IO) {
        copyToStorage { appContext.resources.openRawResource(resId) }
    }

    /**
     * Recopie un flux dans un fichier neuf du dossier des logos.
     *
     * Le fichier existe dès l'ouverture du flux de sortie : une copie
     * interrompue — disque plein — laisserait donc un fichier tronqué que rien
     * ne désigne, l'appelant ne recevant qu'un `null`. Il part avec l'échec.
     */
    private fun copyToStorage(open: () -> InputStream): String? {
        val target = File(dir, "${UUID.randomUUID()}.img")
        return runCatching {
            open().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target.absolutePath
        }.getOrElse {
            target.delete()
            null
        }
    }

    override fun delete(path: String?) {
        file(path)?.let { runCatching { it.delete() } }
    }

    override fun file(path: String?): File? = Logos.resolve(appContext, path)
}
