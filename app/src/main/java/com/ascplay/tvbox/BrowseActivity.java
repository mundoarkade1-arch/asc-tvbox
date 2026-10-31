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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Navegação por categorias ("Ver mais" da página inicial; também as abas Canais, Filmes e Séries
 * do menu à esquerda).
 * Esquerda: botão "Pesquisar" fixo e, abaixo, as categorias. Direita: itens, carregados por
 * páginas de 40, só quando a seleção chega perto do fim da grade (M12: teto de segurança de
 * 5000 por categoria; antes era 400).
 * Nunca baixa a lista inteira: pede ao servidor só a página necessária.
 * Nos canais ao vivo, cada cartão mostra o programa que passa agora (EPG por canal).
 * Sobre um canal ao vivo, a tecla das três listras ou segurar o OK abre a janela com a
 * programação do canal (o que passa agora e os próximos programas).
 * Ao escolher uma série, abre a tela da série (temporadas e episódios), levando a capa do item.
 * Ao escolher um filme, abre a página do filme (MovieActivity), com capa e sinopse.
 * Ao escolher um canal ao vivo, o player pode trocar de canal com cima e baixo (R4): esta tela
 * manda EXTRA_QUEUE = true e a categoria aberta (EXTRA_QUEUE_CAT; nada = todas as categorias).
 *
 * Ordem do foco: menu, Pesquisar, categorias, itens. Esquerda na primeira coluna dos itens
 * leva o foco às categorias (Rodada 1, R3).
 *
 * Menu à esquerda (ícone e nome): ligado pelo NavBar. Troca de aba, volta à tela inicial e
 * recarrega a categoria aberta (Atualizar).
 *
 * Pesquisa (Rodada 4, R4-1): o botão "Pesquisar" (btn_search_top) fica FIXO acima da lista de
 * categorias, sempre à vista. No layout ele nasce escondido (a SearchActivity, que reaproveita
 * o mesmo layout, não o mostra); esta tela o mostra. OK nele pesquisa SÓ o tipo desta tela
 * (canais, filmes ou séries): abre o teclado de letras e, ao buscar, a tela de resultados
 * (SearchActivity). A lista de categorias NÃO tem mais a linha "Pesquisar": a posição da lista
 * é o índice em catIds/catNames.
 *
 * Controle parental (quando ligado): categorias adultas mostram um cadeado e pedem a senha
 * de 4 números para abrir. A liberação vale só para aquela categoria, enquanto a pessoa está
 * nela. Na lista "Todas as categorias", conteúdo adulto não aparece.
 */
public class BrowseActivity extends Activity {

    private static final int PAGE = 40;
    private static final int MAX_ITEMS = 5000;
    private static final int LOAD_AHEAD = 12;
    private static final int COLOR_NEUTRAL = 0xFFA99FA3;
    private static final int COLOR_ERROR = 0xFFFF6B78;
    private static final String LOCK = "\uD83D\uDD12 ";

    private CredentialStore.Credentials creds;
    private XtreamApi.Kind kind;

    private TextView tvTitle;
    private TextView tvStatus;
    private TextView btnSearch;
    private ListView lvCategories;
    private GridView gvItems;

    private final List<String> catIds = new ArrayList<>();
    private final List<String> catNames = new ArrayList<>();
    private final List<XtreamApi.Item> items = new ArrayList<>();
    private CategoryAdapter catAdapter;
    private ItemAdapter itemAdapter;

    // Programa de agora de cada canal já consultado nesta categoria (id do canal -> texto).
    private final Map<String, String> nowText = new HashMap<>();
    private final Set<String> nowAsked = new HashSet<>();

    private String currentCat = null;
    private String currentName = "Todas as categorias";

    // Controle das páginas. "gen" muda a cada troca de categoria para ignorar respostas atrasadas.
    private int gen = 0;
    private boolean loading = false;
    private boolean ended = false;
    private boolean loadFailed = false;
    private boolean focusGridWhenReady = true;
    // Quantos itens o servidor já entregou (antes do filtro do controle parental)
    private int rawLoaded = 0;

