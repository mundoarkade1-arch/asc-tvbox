package com.ascplay.tvbox;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Baixa logos de forma leve (sem biblioteca extra), pensado para 1 GB de RAM.
 *
 * - cache em memória de no máximo ~8 MB (descarta as imagens mais antigas);
 * - cada imagem é reduzida ao abrir (lado maior de no máximo 256 pixels);
 * - no máximo 2 downloads ao mesmo tempo, com limite de 2 MB por imagem;
 * - só aceita http e https;
 * - não registra endereços em log.
 *
 * Use sempre na thread principal: ImageLoader.load(imageView, url).
 */
public final class ImageLoader {

    private static final int MAX_SIDE = 256;
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int CACHE_BYTES = 8 * 1024 * 1024;
    private static final int TIMEOUT_MS = 8000;
    private static final int MAX_REDIRECTS = 4;
    private static final int MAX_FAILED = 500;
    private static final String USER_AGENT = "ASC-TVBox/1.0 (Android)";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2);

    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>(CACHE_BYTES) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount();
                }
            };

    /** Endereços que já falharam nesta sessão (para não tentar de novo e de novo). */
    private static final Set<String> FAILED = new HashSet<>();

    /** Qual endereço cada ImageView está esperando. Usado só na thread principal. */
    private static final WeakHashMap<ImageView, String> WANTED = new WeakHashMap<>();

    private ImageLoader() {
    }

    /** Mostra a imagem do endereço; se não houver imagem, deixa vazio (a letra do cartão aparece). */
    public static void load(final ImageView view, final String url) {
        view.setImageDrawable(null);
        if (url == null || url.isEmpty() || !isHttp(url)) {
            WANTED.remove(view);
            return;
        }
        WANTED.put(view, url);

        Bitmap cached = CACHE.get(url);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        synchronized (FAILED) {
            if (FAILED.contains(url)) {
                return;
            }
        }
        POOL.execute(new Runnable() {
            @Override
            public void run() {
                final Bitmap bmp = fetch(url);
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        if (bmp == null) {
                            synchronized (FAILED) {
                                if (FAILED.size() >= MAX_FAILED) {
                                    FAILED.clear();
                                }
                                FAILED.add(url);
                            }
                            return;
                        }
                        CACHE.put(url, bmp);
                        if (url.equals(WANTED.get(view))) {
                            view.setImageBitmap(bmp);
                        }
                    }
                });
            }
        });
    }

    /** Libera a memória das imagens (chamar quando a tela sair de cena ou faltar memória). */
    public static void clear() {
        CACHE.evictAll();
    }

    // ---------- Download e redução ----------

    private static Bitmap fetch(String address) {
        HttpURLConnection conn = null;
        try {
            conn = open(address);
            if (conn.getResponseCode() != 200) {
                return null;
            }
            byte[] data;
            InputStream in = conn.getInputStream();
            try {
                data = readUpTo(in, MAX_BYTES);
            } finally {
                in.close();
            }
            if (data == null) {
                return null;
            }
            return decode(data);
        } catch (Throwable e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static Bitmap decode(byte[] data) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }
        int sample = 1;
        int longest = Math.max(bounds.outWidth, bounds.outHeight);
        while (longest / (sample * 2) >= MAX_SIDE) {
            sample *= 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
    }

    /** Lê até "max" bytes. Devolve null se a imagem for maior que o limite. */
    private static byte[] readUpTo(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(buf)) >= 0) {
            total += n;
            if (total > max) {
                return null;
            }
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static HttpURLConnection open(String address) throws IOException {
        URL url = new URL(address);
        for (int i = 0; i <= MAX_REDIRECTS; i++) {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            int code = conn.getResponseCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null || location.isEmpty()) {
                    throw new IOException("sem destino");
                }
                url = new URL(url, location);
                String protocol = url.getProtocol();
                if (!"http".equals(protocol) && !"https".equals(protocol)) {
                    throw new IOException("protocolo");
                }
                continue;
            }
            return conn;
        }
        throw new IOException("redirecionamentos demais");
    }

    private static boolean isHttp(String s) {
        String lower = s.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }
}
