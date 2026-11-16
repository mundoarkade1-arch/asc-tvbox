package com.ascplay.tvbox;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Aba Ajustes no visual "Glass": menu flutuante à esquerda (SideRail) e, ao lado, cards de
 * vidro com as opções em grade (4 por linha, quebra a linha). OK num card abre a opção: as que
 * têm conteúdo (Listas, Atualização, Sobre...) abrem um painel de vidro com botão Voltar;
 * Controle parental e Apagar dados abrem a janela de ação direto.
 * Tela montada em código (como a Home), com o fundo da imagem 30% mais escuro (lista).
 *
 * Opções:
 * - Listas: o app aceita VÁRIAS listas salvas, mas carrega só a selecionada. OK em uma lista:
 *   usar, remover ou voltar. "Adicionar lista" abre a tela de login. (CredentialStore guarda.)
 * - Controle parental: ativar, trocar a senha ou desativar.
 * - Atualização: procura uma versão nova. Se achar, OK instala.
 * - Sobre o dispositivo: modelo, Android, memória, espaço livre e tela.
 * - Sobre o aplicativo: versão instalada.
 * - Apagar dados: limpa histórico, progresso e caches; preserva as listas e o controle parental.
 *
 * Foco: seta para a direita no menu volta à última opção; seta para a direita numa opção entra
 * nos cards do painel (quando há, como em Listas).
 */
public class SettingsActivity extends AscActivity {

    private static final int ROW_LISTS = 0;
    private static final int ROW_PARENTAL = 1;
    private static final int ROW_UPDATE = 2;
    private static final int ROW_DEVICE = 3;
    private static final int ROW_APP = 4;
    private static final int ROW_CLEAR_DATA = 5;
    private static final int ROW_COUNT = 6;

    /** Cards por linha na grade de opções. */
    private static final int OPTION_COLS = 4;

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_NEUTRAL = 0xFFB8C0CC;
    private static final int COLOR_ERROR = 0xFFF28B82;
    private static final int COLOR_OK = 0xFF5AA2FF;

    /** Card de opção (coluna do meio). */
    private static final class OptionCard {
        GlassSurface view;
        TextView sub;
    }

    private SideRail rail;
    private final OptionCard[] cards = new OptionCard[ROW_COUNT];
    private GlassSurface panel;
    private ScrollView optScroll;
    private GlassSurface.Button backButton;
    private boolean panelOpen = false;
    private TextView panelTitle;
    private LinearLayout panelBody;
    private ScrollView panelScroll;
    private int currentRow = -1;
    private View lastOptionFocus;

    private PinDialog pinDialog;

