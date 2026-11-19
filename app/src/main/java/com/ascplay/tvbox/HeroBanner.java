package com.ascplay.tvbox;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Banner de destaque (hero), mais baixo (350): painel de vidro à esquerda com o selo EM DESTAQUE,
 * o título e os botões "Assistir" e "+ Minha lista"; à direita, um card 2:3 com a capa INTEIRA
 * (sem cortar), abaixo da barra de busca. Sem card de canal ao vivo (removido a pedido do dono).
 * O banner vai até a borda direita da tela: cantos da direita retos e os da esquerda arredondados.
 *
 * Esta classe só desenha. Quem escolhe o destaque, gira a cada 8 s e abre as telas é a
 * HomeActivity. Trocar de destaque NUNCA mexe no foco.
 */
final class HeroBanner extends FrameLayout {

    static final int HEIGHT = 350; // desenho 1920x1080 (antes 500): banner mais baixo, sobra espaço para as linhas
    /** Altura extra (dp): o banner sobe até o topo da tela, atrás da barra de busca/relógio. */
    static final int TOP_EXTRA_DP = 74;

    private final ImageView art;
    private final FrameLayout posterCard;
    private final TextView chipKind;
    private final TextView chipMeta;
    private final TextView chipRating;
    private final TextView title;
    private final GlassSurface.Button playButton;
    private final GlassSurface.Button listButton;

