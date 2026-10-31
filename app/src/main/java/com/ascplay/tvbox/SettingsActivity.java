package com.ascplay.tvbox;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * Aba Ajustes (menu à esquerda, lista de opções no meio, informações à direita).
 *
 * Opções:
 * - Controle parental: ativar, trocar a senha ou desativar (igual ao botão que ficava na
 *   página inicial).
 * - Trocar de lista: apaga a lista do "Continue assistindo" (M6) e volta para a tela de login.
 * - Sobre o dispositivo: modelo, Android, memória, espaço livre e tela.
 * - Sobre o aplicativo: versão instalada.
 * - Verificar atualização: procura uma versão nova. Se achar, a mesma linha passa a
 *   "Instalar atualização" e OK instala.
 *
 * A lista controla o foco (as linhas ficam com setFocusable(false)), como nas outras telas.
 */
public class SettingsActivity extends Activity {

    private static final int ROW_PARENTAL = 0;
    private static final int ROW_LOGIN = 1;
    private static final int ROW_DEVICE = 2;
    private static final int ROW_APP = 3;
    private static final int ROW_UPDATE = 4;
    private static final int ROW_COUNT = 5;

    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_ERROR = 0xFFFF6B78;
    private static final int COLOR_OK = 0xFF4ADE80;

    private ListView lvSettings;
    private TextView tvDetail;
    private RowAdapter adapter;

    private PinDialog pinDialog;

    // Atualização
    private Updater.Info pendingUpdate;
    private boolean checking = false;
    private boolean downloading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (CredentialStore.load(this) == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_settings);
        lvSettings = findViewById(R.id.lv_settings);
        tvDetail = findViewById(R.id.tv_detail);

        lvSettings.setDividerHeight(dp(6));

        // Menu à esquerda (só ícones). Aqui o ícone Atualizar não tem o que recarregar.
        NavBar.bind(this, NavBar.SETTINGS, null);

