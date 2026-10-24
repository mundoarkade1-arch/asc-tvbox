package com.ascplay.tvbox;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
            if (code <= 0 || !url.startsWith(DOWNLOAD_PREFIX) || !sha.matches("[0-9a-f]{64}")) {
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (info == null || !info.url.startsWith(DOWNLOAD_PREFIX)) {
                        throw new IOException("endereco invalido");
                    }
                    final File apk = download(app, info, listener);
                    MAIN.post(new Runnable() {
                        @Override
                        public void run() {
                            startInstaller(app, apk, listener);
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

    private static void startInstaller(Context app, File apk, InstallListener listener) {
        try {
            if (Build.VERSION.SDK_INT >= 26 && !app.getPackageManager().canRequestPackageInstalls()) {
                Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + app.getPackageName()));
                settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                app.startActivity(settings);
                listener.onFailed("Permita instalar aplicativos por este app e tente de novo.");
                return;
            }
            Uri uri = FileProvider.getUriForFile(app, app.getPackageName() + ".fileprovider", apk);
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(uri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            app.startActivity(install);
            listener.onInstallerOpened();
        } catch (ActivityNotFoundException e) {
            listener.onFailed("Este aparelho não tem instalador de aplicativos.");
        } catch (Exception e) {
            listener.onFailed("Não foi possível abrir o instalador.");
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
