package com.ascplay.tvbox;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Janela "Nova versão disponível" no mesmo estilo da "Deseja sair?" (vidro transparente, duas
 * opções empilhadas): "Atualizar agora" em cima e "Depois" embaixo. Depois de "Atualizar agora"
 * a janela mostra o andamento do download no mesmo lugar (e não deixa fechar até acabar).
 * Voltar = Depois. Só visual: quem baixa e instala é a HomeActivity (ou quem chamou).
 */
final class UpdateDialog {

    private final Dialog dialog;
    private final GlassSurface now;
    private final GlassSurface later;
    private final TextView status;
    private final TextView nowLabel;
    private boolean busy = false;

    private UpdateDialog(Dialog dialog, GlassSurface now, TextView nowLabel, GlassSurface later,
                         TextView status) {
        this.dialog = dialog;
        this.now = now;
        this.nowLabel = nowLabel;
        this.later = later;
        this.status = status;
    }

    /** Mostra a janela. "onNow" = Atualizar agora, "onLater" = Depois (ou Voltar). */
    static UpdateDialog show(Activity activity, String versionName, final Runnable onNow,
                             final Runnable onLater) {
        final Dialog d = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        d.setCancelable(false); // Voltar é tratado abaixo (não fecha durante o download)

        FrameLayout root = new FrameLayout(activity);
        root.setClipChildren(false);
        root.setClipToPadding(false);

        GlassSurface card = new GlassSurface(activity, GlassKit.dp(activity, 28), false,
                GlassSurface.STYLE_CLEAR, true);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        LinearLayout col = new LinearLayout(activity);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setClipChildren(false);
        col.setClipToPadding(false);
        col.setPadding(GlassKit.dp(activity, 30), GlassKit.dp(activity, 30),
                GlassKit.dp(activity, 30), GlassKit.dp(activity, 30));
        card.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(activity);
        title.setText("Nova versão disponível");
        title.setTextSize(GlassKit.sp(26));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(GlassKit.color(activity, R.color.asc_text_primary));
        col.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView sub = new TextView(activity);
        sub.setText(versionName == null || versionName.isEmpty()
                ? "Deseja atualizar agora?" : "Versão " + versionName + ". Deseja atualizar agora?");
        sub.setTextSize(GlassKit.sp(15));
        sub.setGravity(Gravity.CENTER);
        sub.setTextColor(GlassKit.color(activity, R.color.asc_text_secondary));
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sl.topMargin = GlassKit.dp(activity, 8);
        col.addView(sub, sl);

        TextView status = new TextView(activity);
        status.setTextSize(GlassKit.sp(15));
        status.setTypeface(status.getTypeface(), Typeface.BOLD);
        status.setGravity(Gravity.CENTER);
        status.setTextColor(GlassKit.color(activity, R.color.asc_gold));
        status.setVisibility(View.GONE);
        LinearLayout.LayoutParams stl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stl.topMargin = GlassKit.dp(activity, 10);
        col.addView(status, stl);

        final TextView nowLabel = new TextView(activity);
        final GlassSurface now = button(activity, "Atualizar agora", nowLabel);
        final GlassSurface later = button(activity, "Depois", new TextView(activity));
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, GlassKit.dp(activity, 54));
        nl.topMargin = GlassKit.dp(activity, 22);
        nl.leftMargin = GlassKit.dp(activity, 6);
        nl.rightMargin = GlassKit.dp(activity, 6);
        col.addView(now, nl);
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, GlassKit.dp(activity, 54));
        ll.topMargin = GlassKit.dp(activity, 12);
        ll.leftMargin = GlassKit.dp(activity, 6);
        ll.rightMargin = GlassKit.dp(activity, 6);
        col.addView(later, ll);

        // foco: Atualizar agora em cima, Depois embaixo; os extremos não escapam
        now.setNextFocusUpId(GlassKit.ensureId(now));
        now.setNextFocusDownId(GlassKit.ensureId(later));
        later.setNextFocusUpId(GlassKit.ensureId(now));
        later.setNextFocusDownId(GlassKit.ensureId(later));
        now.setNextFocusLeftId(GlassKit.ensureId(now));
        now.setNextFocusRightId(GlassKit.ensureId(now));
        later.setNextFocusLeftId(GlassKit.ensureId(later));
        later.setNextFocusRightId(GlassKit.ensureId(later));

        final UpdateDialog handle = new UpdateDialog(d, now, nowLabel, later, status);

        now.setOnClickListener(v -> {
            if (!handle.busy && onNow != null) {
                onNow.run();
            }
        });
        later.setOnClickListener(v -> {
            if (handle.busy) {
                return;
            }
            d.dismiss();
            if (onLater != null) {
                onLater.run();
            }
        });
        d.setOnKeyListener((dlg, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK
                    && event.getAction() == android.view.KeyEvent.ACTION_UP) {
                if (!handle.busy) {
                    d.dismiss();
                    if (onLater != null) {
                        onLater.run();
                    }
                }
                return true;
            }
            return false;
        });

        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(
                GlassKit.dp(activity, 360), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        root.addView(card, cl);
        d.setContentView(root);

        if (d.getWindow() != null) {
            d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            d.getWindow().setDimAmount(0.45f);
        }
        d.setOnShowListener(x -> now.requestFocus());
        d.show();
        return handle;
    }

    boolean isShowing() {
        return dialog.isShowing();
    }

    void dismiss() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /** Escreve o andamento ("Baixando... 40%") e, se "busy", trava os botões. */
    void setStatus(String text, boolean busy) {
        this.busy = busy;
        status.setText(text);
        status.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
        now.setAlpha(busy ? 0.5f : 1f);
        later.setAlpha(busy ? 0.5f : 1f);
        nowLabel.setText(busy ? "Aguarde..." : "Atualizar agora");
    }

    /** Volta o foco para "Atualizar agora" (depois de um erro, por exemplo). */
    void focusNow() {
        now.requestFocus();
    }

    private static GlassSurface button(Context c, String text, TextView t) {
        GlassSurface b = new GlassSurface(c, GlassKit.dp(c, 18), true,
                GlassSurface.STYLE_CLEAR, false);
        t.setText(text);
        t.setTextSize(GlassKit.sp(20));
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(GlassKit.color(c, R.color.asc_text_primary));
        t.setSingleLine(true);
        b.addView(t, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));
        GlassKit.focusScale(b, 1.03f, null);
        return b;
    }
}
