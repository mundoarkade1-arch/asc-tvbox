package com.ascplay.tvbox;

import android.app.Activity;
import android.app.Dialog;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Teclado de letras na tela, para digitar o nome da busca só com setas e OK.
 * Tem letras, números, Espaço, Apagar, Buscar e Sair.
 * O botão Voltar do controle e a tecla "Sair" fecham o teclado (onCancel).
 * "Buscar" entrega o texto digitado (onSearch), se tiver pelo menos 2 letras.
 */
public final class SearchKeyboard {

    public interface Listener {
        /** A pessoa apertou Buscar. O teclado já foi fechado. */
        void onSearch(String text);

        /** A pessoa saiu sem buscar. */
        void onCancel();
    }

    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_NEUTRAL = 0xFFA39A9D;
    private static final int COLOR_ERROR = 0xFFFF6B78;
    private static final int COLOR_BACK = 0xFF0C0A0B;
    private static final int MAX_LEN = 40;
    private static final int MIN_LEN = 2;

    private final Activity activity;
    private final Listener listener;
    private final StringBuilder buf = new StringBuilder();
    private Dialog dialog;
    private TextView tvText;
    private TextView tvMessage;
    private boolean searched = false;

    private SearchKeyboard(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    /** Abre o teclado. */
    public static SearchKeyboard show(Activity activity, String title, Listener listener) {
        SearchKeyboard k = new SearchKeyboard(activity, listener);
        k.build(title);
        if (!activity.isFinishing() && !activity.isDestroyed()) {
            k.dialog.show();
        }
        return k;
    }

    private void build(String title) {
        dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(d -> {
            if (!searched && listener != null) {
                listener.onCancel();
            }
        });

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(COLOR_BACK);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(16), dp(8), dp(16), dp(8));
        scroll.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextColor(COLOR_WHITE);
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(tvTitle.getTypeface(), android.graphics.Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        root.addView(tvTitle, wrap());

        tvText = new TextView(activity);
        tvText.setTextColor(COLOR_WHITE);
        tvText.setTextSize(26);
        tvText.setGravity(Gravity.CENTER);
        tvText.setSingleLine(true);
        root.addView(tvText, wrap());

        tvMessage = new TextView(activity);
        tvMessage.setTextSize(15);
        tvMessage.setGravity(Gravity.CENTER);
        tvMessage.setTextColor(COLOR_NEUTRAL);
        root.addView(tvMessage, wrap());

        String[][] rows = {
                {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"},
                {"K", "L", "M", "N", "O", "P", "Q", "R", "S", "T"},
                {"U", "V", "W", "X", "Y", "Z", "1", "2", "3", "4"},
                {"5", "6", "7", "8", "9", "0"},
                {"Espaço", "Apagar", "Buscar", "Sair"}
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
        refreshText();
        if (firstKey != null) {
            firstKey.requestFocus();
        }
    }

    private Button makeKey(final String label) {
        Button b = new Button(activity);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(COLOR_WHITE);
        boolean single = label.length() == 1;
        b.setTextSize(single ? 20 : 16);
        b.setBackgroundResource(R.drawable.bg_button);
        b.setMinWidth(0);
        b.setMinHeight(0);
        b.setMinimumWidth(0);
        b.setMinimumHeight(0);
        b.setFocusable(true);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(single ? dp(64) : dp(120), dp(44));
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> onKey(label));
        return b;
    }

    private void onKey(String label) {
        if ("Apagar".equals(label)) {
            if (buf.length() > 0) {
                buf.setLength(buf.length() - 1);
                refreshText();
            }
            return;
        }
        if ("Sair".equals(label)) {
            dialog.cancel();
            return;
        }
        if ("Buscar".equals(label)) {
            String text = buf.toString().trim();
            if (text.length() < MIN_LEN) {
                tvMessage.setTextColor(COLOR_ERROR);
                tvMessage.setText("Digite pelo menos " + MIN_LEN + " letras.");
                return;
            }
            searched = true;
            try {
                dialog.dismiss();
            } catch (IllegalArgumentException e) {
                // a tela já foi fechada: nada a fazer
            }
            if (listener != null) {
                listener.onSearch(text);
            }
            return;
        }
        if (buf.length() >= MAX_LEN) {
            return;
        }
        if ("Espaço".equals(label)) {
            if (buf.length() == 0 || buf.charAt(buf.length() - 1) == ' ') {
                return;
            }
            buf.append(' ');
        } else {
            buf.append(label);
        }
        tvMessage.setText("");
        refreshText();
    }

    private void refreshText() {
        tvText.setText(buf.length() == 0 ? "_" : buf.toString() + "_");
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
