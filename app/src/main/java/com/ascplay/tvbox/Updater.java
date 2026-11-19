package com.ascplay.tvbox;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.List;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Atualização automática do app (APK publicado em Releases do GitHub).
 *
 * Como funciona:
 *  1) checkAsync(): baixa o version.json e compara com a versão instalada.
 *  2) downloadAndInstallAsync(): baixa o APK, confere o SHA-256 e abre o instalador.
 *
 * Segurança:
 *  - só fala com endereços https;
 *  - só baixa APK do próprio repositório (DOWNLOAD_PREFIX);
 *  - recusa o APK se o SHA-256 não bater;
 *  - o Android só instala se a assinatura for a mesma do app instalado;
 *  - não registra endereços nem dados em log.
 */
public final class Updater {

    /** Dados da versão mais nova publicada. */
    public static final class Info {
        public final int code;
        public final String name;
        public final String url;
        public final String sha256;

        Info(int code, String name, String url, String sha256) {
            this.code = code;
            this.name = name;
            this.url = url;
            this.sha256 = sha256;
        }
    }

    /** Resultado da verificação. Sempre chamado na thread principal. */
    public interface CheckListener {
        void onUpdateAvailable(Info info);

        void onNoUpdate();
    }

    /** Andamento do download/instalação. Sempre chamado na thread principal. */
    public interface InstallListener {
        /** De 0 a 100, ou -1 se o tamanho não for conhecido. */
        void onProgress(int percent);

        /** O instalador do Android foi aberto. */
        void onInstallerOpened();

        void onFailed(String message);
    }

    private static final String VERSION_URL =
            "https://github.com/mundoarkade1-arch/asc-tvbox/releases/latest/download/version.json";
    private static final String DOWNLOAD_PREFIX =
            "https://github.com/mundoarkade1-arch/asc-tvbox/releases/download/";

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_JSON_BYTES = 16 * 1024;
    private static final long MAX_APK_BYTES = 150L * 1024L * 1024L;
    private static final String USER_AGENT = "ASC-TVBox/1.0 (Android)";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean BUSY = new AtomicBoolean(false);

    private Updater() {
    }

    // ---------- 1) Verificar ----------

