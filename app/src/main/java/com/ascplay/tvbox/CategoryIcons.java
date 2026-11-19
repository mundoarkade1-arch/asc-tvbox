package com.ascplay.tvbox;

/**
 * Ícones das categorias (lista lateral). O NOME vem primeiro e o ícone DEPOIS (CategoryLabel).
 * Desenhos de linha no estilo Lucide (res/drawable/ic_cat_*.xml), 26dp, cor viva
 * (@color/cat_icon). Adultos não usa ícone: usa a tarja vermelha "18+" depois do nome.
 * Não usa emoji: o emoji muda de desenho de aparelho para aparelho e some em TV box antigo.
 */
final class CategoryIcons {

    /** Marcador (não é recurso): categoria adulta, que ganha a tarja 18+. */
    private static final int ADULT = -18;
    /** Altura extra acima da primeira categoria adulta, para separá-la das outras (dp). */
    private static final int ADULT_GAP_DP = 16;

    private CategoryIcons() {
    }

    /** Ícone do grupo (0 = sem ícone). */
    static int forGroup(CategoryGroups.Group g) {
        switch (g) {
            case INICIO:
                return R.drawable.ic_cat_inicio;
            case ESPORTES:
                return R.drawable.ic_cat_esportes;
            case REALITY:
                return R.drawable.ic_cat_reality;
            case ABERTOS:
                return R.drawable.ic_cat_abertos;
            case FILMES:
                return R.drawable.ic_cat_filmes;
            case SERIES:
                return R.drawable.ic_cat_series;
            case DOCUMENTARIOS:
                return R.drawable.ic_cat_documentarios;
            case NOTICIAS:
                return R.drawable.ic_cat_noticias;
            case INFANTIL:
                return R.drawable.ic_cat_infantil;
            case VARIEDADES:
                return R.drawable.ic_cat_variedades;
            case H24:
                return R.drawable.ic_cat_h24;
            case INTERNACIONAIS:
                return R.drawable.ic_cat_internacionais;
            case ADULTOS:
                return ADULT;
            default:
                return R.drawable.ic_cat_outros;
        }
    }

    /** Marcador de categoria adulta (canais, filmes ou séries): vira a tarja 18+. */
    static int adult() {
        return ADULT;
    }

    /**
     * Põe o ícone (ou a tarja 18+) DEPOIS do nome. "separateTop" afasta a linha de cima (primeira
     * categoria adulta). "locked" põe um cadeado pequeno no fim (controle parental ligado).
     */
    static void apply(CategoryLabel tv, int icon, boolean separateTop, boolean locked) {
        int extra = separateTop ? GlassKit.dp(tv.getContext(), ADULT_GAP_DP) : 0;
        if (icon == ADULT) {
            tv.setTrailing(0, true, locked, extra);
        } else {
            tv.setTrailing(icon, false, locked, extra);
        }
    }
}
