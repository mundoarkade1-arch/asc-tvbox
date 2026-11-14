package com.ascplay.tvbox;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Janela "Deseja sair?" em vidro transparente (GlassSurface.STYLE_CLEAR): título grande e,
 * embaixo, duas opções empilhadas: "Sim" em cima e "Não" embaixo. A imagem de fundo do app
 * aparece por trás do vidro. Voltar fecha a janela (= Não). Só visual.
 */
final class ExitDialog {

    private ExitDialog() {
    }

    /** Mostra a janela. "onYes" roda quando a pessoa escolhe Sim. */
    static Dialog show(Activity activity, final Runnable onYes) {
        final Dialog d = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        d.setCancelable(true);

        FrameLayout root = new FrameLayout(activity);

        GlassSurface card = new GlassSurface(activity, GlassKit.dp(activity, 28), false,
                GlassSurface.STYLE_CLEAR, true);
        LinearLayout col = new LinearLayout(activity);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setClipChildren(false);
        col.setPadding(GlassKit.dp(activity, 30), GlassKit.dp(activity, 30),
                GlassKit.dp(activity, 30), GlassKit.dp(activity, 30));
        card.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(activity);
        title.setText("Deseja sair?");
        title.setTextSize(GlassKit.sp(30));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(GlassKit.color(activity, R.color.asc_text_primary));
        title.setSingleLine(true);
        col.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final GlassSurface yes = button(activity, "Sim");
        final GlassSurface no = button(activity, "Não");
        LinearLayout.LayoutParams yl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, GlassKit.dp(activity, 54));
        yl.topMargin = GlassKit.dp(activity, 24);
        col.addView(yes, yl);
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, GlassKit.dp(activity, 54));
        nl.topMargin = GlassKit.dp(activity, 12);
        col.addView(no, nl);

        // foco: Sim em cima, Não embaixo; os extremos não escapam
        yes.setNextFocusUpId(GlassKit.ensureId(yes));
        yes.setNextFocusDownId(GlassKit.ensureId(no));
        no.setNextFocusUpId(GlassKit.ensureId(yes));
        no.setNextFocusDownId(GlassKit.ensureId(no));
        yes.setNextFocusLeftId(GlassKit.ensureId(yes));
        yes.setNextFocusRightId(GlassKit.ensureId(yes));
        no.setNextFocusLeftId(GlassKit.ensureId(no));
        no.setNextFocusRightId(GlassKit.ensureId(no));

        yes.setOnClickListener(v -> {
            d.dismiss();
            if (onYes != null) {
                onYes.run();
            }
        });
        no.setOnClickListener(v -> d.dismiss());

        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(
                GlassKit.dp(activity, 330), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        root.addView(card, cl);
        d.setContentView(root);

        if (d.getWindow() != null) {
            d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            d.getWindow().setDimAmount(0.45f);
        }
        d.setOnShowListener(x -> yes.requestFocus());
        d.show();
        return d;
    }

    private static GlassSurface button(Context c, String text) {
        GlassSurface b = new GlassSurface(c, GlassKit.dp(c, 18), true,
                GlassSurface.STYLE_CLEAR, false);
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(GlassKit.sp(20));
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(GlassKit.color(c, R.color.asc_text_primary));
        t.setSingleLine(true);
        b.addView(t, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));
        GlassKit.focusScale(b, 1.05f, null);
        return b;
    }
}
