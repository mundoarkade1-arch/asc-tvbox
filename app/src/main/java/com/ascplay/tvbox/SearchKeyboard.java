package com.ascplay.tvbox;

import android.app.Activity;
import android.app.Dialog;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Teclado de letras na tela, para digitar o nome da busca só com setas e OK.
 * Formato QWERTY brasileiro (ABNT): fileira de números, Q a P, A a Ç, Z a M.
 * Última fileira: Apagar, Espaço (barra grande no meio), Buscar e Sair.
 * Segurando o OK numa vogal (ou no N), abre uma janelinha com os acentos dela.
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
    private static final int COLOR_NEUTRAL = 0xFFA99FA3;
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
                {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
                {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"},
                {"A", "S", "D", "F", "G", "H", "J", "K", "L", "Ç"},
                {"Z", "X", "C", "V", "B", "N", "M"},
                {"Apagar", "Espaço", "Buscar", "Sair"}
        };
        Button firstKey = null;
        for (String[] row : rows) {
            LinearLayout line = new LinearLayout(activity);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER);
            for (final String label : row) {
                int width = 64;
                if (label.length() > 1) {
                    width = "Espaço".equals(label) ? 300 : 120;
                }
                Button key = makeKey(label, width);
                key.setOnClickListener(v -> onKey(label));
                final String options = accentsFor(label);
                if (!options.isEmpty()) {
                    key.setOnLongClickListener(v -> {
                        showAccents(options);
                        return true;
                    });
                }
                if ("Q".equals(label)) {
                    firstKey = key;
                }
                line.addView(key);
            }
            root.addView(line, wrap());
        }

        TextView hint = new TextView(activity);
        hint.setText("Segure o OK numa vogal ou no N para ver os acentos.");
        hint.setTextSize(14);
        hint.setGravity(Gravity.CENTER);
        hint.setTextColor(COLOR_NEUTRAL);
        root.addView(hint, wrap());

        dialog.setContentView(scroll);
        refreshText();
        if (firstKey != null) {
            firstKey.requestFocus();
        }
    }

    /** Letras com acento de cada tecla (a primeira é a letra sem acento). Vazio = sem janelinha. */
    private static String accentsFor(String label) {
        switch (label) {
            case "A":
                return "AÁÀÃÂÄ";
            case "E":
                return "EÉÈÊË";
            case "I":
                return "IÍÌÎÏ";
            case "O":
                return "OÓÒÕÔÖ";
            case "U":
                return "UÚÙÛÜ";
            case "N":
                return "NÑ";
            default:
                return "";
        }
    }

    /** Janelinha com os acentos de uma letra. OK escolhe; Voltar fecha só ela. */
    private void showAccents(String options) {
        final Dialog pop = new Dialog(activity);
        pop.requestWindowFeature(Window.FEATURE_NO_TITLE);
        pop.setCancelable(true);

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(COLOR_BACK);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));

        Button first = null;
        for (int i = 0; i < options.length(); i++) {
            final String letter = String.valueOf(options.charAt(i));
            Button b = makeKey(letter, 56);
            b.setOnClickListener(v -> {
                pop.dismiss();
                addText(letter);
            });
            if (first == null) {
                first = b;
            }
            box.addView(b);
        }
        pop.setContentView(box);
        if (first != null) {
            first.requestFocus();
        }
        if (!activity.isFinishing() && !activity.isDestroyed()) {
            pop.show();
        }
    }

    private Button makeKey(final String label, int widthDp) {
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
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(widthDp), dp(44));
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        b.setLayoutParams(lp);
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
        addText(label);
    }

    /** Acrescenta uma letra, número ou espaço ao texto. */
    private void addText(String label) {
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
