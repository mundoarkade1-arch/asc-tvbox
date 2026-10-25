package com.ascplay.tvbox;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Janela da programação de um canal (tecla das três listras).
 * Mostra o que passa agora (em verde) e os próximos programas, com os horários.
 *
 * A programação só é pedida ao servidor quando a janela abre (nunca antes), e só no login
 * por servidor (a lista M3U não tem guia). Fecha com OK no botão "Fechar", com Voltar ou
 * apertando a tecla das três listras de novo.
 *
 * Não registra senhas nem endereços em log.
 */
public final class EpgDialog {

    private static final int COLOR_PANEL = 0xFF1A1517;
    private static final int COLOR_BORDER = 0xFF2A2326;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_NEUTRAL = 0xFFA39A9D;
    private static final int COLOR_ERROR = 0xFFFF6B78;
    private static final int COLOR_NOW = 0xFF4ADE80;

    private EpgDialog() {
    }

    /** Abre a janela e pede a programação do canal. Chamar na thread principal. */
    public static void show(final Activity activity, final String server, final String user,
                            final String pass, final String streamId, String channelName) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 26);
        root.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_PANEL);
        bg.setCornerRadius(dp(activity, 12));
        bg.setStroke(dp(activity, 2), COLOR_BORDER);
        root.setBackground(bg);

        TextView label = new TextView(activity);
        label.setText("Programação");
        label.setTextColor(COLOR_NEUTRAL);
        label.setTextSize(18);
        root.addView(label);

        TextView title = new TextView(activity);
        title.setText(channelName == null ? "" : channelName);
        title.setTextColor(COLOR_WHITE);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        root.addView(title);

        final TextView status = new TextView(activity);
        status.setText("Carregando...");
        status.setTextColor(COLOR_NEUTRAL);
        status.setTextSize(20);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusLp.topMargin = dp(activity, 14);
        root.addView(status, statusLp);

        final LinearLayout rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowsLp.topMargin = dp(activity, 8);
        root.addView(rows, rowsLp);

        Button close = new Button(activity);
        close.setText("Fechar");
        close.setAllCaps(false);
        close.setTextColor(COLOR_WHITE);
        close.setTextSize(18);
        close.setBackgroundResource(R.drawable.bg_button);
        close.setMinWidth(dp(activity, 160));
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        closeLp.topMargin = dp(activity, 18);
        closeLp.gravity = Gravity.END;
        root.addView(close, closeLp);

        dialog.setContentView(root);

        // A tecla das três listras, apertada de novo, fecha a janela
        dialog.setOnKeyListener(new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(android.content.DialogInterface d, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_MENU && event.getAction() == KeyEvent.ACTION_UP) {
                    dialog.dismiss();
                    return true;
                }
                return false;
            }
        });

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            DisplayMetrics dm = activity.getResources().getDisplayMetrics();
            window.setLayout((int) (dm.widthPixels * 0.7f), WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0x00000000));
        }
        close.requestFocus();

        // Pede a programação ao servidor, fora da thread principal
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<XtreamApi.Program> result = null;
                try {
                    result = XtreamApi.schedule(server, user, pass, streamId);
                } catch (Exception e) {
                    // sem rede ou servidor fora do ar: mostra aviso abaixo
                }
                final List<XtreamApi.Program> res = result;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (activity.isFinishing() || activity.isDestroyed()
                                || !dialog.isShowing()) {
                            return;
                        }
                        fill(activity, status, rows, res);
                    }
                });
            }
        }).start();
    }

    private static void fill(Activity activity, TextView status, LinearLayout rows,
                             List<XtreamApi.Program> programs) {
        if (programs == null) {
            status.setTextColor(COLOR_ERROR);
            status.setText("Não foi possível carregar a programação.");
            return;
        }
        if (programs.isEmpty()) {
            status.setTextColor(COLOR_NEUTRAL);
            status.setText("Programação indisponível para este canal.");
            return;
        }
        status.setVisibility(View.GONE);
        long now = System.currentTimeMillis();
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        for (XtreamApi.Program p : programs) {
            boolean current = p.start <= now && now < p.end;
            addRow(activity, rows,
                    fmt.format(new Date(p.start)) + " - " + fmt.format(new Date(p.end)),
                    p.title, current);
        }
    }

    private static void addRow(Activity activity, LinearLayout rows, String time, String name,
                               boolean current) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(activity, 6), 0, dp(activity, 6));

        TextView t = new TextView(activity);
        t.setText(time);
        t.setTextColor(current ? COLOR_NOW : COLOR_NEUTRAL);
        t.setTextSize(19);
        row.addView(t, new LinearLayout.LayoutParams(dp(activity, 150),
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView n = new TextView(activity);
        n.setText(name);
        n.setTextColor(current ? COLOR_NOW : COLOR_WHITE);
        n.setTextSize(21);
        n.setMaxLines(2);
        n.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (current) {
            n.setTypeface(Typeface.DEFAULT_BOLD);
        }
        row.addView(n, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        rows.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
