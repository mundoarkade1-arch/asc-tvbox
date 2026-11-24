package com.ascplay.tvbox;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import androidx.core.content.ContextCompat;
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

    /** Esperas (ms) antes de cada tentativa da verificação: a internet do TV box pode demorar. */
    private static final long[] CHECK_WAITS_MS = {0L, 5000L, 15000L, 30000L};

    /** true se a última verificação NÃO conseguiu falar com o servidor (≠ "não há versão nova"). */
    private static volatile boolean lastCheckFailed = false;

    /** Diz se a última verificação falhou (sem internet, GitHub fora, resposta estranha). */
    public static boolean lastCheckFailed() {
        return lastCheckFailed;
    }

    /**
     * Verifica em segundo plano se existe versão mais nova. Se não conseguir falar com o servidor
     * (internet ainda subindo no TV box, por exemplo), tenta de novo algumas vezes antes de desistir.
     * Só avisa "sem novidade" (onNoUpdate) no fim; lastCheckFailed() diz se foi por falha.
     */
    public static void checkAsync(final Context context, final CheckListener listener) {
        final Context app = context.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Info found = null;
                boolean answered = false;
                int installed = installedVersionCode(app);
                for (int attempt = 0; attempt < CHECK_WAITS_MS.length && !answered; attempt++) {
                    if (CHECK_WAITS_MS[attempt] > 0) {
                        try {
                            Thread.sleep(CHECK_WAITS_MS[attempt]);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    try {
                        Info latest = fetchLatest();
                        if (latest != null) {
                            answered = true;
                            if (installed > 0 && latest.code > installed) {
                                found = latest;
                            }
                        }
                    } catch (Exception ignored) {
                        // sem internet agora: tenta de novo
                    }
                }
                lastCheckFailed = !answered;
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
     * Instala a atualização. Ordem: (1) sessão do PackageInstaller, que fala direto com o
     * instalador do sistema e NÃO abre a lista "escolha o instalador" (era ela que mostrava um tal
     * de "Assistente" que não instala); (2) só se a sessão nem puder ser criada, INSTALL_PACKAGE e
     * VIEW do APK pelo jeito antigo.
     *
     * Antes de criar a sessão, as sessões antigas deste app (que ficavam pendentes e pediam
     * confirmação toda hora) são canceladas.
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
            abandonOldSessions(app);
            if (installWithSession(app, apk, listener)) {
                return; // o resultado chega pelo listener (confirmação do Android)
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
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (!live) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                try {
                    List<ResolveInfo> targets = app.getPackageManager().queryIntentActivities(
                            i, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo ri : targets) {
                        app.grantUriPermission(ri.activityInfo.packageName, uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                } catch (Exception ignored) {
                    // só reforço da permissão de leitura do APK
                }
                try {
                    starter.startActivity(i);
                    listener.onInstallerOpened();
                    return;
                } catch (ActivityNotFoundException | SecurityException e) {
                    // tenta a próxima forma
                }
            }
            listener.onFailed("Este aparelho não conseguiu abrir o instalador de aplicativos.");
        } catch (Exception e) {
            listener.onFailed("Não foi possível abrir o instalador.");
        }
    }

    /** Cancela sessões de instalação antigas deste app (pendentes, que ficam pedindo confirmação). */
    private static void abandonOldSessions(Context app) {
        if (Build.VERSION.SDK_INT < 24) {
            return;
        }
        try {
            PackageInstaller installer = app.getPackageManager().getPackageInstaller();
            for (PackageInstaller.SessionInfo info : installer.getMySessions()) {
                try {
                    installer.abandonSession(info.getSessionId());
                } catch (Exception ignored) {
                    // já encerrada
                }
            }
        } catch (Exception ignored) {
            // sem acesso às sessões neste aparelho: segue
        }
    }

    /**
     * Instala pela sessão do PackageInstaller (o Android mostra a confirmação, sem lista de
     * instaladores). Devolve true se a sessão foi confirmada ao sistema; o resultado vem depois,
     * pelo listener. Devolve false (sem chamar o listener) se nem foi possível criar a sessão.
     */
    private static boolean installWithSession(final Context app, File apk,
                                              final InstallListener listener) {
        try {
            PackageInstaller installer = app.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(app.getPackageName());
            final int id = installer.createSession(params);
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
                final boolean[] confirmShown = {false};
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        // Segurança: só aceita o aviso desta sessão de instalação. Um app
                        // malicioso não deve conseguir mandar um aviso falso para este receiver.
                        if (intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1) != id) {
                            return;
                        }
                        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                                PackageInstaller.STATUS_FAILURE);
                        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                            if (confirmShown[0]) {
                                return; // a confirmação já foi aberta: não abre outra
                            }
                            Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                            if (confirm != null && targetsOwnApp(app, confirm)) {
                                return; // nunca abre tela deste próprio app por pedido de fora
                            }
                            if (confirm != null) {
                                confirmShown[0] = true;
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
                        // Cancelou na confirmação: não é erro, segue sem aviso
                        if (status != PackageInstaller.STATUS_SUCCESS
                                && status != PackageInstaller.STATUS_FAILURE_ABORTED) {
                            listener.onFailed("O Android não conseguiu instalar a atualização.");
                        }
                    }
                };
                IntentFilter filter = new IntentFilter(action);
                // Receiver NÃO exportado em qualquer versão do Android (antes do Android 13,
                // o ContextCompat protege com uma permissão interna do próprio app).
                ContextCompat.registerReceiver(app, receiver, filter,
                        ContextCompat.RECEIVER_NOT_EXPORTED);
                int flags = PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
                Intent result = new Intent(action).setPackage(app.getPackageName());
                PendingIntent pending = PendingIntent.getBroadcast(app, id, result, flags);
                session.commit(pending.getIntentSender());
                return true;
            } finally {
                session.close();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /** true se o intent aponta para uma tela ou componente do próprio app. */
    private static boolean targetsOwnApp(Context app, Intent intent) {
        String mine = app.getPackageName();
        if (intent.getComponent() != null && mine.equals(intent.getComponent().getPackageName())) {
            return true;
        }
        return mine.equals(intent.getPackage());
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
            conn.setUseCaches(false); // sempre a resposta nova (não a guardada)
            conn.setRequestProperty("Cache-Control", "no-cache");
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
