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
 * Banner de destaque (hero): arte grande ao fundo com escurecimento à esquerda, painel de vidro
 * com chips (EM DESTAQUE, tipo, gênero · ano, nota), título grande e botões "Assistir" e
 * "+ Minha lista". Sem card de canal ao vivo (removido a pedido do dono). O banner vai até a
 * borda direita da tela: os cantos da direita são retos e os da esquerda arredondados.
 *
 * Esta classe só desenha. Quem escolhe o destaque, gira a cada 8 s e abre as telas é a
 * HomeActivity. Trocar de destaque NUNCA mexe no foco.
 */
final class HeroBanner extends FrameLayout {

    static final int HEIGHT = 410; // desenho 1920x1080 (era 296: ficava curtinho)

    private final ImageView art;
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
        GlassKit.roundClipStart(this, corner);
        setBackground(GlassKit.placeholder(c, corner, true));

        // arte ao fundo
        art = new ImageView(c);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        art.setContentDescription(null);
        addView(art, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // escurecimento à esquerda (cor de fundo do app, de opaca para transparente)
        View scrim = new View(c);
        GradientDrawable sg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{GlassKit.alpha(c, R.color.asc_bg_scrim_dialog, 0.95f),
                        GlassKit.alpha(c, R.color.asc_bg_scrim_dialog, 0.70f),
                        GlassKit.alpha(c, R.color.asc_bg_scrim_dialog, 0.0f)});
        scrim.setBackground(sg);
        addView(scrim, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // painel de vidro (esquerda)
        GlassSurface panel = new GlassSurface(c, GlassKit.d(c, 32), false,
                GlassSurface.STYLE_GLASS, true);
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
        GlassKit.chip(chipFeatured, GlassKit.dp(c, 10), R.color.asc_glass_light, 0.95f,
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
        listButton = new GlassSurface.Button(c, "+ Minha lista", false);
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

        showLoading();
    }

    /** rightBleedPx: quanto o banner passa da margem direita da coluna, até a borda da tela. */
    static LinearLayout.LayoutParams params(Context c, int rightBleedPx) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, GlassKit.d(c, HEIGHT));
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
        GlassKit.chip(t, GlassKit.dp(c, 10), R.color.asc_glass_light, 0.90f, R.color.asc_text_primary);
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
    }

    /** Mostra um destaque. Não pede foco. */
    void showEntry(String name, String kindLabel, String meta, String rating, String artUrl,
                   boolean inList) {
        title.setText(name == null ? "" : name);
        setChip(chipKind, kindLabel);
        setChip(chipMeta, meta);
        setChip(chipRating, rating == null || rating.isEmpty() ? "" : "\u2605 " + rating);
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
