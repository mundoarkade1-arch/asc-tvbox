package com.ascplay.tvbox;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Uma linha da tela inicial: título (bold), contador ("10 canais"), botão "Ver tudo" em pílula
 * de vidro à direita e, embaixo, a fileira de cards que rola na horizontal. A borda direita da
 * fileira esmaece (fade) para mostrar que dá para rolar. Também mostra os estados de
 * carregando, vazio e erro com o mesmo visual de vidro.
 */
final class SectionRow extends LinearLayout {

    private final TextView title;
    private final TextView count;
    private final GlassSurface.Button viewAll;
    private final GlassSurface statusBox;
    private final TextView statusText;
    private final HorizontalScrollView scroll;
    private final LinearLayout rail;

    SectionRow(Context c, String titleText) {
        super(c);
        setOrientation(VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);

        // cabeçalho
        LinearLayout header = new LinearLayout(c);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setClipChildren(false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, GlassKit.dp(c, 32));
        hp.rightMargin = GlassKit.dp(c, 24); // o botão "Ver tudo" não encosta na borda
        addView(header, hp);

        // traço dourado discreto ao lado do título
        View mark = new View(c);
        GradientDrawable markBg = new GradientDrawable();
        markBg.setColor(GlassKit.alpha(c, R.color.asc_gold, 0.90f));
        markBg.setCornerRadius(GlassKit.dp(c, 2));
        mark.setBackground(markBg);
        LinearLayout.LayoutParams ml = new LinearLayout.LayoutParams(
                GlassKit.dp(c, 3), GlassKit.dp(c, 18));
        ml.leftMargin = GlassKit.dp(c, 6);
        ml.rightMargin = GlassKit.dp(c, 8);
        header.addView(mark, ml);

        title = new TextView(c);
        title.setText(titleText);
        title.setTextSize(GlassKit.sp(18));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setTextColor(GlassKit.color(c, R.color.asc_text_primary));
        header.addView(title, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        count = new TextView(c);
        count.setTextSize(GlassKit.sp(13));
        count.setTextColor(GlassKit.color(c, R.color.asc_text_secondary));
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                0, LayoutParams.WRAP_CONTENT, 1f);
        cl.leftMargin = GlassKit.dp(c, 10);
        header.addView(count, cl);

        viewAll = new GlassSurface.Button(c, "Ver tudo", false);
        header.addView(viewAll, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, GlassKit.dp(c, 28)));

        // estado (carregando / vazio / erro)
        statusBox = new GlassSurface(c, GlassKit.dp(c, 14), false);
        statusText = new TextView(c);
        statusText.setTextSize(GlassKit.sp(13));
        statusText.setTextColor(GlassKit.color(c, R.color.asc_text_secondary));
        statusText.setPadding(GlassKit.dp(c, 14), GlassKit.dp(c, 10), GlassKit.dp(c, 14),
                GlassKit.dp(c, 10));
        statusBox.addView(statusText, new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        sl.topMargin = GlassKit.dp(c, 6);
        sl.bottomMargin = GlassKit.dp(c, 6);
        addView(statusBox, sl);

        // fileira de cards
        scroll = new HorizontalScrollView(c);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setFillViewport(false);
        // a fileira vai até a borda da tela; só o último card ganha folga para respirar
        scroll.setPadding(GlassKit.dp(c, 6), GlassKit.dp(c, 9), GlassKit.dp(c, 30), GlassKit.dp(c, 9));
        // SEM esmaecer nas bordas: o efeito desenhava uma faixa cortada na direita da tela
        scroll.setHorizontalFadingEdgeEnabled(false);
        scroll.setFadingEdgeLength(0);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        addView(scroll, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        rail = new LinearLayout(c);
        rail.setOrientation(HORIZONTAL);
        rail.setClipChildren(false);
        scroll.addView(rail, new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        showLoading();
    }

    LinearLayout rail() {
        return rail;
    }

    GlassSurface.Button viewAllButton() {
        return viewAll;
    }

    void setCount(String text) {
        count.setText(text == null ? "" : text);
    }

    void showLoading() {
        setStatus("Carregando...", false);
    }

    void showMessage(String text) {
        setStatus(text, false);
    }

    /** Mesmo visual de vidro, com o texto em branco para chamar a atenção. */
    void showError(String text) {
        setStatus(text, true);
    }

    private void setStatus(String text, boolean error) {
        Context c = getContext();
        statusText.setText(text);
        statusText.setTextColor(GlassKit.color(c,
                error ? R.color.asc_text_primary : R.color.asc_text_secondary));
        statusBox.setVisibility(VISIBLE);
        scroll.setVisibility(GONE);
        count.setText("");
    }

    /** Há cards: esconde o estado e mostra a fileira. */
    void showItems() {
        statusBox.setVisibility(GONE);
        scroll.setVisibility(VISIBLE);
    }

    /** Primeiro card focável da fileira, ou null. */
    View firstCard() {
        return rail.getChildCount() > 0 ? rail.getChildAt(0) : null;
    }

    boolean hasCards() {
        return rail.getChildCount() > 0 && scroll.getVisibility() == VISIBLE;
    }
}
