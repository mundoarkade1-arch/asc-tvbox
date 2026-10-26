package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * "Ver mais" para o login por lista M3U (também as abas Canais, Filmes e Séries do menu
 * à esquerda).
 * Esquerda: botão "Pesquisar" fixo e, abaixo, as categorias (group-title da lista).
 * Direita: itens, por páginas de 40 (máximo 400).
 *
 * A lista é lida aos poucos e de novo a cada página (a lista M3U não tem como pedir "só a página
 * 2"). Só uma página fica na memória. As categorias são achadas lendo a lista uma vez, depois que
 * a primeira página aparece, e só os nomes são guardados.
 *
 * Ordem do foco: menu, Pesquisar, categorias, itens. Esquerda na primeira coluna dos itens
 * leva o foco às categorias (Rodada 1, R3).
 *
 * Menu à esquerda (ícone e nome): ligado pelo NavBar, igual à BrowseActivity. Atualizar
 * recarrega a categoria aberta.
 *
 * Pesquisa (Rodada 4, R4-1): o botão "Pesquisar" (btn_search_top) fica FIXO acima da lista de
 * categorias, sempre à vista. No layout ele nasce escondido (a SearchActivity, que reaproveita
 * o mesmo layout, não o mostra); esta tela o mostra. OK nele pesquisa SÓ o tipo desta tela
 * (canais, filmes ou séries): abre o teclado de letras e, ao buscar, a tela de resultados
 * (SearchActivity). A lista de categorias NÃO tem mais a linha "Pesquisar": a posição da lista
 * é o índice em catNames (onde o índice 0 é "Todas as categorias").
 *
 * Controle parental (quando ligado): categorias adultas mostram um cadeado e pedem a senha de
 * 4 números. A liberação vale só para aquela categoria, enquanto a pessoa está nela. Em "Todas as
 * categorias" e nas outras, itens adultos não aparecem.
 *
 * Canal toca direto pelo endereço da lista (MainActivity.EXTRA_URL). O player pode trocar de
 * canal com cima e baixo (R4): esta tela manda EXTRA_QUEUE = true e o grupo aberto
 * (EXTRA_QUEUE_CAT; nada = todas as categorias).
 * Filme: abre a página do filme (MovieActivity), que leva o endereço e a capa do item.
 * Série: abre a tela da série da lista M3U (M3uSeriesActivity), com temporadas e episódios.
 */
public class M3uBrowseActivity extends Activity {

    private static final int PAGE = 40;
    private static final int MAX_ITEMS = 400;
    private static final int LOAD_AHEAD = 12;
    private static final int COLOR_NEUTRAL = 0xFFA39A9D;
    private static final int COLOR_ERROR = 0xFFFF6B78;
    private static final String LOCK = "\uD83D\uDD12 ";

    private CredentialStore.Credentials creds;
    private XtreamApi.Kind kind;

    private TextView tvTitle;
    private TextView tvStatus;
    private TextView btnSearch;
    private ListView lvCategories;
    private GridView gvItems;

    // Nomes das categorias. A posição 0 é "Todas as categorias" (grupo null).
    private final List<String> catNames = new ArrayList<>();
    private final List<XtreamApi.Item> items = new ArrayList<>();
    private CategoryAdapter catAdapter;
    private ItemAdapter itemAdapter;

    private String currentGroup = null;
    private String currentName = "Todas as categorias";

    // Controle das páginas. "gen" muda a cada troca de categoria para ignorar respostas atrasadas.
    private int gen = 0;
    private boolean loading = false;
    private boolean ended = false;
    private boolean loadFailed = false;
    private boolean focusGridWhenReady = true;
    private boolean catsStarted = false;

    // Controle parental
    private boolean parentalOn = false;
    private String adultOkName = null;
    private PinDialog pinDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        creds = CredentialStore.load(this);
        try {
            kind = XtreamApi.Kind.valueOf(getIntent().getStringExtra(HomeActivity.EXTRA_KIND));
        } catch (Exception e) {
            kind = null;
        }
        if (creds == null || !CredentialStore.MODE_M3U.equals(creds.mode)
                || creds.m3uUrl == null || kind == null) {
            finish();
            return;
        }

        parentalOn = ParentalStore.isEnabled(this);

        setContentView(R.layout.activity_browse);
        tvTitle = findViewById(R.id.tv_title);
        tvStatus = findViewById(R.id.tv_status);
        btnSearch = findViewById(R.id.btn_search_top);
        lvCategories = findViewById(R.id.lv_categories);
        gvItems = findViewById(R.id.gv_items);

        lvCategories.setDividerHeight(dp(6));
        gvItems.setHorizontalSpacing(dp(10));
        gvItems.setVerticalSpacing(dp(10));

        // Botão "Pesquisar" fixo: sempre à vista, acima da lista de categorias (R4-1).
        // Foco: do botão, baixo vai à lista; do alto da lista, cima volta ao botão.
        btnSearch.setVisibility(View.VISIBLE);
        btnSearch.setNextFocusDownId(R.id.lv_categories);
        lvCategories.setNextFocusUpId(R.id.btn_search_top);
        btnSearch.setOnClickListener(v -> openSearch());

        // Menu à esquerda (ícone e nome)
        NavBar.bind(this, navCurrent(), () -> refreshCurrent());

        catNames.add("Todas as categorias");
        catAdapter = new CategoryAdapter();
        itemAdapter = new ItemAdapter();
        lvCategories.setAdapter(catAdapter);
        gvItems.setAdapter(itemAdapter);

        // Posição da lista = índice da categoria em catNames
        lvCategories.setOnItemClickListener((parent, view, position, id) -> onCategoryClick(position));
        gvItems.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < items.size()) {
                openItem(items.get(position));
            }
        });
        gvItems.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // nada a fazer
            }

            @Override
            public void onScroll(AbsListView view, int first, int visible, int total) {
                if (total > 0 && first + visible >= total - LOAD_AHEAD) {
                    loadPage();
                }
            }
        });

        selectCategory(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pinDialog != null) {
            pinDialog.dismiss();
            pinDialog = null;
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ImageLoader.clear();
    }

    // ---------- Teclas: ordem do foco ----------

    /**
     * Com o foco na grade de itens, esquerda na primeira coluna leva o foco às categorias
     * (ordem: menu, Pesquisar, categorias, itens). Em qualquer outro lugar, as teclas funcionam
     * como antes.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT
                && gvItems != null && gvItems.hasFocus()) {
            int position = gvItems.getSelectedItemPosition();
            int columns = Math.max(1, gvItems.getNumColumns());
            if (position >= 0 && position % columns == 0) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    goToCategories();
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /** Leva o foco à lista de categorias, já na categoria que está aberta. */
    private void goToCategories() {
        int position = 0;
        if (currentGroup != null) {
            int found = catNames.indexOf(currentGroup);
            if (found > 0) {
                position = found;
            }
        }
        lvCategories.requestFocus();
        lvCategories.setSelection(position);
    }

    // ---------- Menu à esquerda ----------

    /** Diz ao menu em qual aba esta tela está. */
    private int navCurrent() {
        switch (kind) {
            case MOVIE:
                return NavBar.MOVIE;
            case SERIES:
                return NavBar.SERIES;
            default:
                return NavBar.LIVE;
        }
    }

    /** Atualizar: recarrega a categoria aberta (a lista M3U não tem guia para limpar). */
    private void refreshCurrent() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        int position = 0;
        if (currentGroup != null) {
            int found = catNames.indexOf(currentGroup);
            if (found > 0) {
                position = found;
            }
        }
        selectCategory(position);
    }

    // ---------- Pesquisa (só o tipo desta tela) ----------

    private String keyboardTitle() {
        switch (kind) {
            case MOVIE:
                return "Buscar filmes";
            case SERIES:
                return "Buscar séries";
            default:
                return "Buscar canais";
        }
    }

    /** Abre o teclado de letras. Ao buscar, abre a tela de resultados só deste tipo. */
    private void openSearch() {
        final XtreamApi.Kind k = kind;
        SearchKeyboard.show(this, keyboardTitle(), new SearchKeyboard.Listener() {
            @Override
            public void onSearch(String text) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                Intent i = new Intent(M3uBrowseActivity.this, SearchActivity.class);
                i.putExtra(HomeActivity.EXTRA_KIND, k.name());
                i.putExtra(SearchActivity.EXTRA_QUERY, text);
                startActivity(i);
            }

            @Override
            public void onCancel() {
                // saiu sem buscar: nada muda
            }
        });
    }

    // ---------- Categorias ----------

    /** Lê a lista uma vez para achar os nomes das categorias. Só os nomes ficam na memória. */
    private void loadCategories() {
        if (catsStarted) {
            return;
        }
        catsStarted = true;
        final String url = creds.m3uUrl;
        final XtreamApi.Kind k = kind;
        new Thread(() -> {
            List<String> result = null;
            try {
                result = M3uApi.categories(url, k);
            } catch (Exception e) {
                // sem rede ou lista estranha: segue só com "Todas as categorias"
            }
            final List<String> res = result;
            runOnUiThread(() -> onCategories(res));
        }).start();
    }

    private void onCategories(List<String> res) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (res == null) {
            Toast.makeText(this, "Não foi possível carregar as categorias.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        catNames.addAll(res);
        catAdapter.notifyDataSetChanged();
    }

    /** A pessoa escolheu uma categoria (índice em catNames). Se for adulta e trancada, pede a senha. */
    private void onCategoryClick(int position) {
        if (position < 0 || position >= catNames.size()) {
            return;
        }
        String name = catNames.get(position);
        boolean needPin = parentalOn && position > 0 && ParentalStore.isAdultCategory(name)
                && !name.equals(adultOkName);
        if (!needPin) {
            selectCategory(position);
            return;
        }
        askPin(position);
    }

    private void askPin(final int position) {
        pinDialog = PinDialog.show(this, "Senha do controle parental",
                catNames.get(position), new PinDialog.Listener() {
                    @Override
                    public void onPin(PinDialog d, String pin) {
                        long wait = ParentalStore.lockedSeconds(M3uBrowseActivity.this);
                        if (wait > 0) {
                            d.setMessage("Muitas tentativas. Aguarde " + wait + " segundos.", true);
                            return;
                        }
                        if (ParentalStore.check(M3uBrowseActivity.this, pin)) {
                            d.dismiss();
                            adultOkName = catNames.get(position);
                            selectCategory(position);
                            return;
                        }
                        wait = ParentalStore.lockedSeconds(M3uBrowseActivity.this);
                        if (wait > 0) {
                            d.setMessage("Muitas tentativas. Aguarde " + wait + " segundos.", true);
                        } else {
                            d.setMessage("Senha incorreta.", true);
                        }
                    }

                    @Override
                    public void onCancel() {
                        // saiu sem digitar a senha: continua na categoria de antes
                    }
                });
    }

    private void selectCategory(int position) {
        if (position < 0 || position >= catNames.size()) {
            return;
        }
        currentGroup = position == 0 ? null : catNames.get(position);
        currentName = catNames.get(position);

        // A liberação da senha vale só para a categoria liberada
        if (adultOkName != null && !adultOkName.equals(currentGroup)) {
            adultOkName = null;
        }

        gen++;
        items.clear();
        itemAdapter.notifyDataSetChanged();
        loading = false;
        ended = false;
        loadFailed = false;
        focusGridWhenReady = true;

        tvTitle.setText(position == 0 ? kindLabel() : kindLabel() + " - " + currentName);
        showStatus("Carregando...", COLOR_NEUTRAL);
        loadPage();
    }

    private String kindLabel() {
        switch (kind) {
            case MOVIE:
                return "Filmes";
            case SERIES:
                return "Séries";
            default:
                return "Canais ao vivo";
        }
    }

    // ---------- Itens, por páginas ----------

    private void loadPage() {
        if (loading || ended || loadFailed) {
            return;
        }
        if (items.size() >= MAX_ITEMS) {
            ended = true;
            return;
        }
        loading = true;
        final int myGen = gen;
        final int offset = items.size();
        final int want = Math.min(PAGE, MAX_ITEMS - items.size());
        final String group = currentGroup;
        // Itens adultos ficam escondidos, menos dentro da categoria adulta liberada com a senha
        final boolean skipAdult = parentalOn && !(group != null && group.equals(adultOkName));
        final String url = creds.m3uUrl;
        final XtreamApi.Kind k = kind;
        new Thread(() -> {
            List<XtreamApi.Item> result = null;
            try {
                result = M3uApi.page(url, k, group, offset, want, skipAdult);
            } catch (Exception e) {
                // sem rede, lista fora do ar ou resposta estranha
            }
            final List<XtreamApi.Item> res = result;
            runOnUiThread(() -> onPage(myGen, res, want));
        }).start();
    }

    private void onPage(int myGen, List<XtreamApi.Item> res, int want) {
        if (isFinishing() || isDestroyed() || myGen != gen) {
            return; // tela fechada ou categoria já trocada: ignora a resposta atrasada
        }
        loading = false;

        if (res == null) {
            loadFailed = true;
            if (items.isEmpty()) {
                showStatus("Não foi possível carregar. Confira a internet e a lista.", COLOR_ERROR);
            } else {
                Toast.makeText(this, "Não foi possível carregar mais itens.",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        items.addAll(res);
        if (res.size() < want) {
            ended = true;
        } else if (items.size() >= MAX_ITEMS) {
            ended = true;
            Toast.makeText(this, "Mostrando os primeiros " + MAX_ITEMS
                    + ". Escolha uma categoria para ver mais.", Toast.LENGTH_LONG).show();
        }
        itemAdapter.notifyDataSetChanged();

        // Com a primeira página na tela, procura as categorias (uma vez só)
        loadCategories();

        if (items.isEmpty()) {
            showStatus("Nada encontrado nesta categoria.", COLOR_NEUTRAL);
            return;
        }
        tvStatus.setVisibility(View.GONE);
        if (focusGridWhenReady) {
            focusGridWhenReady = false;
            gvItems.setSelection(0);
            gvItems.requestFocus();
        }
    }

    private void openItem(XtreamApi.Item item) {
        if (item.kind == XtreamApi.Kind.SERIES) {
            // Lista M3U: a série é achada pelo título
            Intent s = new Intent(this, M3uSeriesActivity.class);
            s.putExtra(HomeActivity.EXTRA_ID, item.id);
            s.putExtra(HomeActivity.EXTRA_NAME, item.name);
            startActivity(s);
            return;
        }
        // Filme: abre a página do filme (capa do item). Canal ao vivo: toca direto.
        Intent i = new Intent(this,
                item.kind == XtreamApi.Kind.MOVIE ? MovieActivity.class : MainActivity.class);
        i.putExtra(HomeActivity.EXTRA_KIND, item.kind.name());
        i.putExtra(HomeActivity.EXTRA_ID, item.id);
        i.putExtra(HomeActivity.EXTRA_EXT, item.ext);
        i.putExtra(HomeActivity.EXTRA_NAME, item.name);
        i.putExtra(MainActivity.EXTRA_URL, item.url);
        if (item.kind == XtreamApi.Kind.MOVIE) {
            i.putExtra(MovieActivity.EXTRA_ICON, item.icon);
        }
        if (item.kind == XtreamApi.Kind.LIVE) {
            // R4: o player troca de canal com cima e baixo, a partir do grupo aberto.
            // Em "Todas as categorias" (currentGroup nulo) não se manda grupo.
            i.putExtra(MainActivity.EXTRA_QUEUE, true);
            if (currentGroup != null) {
                i.putExtra(MainActivity.EXTRA_QUEUE_CAT, currentGroup);
            }
        }
        startActivity(i);
    }

    // ---------- Ajudas ----------

    private void showStatus(String text, int color) {
        tvStatus.setTextColor(color);
        tvStatus.setText(text);
        tvStatus.setVisibility(View.VISIBLE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String firstLetter(String name) {
        String s = name == null ? "" : name.trim();
        if (s.isEmpty()) {
            return "?";
        }
        return new String(Character.toChars(s.codePointAt(0))).toUpperCase();
    }

    // ---------- Adaptadores (reaproveitam as linhas ao rolar) ----------

    /** Posição da lista = índice da categoria em catNames (0 = Todas as categorias). */
    private final class CategoryAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return catNames.size();
        }

        @Override
        public Object getItem(int position) {
            return catNames.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv;
            if (convertView == null) {
                tv = (TextView) LayoutInflater.from(M3uBrowseActivity.this)
                        .inflate(R.layout.item_category, parent, false);
                // Quem controla a seleção e o clique é a própria lista.
                tv.setFocusable(false);
                tv.setClickable(false);
            } else {
                tv = (TextView) convertView;
            }
            String name = catNames.get(position);
            boolean locked = parentalOn && position > 0 && ParentalStore.isAdultCategory(name);
            tv.setText(locked ? LOCK + name : name);
            return tv;
        }
    }

    private final class ItemAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View card = convertView;
            if (card == null) {
                card = LayoutInflater.from(M3uBrowseActivity.this)
                        .inflate(R.layout.item_card, parent, false);
                card.setFocusable(false);
                card.setClickable(false);
            }
            XtreamApi.Item item = items.get(position);
            ((TextView) card.findViewById(R.id.tv_name)).setText(item.name);
            ((TextView) card.findViewById(R.id.tv_initial)).setText(firstLetter(item.name));
            ImageLoader.load((ImageView) card.findViewById(R.id.iv_logo), item.icon);

            // Lista M3U: ainda sem guia de programação
            TextView now = card.findViewById(R.id.tv_now);
            now.setTag(null);
            now.setVisibility(View.GONE);
            return card;
        }
    }
}
