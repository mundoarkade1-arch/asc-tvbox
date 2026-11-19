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
 * Banner de destaque (hero): UM card grande de vidro que começa no topo da tela, ATRÁS da barra
 * de busca e do relógio (eles ficam por cima e continuam visíveis). Dentro do card: à esquerda o
 * selo EM DESTAQUE, o nome e os botões "Assistir" e "+ Minha lista"; logo na frente do nome, a
 * capa INTEIRA (2:3, mais alta que larga, sem cortar) com a altura do card. Sem fundo azul
 * próprio: atrás do card aparece o fundo do app. Sem card de canal ao vivo.
 *
 * Esta classe só desenha. Quem escolhe o destaque, gira a cada 8 s e abre as telas é a
 * HomeActivity. Trocar de destaque NUNCA mexe no foco.
 */
final class HeroBanner extends FrameLayout {

    /** Altura total do banner (dp), do topo da tela até o começo das linhas de baixo. */
    static final int HEIGHT_DP = 200;
    private static final int MARGIN_DP = 8;       // folga do card em cima, embaixo
    private static final int TEXT_WIDTH_DP = 320; // coluna do nome e dos botões

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
        setClipChildren(false);
        setClipToPadding(false);

        // capa inteira, nunca cortada
        art = new ImageView(c);
        art.setScaleType(ImageView.ScaleType.FIT_CENTER);
        art.setContentDescription(null);
        posterCard = new FrameLayout(c);
        final int cardCorner = GlassKit.dp(c, 16);
        posterCard.setBackground(GlassKit.placeholder(c, cardCorner));
        GlassKit.roundClip(posterCard, cardCorner);
        posterCard.addView(art, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // card grande de vidro (começa no topo, atrás da barra de busca)
        GlassSurface panel = new GlassSurface(c, GlassKit.dp(c, 22), false,
                GlassSurface.STYLE_GLASS, false);
        panel.setClipChildren(false);
        panel.setClipToPadding(false);
        LinearLayout inner = new LinearLayout(c);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setClipChildren(false);
        inner.setClipToPadding(false);
        panel.addView(inner, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // coluna do texto: embaixo à esquerda
        LinearLayout pc = new LinearLayout(c);
        pc.setOrientation(LinearLayout.VERTICAL);
        pc.setGravity(Gravity.BOTTOM);
        pc.setClipChildren(false);
        pc.setClipToPadding(false);
        pc.setPadding(GlassKit.dp(c, 16), 0, GlassKit.dp(c, 8), GlassKit.dp(c, 12));
        inner.addView(pc, new LinearLayout.LayoutParams(
                GlassKit.dp(c, TEXT_WIDTH_DP), LayoutParams.MATCH_PARENT));

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
        title.setTextSize(GlassKit.sp(22));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setMaxLines(2);
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
        buttons.addView(playButton, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, GlassKit.dp(c, 36)));
        LinearLayout.LayoutParams lb = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, GlassKit.dp(c, 36));
        lb.leftMargin = GlassKit.dp(c, 10);
        buttons.addView(listButton, lb);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        bl.topMargin = GlassKit.dp(c, 4);
        pc.addView(buttons, bl);

        // capa (2:3) na frente do nome, com a altura do card (menos a folga)
        int cardH = GlassKit.dp(c, HEIGHT_DP - 2 * MARGIN_DP - 2 * MARGIN_DP);
        int cardW = Math.round(cardH * 2f / 3f);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(cardW, cardH);
        cp.topMargin = GlassKit.dp(c, MARGIN_DP);
        cp.bottomMargin = GlassKit.dp(c, MARGIN_DP);
        inner.addView(posterCard, cp);

        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        pp.topMargin = GlassKit.dp(c, MARGIN_DP);
        pp.bottomMargin = GlassKit.dp(c, MARGIN_DP);
        pp.rightMargin = GlassKit.dp(c, 24); // margem de segurança da TV (overscan)
        addView(panel, pp);

        showLoading();
    }

    /** Altura fixa; o banner usa a largura toda da coluna. */
    static FrameLayout.LayoutParams params(Context c, int rightBleedPx) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, GlassKit.dp(c, HEIGHT_DP));
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