    HeroBanner(Context c) {
        super(c);
        final int corner = GlassKit.d(c, 40);
        setClipChildren(false);
        // só o canto de baixo à esquerda é arredondado: em cima o banner encosta no topo da
        // tela e à direita vai até a borda (os outros cantos ficam fora da view)
        setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(-corner, -corner, view.getWidth() + corner,
                        view.getHeight(), corner);
            }
        });
        setClipToOutline(true);
        setBackground(GlassKit.placeholder(c, corner, true));

        // O fundo é só o degradê do app (a capa em tela cheia ficava cortada). A capa aparece
        // INTEIRA no card da direita (posterCard), mais abaixo.
        art = new ImageView(c);
        art.setScaleType(ImageView.ScaleType.FIT_CENTER); // inteira, nunca cortada
        art.setContentDescription(null);
        posterCard = new FrameLayout(c);
        final int cardCorner = GlassKit.d(c, 24);
        posterCard.setBackground(GlassKit.placeholder(c, cardCorner));
        GlassKit.roundClip(posterCard, cardCorner);
        posterCard.addView(art, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // escurecimento no topo: a barra de busca, "Lista atualizada" e o relógio ficam legíveis
        View topScrim = new View(c);
        topScrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{GlassKit.alpha(c, R.color.asc_bg_scrim_dialog, 0.55f),
                        GlassKit.alpha(c, R.color.asc_bg_scrim_dialog, 0.0f)}));
        addView(topScrim, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, GlassKit.dp(c, 110), Gravity.TOP));

        // painel de vidro (esquerda)
        GlassSurface panel = new GlassSurface(c, GlassKit.d(c, 32), false,
                GlassSurface.STYLE_SEE_THROUGH, false);
        LinearLayout pc = new LinearLayout(c);
        pc.setOrientation(LinearLayout.VERTICAL);
        pc.setClipChildren(false);
        pc.setClipToPadding(false);
        panel.setClipChildren(false);
        panel.setClipToPadding(false);
        pc.setPadding(GlassKit.dp(c, 14), GlassKit.dp(c, 10), GlassKit.dp(c, 14), GlassKit.dp(c, 14));
        panel.addView(pc, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout chips = new LinearLayout(c);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setGravity(Gravity.CENTER_VERTICAL);
        TextView chipFeatured = makeChip(c, "EM DESTAQUE");
        GlassKit.chip(chipFeatured, GlassKit.dp(c, 10), R.color.asc_text_primary, 0.14f,
                R.color.asc_gold);
        chips.addView(chipFeatured, chipParams(c));
        chipKind = makeChip(c, "");
        chips.addView(chipKind, chipParams(c));
        chipMeta = makeChip(c, "");
        chipMeta.setMaxWidth(GlassKit.dp(c, 100));
        chipMeta.setEllipsize(TextUtils.TruncateAt.END);
        chips.addView(chipMeta, chipParams(c));
        chipRating = makeChip(c, "");
        chips.addView(chipRating, chipParams(c));
        pc.addView(chips, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        title = new TextView(c);
        title.setTextSize(GlassKit.sp(30));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setTextColor(GlassKit.color(c, R.color.asc_text_primary));
        title.setShadowLayer(GlassKit.dp(c, 4), 0, GlassKit.dp(c, 1), 0xCC000000);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tl.topMargin = GlassKit.dp(c, 6);
        pc.addView(title, tl);

        LinearLayout buttons = new LinearLayout(c);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setClipChildren(false);
        buttons.setClipToPadding(false);
        buttons.setPadding(GlassKit.dp(c, 4), GlassKit.dp(c, 4), GlassKit.dp(c, 4), GlassKit.dp(c, 4));
        playButton = new GlassSurface.Button(c, "\u25B6  Assistir", true);
        listButton = new GlassSurface.Button(c, "+ Minha lista",
                GlassSurface.STYLE_SEE_THROUGH);
        // largura pelo texto (antes era fixa e o "+ Minha lista" ficava cortado)
        buttons.addView(playButton, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, GlassKit.dp(c, 40)));
        LinearLayout.LayoutParams lb = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, GlassKit.dp(c, 40));
        lb.leftMargin = GlassKit.dp(c, 10);
        buttons.addView(listButton, lb);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        bl.topMargin = GlassKit.dp(c, 6);
        pc.addView(buttons, bl);

        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(
                GlassKit.dp(c, 380), LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.BOTTOM);
        pp.leftMargin = GlassKit.dp(c, 16);
        pp.bottomMargin = GlassKit.dp(c, 16);
        addView(panel, pp);

        // Card da capa (2:3), à direita, ABAIXO da barra de busca/relógio (não passa por trás).
        // Altura = banner visível menos as margens; a largura segue a proporção da capa.
        int visibleH = GlassKit.d(c, HEIGHT);
        int cardH = visibleH - GlassKit.dp(c, 8) - GlassKit.dp(c, 16);
        int cardW = Math.round(cardH * 2f / 3f);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(cardW, cardH,
                Gravity.END | Gravity.BOTTOM);
        cp.rightMargin = GlassKit.dp(c, 28); // margem de segurança da TV (overscan)
        cp.bottomMargin = GlassKit.dp(c, 16);
        addView(posterCard, cp);

        showLoading();
    }

    /** rightBleedPx: quanto o banner passa da margem direita da coluna, até a borda da tela. */
    static FrameLayout.LayoutParams params(Context c, int rightBleedPx) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, GlassKit.d(c, HEIGHT) + GlassKit.dp(c, TOP_EXTRA_DP));
        lp.rightMargin = -rightBleedPx;
        return lp;
    }

    private static TextView makeChip(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(GlassKit.sp(12));
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        t.setSingleLine(true);
        t.setPadding(GlassKit.dp(c, 8), GlassKit.dp(c, 3), GlassKit.dp(c, 8), GlassKit.dp(c, 3));
        GlassKit.chip(t, GlassKit.dp(c, 10), R.color.asc_text_primary, 0.14f,
                R.color.asc_text_primary);
        t.setShadowLayer(GlassKit.dp(c, 3), 0, GlassKit.dp(c, 1), 0xCC000000);
        return t;
    }

    private static LinearLayout.LayoutParams chipParams(Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.rightMargin = GlassKit.dp(c, 6);
        return lp;
    }

    // ---------- Acesso para a HomeActivity ----------

    GlassSurface.Button playButton() {
        return playButton;
    }

    GlassSurface.Button listButton() {
        return listButton;
    }

    boolean hasFocusInside() {
        return playButton.hasFocus() || listButton.hasFocus();
    }

    // ---------- Conteúdo ----------

    /** Antes de ter um destaque: painel com texto de espera e sem botões. */
    void showLoading() {
        chipKind.setVisibility(GONE);
        chipMeta.setVisibility(GONE);
        chipRating.setVisibility(GONE);
        title.setText("ASC Play");
        playButton.setVisibility(INVISIBLE);
        listButton.setVisibility(INVISIBLE);
        playButton.setFocusable(false);
        listButton.setFocusable(false);
        art.setImageDrawable(null);
        posterCard.setVisibility(INVISIBLE);
    }

    /** Mostra um destaque. Não pede foco. */
    void showEntry(String name, String kindLabel, String meta, String rating, String artUrl,
                   boolean inList) {
        title.setText(name == null ? "" : name);
        // Só o selo "EM DESTAQUE" (pedido do dono): sem Filme/Série, gênero·ano e nota.
        setChip(chipKind, "");
        setChip(chipMeta, "");
        setChip(chipRating, "");
        posterCard.setVisibility(VISIBLE);
        playButton.setVisibility(VISIBLE);
        listButton.setVisibility(VISIBLE);
        playButton.setFocusable(true);
        listButton.setFocusable(true);
        setInList(inList);
        ImageLoader.load(art, artUrl, 640);
    }

    private static void setChip(TextView chip, String text) {
        if (text == null || text.isEmpty()) {
            chip.setVisibility(GONE);
        } else {
            chip.setText(text);
            chip.setVisibility(VISIBLE);
        }
    }

    void setInList(boolean inList) {
        listButton.setLabel(inList ? "\u2713 Na lista" : "+ Minha lista");
    }
}
