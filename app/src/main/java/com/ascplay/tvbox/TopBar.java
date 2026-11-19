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
 * Barra superior (compacta): campo de busca em pílula de vidro e relógio 24h pequeno em pílula
 * de vidro (TextClock: o próprio Android atualiza a cada minuto). Sem o aviso "Lista atualizada".
 */
final class TopBar extends LinearLayout {

    private final GlassSurface search;

    TopBar(Context c) {
        super(c);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);
        final int h = GlassKit.dp(c, 30);
        final int corner = GlassKit.dp(c, 15);

        // busca
        search = new GlassSurface(c, corner, true, GlassSurface.STYLE_SEE_THROUGH, false);
        search.setContentDescription("Buscar canais, filmes e séries");
        LinearLayout inner = new LinearLayout(c);
        inner.setOrientation(HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(GlassKit.dp(c, 14), 0, GlassKit.dp(c, 14), 0);
        ImageView icon = new ImageView(c);
        icon.setImageResource(R.drawable.ic_search);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        inner.addView(icon, new LinearLayout.LayoutParams(GlassKit.dp(c, 14), GlassKit.dp(c, 14)));
        TextView hint = new TextView(c);
        hint.setText("Buscar canais, filmes e séries");
        hint.setTextSize(GlassKit.sp(12));
        hint.setSingleLine(true);
        hint.setEllipsize(TextUtils.TruncateAt.END);
        hint.setTextColor(GlassKit.color(c, R.color.asc_text_primary));
        hint.setShadowLayer(GlassKit.dp(c, 3), 0, GlassKit.dp(c, 1), 0xCC000000);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        hl.leftMargin = GlassKit.dp(c, 8);
        inner.addView(hint, hl);
        search.addView(inner, new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        GlassKit.focusScale(search, 1.05f, null);
        addView(search, new LinearLayout.LayoutParams(GlassKit.dp(c, 200), h));

        // espaço
        android.view.View spacer = new android.view.View(c);
        addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        // relógio 24h
        GlassSurface clockPill = new GlassSurface(c, corner, false,
                GlassSurface.STYLE_SEE_THROUGH, false);
        TextClock clock = new TextClock(c);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextSize(GlassKit.sp(12));
        clock.setTypeface(clock.getTypeface(), Typeface.BOLD);
        clock.setTextColor(GlassKit.color(c, R.color.asc_text_primary));
        clock.setShadowLayer(GlassKit.dp(c, 3), 0, GlassKit.dp(c, 1), 0xCC000000);
        clock.setGravity(Gravity.CENTER);
        clock.setPadding(GlassKit.dp(c, 10), 0, GlassKit.dp(c, 10), 0);
        clockPill.addView(clock, new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        addView(clockPill, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, h));

    }

    GlassSurface searchPill() {
        return search;
    }

    /** O aviso "Lista atualizada" saiu da barra (pedido do dono); os métodos ficam para a Home compilar. */
    void setUpdatedText(String text) {
    }

    void setUpdating(boolean updating) {
    }
}
