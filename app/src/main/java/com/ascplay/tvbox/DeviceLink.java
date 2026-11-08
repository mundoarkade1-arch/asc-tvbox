package com.ascplay.tvbox;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Liga este aparelho ao app de gerenciar (ASC Admin) por um código tipo MAC.
 * - O código aparece na tela de login. O dono digita no app de gerenciar.
 * - Cada aparelho tem também uma chave secreta, que nunca aparece na tela.
 *   Só quem tem o código E a chave secreta consegue ler a lista.
 * Faz acesso à rede: NUNCA chamar na thread principal.
 * Não registra senhas, chaves nem endereços em log.
 */
public final class DeviceLink {

    private static final String BASE_URL = "https://yfuuhudnvjepyvpewkyg.supabase.co";
    // Chave pública (publishable): foi feita para ficar dentro do app.
    private static final String PUBLIC_KEY = "sb_publishable_2i0flxtVuLyuXj41j--VIA_lQiAIFHm";

    private static final String PREFS = "asc_link";
    private static final int TIMEOUT_MS = 10000;
    private static final int MAX_BODY_BYTES = 64 * 1024;
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    public enum State {
        /** Chegou uma lista nova. */
        READY,
        /** O aparelho está registrado, mas o dono ainda não colocou a lista. */
        WAITING,
        /** Sem internet ou sem resposta. */
        OFFLINE
    }

    public static final class Reply {
        public final State state;
        public final int version;
        public final CredentialStore.Credentials creds;

        Reply(State state, int version, CredentialStore.Credentials creds) {
            this.state = state;
            this.version = version;
            this.creds = creds;
        }
    }

    /** Texto curto para o dono ver na tela de login o que deu errado (sem senhas). */
    public static volatile String lastDetail = "";

    private DeviceLink() {
    }

    // ---------- Código e chave do aparelho ----------

