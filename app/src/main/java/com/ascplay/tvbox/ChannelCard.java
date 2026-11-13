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
 * Card horizontal de canal ao vivo: 264x96 (desenho 1920x1080), cantos de 26.
 * Logo à esquerda (cantos arredondados), nome, programa atual (ou a categoria, se não houver
 * guia), ponto de "ao vivo" e barra fina de progresso embaixo (só com guia de programação).
 */
final class ChannelCard extends GlassSurface {

    static final int W = 340;
    static final int H = 104;

    private final TextView initial;
    private final ImageView logo;
    private final TextView name;
    private final TextView sub;
    private final GlassKit.Bar progress;

    ChannelCard(Context c, View.OnFocusChangeListener extra) {
        super(c, GlassKit.d(c, 26), true);

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(GlassKit.dp(c, 8), 0, GlassKit.dp(c, 8), 0);
        addView(row, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // logo com cantos arredondados
        FrameLayout logoBox = new FrameLayout(c);
        GradientDrawable boxBg = new GradientDrawable();
        boxBg.setColor(GlassKit.alpha(c, R.color.asc_text_primary, 0.10f));
        boxBg.setCornerRadius(GlassKit.dp(c, 9));
        logoBox.setBackground(boxBg);
        GlassKit.roundClip(logoBox, GlassKit.dp(c, 9));
        initial = new TextView(c);
        initial.setGravity(Gravity.CENTER);
        initial.setTextSize(GlassKit.sp(16));
        initial.setTypeface(initial.getTypeface(), Typeface.BOLD);
        initial.setTextColor(GlassKit.color(c, R.color.asc_focus));
        logoBox.addView(initial, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        logo = new ImageView(c);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setPadding(GlassKit.dp(c, 3), GlassKit.dp(c, 3), GlassKit.dp(c, 3), GlassKit.dp(c, 3));
        logoBox.addView(logo, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        int box = GlassKit.dp(c, 36);
        row.addView(logoBox, new LinearLayout.LayoutParams(box, box));

        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(
                0, LayoutParams.WRAP_CONTENT, 1f);
        tl.leftMargin = GlassKit.dp(c, 8);
        tl.rightMargin = GlassKit.dp(c, 6);
        row.addView(texts, tl);

        name = new TextView(c);
        name.setTextSize(GlassKit.sp(13));
        name.setTypeface(name.getTypeface(), Typeface.BOLD);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setTextColor(GlassKit.color(c, R.color.asc_text_primary));
        texts.addView(name, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        sub = new TextView(c);
        sub.setTextSize(GlassKit.sp(12));
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        sub.setTextColor(GlassKit.color(c, R.color.asc_text_secondary));
        texts.addView(sub, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // ponto "ao vivo"
        View dot = new View(c);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(GlassKit.color(c, R.color.asc_gold));
        dot.setBackground(dotBg);
        int ds = GlassKit.dp(c, 6);
        FrameLayout.LayoutParams dl = new FrameLayout.LayoutParams(ds, ds, Gravity.TOP | Gravity.END);
        dl.topMargin = GlassKit.dp(c, 8);
        dl.rightMargin = GlassKit.dp(c, 9);
        addView(dot, dl);

        progress = new GlassKit.Bar(c);
        progress.setVisibility(GONE);
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, GlassKit.dp(c, 2), Gravity.BOTTOM);
        pl.leftMargin = GlassKit.dp(c, 10);
        pl.rightMargin = GlassKit.dp(c, 10);
        pl.bottomMargin = GlassKit.dp(c, 4);
        addView(progress, pl);

        GlassKit.focusScale(this, 1.08f, extra);
    }

    static LinearLayout.LayoutParams params(Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                GlassKit.d(c, W), GlassKit.d(c, H));
        lp.rightMargin = GlassKit.d(c, 20);
        return lp;
    }

    void bind(String channelName, String subtitle, String iconUrl) {
        name.setText(channelName == null ? "" : channelName);
        sub.setText(subtitle == null ? "" : subtitle);
        initial.setText(GlassKit.firstLetter(channelName));
        ImageLoader.load(logo, iconUrl);
    }

    void setSubtitle(String text) {
        sub.setText(text == null ? "" : text);
    }

    /** Progresso do programa de 0 a 1; negativo esconde a barra (sem guia de programação). */
    void setProgressFraction(float f) {
        if (f < 0f) {
            progress.setVisibility(GONE);
        } else {
            progress.setFraction(f);
            progress.setVisibility(VISIBLE);
        }
    }
}
