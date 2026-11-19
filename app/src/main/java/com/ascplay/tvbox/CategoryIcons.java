package com.ascplay.tvbox;

import android.widget.TextView;

/**
 * Ícones das categorias (lista lateral). Desenhos de linha fina, da mesma família dos ícones do
 * menu (res/drawable/ic_cat_*.xml). Identificam o TIPO da categoria; os canais continuam com as
 * suas próprias logos. Não usa emoji: o emoji muda de desenho de aparelho para aparelho e some em
 * TV box antigo (Android 7).
 */
final class CategoryIcons {

    /** Altura extra acima da primeira categoria adulta, para separá-la das outras (dp). */
    private static final int ADULT_GAP_DP = 16;
    /** Margem de cima do item (igual ao item_category.xml). */
    private static final int BASE_TOP_DP = 8;
    private static final int ICON_GAP_DP = 10;

    private CategoryIcons() {
    }

    /** Ícone do grupo (0 = sem ícone). */
    static int forGroup(CategoryGroups.Group g) {
        switch (g) {
            case INICIO:
                return R.drawable.ic_cat_inicio;
            case ESPORTES:
                return R.drawable.ic_cat_esportes;
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
                return R.drawable.ic_cat_adultos;
            default:
                return R.drawable.ic_cat_outros;
        }
    }

    /** Ícone de uma categoria adulta (filmes, séries ou canais): sempre o mesmo, em vermelho. */
    static int adult() {
        return R.drawable.ic_cat_adultos;
    }

    /**
     * Põe o ícone ao lado do nome. "separateTop" afasta a linha de cima (primeira categoria
     * adulta). Tem que redefinir tudo a cada chamada, porque a linha da lista é reaproveitada.
     */
    static void apply(TextView tv, int iconRes, boolean separateTop) {
        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0);
        tv.setCompoundDrawablePadding(iconRes == 0 ? 0 : GlassKit.dp(tv.getContext(), ICON_GAP_DP));
        int top = GlassKit.dp(tv.getContext(), BASE_TOP_DP
                + (separateTop ? ADULT_GAP_DP : 0));
        tv.setPaddingRelative(tv.getPaddingStart(), top, tv.getPaddingEnd(), tv.getPaddingBottom());
    }
}