    /** Código do aparelho no formato A4:7B:1C:9E:02:F3 (cria na primeira vez). */
    public static synchronized String getDisplayCode(Context context) {
        String code = ensureIdentity(context).getString("code", "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < code.length(); i += 2) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(code, i, i + 2);
        }
        return sb.toString();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static SharedPreferences ensureIdentity(Context context) {
        SharedPreferences p = prefs(context);
        String code = p.getString("code", "");
        String secret = p.getString("secret", "");
        if (code.length() != 12 || secret.length() < 32) {
            newIdentity(p);
        }
        return p;
    }

    private static void newIdentity(SharedPreferences p) {
        p.edit()
                .putString("code", randomHex(6))
                .putString("secret", randomHex(32))
                .putInt("applied", 0)
                .commit();
    }

    private static String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        new SecureRandom().nextBytes(b);
        char[] out = new char[bytes * 2];
        for (int i = 0; i < bytes; i++) {
            int v = b[i] & 0xFF;
            out[i * 2] = HEX[v >> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    // ---------- Conversa com a caixinha na internet ----------

    /**
     * Registra o aparelho (se preciso) e busca a lista.
     * Se o código já estiver em uso por outro aparelho, troca por um novo.
     */
    public static synchronized Reply poll(Context context) {
        SharedPreferences p = ensureIdentity(context);
        lastDetail = "";
        for (int attempt = 0; attempt < 3; attempt++) {
            String reg = rpc("register_device", body(p));
            if (reg == null) {
                return new Reply(State.OFFLINE, 0, null);
            }
            if ("created".equals(reg) || "ok".equals(reg)) {
                return fetch(p);
            }
            // "taken" ou "bad": gera outro código e tenta de novo.
            newIdentity(p);
        }
        return new Reply(State.OFFLINE, 0, null);
    }

    private static Reply fetch(SharedPreferences p) {
        String text = rpc("fetch_playlist", body(p));
        if (text == null) {
            return new Reply(State.OFFLINE, 0, null);
        }
        try {
            JSONObject o = new JSONObject(text);
            if (!"ready".equals(o.optString("status", ""))) {
                return new Reply(State.WAITING, 0, null);
            }
            int version = o.optInt("version", 0);
            String mode = str(o, "mode");
            CredentialStore.Credentials c = null;
            if (CredentialStore.MODE_M3U.equals(mode)) {
                String m3u = str(o, "m3u");
                if (m3u.startsWith("http://") || m3u.startsWith("https://")) {
                    c = new CredentialStore.Credentials(mode, "", "", "", m3u);
                }
            } else if (CredentialStore.MODE_XTREAM.equals(mode)) {
                String server = str(o, "server");
                String user = str(o, "user");
                String pass = str(o, "pass");
                String origin = LoginChecker.normalizeServer(server);
                lastDetail = "Servidor " + (origin != null ? "ok" : "invalido")
                        + ", usuario " + user.length() + " letras, senha " + pass.length()
                        + " letras.";
                if (origin != null && !user.isEmpty() && !pass.isEmpty()) {
                    c = new CredentialStore.Credentials(mode, origin, user, pass, "");
                }
            }
            if (c == null) {
                if (lastDetail.isEmpty()) {
                    lastDetail = "Lista incompleta (modo: " + mode + ").";
                }
                // Lista incompleta: espera o dono corrigir.
                return new Reply(State.WAITING, 0, null);
            }
            return new Reply(State.READY, version, c);
        } catch (Exception e) {
            return new Reply(State.OFFLINE, 0, null);
        }
    }

    /** Guarda a lista recebida como login do app. */
    public static synchronized void apply(Context context, Reply reply) {
        if (reply == null || reply.state != State.READY || reply.creds == null) {
            return;
        }
        CredentialStore.save(context, reply.creds);
        prefs(context).edit().putInt("applied", reply.version).commit();
    }

    /** Só vale a pena aplicar se for mais nova que a última aplicada. */
    public static synchronized boolean isNewer(Context context, Reply reply) {
        return reply != null
                && reply.state == State.READY
                && reply.version > prefs(context).getInt("applied", 0);
    }

    /**
     * Testa a lista recebida ANTES de usar (para uma lista com defeito não
     * trocar um login que funciona). Faz rede: só em thread de fundo.
     */
    public static LoginChecker.Result verify(Reply reply) {
        if (reply == null || reply.creds == null) {
            return LoginChecker.Result.UNKNOWN;
        }
        CredentialStore.Credentials c = reply.creds;
        if (CredentialStore.MODE_M3U.equals(c.mode)) {
            return LoginChecker.checkM3u(c.m3uUrl);
        }
        return LoginChecker.checkXtream(c.server, c.user, c.pass);
    }

    /**
     * Para quando o app abre com login já salvo: confere em segundo plano
     * se o dono mandou uma lista nova. Vale na próxima vez que o app abrir.
     */
    public static void syncInBackground(final Context context) {
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            try {
                Reply r = poll(app);
                if (isNewer(app, r) && verify(r) == LoginChecker.Result.OK) {
                    apply(app, r);
                }
            } catch (Exception ignored) {
                // sem internet ou erro: segue com o login atual
            }
        }).start();
    }

    // ---------- Rede ----------

    private static String str(JSONObject o, String key) {
        if (o.isNull(key)) {
            return "";
        }
        return o.optString(key, "").trim();
    }

    private static String body(SharedPreferences p) {
        try {
            JSONObject o = new JSONObject();
            o.put("p_code", p.getString("code", ""));
            o.put("p_secret", p.getString("secret", ""));
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    /** Chama uma função da caixinha. Devolve o texto da resposta, ou null se falhar. */
    private static String rpc(String function, String json) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(BASE_URL + "/rest/v1/rpc/" + function).openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("apikey", PUBLIC_KEY);
            if (PUBLIC_KEY.startsWith("eyJ")) {
                conn.setRequestProperty("Authorization", "Bearer " + PUBLIC_KEY);
            }
            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(out.length);
            OutputStream os = conn.getOutputStream();
            try {
                os.write(out);
            } finally {
                os.close();
            }
            int httpCode = conn.getResponseCode();
            if (httpCode != 200) {
                lastDetail = "Erro " + httpCode + " em " + function + ".";
                return null;
            }
            InputStream in = conn.getInputStream();
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[2048];
                int n;
                while ((n = in.read(buf)) > 0 && bos.size() < MAX_BODY_BYTES) {
                    bos.write(buf, 0, n);
                }
                String s = new String(bos.toByteArray(), StandardCharsets.UTF_8).trim();
                if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
                    s = s.substring(1, s.length() - 1);
                }
                return s;
            } finally {
                in.close();
            }
        } catch (IOException e) {
            lastDetail = "Falha de rede em " + function + ".";
            return null;
        } catch (Exception e) {
            lastDetail = "Falha em " + function + ".";
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
