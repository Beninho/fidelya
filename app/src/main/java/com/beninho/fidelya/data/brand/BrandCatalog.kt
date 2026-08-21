package com.beninho.fidelya.data.brand

import androidx.annotation.DrawableRes
import com.beninho.fidelya.R

/**
 * Les enseignes dont l'application embarque déjà le logo.
 *
 * Fichier généré par `scripts/brand_logos.py` — les logos et leurs couleurs
 * sont extraits des sources décrites là-bas. Ne pas éditer à la main : ajouter
 * l'enseigne à la table du script et le relancer, sinon le catalogue et les
 * drawables se désynchronisent. Voir `docs/brand-logos.md`.
 *
 * Les logos restent la propriété de leurs titulaires ; ils ne servent ici qu'à
 * désigner l'enseigne de la carte que l'utilisateur possède.
 */
data class Brand(
    val name: String,
    /** Le nom du programme de fidélité, en second de la suggestion. */
    val program: String,
    val sector: String,
    @DrawableRes val logo: Int,
    /**
     * La couleur dominante du logo, telle quelle. C'est
     * `nearestModernistColor` qui la ramène sur un pas de la palette au moment
     * de préremplir le fond de la carte.
     */
    val color: String
)

/** Les enseignes, par secteur puis par notoriété — l'ordre de l'issue #5. */
val BRAND_CATALOG: List<Brand> = listOf(
    Brand("Carrefour", "Carrefour Pass", "Grande distribution", R.drawable.brand_carrefour, "#365696"),
    Brand("Leclerc", "Carte Leclerc / Moi+", "Grande distribution", R.drawable.brand_leclerc, "#2473A3"),
    Brand("Auchan", "Carte Auchan / My Auchan", "Grande distribution", R.drawable.brand_auchan, "#C71D28"),
    Brand("Intermarché", "Carte Intermarché", "Grande distribution", R.drawable.brand_intermarche, "#BC2329"),
    Brand("Casino", "Club Casino", "Grande distribution", R.drawable.brand_casino, "#1E6C35"),
    Brand("Monoprix", "Carte Monoprix", "Grande distribution", R.drawable.brand_monoprix, "#E30513"),
    Brand("Franprix", "Carte Franprix", "Grande distribution", R.drawable.brand_franprix, "#EB3340"),
    Brand("Lidl", "Lidl Plus", "Grande distribution", R.drawable.brand_lidl, "#9F9B40"),
    Brand("Aldi", "Offres via l'appli", "Grande distribution", R.drawable.brand_aldi, "#44386B"),
    Brand("Kiabi", "Carte Kiabi", "Mode", R.drawable.brand_kiabi, "#111532"),
    Brand("Uniqlo", "Carte Uniqlo", "Mode", R.drawable.brand_uniqlo, "#ED1E25"),
    Brand("Zara", "Zara Club", "Mode", R.drawable.brand_zara, "#000000"),
    Brand("H&M", "H&M Club", "Mode", R.drawable.brand_hm, "#CC061E"),
    Brand("Lacoste", "Carte Lacoste", "Mode", R.drawable.brand_lacoste, "#000000"),
    Brand("Etam", "Carte Etam", "Mode", R.drawable.brand_etam, "#8D8D8D"),
    Brand("Promod", "Carte Promod", "Mode", R.drawable.brand_promod, "#000000"),
    Brand("Citadium", "Carte Citadium", "Mode", R.drawable.brand_citadium, "#000000"),
    Brand("Decathlon", "Carte Decathlon", "Mode", R.drawable.brand_decathlon, "#39524B"),
    Brand("Sephora", "Carte Sephora", "Beauté", R.drawable.brand_sephora, "#000000"),
    Brand("Marionnaud", "Carte Marionnaud", "Beauté", R.drawable.brand_marionnaud, "#631B4B"),
    Brand("Nocibé", "Carte Nocibé", "Beauté", R.drawable.brand_nocibe, "#E20054"),
    Brand("Douglas", "Carte Douglas", "Beauté", R.drawable.brand_douglas, "#ACCFC9"),
    Brand("Yves Rocher", "Carte Yves Rocher", "Beauté", R.drawable.brand_yves_rocher, "#6C7044"),
    Brand("The Body Shop", "Love Your Body Club", "Beauté", R.drawable.brand_body_shop, "#004034"),
    Brand("L'Occitane", "Carte L'Occitane", "Beauté", R.drawable.brand_occitane, "#001868"),
    Brand("Fnac", "Carte Fnac", "Culture & loisirs", R.drawable.brand_fnac, "#EBB300"),
    Brand("Cultura", "Carte Cultura", "Culture & loisirs", R.drawable.brand_cultura, "#0E3A74"),
    Brand("Micromania", "Carte Micromania", "Culture & loisirs", R.drawable.brand_micromania, "#2C6577"),
    Brand("Darty", "Carte Darty", "Culture & loisirs", R.drawable.brand_darty, "#9F0D15"),
    Brand("Boulanger", "Carte Boulanger", "Culture & loisirs", R.drawable.brand_boulanger, "#FC5300"),
    Brand("Go Sport", "Carte Go Sport", "Culture & loisirs", R.drawable.brand_go_sport, "#000000"),
    Brand("IKEA", "IKEA Family", "Maison", R.drawable.brand_ikea, "#769462"),
    Brand("Conforama", "Carte Conforama", "Maison", R.drawable.brand_conforama, "#E30513"),
    Brand("But", "Carte But", "Maison", R.drawable.brand_but, "#ED1C24"),
    Brand("Fly", "Carte Fly", "Maison", R.drawable.brand_fly, "#AB302E"),
    Brand("Maisons du Monde", "Carte Maisons du Monde", "Maison", R.drawable.brand_maisons_du_monde, "#3C393A"),
    Brand("Leroy Merlin", "Carte Leroy Merlin", "Maison", R.drawable.brand_leroy_merlin, "#7BB51C"),
    Brand("Castorama", "Carte Castorama", "Maison", R.drawable.brand_castorama, "#1479AA"),
    Brand("Truffaut", "Carte Truffaut", "Maison", R.drawable.brand_truffaut, "#005850"),
    Brand("Starbucks", "Starbucks Rewards", "Restauration", R.drawable.brand_starbucks, "#4F4F4F"),
    Brand("McDonald's", "McDonald's App", "Restauration", R.drawable.brand_mcdonalds, "#E01A07"),
    Brand("Paul", "Carte Paul", "Restauration", R.drawable.brand_paul, "#A69E8F"),
    Brand("Subway", "Subway Club", "Restauration", R.drawable.brand_subway, "#709E1E"),
    Brand("La Mie Câline", "Carte La Mie Câline", "Restauration", R.drawable.brand_mie_caline, "#886730"),
    Brand("Picard", "Carte Picard", "Restauration", R.drawable.brand_picard, "#39437A"),
    Brand("Grand Frais", "Carte Grand Frais", "Restauration", R.drawable.brand_grand_frais, "#C6572F"),
    Brand("TotalEnergies", "Carte TotalEnergies Club", "Transport", R.drawable.brand_totalenergies, "#B95C65"),
    Brand("Shell", "Shell ClubSmart", "Transport", R.drawable.brand_shell, "#DD1D21"),
    Brand("Norauto", "Carte Norauto", "Transport", R.drawable.brand_norauto, "#2A446C"),
    Brand("Feu Vert", "Carte Feu Vert", "Transport", R.drawable.brand_feu_vert, "#0B8054"),
    Brand("SNCF", "Carte Avantage", "Transport", R.drawable.brand_sncf, "#BB1248"),
    Brand("Optic 2000", "Carte Optic 2000", "Santé", R.drawable.brand_optic_2000, "#A09CA7"),
    Brand("GrandVision", "Carte GrandVision", "Santé", R.drawable.brand_grandvision, "#8C8C8C"),
    Brand("Audika", "Carte Audika", "Santé", R.drawable.brand_audika, "#196FAE"),
    Brand("Accor", "ALL — Accor Live Limitless", "Divers", R.drawable.brand_accor, "#273437"),
    Brand("SNCF Connect", "Carte Avantage", "Divers", R.drawable.brand_sncf_connect, "#2C4856"),
    Brand("Air France", "Flying Blue", "Divers", R.drawable.brand_air_france, "#220E32"),
    Brand("Amazon", "Amazon Prime", "Divers", R.drawable.brand_amazon, "#43311A"),
    Brand("Cdiscount", "Carte Cdiscount", "Divers", R.drawable.brand_cdiscount, "#3934FF"),
    Brand("Showroomprivé", "Carte Showroomprivé", "Divers", R.drawable.brand_showroomprive, "#F2554F"),
    Brand("Veepee", "Carte Veepee", "Divers", R.drawable.brand_veepee, "#EA008B"),
)
