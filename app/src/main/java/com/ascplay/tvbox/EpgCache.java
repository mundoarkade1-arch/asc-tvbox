package com.ascplay.tvbox;

import android.os.Handler;
import android.os.Looper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Guarda na memória a programação (EPG) dos canais já consultados.
 *
 * Economia de memória: no máximo 80 canais guardados, cada um por 1 minuto.
 * Economia de rede: no máximo 2 pedidos ao servidor ao mesmo tempo.
 *
 * Quem pede (get) é avisado SEMPRE na thread principal, pode mexer na tela direto.
 * Não registra senhas nem endereços em log.
 */
public final class EpgCache {

    /** epg vem null quando o canal não tem guia, ou quando nada passa agora. */
    public interface Listener {
        void onEpg(XtreamApi.Epg epg);
    }

    private static final int MAX_ENTRIES = 80;
    private static final long TTL_MS = 60000L;

    private static final class Entry {
        final XtreamApi.Epg epg;
        final long savedAt;

        Entry(XtreamApi.Epg epg, long savedAt) {
            this.epg = epg;
            this.savedAt = savedAt;
        }
    }

    private static final Map<String, Entry> CACHE =
            new LinkedHashMap<String, Entry>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
                    return size() > MAX_ENTRIES;
                }
            };

    private static final ExecutorService POOL = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private EpgCache() {
    }

    /**
     * Devolve o que já está guardado e ainda vale, ou null se não houver.
     * Não faz acesso à rede. Pode ser chamado na thread principal.
     */
    public static XtreamApi.Epg peek(String server, String user, String streamId) {
        Entry e = fresh(key(server, user, streamId));
        return e == null ? null : e.epg;
    }

    /**
     * Busca a programação de um canal. Se já estiver guardada e valendo, avisa na hora.
     * Se o pedido ao servidor falhar, avisa com null e não guarda o resultado.
     * Chamar na thread principal.
     */
    public static void get(final String server, final String user, final String pass,
                           final String streamId, final Listener listener) {
        final String key = key(server, user, streamId);
        Entry e = fresh(key);
        if (e != null) {
            listener.onEpg(e.epg);
            return;
        }
        POOL.execute(new Runnable() {
            @Override
            public void run() {
                XtreamApi.Epg result = null;
                boolean ok = false;
                try {
                    result = XtreamApi.epg(server, user, pass, streamId);
                    ok = true;
                } catch (Exception ignored) {
                    // sem rede ou servidor fora do ar: avisa com null abaixo
                }
                if (ok) {
                    synchronized (CACHE) {
                        CACHE.put(key, new Entry(result, System.currentTimeMillis()));
                    }
                }
                final XtreamApi.Epg out = result;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onEpg(out);
                    }
                });
            }
        });
    }

    /** Esvazia tudo (usar quando faltar memória ou ao trocar de login). */
    public static void clear() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    // ---------- Ajudas ----------

    private static String key(String server, String user, String streamId) {
        return server + "|" + user + "|" + streamId;
    }

    /** Entrada guardada que ainda vale: menos de 1 minuto e o programa ainda não acabou. */
    private static Entry fresh(String key) {
        Entry e;
        synchronized (CACHE) {
            e = CACHE.get(key);
        }
        if (e == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now - e.savedAt > TTL_MS) {
            return null;
        }
        if (e.epg != null && now >= e.epg.end) {
            return null;
        }
        return e;
    }
}
