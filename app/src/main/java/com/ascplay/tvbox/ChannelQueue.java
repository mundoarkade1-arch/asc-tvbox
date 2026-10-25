package com.ascplay.tvbox;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

/**
 * Fila de canais ao vivo para trocar de canal com as setas do player (Rodada 1, R4).
 *
 * Ordem: todas as categorias, uma depois da outra, começando no canal que está tocando.
 * No fim de uma categoria passa para a seguinte. No último canal de todos dá a volta para o
 * primeiro (e do primeiro para cima, para o último).
 *
 * Só uma categoria fica na memória por vez (páginas de 40, máximo de 400 canais).
 * Servidor (Xtream): a categoria é o número dela. Lista M3U: a categoria é o nome do grupo.
 * Categoria nula = "Todas as categorias" (uma fila só).
 * Com o controle parental ligado, categorias adultas são puladas e canais com nome adulto
 * não entram (menos na categoria de onde a pessoa saiu).
 *
 * Tudo que fala com a internet roda fora da tela principal. As respostas voltam pela
 * tela principal, no Listener.
 */
public final class ChannelQueue {

    public interface Listener {
        /** O canal novo escolhido pelas setas. */
        void onChannel(XtreamApi.Item item);

        /** Não deu para trocar de canal (sem rede, ou não achou outro canal). */
        void onFail();
    }

    private static final int PAGE = 40;
    private static final int MAX_ITEMS = 400;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final boolean m3u;
    private final String server;
    private final String user;
    private final String pass;
    private final String m3uUrl;
    private final String startCat;
    private final boolean hideAdult;
    private final String currentKey;
    private final Listener listener;

    // Categorias na ordem de troca. Nula = todas.
    private final List<String> cats = new ArrayList<>();
    private int catIdx = 0;

    // Canais da categoria atual e posição do canal que toca
    private final List<XtreamApi.Item> list = new ArrayList<>();
    private int pos = -1;
    private int raw = 0;
    private boolean ended = false;

    private volatile boolean busy = false;
    private volatile boolean ready = false;
    private volatile boolean closed = false;

    /**
     * @param currentKey servidor: número (id) do canal que toca. Lista M3U: endereço do canal.
     * @param startCat   servidor: número da categoria; lista M3U: nome do grupo; nulo = todas.
     */
    public ChannelQueue(boolean m3u, String server, String user, String pass, String m3uUrl,
                        String startCat, boolean hideAdult, String currentKey,
                        Listener listener) {
        this.m3u = m3u;
        this.server = server;
        this.user = user;
        this.pass = pass;
        this.m3uUrl = m3uUrl;
        this.startCat = startCat;
        this.hideAdult = hideAdult;
        this.currentKey = currentKey;
        this.listener = listener;
    }

    /** Descobre as categorias e acha o canal que toca. Pode demorar um pouco (lista M3U). */
    public void start() {
        new Thread(() -> {
            boolean ok = false;
            try {
                buildCategories();
                ok = locate();
            } catch (Exception e) {
                // sem rede ou resposta estranha: a troca de canal fica desligada
            }
            final boolean result = ok;
            ui.post(() -> {
                if (!closed) {
                    ready = result;
                }
            });
        }).start();
    }

    /** Já dá para trocar de canal? */
    public boolean isReady() {
        return ready && !closed;
    }

    /** Desliga a fila (ao sair do player). */
    public void close() {
        closed = true;
        ready = false;
    }

    /** Próximo canal (seta para baixo). */
    public void next() {
        move(true);
    }

    /** Canal anterior (seta para cima). */
    public void previous() {
        move(false);
    }

    private void move(final boolean forward) {
        if (!isReady() || busy) {
            return;
        }
        busy = true;
        new Thread(() -> {
            XtreamApi.Item found = null;
            try {
                found = forward ? moveNext() : movePrevious();
            } catch (Exception e) {
                // sem rede ou resposta estranha
            }
            final XtreamApi.Item result = found;
            ui.post(() -> {
                busy = false;
                if (closed) {
                    return;
                }
                if (result == null) {
                    listener.onFail();
                } else {
                    listener.onChannel(result);
                }
            });
        }).start();
    }

    // ---------- Trabalho feito fora da tela principal ----------

