package com.ascplay.tvbox;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Menu lateral flutuante: cápsula de vidro à esquerda. Logo no topo; Início, Ao vivo, Filmes e
 * Séries; e, ancorados embaixo, Atualizar e Ajustes. O item ativo tem vidro mais claro, borda
 * mais forte e brilho na cor primária do app (estado "selecionado" do GlassSurface).
 * Ícones: os que o app já tem (ic_nav_*). Cor do ícone e do texto: res/color/nav_text.xml.
 */
final class SideRail extends GlassSurface {

    final GlassSurface home;
    final GlassSurface live;
    final GlassSurface movie;
    final GlassSurface series;
    final GlassSurface refresh;
    final GlassSurface settings;

    private final ImageView refreshIcon;

    SideRail(Context c) {
        super(c, GlassKit.d(c, 40), false, STYLE_GLASS, true);

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(GlassKit.dp(c, 5), GlassKit.dp(c, 10), GlassKit.dp(c, 5), GlassKit.dp(c, 12));
        addView(col, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        ImageView logo = new ImageView(c);
        logo.setImageResource(R.drawable.brand_icon);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription(null);
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
                GlassKit.dp(c, 32), GlassKit.dp(c, 32));
        ll.bottomMargin = GlassKit.dp(c, 8);
        col.addView(logo, ll);

        home = item(c, col, R.drawable.ic_nav_home, "Início");
        live = item(c, col, R.drawable.ic_nav_live, "Ao vivo");
        movie = item(c, col, R.drawable.ic_nav_movie, "Filmes");
        series = item(c, col, R.drawable.ic_nav_series, "Séries");

        View spacer = new View(c);
        col.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));

        refresh = item(c, col, R.drawable.ic_nav_refresh, "Atualizar");
        refreshIcon = (ImageView) ((LinearLayout) refresh.getChildAt(0)).getChildAt(0);
        settings = item(c, col, R.drawable.ic_nav_settings, "Ajustes");

        home.setSelected(true);
    }

    private GlassSurface item(Context c, LinearLayout parent, int iconRes, String label) {
        GlassSurface s = new GlassSurface(c, GlassKit.dp(c, 16), true);
        s.setContentDescription(label);

        LinearLayout inner = new LinearLayout(c);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setDuplicateParentStateEnabled(true);
        s.addView(inner, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        ImageView icon = new ImageView(c);
        icon.setImageResource(iconRes);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setDuplicateParentStateEnabled(true);
        inner.addView(icon, new LinearLayout.LayoutParams(GlassKit.dp(c, 22), GlassKit.dp(c, 22)));

        TextView t = new TextView(c);
        t.setText(label);
        t.setTextSize(12);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        t.setTextColor(GlassKit.navText(c));
        t.setDuplicateParentStateEnabled(true);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tl.topMargin = GlassKit.dp(c, 1);
        inner.addView(t, tl);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, GlassKit.dp(c, 46));
        lp.topMargin = GlassKit.dp(c, 3);
        lp.bottomMargin = GlassKit.dp(c, 3);
        parent.addView(s, lp);

        GlassKit.focusScale(s, 1.06f, null);
        return s;
    }

    /** Todos os itens, de cima para baixo (sem o espaço). */
    GlassSurface[] items() {
        return new GlassSurface[]{home, live, movie, series, refresh, settings};
    }

    /** Seta para a direita em qualquer item do menu: devolve o foco ao conteúdo. */
    void setOnRight(final Runnable onRight) {
        View.OnKeyListener k = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN && onRight != null) {
                        onRight.run();
                    }
                    return true;
                }
                return false;
            }
        };
        for (GlassSurface s : items()) {
            s.setOnKeyListener(k);
        }
    }

    /** Durante a atualização o ícone fica mais fraco (indicador discreto). */
    void setRefreshing(boolean refreshing) {
        refreshIcon.setAlpha(refreshing ? 0.4f : 1f);
        refresh.setEnabled(!refreshing);
    }
}