    // Controle parental
    private boolean parentalOn = false;
    private boolean catsDone = false;
    private final Set<String> adultCatIds = new HashSet<>();
    private String adultOkId = null;
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
        if (creds == null || !CredentialStore.MODE_XTREAM.equals(creds.mode) || kind == null) {
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

        catIds.add(null);
        catNames.add("Todas as categorias");
        catAdapter = new CategoryAdapter();
        itemAdapter = new ItemAdapter();
        lvCategories.setAdapter(catAdapter);
        gvItems.setAdapter(itemAdapter);

        // Posição da lista = índice da categoria em catIds/catNames
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

        loadCategories();
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
        EpgCache.clear();
    }

    // ---------- Teclas: ordem do foco e programação do canal ----------

    /**
     * Com o foco na grade de itens:
     * - esquerda na primeira coluna leva o foco às categorias (ordem: menu, Pesquisar,
     *   categorias, itens);
     * - com um canal ao vivo selecionado, a tecla das três listras (menu) abre a janela da
     *   programação do canal, e OK segurado faz o mesmo (segunda via).
     * Em qualquer outro lugar, as teclas funcionam como antes.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();

        if (code == KeyEvent.KEYCODE_DPAD_LEFT && gvItems != null && gvItems.hasFocus()) {
            int position = gvItems.getSelectedItemPosition();
            int columns = Math.max(1, gvItems.getNumColumns());
            if (position >= 0 && position % columns == 0) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    goToCategories();
                }
                return true;
            }
        }

        boolean okKey = code == KeyEvent.KEYCODE_DPAD_CENTER
                || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER;

        if (event.getAction() == KeyEvent.ACTION_DOWN
                && ((code == KeyEvent.KEYCODE_MENU && event.getRepeatCount() == 0)
                || (okKey && event.getRepeatCount() == 1))) {
            XtreamApi.Item selected = selectedLiveItem();
            if (selected != null) {
                gvItems.setPressed(false);
                openSchedule(selected);
                return true;
            }
        }
        if (code == KeyEvent.KEYCODE_MENU && selectedLiveItem() != null) {
            return true; // a tecla das três listras não faz mais nada aqui
        }
        return super.dispatchKeyEvent(event);
    }

    /** Leva o foco à lista de categorias, já na categoria que está aberta. */
    private void goToCategories() {
        int position = catIds.indexOf(currentCat);
        if (position < 0) {
            position = 0;
        }
        lvCategories.requestFocus();
        lvCategories.setSelection(position);
    }

    /** O canal ao vivo selecionado na grade, ou null se o foco está em outro lugar. */
    private XtreamApi.Item selectedLiveItem() {
        if (kind != XtreamApi.Kind.LIVE || gvItems == null || !gvItems.hasFocus()) {
            return null;
        }
        int position = gvItems.getSelectedItemPosition();
        if (position < 0 || position >= items.size()) {
            return null;
        }
        XtreamApi.Item item = items.get(position);
        return item.kind == XtreamApi.Kind.LIVE ? item : null;
    }

    /** Abre a janela da programação de um canal. */
    private void openSchedule(XtreamApi.Item item) {
        EpgDialog.show(this, creds.server, creds.user, creds.pass, item.id, item.name);
    }

    // ---------- Menu à esquerda ----------

    /** Qual aba do menu esta tela representa. */
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

    /** Atualizar: recarrega a categoria que está aberta. */
    private void refreshCurrent() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        EpgCache.clear();
        int position = catIds.indexOf(currentCat);
        if (position < 0) {
            position = 0;
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
                Intent i = new Intent(BrowseActivity.this, SearchActivity.class);
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

    private void loadCategories() {
        final String server = creds.server;
        final String user = creds.user;
        final String pass = creds.pass;
        final XtreamApi.Kind k = kind;
        new Thread(() -> {
            List<XtreamApi.Category> result = null;
            try {
                result = XtreamApi.categories(server, user, pass, k);
            } catch (Exception e) {
                // sem rede ou resposta estranha: segue só com "Todas as categorias"
            }
            final List<XtreamApi.Category> res = result;
            runOnUiThread(() -> onCategories(res));
        }).start();
    }

    private void onCategories(List<XtreamApi.Category> res) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (res == null) {
            Toast.makeText(this, "Não foi possível carregar as categorias.",
                    Toast.LENGTH_SHORT).show();
        } else {
            for (XtreamApi.Category c : res) {
                catIds.add(c.id);
                catNames.add(c.name);
                if (parentalOn && ParentalStore.isAdultCategory(c.name)) {
                    adultCatIds.add(c.id);
                }
            }
            catAdapter.notifyDataSetChanged();
        }
        catsDone = true;
        // Com o controle parental ligado, a primeira página esperou as categorias chegarem.
        if (items.isEmpty() && !loading && !ended && !loadFailed) {
            loadPage();
        }
    }