    private void buildCategories() {
        cats.clear();
        catIdx = 0;
        if (startCat == null) {
            cats.add(null);
            return;
        }
        List<String> ids = new ArrayList<>();
        try {
            if (m3u) {
                List<String> names = M3uApi.categories(m3uUrl, XtreamApi.Kind.LIVE);
                if (names != null) {
                    for (String n : names) {
                        if (n.equals(startCat) || !(hideAdult && ParentalStore.isAdultCategory(n))) {
                            ids.add(n);
                        }
                    }
                }
            } else {
                List<XtreamApi.Category> res =
                        XtreamApi.categories(server, user, pass, XtreamApi.Kind.LIVE);
                if (res != null) {
                    for (XtreamApi.Category c : res) {
                        if (startCat.equals(c.id)
                                || !(hideAdult && ParentalStore.isAdultCategory(c.name))) {
                            ids.add(c.id);
                        }
                    }
                }
            }
        } catch (Exception e) {
            ids.clear();
        }
        int at = ids.indexOf(startCat);
        if (at < 0) {
            ids.clear();
            ids.add(startCat);
            at = 0;
        }
        cats.addAll(ids);
        catIdx = at;
    }

    /** Lê a categoria de onde ela começa até achar o canal que toca. */
    private boolean locate() throws Exception {
        resetCategory(catIdx);
        int scanned = 0;
        while (true) {
            while (scanned < list.size()) {
                if (matches(list.get(scanned))) {
                    pos = scanned;
                    return true;
                }
                scanned++;
            }
            if (ended) {
                return false;
            }
            loadPage();
        }
    }

    private boolean matches(XtreamApi.Item it) {
        if (currentKey == null) {
            return false;
        }
        return m3u ? currentKey.equals(it.url) : currentKey.equals(it.id);
    }

    private XtreamApi.Item moveNext() throws Exception {
        if (pos + 1 < list.size()) {
            pos++;
            return list.get(pos);
        }
        while (!ended) {
            int before = list.size();
            loadPage();
            if (list.size() > before) {
                pos++;
                return list.get(pos);
            }
        }
        // Acabou a categoria: vai para a seguinte (no fim de todas, dá a volta)
        for (int tries = 0; tries < cats.size(); tries++) {
            resetCategory((catIdx + 1) % cats.size());
            while (!ended && list.isEmpty()) {
                loadPage();
            }
            if (!list.isEmpty()) {
                pos = 0;
                return list.get(0);
            }
        }
        return null;
    }

    private XtreamApi.Item movePrevious() throws Exception {
        if (pos > 0) {
            pos--;
            return list.get(pos);
        }
        // Estava no primeiro da categoria: vai para a anterior, no último canal dela
        for (int tries = 0; tries < cats.size(); tries++) {
            resetCategory((catIdx - 1 + cats.size()) % cats.size());
            while (!ended) {
                loadPage();
            }
            if (!list.isEmpty()) {
                pos = list.size() - 1;
                return list.get(pos);
            }
        }
        return null;
    }

    private void resetCategory(int index) {
        catIdx = index;
        list.clear();
        pos = -1;
        raw = 0;
        ended = false;
    }

    /** Lê mais uma página da categoria atual. */
    private void loadPage() throws Exception {
        if (ended) {
            return;
        }
        int want = Math.min(PAGE, MAX_ITEMS - list.size());
        if (want <= 0) {
            ended = true;
            return;
        }
        String cat = cats.get(catIdx);
        List<XtreamApi.Item> res;
        if (m3u) {
            boolean skipAdult = hideAdult && !(cat != null && cat.equals(startCat));
            res = M3uApi.page(m3uUrl, XtreamApi.Kind.LIVE, cat, list.size(), want, skipAdult);
        } else {
            res = XtreamApi.items(server, user, pass, XtreamApi.Kind.LIVE, cat, raw, want);
        }
        if (res == null) {
            throw new Exception("sem resposta");
        }
        raw += res.size();
        for (XtreamApi.Item it : res) {
            if (keep(it, cat)) {
                list.add(it);
            }
        }
        if (res.size() < want || list.size() >= MAX_ITEMS) {
            ended = true;
        }
    }

    /** Este canal pode entrar na fila? (Servidor: filtra nome adulto; a lista M3U já filtra.) */
    private boolean keep(XtreamApi.Item it, String cat) {
        if (m3u || !hideAdult) {
            return true;
        }
        if (cat != null && cat.equals(startCat)) {
            return true;
        }
        return !ParentalStore.isAdultName(it.name);
    }
}
