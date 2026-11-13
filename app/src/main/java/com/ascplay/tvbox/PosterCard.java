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
 * Pôster vertical 2:3 (filmes, séries, continuar assistindo, minha lista).
 * 152x228 (desenho 1920x1080), cantos de 22. Nota no canto superior esquerdo (pílula escura),
 * faixa de vidro embaixo com título (1 linha) e subtítulo. Sem capa: letra inicial sobre um
 * degradê sutil. Barra fina de progresso (só no "Continue assistindo").
 */
final class PosterCard extends GlassSurface {

    static final int W = 152;
    static final int H = 228;

    private final TextView initial;
    private final ImageView image;
    private final TextView rating;
    private final TextView title;
    private final TextView subtitle;
    private final GlassKit.Bar progress;

    PosterCard(Context c, View.OnFocusChangeListener extra) {
        super(c, GlassKit.d(c, 22), true);
        final int corner = GlassKit.d(c, 22);

        setForeground(new Ring(c, corner, true));
        GlassKit.roundClip(this, corner);

        initial = new TextView(c);
        initial.setGravity(Gravity.CENTER);
        initial.setTextSize(GlassKit.sp(30));
        initial.setTypeface(initial.getTypeface(), Typeface.BOLD);
        initial.setTextColor(GlassKit.alpha(c, R.color.asc_text_primary, 0.85f));
        initial.setBackground(GlassKit.placeholder(c, 0));
        addView(initial, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        image = new ImageView(c);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setContentDescription(null);
        addView(image, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // faixa de vidro embaixo
        LinearLayout strip = new LinearLayout(c);
        strip.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable stripBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{GlassKit.alpha(c, R.color.asc_glass_light, 0.55f),
                        GlassKit.alpha(c, R.color.asc_glass_light, 0.92f)});
        strip.setBackground(stripBg);
        strip.setPadding(GlassKit.dp(c, 6), GlassKit.dp(c, 5), GlassKit.dp(c, 6), GlassKit.dp(c, 7));
        addView(strip, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        title = new TextView(c);
        title.setTextSize(GlassKit.sp(13));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setMaxLines(2); // nome em até 2 linhas, para dar para ler
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setTextColor(GlassKit.color(c, R.color.asc_text_primary));
        strip.addView(title, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        subtitle = new TextView(c);
        subtitle.setTextSize(GlassKit.sp(12));
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        subtitle.setTextColor(GlassKit.color(c, R.color.asc_text_secondary));
        strip.addView(subtitle, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        progress = new GlassKit.Bar(c);
        progress.setVisibility(GONE);
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, GlassKit.dp(c, 3), Gravity.BOTTOM);
        pl.leftMargin = GlassKit.dp(c, 6);
        pl.rightMargin = GlassKit.dp(c, 6);
        pl.bottomMargin = GlassKit.dp(c, 2);
        addView(progress, pl);

        rating = new TextView(c);
        rating.setTextSize(GlassKit.sp(12));
        rating.setTypeface(rating.getTypeface(), Typeface.BOLD);
        rating.setSingleLine(true);
        rating.setPadding(GlassKit.dp(c, 6), GlassKit.dp(c, 2), GlassKit.dp(c, 6), GlassKit.dp(c, 2));
        GlassKit.chip(rating, GlassKit.dp(c, 10), R.color.asc_glass_light, 0.90f,
                R.color.asc_gold);
        rating.setVisibility(GONE);
        FrameLayout.LayoutParams rl = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        rl.leftMargin = GlassKit.dp(c, 6);
        rl.topMargin = GlassKit.dp(c, 6);
        addView(rating, rl);

        GlassKit.focusScale(this, 1.08f, extra);
    }

    /** Medidas e margem do card dentro da linha. */
    static LinearLayout.LayoutParams params(Context c) {
        // Tamanho calculado pela largura da tela: aparecem no máximo 6 cards por vez.
        // O resto da linha se vê rolando para a direita.
        int screenW = c.getResources().getDisplayMetrics().widthPixels;
        int overhead = GlassKit.dp(c, 16 + 76 + 16 + 24 + 12); // menu, margens e folgas
        int gap = GlassKit.d(c, 20);
        int w = (screenW - overhead - gap * 6) / 6;
        int h = Math.round(w * 1.5f); // proporção 2:3
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.rightMargin = gap;
        return lp;
    }

    void bind(String name, String sub, String iconUrl) {
        title.setText(name == null ? "" : name);
        subtitle.setText(sub == null ? "" : sub);
        initial.setText(GlassKit.firstLetter(name));
        ImageLoader.load(image, iconUrl);
    }

    /** Nota (ex.: "7.8"). Vazio esconde a pílula. */
    void setRating(String value) {
        if (value == null || value.isEmpty()) {
            rating.setVisibility(GONE);
        } else {
            rating.setText("\u2605 " + value);
            rating.setVisibility(VISIBLE);
        }
    }

    /** Progresso de 0 a 1; valor negativo esconde a barra. */
    void setProgressFraction(float f) {
        if (f < 0f) {
            progress.setVisibility(GONE);
        } else {
            progress.setFraction(f);
            progress.setVisibility(VISIBLE);
        }
    }

    void setSubtitle(String sub) {
        subtitle.setText(sub == null ? "" : sub);
    }
}
