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
 * "+ Minha lista"; à direita, o card "AO VIVO AGORA" (miniatura, canal, programa e progresso).
 *
 * Esta classe só desenha. Quem escolhe o destaque, gira a cada 8 s e abre as telas é a
 * HomeActivity. Trocar de destaque NUNCA mexe no foco.
 */
final class HeroBanner extends FrameLayout {

    static final int HEIGHT = 296; // desenho 1920x1080 (~260 + folga para os botões)

    private final ImageView art;
    private final TextView chipKind;
    private final TextView chipMeta;
    private final TextView chipRating;
    private final TextView title;
    private final GlassSurface.Button playButton;
    private final GlassSurface.Button listButton;

    private final GlassSurface liveCard;
    private final TextView liveInitial;
    private final ImageView liveThumb;
    private final TextView liveName;
    private final TextView liveProgram;
    private final GlassKit.Bar liveBar;

    HeroBanner(Context c) {
        super(c);
        final int corner = GlassKit.d(c, 40);
        setClipChildren(false);
        GlassKit.roundClip(this, corner);
        setBackground(GlassKit.placeholder(c, corner));

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
        pc.setPadding(GlassKit.dp(c, 14), GlassKit.dp(c, 10), GlassKit.dp(c, 14), GlassKit.dp(c, 12));
        panel.addView(pc, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout chips = new LinearLayout(c);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setGravity(Gravity.CENTER_VERTICAL);
        TextView chipFeatured = makeChip(c, "EM DESTAQUE");
        GlassKit.chip(chipFeatured, GlassKit.dp(c, 10), R.color.asc_accent, 0.90f,
                R.color.asc_text_primary);
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
        title.setTextSize(GlassKit.sp(26));
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
        playButton = new GlassSurface.Button(c, "\u25B6  Assistir", true);
        listButton = new GlassSurface.Button(c, "+ Minha lista", false);
        buttons.addView(playButton, new LinearLayout.LayoutParams(
                GlassKit.dp(c, 100), GlassKit.dp(c, 34)));
        LinearLayout.LayoutParams lb = new LinearLayout.LayoutParams(
                GlassKit.dp(c, 112), GlassKit.dp(c, 34));
        lb.leftMargin = GlassKit.dp(c, 10);
        buttons.addView(listButton, lb);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        bl.topMargin = GlassKit.dp(c, 8);
        pc.addView(buttons, bl);

        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(
                GlassKit.dp(c, 340), LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL);
        pp.leftMargin = GlassKit.dp(c, 14);
        addView(panel, pp);

        // card "AO VIVO AGORA" (direita)
        liveCard = new GlassSurface(c, GlassKit.dp(c, 16), true, GlassSurface.STYLE_GLASS, true);
        LinearLayout lc = new LinearLayout(c);
        lc.setOrientation(LinearLayout.VERTICAL);
        lc.setPadding(GlassKit.dp(c, 10), GlassKit.dp(c, 9), GlassKit.dp(c, 10), GlassKit.dp(c, 10));
        liveCard.addView(lc, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout labelRow = new LinearLayout(c);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);
        View dot = new View(c);
        GradientDrawable dg = new GradientDrawable();
        dg.setShape(GradientDrawable.OVAL);
        dg.setColor(GlassKit.color(c, R.color.asc_focus));
        dot.setBackground(dg);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(
                GlassKit.dp(c, 7), GlassKit.dp(c, 7));
        dl.rightMargin = GlassKit.dp(c, 6);
        labelRow.addView(dot, dl);
        TextView label = new TextView(c);
        label.setText("AO VIVO AGORA");
        label.setTextSize(GlassKit.sp(12));
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        label.setTextColor(GlassKit.color(c, R.color.asc_focus));
        labelRow.addView(label, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        lc.addView(labelRow, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout thumbBox = new FrameLayout(c);
        GradientDrawable tb = new GradientDrawable();
        tb.setColor(GlassKit.alpha(c, R.color.asc_text_primary, 0.10f));
        tb.setCornerRadius(GlassKit.dp(c, 9));
        thumbBox.setBackground(tb);
        GlassKit.roundClip(thumbBox, GlassKit.dp(c, 9));
        liveInitial = new TextView(c);
        liveInitial.setGravity(Gravity.CENTER);
        liveInitial.setTextSize(GlassKit.sp(16));
        liveInitial.setTypeface(liveInitial.getTypeface(), Typeface.BOLD);
        liveInitial.setTextColor(GlassKit.color(c, R.color.asc_focus));
        thumbBox.addView(liveInitial, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        liveThumb = new ImageView(c);
        liveThumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
        liveThumb.setPadding(GlassKit.dp(c, 3), GlassKit.dp(c, 3), GlassKit.dp(c, 3), GlassKit.dp(c, 3));
        thumbBox.addView(liveThumb, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        body.addView(thumbBox, new LinearLayout.LayoutParams(GlassKit.dp(c, 40), GlassKit.dp(c, 40)));

        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams xl = new LinearLayout.LayoutParams(
                0, LayoutParams.WRAP_CONTENT, 1f);
        xl.leftMargin = GlassKit.dp(c, 8);
        body.addView(texts, xl);
        liveName = new TextView(c);
        liveName.setTextSize(GlassKit.sp(13));
        liveName.setTypeface(liveName.getTypeface(), Typeface.BOLD);
        liveName.setSingleLine(true);
        liveName.setEllipsize(TextUtils.TruncateAt.END);
        liveName.setTextColor(GlassKit.color(c, R.color.asc_text_primary));
        texts.addView(liveName, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        liveProgram = new TextView(c);
        liveProgram.setTextSize(GlassKit.sp(12));
        liveProgram.setSingleLine(true);
        liveProgram.setEllipsize(TextUtils.TruncateAt.END);
        liveProgram.setTextColor(GlassKit.color(c, R.color.asc_text_secondary));
        texts.addView(liveProgram, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = GlassKit.dp(c, 6);
        lc.addView(body, bodyLp);

        liveBar = new GlassKit.Bar(c);
        liveBar.setVisibility(GONE);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, GlassKit.dp(c, 3));
        barLp.topMargin = GlassKit.dp(c, 8);
        lc.addView(liveBar, barLp);

        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                GlassKit.dp(c, 170), LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL);
        cp.rightMargin = GlassKit.dp(c, 16);
        addView(liveCard, cp);
        GlassKit.focusScale(liveCard, 1.06f, null);
        liveCard.setVisibility(GONE);

        showLoading();
    }

    static LinearLayout.LayoutParams params(Context c) {
        return new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, GlassKit.d(c, HEIGHT));
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

    GlassSurface liveCard() {
        return liveCard;
    }

    boolean hasFocusInside() {
        return playButton.hasFocus() || listButton.hasFocus() || liveCard.hasFocus();
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
        ImageLoader.load(art, artUrl, 512);
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

    /** Card "AO VIVO AGORA". progress < 0 esconde a barra (sem guia de programação). */
    void setLive(String channel, String program, float progress, String iconUrl) {
        liveCard.setVisibility(VISIBLE);
        liveName.setText(channel == null ? "" : channel);
        liveProgram.setText(program == null ? "" : program);
        liveInitial.setText(GlassKit.firstLetter(channel));
        ImageLoader.load(liveThumb, iconUrl);
        setLiveProgress(program, progress);
    }

    void setLiveProgress(String program, float progress) {
        liveProgram.setText(program == null ? "" : program);
        if (progress < 0f) {
            liveBar.setVisibility(GONE);
        } else {
            liveBar.setFraction(progress);
            liveBar.setVisibility(VISIBLE);
        }
    }

    void hideLive() {
        liveCard.setVisibility(GONE);
    }
}