        adapter = new RowAdapter();
        lvSettings.setAdapter(adapter);
        lvSettings.setOnItemClickListener((parent, view, position, id) -> onRowClick(position));
        lvSettings.requestFocus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pinDialog != null) {
            pinDialog.dismiss();
            pinDialog = null;
        }
    }

    // ---------- Linhas ----------

    private String rowLabel(int position) {
        switch (position) {
            case ROW_PARENTAL:
                return "Controle parental";
            case ROW_LOGIN:
                return "Trocar de lista";
            case ROW_DEVICE:
                return "Sobre o dispositivo";
            case ROW_APP:
                return "Sobre o aplicativo";
            default:
                return pendingUpdate != null ? "Instalar atualização" : "Verificar atualização";
        }
    }

    private void onRowClick(int position) {
        switch (position) {
            case ROW_PARENTAL:
                openParentalMenu();
                break;
            case ROW_LOGIN:
                changeLogin();
                break;
            case ROW_DEVICE:
                showDetail(deviceText(), COLOR_WHITE);
                break;
            case ROW_APP:
                showDetail(appText(), COLOR_WHITE);
                break;
            default:
                checkOrInstallUpdate();
                break;
        }
    }

    private void showDetail(String text, int color) {
        tvDetail.setTextColor(color);
        tvDetail.setText(text);
    }

    // ---------- Trocar de lista ----------

    /**
     * Apaga a lista do "Continue assistindo" (era da lista antiga), abre a tela de login e
     * fecha as telas de antes (a página inicial, as abas e esta).
     */
    private void changeLogin() {
        ContinueStore.clear(this);
        Intent i = new Intent(this, LoginActivity.class);
        i.putExtra(HomeActivity.EXTRA_SHOW_LOGIN, true);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    // ---------- Sobre o dispositivo e o aplicativo ----------

    private String deviceText() {
        StringBuilder sb = new StringBuilder();
        String maker = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER;
        String model = Build.MODEL == null ? "" : Build.MODEL;
        sb.append("Aparelho: ").append((maker + " " + model).trim()).append("\n\n");
        sb.append("Android: ").append(Build.VERSION.RELEASE)
                .append(" (nível ").append(Build.VERSION.SDK_INT).append(")\n\n");

        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            sb.append("Memória (RAM): ").append(megaOrGiga(mi.totalMem)).append(" no total, ")
                    .append(megaOrGiga(mi.availMem)).append(" livres agora\n\n");
        } catch (Exception e) {
            sb.append("Memória (RAM): não foi possível ler\n\n");
        }

        try {
            StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
            long total = fs.getTotalBytes();
            long free = fs.getAvailableBytes();
            sb.append("Armazenamento: ").append(megaOrGiga(free)).append(" livres de ")
                    .append(megaOrGiga(total)).append("\n\n");
        } catch (Exception e) {
            sb.append("Armazenamento: não foi possível ler\n\n");
        }

        DisplayMetrics dm = getResources().getDisplayMetrics();
        sb.append("Tela: ").append(dm.widthPixels).append(" x ").append(dm.heightPixels)
                .append(" pixels");
        return sb.toString();
    }

    private String appText() {
        String name = "desconhecida";
        int code = 0;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (pi.versionName != null) {
                name = pi.versionName;
            }
            code = pi.versionCode;
        } catch (Exception e) {
            // segue com "desconhecida"
        }
        return "ASC lite\n\nVersão instalada: " + name + " (número " + code + ")";
    }

    private static String megaOrGiga(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);
        if (mb >= 1024) {
            return String.format(Locale.US, "%.1f GB", mb / 1024.0);
        }
        return String.format(Locale.US, "%d MB", Math.round(mb));
    }

    // ---------- Atualização ----------

    private void checkOrInstallUpdate() {
        if (checking || downloading) {
            return;
        }
        if (pendingUpdate != null) {
            startUpdate();
            return;
        }
        checking = true;
        showDetail("Procurando atualização...", COLOR_WHITE);
        Updater.checkAsync(this, new Updater.CheckListener() {
            @Override
            public void onUpdateAvailable(Updater.Info info) {
                checking = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                pendingUpdate = info;
                adapter.notifyDataSetChanged();
                showDetail(getString(R.string.update_available, info.name)
                        + "\n\nAperte OK em \"Instalar atualização\" para instalar.", COLOR_OK);
            }

            @Override
            public void onNoUpdate() {
                checking = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showDetail("Nenhuma atualização encontrada agora.", COLOR_WHITE);
            }
        });
    }

    private void startUpdate() {
        if (downloading || pendingUpdate == null) {
            return;
        }
        downloading = true;
        showDetail(getString(R.string.msg_downloading_unknown), COLOR_WHITE);

        Updater.downloadAndInstallAsync(this, pendingUpdate, new Updater.InstallListener() {
            @Override
            public void onProgress(int percent) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (percent >= 0) {
                    showDetail(getString(R.string.msg_downloading, percent), COLOR_WHITE);
                } else {
                    showDetail(getString(R.string.msg_downloading_unknown), COLOR_WHITE);
                }
            }

            @Override
            public void onInstallerOpened() {
                downloading = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showDetail(getString(R.string.msg_installer_opened), COLOR_WHITE);
            }

            @Override
            public void onFailed(String message) {
                downloading = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showDetail(message, COLOR_ERROR);
            }
        });
    }

    // ---------- Controle parental ----------

    private void openParentalMenu() {
        final boolean on = ParentalStore.isEnabled(this);
        AlertDialog.Builder b = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        if (on) {
            b.setTitle("Controle parental: ativo");
            b.setItems(new CharSequence[]{"Trocar senha", "Desativar controle parental", "Voltar"},
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                askCurrentPin("Digite a senha atual", new Runnable() {
                                    @Override
                                    public void run() {
                                        createPin();
                                    }
                                });
                            } else if (which == 1) {
                                askCurrentPin("Digite a senha para desativar", new Runnable() {
                                    @Override
                                    public void run() {
                                        ParentalStore.disable(SettingsActivity.this);
                                        Toast.makeText(SettingsActivity.this,
                                                "Controle parental desativado.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });
        } else {
            b.setTitle("Controle parental: desativado");
            b.setItems(new CharSequence[]{"Ativar controle parental", "Voltar"},
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                createPin();
                            }
                        }
                    });
        }
        b.show();
    }

    /** Pede a senha nova (4 números). Depois pede para repetir. */
    private void createPin() {
        pinDialog = PinDialog.show(this, "Crie a senha (4 números)", "", new PinDialog.Listener() {
            @Override
            public void onPin(PinDialog d, String pin) {
                d.dismiss();
                confirmPin(pin);
            }

            @Override
            public void onCancel() {
                // saiu sem criar: nada muda
            }
        });
    }

    private void confirmPin(final String first) {
        pinDialog = PinDialog.show(this, "Repita a senha", "", new PinDialog.Listener() {
            @Override
            public void onPin(PinDialog d, String pin) {
                d.dismiss();
                if (!first.equals(pin)) {
                    Toast.makeText(SettingsActivity.this,
                            "As senhas não são iguais. Tente de novo.",
                            Toast.LENGTH_SHORT).show();
                    createPin();
                    return;
                }
                if (ParentalStore.enable(SettingsActivity.this, pin)) {
                    Toast.makeText(SettingsActivity.this,
                            "Controle parental ativado.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this,
                            "Não foi possível guardar a senha.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancel() {
                // saiu sem confirmar: nada muda
            }
        });
    }

    /** Pede a senha atual. Se estiver certa, fecha o teclado e roda "onOk". */
    private void askCurrentPin(String title, final Runnable onOk) {
        pinDialog = PinDialog.show(this, title, "", new PinDialog.Listener() {
            @Override
            public void onPin(PinDialog d, String pin) {
                long wait = ParentalStore.lockedSeconds(SettingsActivity.this);
                if (wait > 0) {
                    d.setMessage("Muitas tentativas. Aguarde " + wait + " segundos.", true);
                    return;
                }
                if (ParentalStore.check(SettingsActivity.this, pin)) {
                    d.dismiss();
                    onOk.run();
                    return;
                }
                wait = ParentalStore.lockedSeconds(SettingsActivity.this);
                if (wait > 0) {
                    d.setMessage("Muitas tentativas. Aguarde " + wait + " segundos.", true);
                } else {
                    d.setMessage("Senha incorreta.", true);
                }
            }

            @Override
            public void onCancel() {
                // saiu sem digitar: nada muda
            }
        });
    }

    // ---------- Ajudas ----------

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ---------- Adaptador (reaproveita as linhas ao rolar) ----------

    private final class RowAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return ROW_COUNT;
        }

        @Override
        public Object getItem(int position) {
            return rowLabel(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv;
            if (convertView == null) {
                tv = (TextView) LayoutInflater.from(SettingsActivity.this)
                        .inflate(R.layout.item_category, parent, false);
                // Quem controla a seleção e o clique é a própria lista.
                tv.setFocusable(false);
                tv.setClickable(false);
            } else {
                tv = (TextView) convertView;
            }
            tv.setText(rowLabel(position));
            return tv;
        }
    }
}
