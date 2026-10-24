package com.ascplay.tvbox;

import android.app.Activity;
import android.app.Dialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Teclado de números na tela, para digitar a senha só com setas e OK.
 * Quando o último número é digitado, avisa o app na hora (sem botão de confirmar).
 * O botão Voltar do controle e a tecla "Sair" fecham o teclado (onCancel).
 * Não guarda nem registra a senha: só repassa para quem pediu.
 */
public final class PinDialog {

    public interface Listener {
        /** Todos os números foram digitados. Chame d.dismiss() se estiver certo, ou d.setMessage(...) se errado. */
        void onPin(PinDialog d, String pin);

        /** A pessoa saiu sem digitar a senha. */
        void onCancel();
    }

    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_NEUTRAL = 0xFFA39A9D;
    private static final int COLOR_ERROR = 0xFFFF6B78;
    private static final int COLOR_BACK = 0xFF0C0A0B;

    private final Activity activity;
    private final Listener listener;
    private final StringBuilder buf = new StringBuilder();
    private Dialog dialog;
    private TextView tvDots;
    private TextView tvMessage;

    private PinDialog(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    /** Abre o teclado. "message" pode ser vazio. */
    public static PinDialog show(Activity activity, String title, String message, Listener listener) {
        PinDialog d = new PinDialog(activity, listener);
        d.build(title, message);
        if (!activity.isFinishing() && !activity.isDestroyed()) {
            d.dialog.show();
        }
        return d;
    }

    /** Mostra um aviso embaixo dos pontinhos (por exemplo, "Senha incorreta."). */
    public void setMessage(String text, boolean error) {
        tvMessage.setTextColor(error ? COLOR_ERROR : COLOR_NEUTRAL);
        tvMessage.setText(text == null ? "" : text);
    }

    /** Apaga o que foi digitado. */
    public void reset() {
        buf.setLength(0);
        refreshDots();
    }

    public void dismiss() {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (IllegalArgumentException e) {
            // a tela já foi fechada: nada a fazer
        }
    }

    // ---------- Montagem da tela ----------

    private void build(String title, String message) {
        dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(d -> {
            if (listener != null) {
                listener.onCancel();
            }
        });

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(COLOR_BACK);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        scroll.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextColor(COLOR_WHITE);
        tvTitle.setTextSize(22);
        tvTitle.setTypeface(tvTitle.getTypeface(), android.graphics.Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        root.addView(tvTitle, wrap());

        tvDots = new TextView(activity);
        tvDots.setTextColor(COLOR_WHITE);
        tvDots.setTextSize(30);
        tvDots.setGravity(Gravity.CENTER);
        root.addView(tvDots, wrap());

        tvMessage = new TextView(activity);
        tvMessage.setTextSize(16);
        tvMessage.setGravity(Gravity.CENTER);
        root.addView(tvMessage, wrap());
        setMessage(message, false);

        String[][] rows = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"Apagar", "0", "Sair"}
        };
        Button firstKey = null;
        for (String[] row : rows) {
            LinearLayout line = new LinearLayout(activity);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER);
            for (String label : row) {
                Button key = makeKey(label);
                if (firstKey == null) {
                    firstKey = key;
                }
                line.addView(key);
            }
            root.addView(line, wrap());
        }

        dialog.setContentView(scroll);
        refreshDots();
        if (firstKey != null) {
            firstKey.requestFocus();
        }
    }

    private Button makeKey(final String label) {
        Button b = new Button(activity);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(COLOR_WHITE);
        b.setTextSize(label.length() == 1 ? 22 : 17);
        b.setBackgroundResource(R.drawable.bg_button);
        b.setMinWidth(0);
        b.setMinHeight(0);
        b.setMinimumWidth(0);
        b.setMinimumHeight(0);
        b.setFocusable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(104), dp(46));
        lp.setMargins(dp(4), dp(3), dp(4), dp(3));
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> onKey(label));
        return b;
    }

    private void onKey(String label) {
        if ("Apagar".equals(label)) {
            if (buf.length() > 0) {
                buf.setLength(buf.length() - 1);
                refreshDots();
            }
            return;
        }
        if ("Sair".equals(label)) {
            dialog.cancel();
            return;
        }
        if (buf.length() >= ParentalStore.PIN_LENGTH) {
            return;
        }
        buf.append(label);
        refreshDots();
        if (buf.length() == ParentalStore.PIN_LENGTH) {
            String pin = buf.toString();
            buf.setLength(0);
            if (listener != null) {
                listener.onPin(this, pin);
            }
            refreshDots();
        }
    }

    private void refreshDots() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ParentalStore.PIN_LENGTH; i++) {
            if (i > 0) {
                sb.append("  ");
            }
            sb.append(i < buf.length() ? "•" : "–");
        }
        tvDots.setText(sb.toString());
    }

    private LinearLayout.LayoutParams wrap() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.setMargins(0, dp(2), 0, dp(2));
        return lp;
    }

    private int dp(int v) {
        return (int) (v * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}
