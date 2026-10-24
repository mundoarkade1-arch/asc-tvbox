package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends Activity {

    private static final int COLOR_NEUTRAL = 0xFFFFFFFF;
    private static final int COLOR_ERROR = 0xFFFF6B78;
    private static final int COLOR_OK = 0xFF4ADE80;

    private Button btnXtream;
    private Button btnM3u;
    private Button btnEnter;
    private View groupXtream;
    private View groupM3u;
    private EditText etServer;
    private EditText etUser;
    private EditText etPass;
    private EditText etM3u;
    private TextView tvStatus;

    private String mode = CredentialStore.MODE_XTREAM;
    private boolean busy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnXtream = findViewById(R.id.btn_mode_xtream);
        btnM3u = findViewById(R.id.btn_mode_m3u);
        btnEnter = findViewById(R.id.btn_enter);
        groupXtream = findViewById(R.id.group_xtream);
        groupM3u = findViewById(R.id.group_m3u);
        etServer = findViewById(R.id.et_server);
        etUser = findViewById(R.id.et_user);
        etPass = findViewById(R.id.et_pass);
        etM3u = findViewById(R.id.et_m3u);
        tvStatus = findViewById(R.id.tv_status);

        btnXtream.setOnClickListener(v -> setMode(CredentialStore.MODE_XTREAM));
        btnM3u.setOnClickListener(v -> setMode(CredentialStore.MODE_M3U));
        btnEnter.setOnClickListener(v -> attemptLogin());

        TextView.OnEditorActionListener enterOnDone = (v, actionId, event) -> {
            boolean done = actionId == EditorInfo.IME_ACTION_DONE;
            boolean enterKey = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (done || enterKey) {
                attemptLogin();
                return true;
            }
            return false;
        };
        etPass.setOnEditorActionListener(enterOnDone);
        etM3u.setOnEditorActionListener(enterOnDone);

        // Preenche com o último login salvo (se houver).
        CredentialStore.Credentials saved = CredentialStore.load(this);
        if (saved != null) {
            etServer.setText(saved.server);
            etUser.setText(saved.user);
            etPass.setText(saved.pass);
            etM3u.setText(saved.m3uUrl);
            mode = CredentialStore.MODE_M3U.equals(saved.mode)
                    ? CredentialStore.MODE_M3U
                    : CredentialStore.MODE_XTREAM;
        }
        setMode(mode);
    }

    private void setMode(String newMode) {
        mode = newMode;
        boolean xtream = CredentialStore.MODE_XTREAM.equals(mode);
        groupXtream.setVisibility(xtream ? View.VISIBLE : View.GONE);
        groupM3u.setVisibility(xtream ? View.GONE : View.VISIBLE);
        btnXtream.setText((xtream ? "\u25CF " : "") + getString(R.string.mode_xtream));
        btnM3u.setText((xtream ? "" : "\u25CF ") + getString(R.string.mode_m3u));
        tvStatus.setText("");
    }

    private void attemptLogin() {
        if (busy) {
            return;
        }
        final String currentMode = mode;
        final String server = etServer.getText().toString();
        final String user = etUser.getText().toString();
        final String pass = etPass.getText().toString();
        final String m3u = etM3u.getText().toString();

        busy = true;
        btnEnter.setEnabled(false);
        showStatus(getString(R.string.msg_checking), COLOR_NEUTRAL);
        hideKeyboard();

        new Thread(() -> {
            LoginChecker.Result r;
            if (CredentialStore.MODE_XTREAM.equals(currentMode)) {
                r = LoginChecker.checkXtream(server, user, pass);
            } else {
                r = LoginChecker.checkM3u(m3u);
            }
            final LoginChecker.Result result = r;
            runOnUiThread(() -> onResult(result, currentMode, server, user, pass, m3u));
        }).start();
    }

    private void onResult(LoginChecker.Result result, String usedMode,
                          String server, String user, String pass, String m3u) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        busy = false;
        btnEnter.setEnabled(true);

        if (result == LoginChecker.Result.OK) {
            CredentialStore.Credentials c;
            if (CredentialStore.MODE_XTREAM.equals(usedMode)) {
                String origin = LoginChecker.normalizeServer(server);
                c = new CredentialStore.Credentials(usedMode,
                        origin == null ? server.trim() : origin,
                        user.trim(), pass.trim(), "");
            } else {
                c = new CredentialStore.Credentials(usedMode, "", "", "", m3u.trim());
            }
            CredentialStore.save(this, c);
            showStatus(getString(R.string.msg_ok), COLOR_OK);
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        showStatus(getString(messageFor(result)), COLOR_ERROR);
        btnEnter.requestFocus();
    }

    private int messageFor(LoginChecker.Result result) {
        switch (result) {
            case EMPTY:
                return R.string.err_empty;
            case BAD_SERVER:
                return R.string.err_server;
            case BAD_URL:
                return R.string.err_url;
            case NETWORK:
                return R.string.err_network;
            case AUTH:
                return R.string.err_auth;
            case EXPIRED:
                return R.string.err_expired;
            case NOT_M3U:
                return R.string.err_not_m3u;
            default:
                return R.string.err_unknown;
        }
    }

    private void showStatus(String text, int color) {
        tvStatus.setTextColor(color);
        tvStatus.setText(text);
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }
}
