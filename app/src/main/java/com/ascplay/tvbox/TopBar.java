package com.ascplay.tvbox;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextClock;
import android.widget.TextView;

/**
 * Barra superior: campo de busca em pílula de vidro, chip "Lista atualizada ..." (com indicador
 * discreto enquanto atualiza) e relógio 24h em pílula de vidro (TextClock: o próprio Android
 * atualiza a cada minuto).
 */
final class TopBar extends LinearLayout {

    private final GlassSurface search;
    private final TextView chipText;
    private final ProgressBar chipSpinner;

    TopBar(Context c) {
        super(c);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setClipChildren(false);
        final int h = GlassKit.dp(c, 36);
        final int corner = GlassKit.dp(c, 18);

        // busca
        search = new GlassSurface(c, corner, true);
        search.setContentDescription("Buscar canais, filmes e séries");
        LinearLayout inner = new LinearLayout(c);
        inner.setOrientation(HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(GlassKit.dp(c, 14), 0, GlassKit.dp(c, 14), 0);
        ImageView icon = new ImageView(c);
        icon.setImageResource(R.drawable.ic_search);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        inner.addView(icon, new LinearLayout.LayoutParams(GlassKit.dp(c, 16), GlassKit.dp(c, 16)));
        TextView hint = new TextView(c);
        hint.setText("Buscar canais, filmes e séries");
        hint.setTextSize(GlassKit.sp(13));
        hint.setSingleLine(true);
        hint.setEllipsize(TextUtils.TruncateAt.END);
        hint.setTextColor(GlassKit.color(c, R.color.asc_text_secondary));
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        hl.leftMargin = GlassKit.dp(c, 8);
        inner.addView(hint, hl);
        search.addView(inner, new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        GlassKit.focusScale(search, 1.05f, null);
        addView(search, new LinearLayout.LayoutParams(GlassKit.dp(c, 240), h));

        // espaço
        android.view.View spacer = new android.view.View(c);
        addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        // chip "Lista atualizada"
        GlassSurface chip = new GlassSurface(c, corner, false);
        LinearLayout chipRow = new LinearLayout(c);
        chipRow.setOrientation(HORIZONTAL);
        chipRow.setGravity(Gravity.CENTER_VERTICAL);
        chipRow.setPadding(GlassKit.dp(c, 12), 0, GlassKit.dp(c, 12), 0);
        chipSpinner = new ProgressBar(c, null, android.R.attr.progressBarStyleSmall);
        chipSpinner.setIndeterminate(true);
        chipSpinner.setVisibility(GONE);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                GlassKit.dp(c, 14), GlassKit.dp(c, 14));
        sp.rightMargin = GlassKit.dp(c, 6);
        chipRow.addView(chipSpinner, sp);
        chipText = new TextView(c);
        chipText.setTextSize(GlassKit.sp(12));
        chipText.setSingleLine(true);
        chipText.setTypeface(chipText.getTypeface(), Typeface.BOLD);
        chipText.setTextColor(GlassKit.color(c, R.color.asc_text_primary));
        chipRow.addView(chipText, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        chip.addView(chipRow, new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, h);
        cl.rightMargin = GlassKit.dp(c, 8);
        addView(chip, cl);

        // relógio 24h
        GlassSurface clockPill = new GlassSurface(c, corner, false);
        TextClock clock = new TextClock(c);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextSize(GlassKit.sp(14));
        clock.setTypeface(clock.getTypeface(), Typeface.BOLD);
        clock.setTextColor(GlassKit.color(c, R.color.asc_text_primary));
        clock.setGravity(Gravity.CENTER);
        clock.setPadding(GlassKit.dp(c, 14), 0, GlassKit.dp(c, 14), 0);
        clockPill.addView(clock, new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        addView(clockPill, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, h));

        setUpdatedText("Lista ainda não atualizada");
    }

    GlassSurface searchPill() {
        return search;
    }

    void setUpdatedText(String text) {
        chipText.setText(text);
    }

    /** Indicador discreto de carregamento dentro do chip. */
    void setUpdating(boolean updating) {
        chipSpinner.setVisibility(updating ? VISIBLE : GONE);
        if (updating) {
            chipText.setText("Atualizando...");
        }
    }
}
