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
    Brand("Carrefour", "Carrefour Pass", R.drawable.brand_carrefour, "#005BAB"),
    Brand("Leclerc", "Carte Leclerc / Moi+", R.drawable.brand_leclerc, "#0B70B5"),
    Brand("Auchan", "Carte Auchan / My Auchan", R.drawable.brand_auchan, "#E0040D"),
    Brand("Intermarché", "Carte Intermarché", R.drawable.brand_intermarche, "#EC1C23"),
    Brand("Casino", "Club Casino", R.drawable.brand_casino, "#006A36"),
    Brand("Monoprix", "Carte Monoprix", R.drawable.brand_monoprix, "#E40613"),
    Brand("Franprix", "Carte Franprix", R.drawable.brand_franprix, "#EA1C2B"),
    Brand("Lidl", "Lidl Plus", R.drawable.brand_lidl, "#FEF000"),
    Brand("Aldi", "Offres via l'appli", R.drawable.brand_aldi, "#001D77"),
    Brand("Magasins U", "Carte U", R.drawable.brand_u, "#E22019"),
    Brand("Chlorophylle Coop", "Carte chlorophylle", R.drawable.brand_chlorophylle, "#E85214"),
    Brand("Kiabi", "Carte Kiabi", R.drawable.brand_kiabi, "#111532"),
    Brand("Uniqlo", "Carte Uniqlo", R.drawable.brand_uniqlo, "#EC1D24"),
    Brand("Zara", "Zara Club", R.drawable.brand_zara, "#000000"),
    Brand("H&M", "H&M Club", R.drawable.brand_hm, "#CB061D"),
    Brand("Lacoste", "Carte Lacoste", R.drawable.brand_lacoste, "#000000"),
    Brand("Etam", "Carte Etam", R.drawable.brand_etam, "#D8D8D8"),
    Brand("Promod", "Carte Promod", R.drawable.brand_promod, "#000000"),
    Brand("Citadium", "Carte Citadium", R.drawable.brand_citadium, "#000000"),
    Brand("Sephora", "Carte Sephora", R.drawable.brand_sephora, "#000000"),
    Brand("Marionnaud", "Carte Marionnaud", R.drawable.brand_marionnaud, "#631B4B"),
    Brand("Nocibé", "Carte Nocibé", R.drawable.brand_nocibe, "#E30054"),
    Brand("Douglas", "Carte Douglas", R.drawable.brand_douglas, "#C2E9E2"),
    Brand("Yves Rocher", "Carte Yves Rocher", R.drawable.brand_yves_rocher, "#899637"),
    Brand("The Body Shop", "Love Your Body Club", R.drawable.brand_body_shop, "#004135"),
    Brand("L'Occitane", "Carte L'Occitane", R.drawable.brand_occitane, "#001868"),
    Brand("Fnac", "Carte Fnac", R.drawable.brand_fnac, "#EAB200"),
    Brand("Cultura", "Carte Cultura", R.drawable.brand_cultura, "#002E6C"),
    Brand("Micromania", "Carte Micromania", R.drawable.brand_micromania, "#164194"),
    Brand("Darty", "Carte Darty", R.drawable.brand_darty, "#E30613"),
    Brand("Boulanger", "Carte Boulanger", R.drawable.brand_boulanger, "#FC5300"),
    Brand("Go Sport", "Carte Go Sport", R.drawable.brand_go_sport, "#000000"),
    Brand("Decathlon", "Carte Decathlon", R.drawable.brand_decathlon, "#282A29"),
    Brand("Intersport", "Team INTERSPORT", R.drawable.brand_intersport, "#10438E"),
    Brand("IKEA", "IKEA Family", R.drawable.brand_ikea, "#0057A7"),
    Brand("Conforama", "Carte Conforama", R.drawable.brand_conforama, "#E30613"),
    Brand("But", "Carte But", R.drawable.brand_but, "#EC1C24"),
    Brand("Fly", "Carte Fly", R.drawable.brand_fly, "#EE3431"),
    Brand("Maisons du Monde", "Carte Maisons du Monde", R.drawable.brand_maisons_du_monde, "#393536"),
    Brand("Leroy Merlin", "Carte Leroy Merlin", R.drawable.brand_leroy_merlin, "#7BB51C"),
    Brand("Castorama", "Carte Castorama", R.drawable.brand_castorama, "#0071B9"),
    Brand("Truffaut", "Carte Truffaut", R.drawable.brand_truffaut, "#005851"),
    Brand("Jardiland", "Carte jardiland", R.drawable.brand_jardiland, "#FE3C00"),
    Brand("Starbucks", "Starbucks Rewards", R.drawable.brand_starbucks, "#424242"),
    Brand("McDonald's", "McDonald's App", R.drawable.brand_mcdonalds, "#DA0007"),
    Brand("Paul", "Carte Paul", R.drawable.brand_paul, "#F9E8C9"),
    Brand("Subway", "Subway Club", R.drawable.brand_subway, "#008837"),
    Brand("La Mie Câline", "Carte La Mie Câline", R.drawable.brand_mie_caline, "#503327"),
    Brand("Picard", "Carte Picard", R.drawable.brand_picard, "#38427A"),
    Brand("Grand Frais", "Carte Grand Frais", R.drawable.brand_grand_frais, "#C41B26"),
    Brand("TotalEnergies", "Carte TotalEnergies Club", R.drawable.brand_totalenergies, "#FE0301"),
    Brand("Shell", "Shell ClubSmart", R.drawable.brand_shell, "#DC1C20"),
    Brand("Norauto", "Carte Norauto", R.drawable.brand_norauto, "#002A6E"),
    Brand("Feu Vert", "Carte Feu Vert", R.drawable.brand_feu_vert, "#007A4C"),
    Brand("SNCF", "Carte Avantage", R.drawable.brand_sncf, "#DB0D0D"),
    Brand("Optic 2000", "Carte Optic 2000", R.drawable.brand_optic_2000, "#939DCA"),
    Brand("GrandVision", "Carte GrandVision", R.drawable.brand_grandvision, "#D7D7D7"),
    Brand("Audika", "Carte Audika", R.drawable.brand_audika, "#0084C9"),
    Brand("Accor", "ALL — Accor Live Limitless", R.drawable.brand_accor, "#12283B"),
    Brand("SNCF Connect", "Carte Avantage", R.drawable.brand_sncf_connect, "#0B121E"),
    Brand("Air France", "Flying Blue", R.drawable.brand_air_france, "#041039"),
    Brand("Amazon", "Amazon Prime", R.drawable.brand_amazon, "#211E1E"),
    Brand("Cdiscount", "Carte Cdiscount", R.drawable.brand_cdiscount, "#3732FF"),
    Brand("Showroomprivé", "Carte Showroomprivé", R.drawable.brand_showroomprive, "#F1544E"),
    Brand("Veepee", "Carte Veepee", R.drawable.brand_veepee, "#EB008B"),
)
