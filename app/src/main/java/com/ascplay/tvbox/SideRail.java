package com.ascplay.tvbox;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu lateral flutuante: cápsula de vidro transparente à esquerda, sempre do mesmo tamanho
 * (estreita), com o ícone e o nome pequeno embaixo de cada item. Não abre nem fecha. Logo do app
 * no topo; Início, Ao vivo, Filmes e Séries; e, ancorados embaixo, Atualizar e Ajustes. O
 * relógio NÃO fica aqui: fica fixo acima do menu (ver host()). O item ativo tem um véu claro e
 * borda dourada (estado "selecionado" do GlassSurface).
 * Ícones: os que o app já tem (ic_nav_*). Cor do ícone e do texto: res/color/nav_text.xml.
 */
final class SideRail extends GlassSurface {

    /** Largura do menu (dp). */
    static final int WIDTH_DP = 52;

    final GlassSurface home;
    final GlassSurface live;
    final GlassSurface movie;
    final GlassSurface series;
    final GlassSurface refresh;
    final GlassSurface settings;

    private final ImageView refreshIcon;
    private final FitColumn col;

    SideRail(Context c) {
        super(c, GlassKit.d(c, 40), false, STYLE_RAIL, false);
        // O item em foco cresce e tem anel/brilho: sem isto o menu cortava a borda de seleção
        setClipChildren(false);
        setClipToPadding(false);

        col = new FitColumn(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setClipChildren(false);
        col.setClipToPadding(false);
        col.setPadding(GlassKit.dp(c, 4), GlassKit.dp(c, 8), GlassKit.dp(c, 4), GlassKit.dp(c, 10));
        addView(col, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        ImageView logo = new ImageView(c);
        logo.setImageResource(R.drawable.brand_icon);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription(null);
        GlassKit.roundClip(logo, GlassKit.dp(c, 10));
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
                GlassKit.dp(c, 30), GlassKit.dp(c, 30));
        ll.bottomMargin = GlassKit.dp(c, 8);
        col.addView(logo, ll);
        col.logo = logo;

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
        relink();
    }


    /**
     * Coluna do menu que SEMPRE cabe na altura que tem. Se os itens (no tamanho normal) não
     * cabem, encolhe altura, margens e ícones na mesma proporção; sem espaço para o nome,
     * esconde o texto e deixa só o ícone; se ainda assim faltar, esconde o logo do topo.
     */
    private static final class FitColumn extends LinearLayout {

        View logo;

        FitColumn(Context c) {
            super(c);
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            if (MeasureSpec.getMode(heightSpec) != MeasureSpec.UNSPECIFIED) {
                int avail = MeasureSpec.getSize(heightSpec) - getPaddingTop() - getPaddingBottom();
                if (avail > 0) {
                    fit(avail);
                }
            }
            super.onMeasure(widthSpec, heightSpec);
        }

        private int natural(View v, boolean icon) {
            Object t = v.getTag(icon ? R.id.tag_rail_icon : R.id.tag_rail_h);
            if (t instanceof Integer) {
                return (Integer) t;
            }
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            int n = lp.height;
            v.setTag(icon ? R.id.tag_rail_icon : R.id.tag_rail_h, n);
            return n;
        }

        private int margins(View v, float f) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
            Object t = v.getTag(R.id.tag_rail_m);
            int[] m;
            if (t instanceof int[]) {
                m = (int[]) t;
            } else {
                m = new int[]{lp.topMargin, lp.bottomMargin};
                v.setTag(R.id.tag_rail_m, m);
            }
            lp.topMargin = Math.round(m[0] * f);
            lp.bottomMargin = Math.round(m[1] * f);
            return m[0] + m[1];
        }

        private void fit(int avail) {
            // 1) tamanho natural de tudo que é visível (menos o espaço elástico)
            int total = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View v = getChildAt(i);
                if (v.getVisibility() == GONE || !(v instanceof GlassSurface || v == logo)) {
                    continue;
                }
                if (v == logo) {
                    v.setVisibility(VISIBLE);
                }
                total += natural(v, false) + margins(v, 1f);
            }
            float f = total > avail ? (float) avail / total : 1f;
            // 2) sem espaço nem para o logo: esconde o logo e refaz a conta
            if (f < 0.8f && logo != null && logo.getVisibility() != GONE) {
                logo.setVisibility(GONE);
                total -= natural(logo, false) + margins(logo, 1f);
                f = total > avail ? (float) avail / total : 1f;
            }
            f = Math.max(0.3f, f);
            for (int i = 0; i < getChildCount(); i++) {
                View v = getChildAt(i);
                if (!(v instanceof GlassSurface || v == logo)) {
                    continue;
                }
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
                int nat = natural(v, false);
                margins(v, f);
                lp.height = Math.round(nat * f);
                if (v instanceof GlassSurface && ((GlassSurface) v).getChildCount() > 0
                        && ((GlassSurface) v).getChildAt(0) instanceof LinearLayout) {
                    LinearLayout inner = (LinearLayout) ((GlassSurface) v).getChildAt(0);
                    View icon = inner.getChildAt(0);
                    View label = inner.getChildAt(1);
                    int iconNat = natural(icon, true);
                    boolean showLabel = f >= 0.8f;
                    if (label != null) {
                        label.setVisibility(showLabel ? VISIBLE : GONE);
                    }
                    int size = showLabel ? Math.round(iconNat * f)
                            : Math.min(iconNat, Math.round(lp.height * 0.62f));
                    ViewGroup.LayoutParams ilp = icon.getLayoutParams();
                    ilp.width = size;
                    ilp.height = size;
                }
            }
        }
    }

    private GlassSurface item(Context c, LinearLayout parent, int iconRes, String label) {
        GlassSurface s = new GlassSurface(c, GlassKit.dp(c, 12), true, STYLE_RAIL_ITEM, false);
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
        inner.addView(icon, new LinearLayout.LayoutParams(GlassKit.dp(c, 20), GlassKit.dp(c, 20)));

        TextView t = new TextView(c);
        t.setText(label);
        t.setTextSize(GlassKit.sp(8));
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
        lp.topMargin = GlassKit.dp(c, 2);
        lp.bottomMargin = GlassKit.dp(c, 2);
        parent.addView(s, lp);

        GlassKit.focusScale(s, 1.06f, null);
        return s;
    }

    // ---------- Suporte do menu (relógio fixo) ----------

    /**
     * Cria o suporte do menu: um quadro da largura do menu, com o RELÓGIO fixo no topo (sempre
     * visível) e o menu logo abaixo. As telas colocam este quadro no lugar do menu, com as
     * mesmas margens.
     */
    static FrameLayout host(Context c, SideRail rail) {
        FrameLayout h = new FrameLayout(c);
        h.setClipChildren(false);
        h.setClipToPadding(false);

        TextClock clock = new TextClock(c);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextSize(GlassKit.sp(15));
        clock.setTypeface(clock.getTypeface(), Typeface.BOLD);
        clock.setSingleLine(true);
        clock.setGravity(Gravity.CENTER);
        clock.setTextColor(GlassKit.color(c, R.color.asc_text_primary));
        clock.setShadowLayer(GlassKit.dp(c, 4), 0, GlassKit.dp(c, 1), 0xCC000000);
        h.addView(clock, new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, GlassKit.dp(c, 22),
                Gravity.TOP | Gravity.CENTER_HORIZONTAL));

        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(
                GlassKit.dp(c, WIDTH_DP), LayoutParams.MATCH_PARENT);
        rp.topMargin = GlassKit.dp(c, 28);
        h.addView(rail, rp);
        return h;
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

    /** Esconde o botão Atualizar (telas que não têm o que atualizar, como Ajustes). */
    void hideRefresh() {
        refresh.setVisibility(GONE);
        relink();
    }

    /**
     * Ordem FIXA do foco: seta para baixo vai ao próximo ícone, seta para cima ao anterior.
     * No primeiro e no último o foco para (não escapa para o conteúdo). Sem isto o Android
     * escolhia "o mais perto" e, por causa do espaço entre Séries e Atualizar, pulava para os
     * cards da tela. Só entram na ordem os ícones visíveis e ativos.
     */
    private void relink() {
        List<GlassSurface> act = new ArrayList<>();
        for (GlassSurface s : items()) {
            if (s.getVisibility() == VISIBLE && s.isEnabled()) {
                act.add(s);
            }
        }
        for (int i = 0; i < act.size(); i++) {
            GlassSurface s = act.get(i);
            GlassSurface up = i > 0 ? act.get(i - 1) : s;
            GlassSurface down = i < act.size() - 1 ? act.get(i + 1) : s;
            s.setNextFocusUpId(GlassKit.ensureId(up));
            s.setNextFocusDownId(GlassKit.ensureId(down));
        }
    }

    /** Durante a atualização o ícone fica mais fraco (indicador discreto). */
    void setRefreshing(boolean refreshing) {
        refreshIcon.setAlpha(refreshing ? 0.4f : 1f);
        refresh.setEnabled(!refreshing);
        relink();
    }
}