    // Atualização
    private Updater.Info pendingUpdate;
    private boolean checking = false;
    private boolean downloading = false;
    private String updateText = null;
    private int updateColor = COLOR_WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (CredentialStore.load(this) == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        buildUi();

        // Menu à esquerda: Início, Ao vivo, Filmes e Séries levam à tela certa. Aqui não há o
        // que atualizar, então o botão Atualizar fica escondido.
        NavBar.bindRail(this, rail, NavBar.SETTINGS);
        rail.setOnRight(() -> {
            View target = panelOpen ? backButton
                    : (lastOptionFocus != null ? lastOptionFocus : cards[0].view);
            target.requestFocus();
        });

        cards[ROW_LISTS].view.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cards[0] != null) {
            refreshSubtitles();
        }
    }

    /** Voltar: se um painel está aberto, fecha o painel e volta à grade de opções. */
    @Override
    public void onBackPressed() {
        if (panelOpen) {
            closePanel();
            return;
        }
        super.onBackPressed();
    }

    private void openPanel(int row) {
        panelOpen = true;
        optScroll.setVisibility(View.GONE);
        panel.setVisibility(View.VISIBLE);
        showPanel(row, true);
        if (row == ROW_UPDATE) {
            if (pendingUpdate == null && updateText == null) {
                checkOrInstallUpdate();
            }
            return; // o botão do painel de atualização pega o foco
        }
        View first = firstFocusableInPanel();
        (first != null ? first : backButton).requestFocus();
    }

    private void closePanel() {
        panelOpen = false;
        panel.setVisibility(View.GONE);
        optScroll.setVisibility(View.VISIBLE);
        refreshSubtitles();
        View back = currentRow >= 0 && currentRow < ROW_COUNT ? cards[currentRow].view
                : cards[0].view;
        back.requestFocus();
    }

    private View firstFocusableInPanel() {
        for (int i = 0; i < panelBody.getChildCount(); i++) {
            View c = panelBody.getChildAt(i);
            if (c.isFocusable()) {
                return c;
            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pinDialog != null) {
            pinDialog.dismiss();
            pinDialog = null;
        }
    }

    // ---------- Montagem da tela ----------

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        // fundo: a imagem do app, 30% mais escura (tela com lista)
        root.addView(new GlassBackground(this, 0.30f), new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.HORIZONTAL);
        main.setClipChildren(false);
        root.addView(main, new FrameLayout.LayoutParams(MATCH, MATCH));

        // Menu lateral flutuante (igual ao da Home), com Ajustes selecionado
        rail = new SideRail(this);
        rail.home.setSelected(false);
        rail.settings.setSelected(true);
        rail.hideRefresh();
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(GlassKit.dp(this, 76), MATCH);
        rl.leftMargin = GlassKit.dp(this, 16);
        rl.topMargin = GlassKit.dp(this, 16);
        rl.bottomMargin = GlassKit.dp(this, 16);
        main.addView(rail, rl);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        column.setClipToPadding(false);
        column.setPadding(GlassKit.dp(this, 16), GlassKit.dp(this, 20),
                GlassKit.dp(this, 24), GlassKit.dp(this, 16));
        main.addView(column, new LinearLayout.LayoutParams(0, MATCH, 1f));

        column.addView(titleRow("Ajustes", 24), new LinearLayout.LayoutParams(MATCH, WRAP));

        FrameLayout body = new FrameLayout(this);
        body.setClipChildren(false);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(MATCH, 0, 1f);
        bl.topMargin = GlassKit.dp(this, 10);
        column.addView(body, bl);

        // Grade de opções (cards de vidro, OPTION_COLS por linha)
        optScroll = new ScrollView(this);
        optScroll.setFocusable(false);
        optScroll.setVerticalScrollBarEnabled(false);
        optScroll.setClipChildren(false);
        optScroll.setClipToPadding(false);
        optScroll.setPadding(GlassKit.dp(this, 4), GlassKit.dp(this, 8),
                GlassKit.dp(this, 4), GlassKit.dp(this, 8));
        LinearLayout optList = new LinearLayout(this);
        optList.setOrientation(LinearLayout.VERTICAL);
        optList.setClipChildren(false);
        optScroll.addView(optList, new FrameLayout.LayoutParams(MATCH, WRAP));
        body.addView(optScroll, new FrameLayout.LayoutParams(MATCH, MATCH));

        String[] titles = {"Listas", "Controle parental", "Atualização", "Sobre o dispositivo",
                "Sobre o aplicativo", "Apagar dados"};
        LinearLayout gridRow = null;
        for (int i = 0; i < ROW_COUNT; i++) {
            if (i % OPTION_COLS == 0) {
                gridRow = new LinearLayout(this);
                gridRow.setOrientation(LinearLayout.HORIZONTAL);
                gridRow.setClipChildren(false);
                optList.addView(gridRow, new LinearLayout.LayoutParams(MATCH, WRAP));
            }
            cards[i] = makeOption(gridRow, i, titles[i]);
        }
        // espaços vazios para os cards da última linha ficarem do mesmo tamanho
        for (int i = ROW_COUNT; i % OPTION_COLS != 0; i++) {
            View gap = new View(this);
            LinearLayout.LayoutParams gl = new LinearLayout.LayoutParams(
                    0, GlassKit.dp(this, 1), 1f);
            gl.leftMargin = GlassKit.dp(this, 8);
            gl.rightMargin = GlassKit.dp(this, 8);
            gridRow.addView(gap, gl);
        }
        refreshSubtitles();
        linkOptionCards();

        // Painel de vidro com o conteúdo (aparece por cima da grade, com botão Voltar)
        panel = new GlassSurface(this, GlassKit.dp(this, 20), false,
                GlassSurface.STYLE_GLASS, true);
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(MATCH, MATCH);
        pl.topMargin = GlassKit.dp(this, 8);
        pl.bottomMargin = GlassKit.dp(this, 8);
        panel.setVisibility(View.GONE);
        body.addView(panel, pl);

        LinearLayout panelCol = new LinearLayout(this);
        panelCol.setOrientation(LinearLayout.VERTICAL);
        panelCol.setClipChildren(false);
        panelCol.setPadding(GlassKit.dp(this, 20), GlassKit.dp(this, 18),
                GlassKit.dp(this, 20), GlassKit.dp(this, 14));
        panel.addView(panelCol, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout panelHead = titleRow("", 20);
        panelTitle = (TextView) panelHead.getChildAt(1);
        View headGap = new View(this);
        panelHead.addView(headGap, new LinearLayout.LayoutParams(0, 1, 1f));
        backButton = new GlassSurface.Button(this, "\u25C0  Voltar", false);
        backButton.setOnClickListener(v -> closePanel());
        backButton.setNextFocusUpId(GlassKit.ensureId(backButton));
        backButton.setNextFocusRightId(GlassKit.ensureId(backButton));
        backButton.setNextFocusLeftId(GlassKit.ensureId(rail.settings));
        panelHead.addView(backButton, new LinearLayout.LayoutParams(WRAP, GlassKit.dp(this, 38)));
        panelCol.addView(panelHead, new LinearLayout.LayoutParams(MATCH, WRAP));

        panelScroll = new ScrollView(this);
        panelScroll.setFocusable(false);
        panelScroll.setVerticalScrollBarEnabled(false);
        panelScroll.setClipChildren(false);
        panelScroll.setClipToPadding(false);
        panelScroll.setPadding(GlassKit.dp(this, 8), GlassKit.dp(this, 6),
                GlassKit.dp(this, 8), GlassKit.dp(this, 6));
        panelBody = new LinearLayout(this);
        panelBody.setOrientation(LinearLayout.VERTICAL);
        panelBody.setClipChildren(false);
        panelScroll.addView(panelBody, new FrameLayout.LayoutParams(MATCH, WRAP));
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(MATCH, 0, 1f);
        sl.topMargin = GlassKit.dp(this, 8);
        panelCol.addView(panelScroll, sl);

        setContentView(root);
    }

    /** Título com o traço dourado ao lado (igual às faixas da Home). */
    private LinearLayout titleRow(String text, float sizeSp) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false);

        View mark = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setColor(GlassKit.alpha(this, R.color.asc_gold, 0.90f));
        g.setCornerRadius(GlassKit.dp(this, 2));
        mark.setBackground(g);
        LinearLayout.LayoutParams ml = new LinearLayout.LayoutParams(
                GlassKit.dp(this, 3), GlassKit.dp(this, sizeSp));
        ml.leftMargin = GlassKit.dp(this, 6);
        ml.rightMargin = GlassKit.dp(this, 8);
        row.addView(mark, ml);

        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(GlassKit.sp(sizeSp));
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        t.setTextColor(GlassKit.color(this, R.color.asc_text_primary));
        t.setSingleLine(true);
        row.addView(t, new LinearLayout.LayoutParams(WRAP, WRAP));
        return row;
    }

    private OptionCard makeOption(LinearLayout parent, final int row, String title) {
        OptionCard oc = new OptionCard();
        GlassSurface s = new GlassSurface(this, GlassKit.dp(this, 18), true,
                GlassSurface.STYLE_GLASS, true);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setPadding(GlassKit.dp(this, 12), GlassKit.dp(this, 10),
                GlassKit.dp(this, 12), GlassKit.dp(this, 12));
        s.addView(inner, new FrameLayout.LayoutParams(MATCH, MATCH));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(GlassKit.sp(18));
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(GlassKit.color(this, R.color.asc_text_primary));
        t.setMaxLines(2);
        t.setEllipsize(TextUtils.TruncateAt.END);
        inner.addView(t, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView sub = new TextView(this);
        sub.setTextSize(GlassKit.sp(13));
        sub.setGravity(Gravity.CENTER);
        sub.setTextColor(GlassKit.color(this, R.color.asc_text_secondary));
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        subLp.topMargin = GlassKit.dp(this, 6);
        inner.addView(sub, subLp);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, GlassKit.dp(this, 110), 1f);
        lp.leftMargin = GlassKit.dp(this, 8);
        lp.rightMargin = GlassKit.dp(this, 8);
        lp.topMargin = GlassKit.dp(this, 8);
        lp.bottomMargin = GlassKit.dp(this, 8);
        parent.addView(s, lp);

        s.setOnClickListener(v -> onRowClick(row));
        GlassKit.focusScale(s, 1.04f, (v, hasFocus) -> {
            if (hasFocus) {
                lastOptionFocus = v;
                currentRow = row;
            }
        });

        oc.view = s;
        oc.sub = sub;
        return oc;
    }

    /**
     * Foco na grade: setas andam entre os cards (OPTION_COLS por linha); os extremos param.
     * Esquerda na primeira coluna volta ao menu.
     */
    private void linkOptionCards() {
        int railId = GlassKit.ensureId(rail.settings);
        for (int i = 0; i < ROW_COUNT; i++) {
            GlassSurface v = cards[i].view;
            int col = i % OPTION_COLS;
            GlassSurface up = i - OPTION_COLS >= 0 ? cards[i - OPTION_COLS].view : v;
            GlassSurface down = i + OPTION_COLS < ROW_COUNT ? cards[i + OPTION_COLS].view : v;
            GlassSurface right = col < OPTION_COLS - 1 && i + 1 < ROW_COUNT ? cards[i + 1].view : v;
            v.setNextFocusUpId(GlassKit.ensureId(up));
            v.setNextFocusDownId(GlassKit.ensureId(down));
            v.setNextFocusRightId(GlassKit.ensureId(right));
            v.setNextFocusLeftId(col > 0 ? GlassKit.ensureId(cards[i - 1].view) : railId);
        }
    }

    /** Texto pequeno embaixo do nome de cada opção. */
    private void refreshSubtitles() {
        int n = CredentialStore.lists(this).size();
        cards[ROW_LISTS].sub.setText(n == 1 ? "1 lista salva" : n + " listas salvas");
        cards[ROW_PARENTAL].sub.setText(ParentalStore.isEnabled(this) ? "Ativo" : "Desativado");
        cards[ROW_UPDATE].sub.setText(
                pendingUpdate != null ? "Nova versão disponível" : "Verificar agora");
        String model = Build.MODEL == null ? "" : Build.MODEL;
        cards[ROW_DEVICE].sub.setText(model.isEmpty() ? "Modelo e memória" : model);
        cards[ROW_APP].sub.setText("Versão instalada");
        cards[ROW_CLEAR_DATA].sub.setText("Histórico e progresso");
    }

    // ---------- Painel ----------

    private void showPanel(int row, boolean force) {
        if (row == currentRow && !force) {
            return;
        }
        currentRow = row;
        panelBody.removeAllViews();
        panelScroll.scrollTo(0, 0);
        switch (row) {
            case ROW_LISTS:
                panelTitle.setText("Listas");
                buildLists();
                break;
            case ROW_PARENTAL:
                panelTitle.setText("Controle parental");
                setPanelText(parentalText(), COLOR_WHITE);
                break;
            case ROW_UPDATE:
                panelTitle.setText("Atualização");
                setPanelText(updateText != null ? updateText : updateDefaultText(), updateColor);
                addUpdateButton();
                break;
            case ROW_DEVICE:
                panelTitle.setText("Sobre o dispositivo");
                setPanelText(deviceText(), COLOR_WHITE);
                break;
            case ROW_APP:
                panelTitle.setText("Sobre o aplicativo");
                setPanelText(appText(), COLOR_WHITE);
                break;
            default:
                panelTitle.setText("Apagar dados");
                setPanelText("Apaga o \"Continue assistindo\" e o que você já assistiu. "
                        + "As listas salvas e o controle parental não são apagados."
                        + "\n\nAperte OK para apagar.", COLOR_NEUTRAL);
                break;
        }
    }

    private void setPanelText(String text, int color) {
        panelBody.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(GlassKit.sp(15));
        tv.setLineSpacing(0f, 1.15f);
        panelBody.addView(tv, new LinearLayout.LayoutParams(MATCH, WRAP));
    }

    /** Botão do painel de Atualização: procurar de novo ou instalar. */
    private void addUpdateButton() {
        String label = checking || downloading ? "Aguarde..."
                : (pendingUpdate != null ? "Instalar agora" : "Procurar atualização");
        GlassSurface.Button b = new GlassSurface.Button(this, label, true);
        b.setOnClickListener(v -> checkOrInstallUpdate());
        b.setNextFocusUpId(GlassKit.ensureId(backButton));
        b.setNextFocusDownId(GlassKit.ensureId(b));
        b.setNextFocusLeftId(GlassKit.ensureId(b));
        b.setNextFocusRightId(GlassKit.ensureId(b));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, GlassKit.dp(this, 42));
        lp.topMargin = GlassKit.dp(this, 14);
        panelBody.addView(b, lp);
        backButton.setNextFocusDownId(GlassKit.ensureId(b));
        if (panelOpen) {
            b.requestFocus();
        }
    }

    private String parentalText() {
        return (ParentalStore.isEnabled(this)
                ? "O controle parental está ATIVO."
                : "O controle parental está desativado.")
                + "\n\nAperte OK para ativar, trocar a senha ou desativar.";
    }

    private String updateDefaultText() {
        if (pendingUpdate != null) {
            return getString(R.string.update_available, pendingUpdate.name)
                    + "\n\nAperte OK para instalar.";
        }
        return "Aperte OK para procurar uma versão nova do aplicativo.";
    }

    private void onRowClick(int row) {
        switch (row) {
            case ROW_PARENTAL:
                openParentalMenu();
                break;
            case ROW_CLEAR_DATA:
                confirmClearData();
                break;
            default:
                openPanel(row); // Listas, Atualização, Sobre o dispositivo, Sobre o aplicativo
                break;
        }
    }

    private void focusFirstInPanel() {
        for (int i = 0; i < panelBody.getChildCount(); i++) {
            View c = panelBody.getChildAt(i);
            if (c.isFocusable()) {
                c.requestFocus();
                return;
            }
        }
    }

    // ---------- Listas ----------

    private void buildLists() {
        TextView hint = new TextView(this);
        hint.setText("O app carrega só a lista selecionada. "
                + "Aperte OK em uma lista para usar ou remover.");
        hint.setTextSize(GlassKit.sp(13));
        hint.setTextColor(GlassKit.color(this, R.color.asc_text_secondary));
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(MATCH, WRAP);
        hl.bottomMargin = GlassKit.dp(this, 8);
        panelBody.addView(hint, hl);

        List<CredentialStore.SavedList> all = CredentialStore.lists(this);
        for (CredentialStore.SavedList l : all) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
            lp.topMargin = GlassKit.dp(this, 4);
            lp.bottomMargin = GlassKit.dp(this, 4);
            panelBody.addView(listCard(l), lp);
        }

        GlassSurface.Button add = new GlassSurface.Button(this, "+ Adicionar lista", true);
        add.setOnClickListener(v -> addList());
        LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(WRAP, GlassKit.dp(this, 40));
        al.topMargin = GlassKit.dp(this, 10);
        panelBody.addView(add, al);
        linkPanelCards();
    }

    /**
     * Ordem fixa do foco no painel de Listas: cima/baixo só entre os cards (os extremos
     * param) e esquerda volta à opção "Listas". A direita da opção "Listas" entra no 1º card.
     */
    private void linkPanelCards() {
        List<View> items = new ArrayList<>();
        for (int i = 0; i < panelBody.getChildCount(); i++) {
            View c = panelBody.getChildAt(i);
            if (c.isFocusable()) {
                items.add(c);
            }
        }
        int listsId = GlassKit.ensureId(backButton);
        for (int i = 0; i < items.size(); i++) {
            View v = items.get(i);
            View up = i > 0 ? items.get(i - 1) : backButton;
            View down = i < items.size() - 1 ? items.get(i + 1) : v;
            v.setNextFocusUpId(GlassKit.ensureId(up));
            v.setNextFocusDownId(GlassKit.ensureId(down));
            v.setNextFocusLeftId(listsId);
            v.setNextFocusRightId(GlassKit.ensureId(v));
        }
        if (!items.isEmpty()) {
            backButton.setNextFocusDownId(GlassKit.ensureId(items.get(0)));
        }
    }

    private View listCard(final CredentialStore.SavedList l) {
        GlassSurface s = new GlassSurface(this, GlassKit.dp(this, 16), true,
                GlassSurface.STYLE_GLASS, true);
        s.setSelected(l.active);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(GlassKit.dp(this, 16), GlassKit.dp(this, 12),
                GlassKit.dp(this, 16), GlassKit.dp(this, 14));
        s.addView(row, new FrameLayout.LayoutParams(MATCH, WRAP));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        row.addView(texts, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView title = new TextView(this);
        title.setText("Lista " + l.number);
        title.setTextSize(GlassKit.sp(16));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setTextColor(GlassKit.color(this, R.color.asc_text_primary));
        title.setSingleLine(true);
        texts.addView(title, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView sub = new TextView(this);
        sub.setText(CredentialStore.describe(l.creds));
        sub.setTextSize(GlassKit.sp(12));
        sub.setTextColor(GlassKit.color(this, R.color.asc_text_secondary));
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(MATCH, WRAP);
        sl.topMargin = GlassKit.dp(this, 2);
        texts.addView(sub, sl);

        if (l.active) {
            TextView badge = new TextView(this);
            badge.setText("EM USO");
            badge.setTextSize(GlassKit.sp(11));
            badge.setTypeface(badge.getTypeface(), Typeface.BOLD);
            badge.setPadding(GlassKit.dp(this, 10), GlassKit.dp(this, 4),
                    GlassKit.dp(this, 10), GlassKit.dp(this, 4));
            GlassKit.chip(badge, GlassKit.dp(this, 10), R.color.asc_accent, 0.60f,
                    R.color.asc_text_primary);
            LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(WRAP, WRAP);
            bl.leftMargin = GlassKit.dp(this, 10);
            row.addView(badge, bl);
        }

        s.setOnClickListener(v -> openListMenu(l));
        GlassKit.focusScale(s, 1.02f, null);
        return s;
    }

    private void openListMenu(final CredentialStore.SavedList l) {
        final String use = "Usar esta lista";
        final String remove = "Remover esta lista";
        final String back = "Voltar";
        final String[] items = l.active
                ? new String[]{remove, back}
                : new String[]{use, remove, back};
        new AlertDialog.Builder(this, R.style.AscDialog)
                .setTitle("Lista " + l.number + (l.active ? " (em uso)" : ""))
                .setItems(items, (d, which) -> {
                    String chosen = items[which];
                    if (use.equals(chosen)) {
                        useList(l);
                    } else if (remove.equals(chosen)) {
                        confirmRemove(l);
                    }
                })
                .show();
    }

    /** Passa a usar a lista e reabre a Home do zero, para carregar só ela. */
    private void useList(CredentialStore.SavedList l) {
        if (!CredentialStore.select(this, l.id)) {
            Toast.makeText(this, "Essa lista não existe mais.", Toast.LENGTH_SHORT).show();
            showPanel(ROW_LISTS, true);
            return;
        }
        Toast.makeText(this, "Lista " + l.number + " selecionada. Carregando...",
                Toast.LENGTH_SHORT).show();
        restartHome();
    }

    private void confirmRemove(final CredentialStore.SavedList l) {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AscDialog)
                .setMessage("Remover a Lista " + l.number + "? Para usar de novo, será preciso "
                        + "adicionar a lista outra vez.")
                .setNegativeButton("Cancelar", (d, which) -> d.dismiss())
                .setPositiveButton("Remover", (d, which) -> removeList(l))
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus());
        dialog.show();
    }

    private void removeList(CredentialStore.SavedList l) {
        CredentialStore.remove(this, l.id);
        if (CredentialStore.load(this) == null) {
            // não sobrou nenhuma lista: volta para a tela de login
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }
        if (l.active) {
            // a lista em uso mudou: reabre a Home para carregar a nova
            Toast.makeText(this, "Lista removida.", Toast.LENGTH_SHORT).show();
            restartHome();
            return;
        }
        Toast.makeText(this, "Lista removida.", Toast.LENGTH_SHORT).show();
        refreshSubtitles();
        showPanel(ROW_LISTS, true);
        focusFirstInPanel();
    }

    private void addList() {
        Intent i = new Intent(this, LoginActivity.class);
        i.putExtra(HomeActivity.EXTRA_SHOW_LOGIN, true);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void restartHome() {
        Intent home = new Intent(this, HomeActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(home);
        finish();
    }

    // ---------- Apagar dados ----------

    private void confirmClearData() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AscDialog)
                .setMessage("Isso apaga tudo: continuar assistindo, assistidos e favoritos. "
                        + "Deseja continuar?")
                .setNegativeButton("Cancelar", (d, which) -> d.dismiss())
                .setPositiveButton("Apagar", (d, which) -> clearUsageData())
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus());
        dialog.show();
    }

    private void clearUsageData() {
        ContinueStore.clear(this);
        PlaybackProgress.clear(this);
        SeriesProgress.clear(this);
        SearchIndex.clear(this);
        ImageLoader.clear();
        EpgCache.clear();

        Toast.makeText(this, "Dados de uso apagados.", Toast.LENGTH_SHORT).show();
        Intent home = new Intent(this, HomeActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(home);
        finish();
    }

    // ---------- Sobre o dispositivo e o aplicativo ----------

    private String deviceText() {
        StringBuilder sb = new StringBuilder();
        String maker = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER;
        String model = Build.MODEL == null ? "" : Build.MODEL;
        sb.append("Aparelho: ").append((maker + " " + model).trim()).append("\n\n");
        sb.append("Android: ").append(Build.VERSION.RELEASE)
                .append(" (nível ").append(Build.VERSION.SDK_INT).append(")\n\n");

        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            sb.append("Memória (RAM): ").append(megaOrGiga(mi.totalMem)).append(" no total, ")
                    .append(megaOrGiga(mi.availMem)).append(" livres agora\n\n");
        } catch (Exception e) {
            sb.append("Memória (RAM): não foi possível ler\n\n");
        }

        try {
            StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
            long total = fs.getTotalBytes();
            long free = fs.getAvailableBytes();
            sb.append("Armazenamento: ").append(megaOrGiga(free)).append(" livres de ")
                    .append(megaOrGiga(total)).append("\n\n");
        } catch (Exception e) {
            sb.append("Armazenamento: não foi possível ler\n\n");
        }

        DisplayMetrics dm = getResources().getDisplayMetrics();
        sb.append("Tela: ").append(dm.widthPixels).append(" x ").append(dm.heightPixels)
                .append(" pixels");
        return sb.toString();
    }

    private String appText() {
        String name = "desconhecida";
        int code = 0;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (pi.versionName != null) {
                name = pi.versionName;
            }
            code = pi.versionCode;
        } catch (Exception e) {
            // segue com "desconhecida"
        }
        return "ASC Play\n\nVersão instalada: " + name + " (número " + code + ")";
    }

    // ---------- Atualização ----------

    /** Guarda a mensagem da atualização e, se o painel dela está aberto, mostra na hora. */
    private void showUpdate(String text, int color) {
        updateText = text;
        updateColor = color;
        if (panelOpen && currentRow == ROW_UPDATE) {
            showPanel(ROW_UPDATE, true);
        }
    }

    private void checkOrInstallUpdate() {
        if (checking || downloading) {
            return;
        }
        if (pendingUpdate != null) {
            startUpdate();
            return;
        }
        checking = true;
        showUpdate("Procurando atualização...", COLOR_WHITE);
        Updater.checkAsync(this, new Updater.CheckListener() {
            @Override
            public void onUpdateAvailable(Updater.Info info) {
                checking = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                pendingUpdate = info;
                refreshSubtitles();
                showUpdate(getString(R.string.update_available, info.name)
                        + "\n\nAperte OK para instalar.", COLOR_OK);
            }

            @Override
            public void onNoUpdate() {
                checking = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showUpdate("Nenhuma atualização encontrada agora.", COLOR_WHITE);
            }
        });
    }

    private void startUpdate() {
        if (downloading || pendingUpdate == null) {
            return;
        }
        downloading = true;
        showUpdate(getString(R.string.msg_downloading_unknown), COLOR_WHITE);

        Updater.downloadAndInstallAsync(this, pendingUpdate, new Updater.InstallListener() {
            @Override
            public void onProgress(int percent) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (percent >= 0) {
                    showUpdate(getString(R.string.msg_downloading, percent), COLOR_WHITE);
                } else {
                    showUpdate(getString(R.string.msg_downloading_unknown), COLOR_WHITE);
                }
            }

            @Override
            public void onInstallerOpened() {
                downloading = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showUpdate(getString(R.string.msg_installer_opened), COLOR_WHITE);
            }

            @Override
            public void onFailed(String message) {
                downloading = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showUpdate(message, COLOR_ERROR);
            }
        });
    }

    // ---------- Controle parental ----------

    private void openParentalMenu() {
        final boolean on = ParentalStore.isEnabled(this);
        AlertDialog.Builder b = new AlertDialog.Builder(this, R.style.AscDialog);
        if (on) {
            b.setTitle("Controle parental: ativo");
            b.setItems(new CharSequence[]{"Trocar senha", "Desativar controle parental", "Voltar"},
                    (dialog, which) -> {
                        if (which == 0) {
                            askCurrentPin("Digite a senha atual", () -> createPin());
                        } else if (which == 1) {
                            askCurrentPin("Digite a senha para desativar", () -> {
                                ParentalStore.disable(SettingsActivity.this);
                                Toast.makeText(SettingsActivity.this,
                                        "Controle parental desativado.",
                                        Toast.LENGTH_SHORT).show();
                                parentalChanged();
                            });
                        }
                    });
        } else {
            b.setTitle("Controle parental: desativado");
            b.setItems(new CharSequence[]{"Ativar controle parental", "Voltar"},
                    (dialog, which) -> {
                        if (which == 0) {
                            createPin();
                        }
                    });
        }
        b.show();
    }

    private void parentalChanged() {
        refreshSubtitles();
        // (o controle parental abre só a janela de ação: nada mais a refazer)
    }

    /** Pede a senha nova (4 números). Depois pede para repetir. */
    private void createPin() {
        pinDialog = PinDialog.show(this, "Crie a senha (4 números)", "", new PinDialog.Listener() {
            @Override
            public void onPin(PinDialog d, String pin) {
                d.dismiss();
                confirmPin(pin);
            }

            @Override
            public void onCancel() {
                // saiu sem criar: nada muda
            }
        });
    }

    private void confirmPin(final String first) {
        pinDialog = PinDialog.show(this, "Repita a senha", "", new PinDialog.Listener() {
            @Override
            public void onPin(PinDialog d, String pin) {
                d.dismiss();
                if (!first.equals(pin)) {
                    Toast.makeText(SettingsActivity.this,
                            "As senhas não são iguais. Tente de novo.",
                            Toast.LENGTH_SHORT).show();
                    createPin();
                    return;
                }
                if (ParentalStore.enable(SettingsActivity.this, pin)) {
                    Toast.makeText(SettingsActivity.this,
                            "Controle parental ativado.", Toast.LENGTH_SHORT).show();
                    parentalChanged();
                } else {
                    Toast.makeText(SettingsActivity.this,
                            "Não foi possível guardar a senha.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancel() {
                // saiu sem confirmar: nada muda
            }
        });
    }

    /** Pede a senha atual. Se estiver certa, fecha o teclado e roda "onOk". */
    private void askCurrentPin(String title, final Runnable onOk) {
        pinDialog = PinDialog.show(this, title, "", new PinDialog.Listener() {
            @Override
            public void onPin(PinDialog d, String pin) {
                long wait = ParentalStore.lockedSeconds(SettingsActivity.this);
                if (wait > 0) {
                    d.setMessage("Muitas tentativas. Aguarde " + wait + " segundos.", true);
                    return;
                }
                if (ParentalStore.check(SettingsActivity.this, pin)) {
                    d.dismiss();
                    onOk.run();
                    return;
                }
                wait = ParentalStore.lockedSeconds(SettingsActivity.this);
                if (wait > 0) {
                    d.setMessage("Muitas tentativas. Aguarde " + wait + " segundos.", true);
                } else {
                    d.setMessage("Senha incorreta.", true);
                }
            }

            @Override
            public void onCancel() {
                // saiu sem digitar: nada muda
            }
        });
    }

    // ---------- Ajudas ----------

    private static String megaOrGiga(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);
        if (mb >= 1024) {
            return String.format(Locale.US, "%.1f GB", mb / 1024.0);
        }
        return String.format(Locale.US, "%d MB", Math.round(mb));
    }
}
