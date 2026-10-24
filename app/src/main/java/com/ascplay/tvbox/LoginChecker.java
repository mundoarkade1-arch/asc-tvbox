package com.ascplay.tvbox;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Confere se o login funciona.
 * Faz acesso à rede: NUNCA chamar na thread principal (use uma thread de fundo).
 * Não registra senhas nem endereços em log.
 */
public final class LoginChecker {

    public enum Result {
        OK, EMPTY, BAD_SERVER, BAD_URL, NETWORK, AUTH, EXPIRED, NOT_M3U, UNKNOWN
    }

    private static final int TIMEOUT_MS = 15000;
    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_JSON_BYTES = 256 * 1024;
    private static final int MAX_M3U_PEEK_BYTES = 4096;
    private static final String USER_AGENT = "ASC-TVBox/1.0 (Android)";

    /** O servidor recusou o login (o Android às vezes devolve isso como erro de IO). */
    private static final class AuthRefusedException extends IOException {
        AuthRefusedException() {
            super("login recusado");
        }
    }

    private LoginChecker() {
    }

    // ---------- Servidor e login (Xtream Codes) ----------

    public static Result checkXtream(String server, String user, String pass) {
        if (isBlank(server) || isBlank(user) || isBlank(pass)) {
            return Result.EMPTY;
        }
        String origin = normalizeServer(server);
        if (origin == null) {
            return Result.BAD_SERVER;
        }
        HttpURLConnection conn = null;
        try {
            String api = origin + "/player_api.php?username="
                    + URLEncoder.encode(user.trim(), "UTF-8")
                    + "&password=" + URLEncoder.encode(pass.trim(), "UTF-8");
            conn = open(api);
            int code = conn.getResponseCode();
            if (code >= 400 && code < 500) {
                // Servidor respondeu e recusou: login errado.
                return Result.AUTH;
            }
            if (code != 200) {
                return Result.NETWORK;
            }
            InputStream in = conn.getInputStream();
            String body;
            try {
                body = new String(readUpTo(in, MAX_JSON_BYTES), StandardCharsets.UTF_8);
            } finally {
                in.close();
            }
            return evaluateXtream(body);
        } catch (AuthRefusedException e) {
            return Result.AUTH;
        } catch (IOException e) {
            return Result.NETWORK;
        } catch (Exception e) {
            return Result.UNKNOWN;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static Result evaluateXtream(String body) {
        String text = body.trim();
        if (text.isEmpty()) {
            return Result.AUTH;
        }
        JSONObject root;
        try {
            root = new JSONObject(text);
        } catch (Exception e) {
            return Result.UNKNOWN;
        }
        JSONObject info = root.optJSONObject("user_info");
        if (info == null || info.optInt("auth", 0) != 1) {
            return Result.AUTH;
        }
        String status = info.optString("status", "");
        if (!status.isEmpty() && !"null".equals(status) && !"Active".equalsIgnoreCase(status)) {
            return Result.EXPIRED;
        }
        String exp = info.optString("exp_date", "");
        if (!exp.isEmpty() && !"null".equals(exp)) {
            try {
                long seconds = Long.parseLong(exp.trim());
                if (seconds > 0 && seconds * 1000L < System.currentTimeMillis()) {
                    return Result.EXPIRED;
                }
            } catch (NumberFormatException ignored) {
                // data em formato estranho: ignora
            }
        }
        return Result.OK;
    }

    // ---------- Lista M3U ----------

    public static Result checkM3u(String url) {
        if (isBlank(url)) {
            return Result.EMPTY;
        }
        String clean = url.trim();
        if (!isHttpUrl(clean)) {
            return Result.BAD_URL;
        }
        HttpURLConnection conn = null;
        try {
            conn = open(clean);
            int code = conn.getResponseCode();
            if (code == 401 || code == 403) {
                return Result.AUTH;
            }
            if (code != 200) {
                return Result.NETWORK;
            }
            InputStream in = conn.getInputStream();
            String head;
            try {
                head = new String(readUpTo(in, MAX_M3U_PEEK_BYTES), StandardCharsets.UTF_8);
            } finally {
                in.close();
            }
            if (head.startsWith("\uFEFF")) {
                head = head.substring(1);
            }
            head = head.trim().toUpperCase();
            if (head.startsWith("#EXTM3U") || head.startsWith("#EXTINF")) {
                return Result.OK;
            }
            return Result.NOT_M3U;
        } catch (AuthRefusedException e) {
            return Result.AUTH;
        } catch (IOException e) {
            return Result.NETWORK;
        } catch (Exception e) {
            return Result.UNKNOWN;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // ---------- Endereços ----------

    /**
     * Devolve só "http(s)://servidor:porta", ou null se o endereço for inválido.
     * Se faltar o http://, acrescenta.
     */
    public static String normalizeServer(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        String lower = s.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            s = "http://" + s;
        }
        try {
            URL u = new URL(s);
            String protocol = u.getProtocol();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                return null;
            }
            String host = u.getHost();
            if (host == null || host.isEmpty()) {
                return null;
            }
            return protocol + "://" + host + (u.getPort() != -1 ? ":" + u.getPort() : "");
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isHttpUrl(String s) {
        try {
            URL u = new URL(s);
            String protocol = u.getProtocol();
            return ("http".equals(protocol) || "https".equals(protocol))
                    && u.getHost() != null && !u.getHost().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    // ---------- Rede ----------

    /** Abre o endereço seguindo redirecionamentos (inclusive de http para https). */
    private static HttpURLConnection open(String address) throws IOException {
        URL url = new URL(address);
        for (int i = 0; i <= MAX_REDIRECTS; i++) {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            int code;
            try {
                code = conn.getResponseCode();
            } catch (IOException e) {
                conn.disconnect();
                // Android lança este erro quando o servidor responde 401 sem pedir autenticação.
                String msg = e.getMessage();
                if (msg != null && msg.toLowerCase().contains("authentication challenge")) {
                    throw new AuthRefusedException();
                }
                throw e;
            }
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null || location.isEmpty()) {
                    throw new IOException("redirecionamento sem destino");
                }
                url = new URL(url, location);
                String protocol = url.getProtocol();
                if (!"http".equals(protocol) && !"https".equals(protocol)) {
                    throw new IOException("redirecionamento invalido");
                }
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

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