    /** A pessoa escolheu uma categoria (índice em catIds). Se for adulta e trancada, pede a senha. */
    private void onCategoryClick(int position) {
        if (position < 0 || position >= catIds.size()) {
            return;
        }
        String id = catIds.get(position);
        boolean needPin = parentalOn && id != null && adultCatIds.contains(id)
                && !id.equals(adultOkId);
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
                        long wait = ParentalStore.lockedSeconds(BrowseActivity.this);
                        if (wait > 0) {
                            d.setMessage("Muitas tentativas. Aguarde " + wait + " segundos.", true);
                            return;
                        }
                        if (ParentalStore.check(BrowseActivity.this, pin)) {
                            d.dismiss();
                            adultOkId = catIds.get(position);
                            selectCategory(position);
                            return;
                        }
                        wait = ParentalStore.lockedSeconds(BrowseActivity.this);
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
        if (position < 0 || position >= catIds.size()) {
            return;
        }
        currentCat = catIds.get(position);
        currentName = catNames.get(position);

        // A liberação da senha vale só para a categoria liberada
        if (adultOkId != null && !adultOkId.equals(currentCat)) {
            adultOkId = null;
        }

        gen++;
        items.clear();
        nowText.clear();
        nowAsked.clear();
        itemAdapter.notifyDataSetChanged();
        loading = false;
        ended = false;
        loadFailed = false;
        focusGridWhenReady = true;
        rawLoaded = 0;

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

    /** Este item pode aparecer? Com o controle parental ligado, adulto fica escondido. */
    private boolean allowed(XtreamApi.Item item) {
        if (!parentalOn) {
            return true;
        }
        if (adultOkId != null && adultOkId.equals(item.categoryId)) {
            return true;
        }
        if (adultCatIds.contains(item.categoryId)) {
            return false;
        }
        return !ParentalStore.isAdultName(item.name);
    }

    // ---------- Itens, por páginas ----------

    private void loadPage() {
        if (loading || ended || loadFailed) {
            return;
        }
        // Com o controle parental ligado, espera saber quais categorias são adultas
        if (parentalOn && !catsDone) {
            return;
        }
        if (items.size() >= MAX_ITEMS) {
            ended = true;
            return;
        }
        loading = true;
        final int myGen = gen;
        final int offset = rawLoaded;
        final int want = Math.min(PAGE, MAX_ITEMS - items.size());
        final String cat = currentCat;
        final String server = creds.server;
        final String user = creds.user;
        final String pass = creds.pass;
        final XtreamApi.Kind k = kind;
        new Thread(() -> {
            List<XtreamApi.Item> result = null;
            try {
                result = XtreamApi.items(server, user, pass, k, cat, offset, want);
            } catch (Exception e) {
                // sem rede, servidor fora do ar ou resposta estranha
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
                showStatus("Não foi possível carregar. Confira a internet e o servidor.", COLOR_ERROR);
            } else {
                Toast.makeText(this, "Não foi possível carregar mais itens.",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        rawLoaded += res.size();
        int added = 0;
        for (XtreamApi.Item it : res) {
            if (allowed(it)) {
                items.add(it);
                added++;
            }
        }
        if (res.size() < want) {
            ended = true;
        } else if (items.size() >= MAX_ITEMS) {
            ended = true;
            Toast.makeText(this, "Limite de " + MAX_ITEMS
                    + " itens atingido. Use o Pesquisar para achar o resto.",
                    Toast.LENGTH_LONG).show();
        }
        itemAdapter.notifyDataSetChanged();

        // A página veio toda escondida pelo controle parental: busca a próxima
        if (added == 0 && !ended) {
            loadPage();
            return;
        }

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
            Intent s = new Intent(this, SeriesActivity.class);
            s.putExtra(HomeActivity.EXTRA_ID, item.id);
            s.putExtra(HomeActivity.EXTRA_NAME, item.name);
            s.putExtra(MovieActivity.EXTRA_ICON, item.icon);
            startActivity(s);
            return;
        }
        // Filme: abre a página do filme (capa e sinopse). Canal ao vivo: toca direto.
        Intent i = new Intent(this,
                item.kind == XtreamApi.Kind.MOVIE ? MovieActivity.class : MainActivity.class);
        i.putExtra(HomeActivity.EXTRA_KIND, item.kind.name());
        i.putExtra(HomeActivity.EXTRA_ID, item.id);
        i.putExtra(HomeActivity.EXTRA_EXT, item.ext);
        i.putExtra(HomeActivity.EXTRA_NAME, item.name);
        if (item.kind == XtreamApi.Kind.MOVIE) {
            i.putExtra(MovieActivity.EXTRA_ICON, item.icon);
        }
        if (item.kind == XtreamApi.Kind.LIVE) {
            // R4: o player troca de canal com cima e baixo, a partir da categoria aberta.
            // Em "Todas as categorias" (currentCat nulo) não se manda categoria.
            i.putExtra(MainActivity.EXTRA_QUEUE, true);
            if (currentCat != null) {
                i.putExtra(MainActivity.EXTRA_QUEUE_CAT, currentCat);
            }
        }
        startActivity(i);
    }

    // ---------- Programa de agora (EPG) ----------

    /**
     * Escreve na linha do cartão o programa de agora. Usa o que já foi consultado;
     * se ainda não foi, pede ao servidor uma única vez por canal.
     */
    private void showNow(final XtreamApi.Item item, final TextView now) {
        now.setTag(item.id);
        now.setVisibility(View.VISIBLE);
        String known = nowText.get(item.id);
        if (known != null) {
            now.setText(known);
            return;
        }
        now.setText("");
        if (!nowAsked.add(item.id)) {
            return; // já pedido: a resposta vai preencher o cartão quando chegar
        }
        final int myGen = gen;
        EpgCache.get(creds.server, creds.user, creds.pass, item.id, epg -> {
            if (isFinishing() || isDestroyed() || myGen != gen) {
                return;
            }
            String text = epg == null ? "Programação indisponível" : "Agora: " + epg.title;
            nowText.put(item.id, text);
            if (item.id.equals(now.getTag())) {
                now.setText(text);
            }
        });
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

    /** Posição da lista = índice da categoria em catIds/catNames. */
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
                tv = (TextView) LayoutInflater.from(BrowseActivity.this)
                        .inflate(R.layout.item_category, parent, false);
                // Quem controla a seleção e o clique é a própria lista.
                tv.setFocusable(false);
                tv.setClickable(false);
            } else {
                tv = (TextView) convertView;
            }
            String id = catIds.get(position);
            boolean locked = parentalOn && id != null && adultCatIds.contains(id);
            tv.setText(locked ? LOCK + catNames.get(position) : catNames.get(position));
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
                card = LayoutInflater.from(BrowseActivity.this)
                        .inflate(R.layout.item_card, parent, false);
                card.setFocusable(false);
                card.setClickable(false);
            }
            XtreamApi.Item item = items.get(position);
            ((TextView) card.findViewById(R.id.tv_name)).setText(item.name);
            ((TextView) card.findViewById(R.id.tv_initial)).setText(firstLetter(item.name));
            ImageLoader.load((ImageView) card.findViewById(R.id.iv_logo), item.icon);

            TextView now = card.findViewById(R.id.tv_now);
            if (item.kind == XtreamApi.Kind.LIVE) {
                showNow(item, now);
            } else {
                now.setTag(null);
                now.setVisibility(View.GONE);
            }
            return card;
        }
    }
}