    /** Verifica em segundo plano se existe versão mais nova. Falhas ficam em silêncio. */
    public static void checkAsync(final Context context, final CheckListener listener) {
        final Context app = context.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Info found = null;
                try {
                    int installed = installedVersionCode(app);
                    Info latest = fetchLatest();
                    if (installed > 0 && latest != null && latest.code > installed) {
                        found = latest;
                    }
                } catch (Exception ignored) {
                    // sem internet ou resposta estranha: segue sem avisar
                }
                final Info result = found;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        if (result != null) {
                            listener.onUpdateAvailable(result);
                        } else {
                            listener.onNoUpdate();
                        }
                    }
                });
            }
        }).start();
    }

    /** Lê a versão instalada pelo PackageManager (o BuildConfig não existe neste projeto). */
    @SuppressWarnings("deprecation")
    private static int installedVersionCode(Context app) {
        try {
            PackageInfo pi = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            return pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private static Info fetchLatest() throws IOException {
        HttpURLConnection conn = open(VERSION_URL);
        try {
            if (conn.getResponseCode() != 200) {
                return null;
            }
            InputStream in = conn.getInputStream();
            String body;
            try {
                body = new String(readUpTo(in, MAX_JSON_BYTES), "UTF-8");
            } finally {
                in.close();
            }
            JSONObject json = new JSONObject(body);
            int code = json.optInt("code", 0);
            String name = json.optString("name", "");
            String url = json.optString("url", "");
            String sha = json.optString("sha256", "").trim().toLowerCase();
            if (code <= 0 || !isSafeDownloadUrl(url) || !sha.matches("[0-9a-f]{64}")) {
                return null;
            }
            return new Info(code, name, url, sha);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            return null;
        } finally {
            conn.disconnect();
        }
    }

    // ---------- 2) Baixar e instalar ----------

    public static void downloadAndInstallAsync(final Context context, final Info info,
                                               final InstallListener listener) {
        if (!BUSY.compareAndSet(false, true)) {
            return; // já tem um download em andamento
        }
        final Context app = context.getApplicationContext();
        final Context ui = context;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (info == null || !isSafeDownloadUrl(info.url)) {
                        throw new IOException("endereco invalido");
                    }
                    final File apk = download(app, info, listener);
                    MAIN.post(new Runnable() {
                        @Override
                        public void run() {
                            startInstaller(app, ui, apk, listener);
                        }
                    });
                } catch (final Exception e) {
                    final String msg = (e instanceof SecurityException)
                            ? "O arquivo baixado não passou na conferência de segurança."
                            : "Não foi possível baixar a atualização. Tente de novo mais tarde.";
                    postFailed(listener, msg);
                } finally {
                    BUSY.set(false);
                }
            }
        }).start();
    }

    private static File download(Context app, Info info, final InstallListener listener)
            throws Exception {
        File dir = new File(app.getCacheDir(), "updates");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("pasta");
        }
        File[] old = dir.listFiles();
        if (old != null) {
            for (File f : old) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
        File out = new File(dir, "ASC-TVBox.apk");

        HttpURLConnection conn = open(info.url);
        try {
            if (conn.getResponseCode() != 200) {
                throw new IOException("resposta");
            }
            long total = conn.getContentLength();
            if (total > MAX_APK_BYTES) {
                throw new IOException("grande demais");
            }
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            InputStream in = conn.getInputStream();
            OutputStream os = new FileOutputStream(out);
            try {
                byte[] buf = new byte[16 * 1024];
                long done = 0;
                int lastPercent = -2;
                int n;
                while ((n = in.read(buf)) >= 0) {
                    done += n;
                    if (done > MAX_APK_BYTES) {
                        throw new IOException("grande demais");
                    }
                    sha.update(buf, 0, n);
                    os.write(buf, 0, n);
                    final int percent = total > 0 ? (int) (done * 100L / total) : -1;
                    if (percent != lastPercent) {
                        lastPercent = percent;
                        MAIN.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onProgress(percent);
                            }
                        });
                    }
                }
            } finally {
                os.close();
                in.close();
            }
            if (!toHex(sha.digest()).equalsIgnoreCase(info.sha256)) {
                //noinspection ResultOfMethodCallIgnored
                out.delete();
                throw new SecurityException("sha256");
            }
            return out;
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Abre o instalador do Android. Tenta, nesta ordem: (1) INSTALL_PACKAGE, (2) VIEW do APK e
     * (3) a sessão do PackageInstaller (funciona em TV boxes sem a tela de instalação comum).
     *
     * Duas regras (19/11/2026):
     *  - O instalador abre SEMPRE em tarefa própria (FLAG_ACTIVITY_NEW_TASK). Antes ele abria
     *    dentro da tarefa do app; ao terminar de instalar, o Android encerra o app e levava junto
     *    a tela "Concluído / Abrir", que sumia antes de dar para escolher.
     *  - Se mais de um app responde ao pedido de instalar, escolhe sozinho o instalador do
     *    sistema (chooseInstaller) e abre direto, sem a janela "Completar ação usando".
     */
    private static void startInstaller(Context app, Context ui, File apk,
                                       InstallListener listener) {
        try {
            if (Build.VERSION.SDK_INT >= 26 && !app.getPackageManager().canRequestPackageInstalls()) {
                try {
                    Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + app.getPackageName()));
                    settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(settings);
                    listener.onFailed("Permita instalar aplicativos por este app e tente de novo.");
                    return;
                } catch (Exception e) {
                    // Sem essa tela neste aparelho: segue, o próprio instalador pede a permissão.
                }
            }
            Uri uri = FileProvider.getUriForFile(app, app.getPackageName() + ".fileprovider", apk);
            boolean live = ui instanceof Activity && !((Activity) ui).isFinishing();
            Context starter = live ? ui : app;

            Intent[] tries = new Intent[2];
            tries[0] = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            tries[0].setData(uri);
            tries[0].putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
            tries[1] = new Intent(Intent.ACTION_VIEW);
            tries[1].setDataAndType(uri, "application/vnd.android.package-archive");
            for (Intent i : tries) {
                // Sempre em tarefa própria: a tela final do instalador não pode morrer junto
                // com o app quando o Android o encerrar para trocar a versão.
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    List<ResolveInfo> targets = app.getPackageManager().queryIntentActivities(
                            i, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo ri : targets) {
                        app.grantUriPermission(ri.activityInfo.packageName, uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    ComponentName only = chooseInstaller(app, targets);
                    if (only != null) {
                        i.setComponent(only); // sem janela de escolha do instalador
                    }
                } catch (Exception ignored) {
                    // só reforço; se falhar, abre do jeito comum (o Android pode perguntar)
                }
                try {
                    starter.startActivity(i);
                    listener.onInstallerOpened();
                    return;
                } catch (ActivityNotFoundException | SecurityException e) {
                    // tenta a próxima forma
                }
            }
            installWithSession(app, apk, listener);
        } catch (Exception e) {
            listener.onFailed("Não foi possível abrir o instalador.");
        }
    }

    /**
     * Entre os apps que sabem instalar APK, escolhe UM: o instalador do sistema (nome com
     * "packageinstaller" e app do sistema) vale mais; depois qualquer app do sistema; depois os
     * outros. Devolve null se não houver nenhum (aí o Android decide).
     */
    private static ComponentName chooseInstaller(Context app, List<ResolveInfo> targets) {
        if (targets == null) {
            return null;
        }
        ResolveInfo best = null;
        int bestScore = -1;
        for (ResolveInfo ri : targets) {
            if (ri == null || ri.activityInfo == null || !ri.activityInfo.exported) {
                continue;
            }
            String pkg = ri.activityInfo.packageName;
            if (pkg == null || pkg.equals(app.getPackageName())) {
                continue;
            }
            boolean system = ri.activityInfo.applicationInfo != null
                    && (ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            int score = (pkg.contains("packageinstaller") ? 2 : 0) + (system ? 1 : 0);
            if (score > bestScore) {
                bestScore = score;
                best = ri;
            }
        }
        if (best == null) {
            return null;
        }
        return new ComponentName(best.activityInfo.packageName, best.activityInfo.name);
    }

    /** Última tentativa: instala pela sessão do PackageInstaller (o Android pede a confirmação). */
    private static void installWithSession(final Context app, File apk,
                                           final InstallListener listener) {
        try {
            PackageInstaller installer = app.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int id = installer.createSession(params);
            PackageInstaller.Session session = installer.openSession(id);
            try {
                OutputStream out = session.openWrite("ASC-TVBox.apk", 0, apk.length());
                InputStream in = new java.io.FileInputStream(apk);
                try {
                    byte[] buf = new byte[64 * 1024];
                    int n;
                    while ((n = in.read(buf)) >= 0) {
                        out.write(buf, 0, n);
                    }
                    session.fsync(out);
                } finally {
                    in.close();
                    out.close();
                }

                final String action = app.getPackageName() + ".INSTALL_RESULT";
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                                PackageInstaller.STATUS_FAILURE);
                        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                            Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                            if (confirm != null) {
                                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                try {
                                    app.startActivity(confirm);
                                    listener.onInstallerOpened();
                                } catch (Exception e) {
                                    listener.onFailed("Não foi possível abrir o instalador.");
                                }
                            }
                            return;
                        }
                        try {
                            app.unregisterReceiver(this);
                        } catch (Exception ignored) {
                            // já removido
                        }
                        if (status != PackageInstaller.STATUS_SUCCESS) {
                            listener.onFailed("O Android não conseguiu instalar a atualização.");
                        }
                    }
                };
                IntentFilter filter = new IntentFilter(action);
                if (Build.VERSION.SDK_INT >= 33) {
                    app.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
                } else {
                    app.registerReceiver(receiver, filter);
                }
                int flags = PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
                Intent result = new Intent(action).setPackage(app.getPackageName());
                PendingIntent pending = PendingIntent.getBroadcast(app, id, result, flags);
                session.commit(pending.getIntentSender());
            } finally {
                session.close();
            }
        } catch (Exception e) {
            listener.onFailed("Este aparelho não conseguiu abrir o instalador de aplicativos.");
        }
    }

    private static void postFailed(final InstallListener listener, final String message) {
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                listener.onFailed(message);
            }
        });
    }

    // ---------- Rede ----------

    /** Abre um endereço https seguindo redirecionamentos (só para https). */
    private static HttpURLConnection open(String address) throws IOException {
        URL url = new URL(address);
        for (int i = 0; i <= MAX_REDIRECTS; i++) {
            if (!"https".equals(url.getProtocol())) {
                throw new IOException("somente https");
            }
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            int code = conn.getResponseCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null || location.isEmpty()) {
                    throw new IOException("redirecionamento sem destino");
                }
                url = new URL(url, location);
                continue;
            }
            return conn;
        }
        throw new IOException("redirecionamentos demais");
    }

    /** Endereço do APK: tem que começar pelo repositório e não pode ter "..", "%2e", "%5c" nem barra invertida. */
    private static boolean isSafeDownloadUrl(String url) {
        // Sem diferenciar maiúscula/minúscula: o GitHub guarda o nome do repositório como foi
        // criado (ex.: "Dono/Repo") e o endereço do version.json pode vir com outra caixa.
        if (url == null || !url.regionMatches(true, 0, DOWNLOAD_PREFIX, 0, DOWNLOAD_PREFIX.length())) {
            return false;
        }
        String lower = url.toLowerCase();
        return !lower.contains("..") && !lower.contains("%2e")
                && !lower.contains("\\") && !lower.contains("%5c");
    }

    private static byte[] readUpTo(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int total = 0;
        while (total < max) {
            int n = in.read(buf, 0, Math.min(buf.length, max - total));
            if (n < 0) {
                break;
            }
            out.write(buf, 0, n);
            total += n;
        }
        return out.toByteArray();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
