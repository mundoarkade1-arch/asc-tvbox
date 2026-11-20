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
 * Aba Ajustes no visual "Glass": menu flutuante à esquerda (SideRail), uma lista simples de
 * opções (com títulos de seção) e um painel de vidro à direita com o conteúdo da opção em foco.
 * Tela montada em código (como a Home), com o fundo da imagem 30% mais escuro (lista).
 *
 * Opções:
 * - Listas: usar, remover ou adicionar listas. (CredentialStore guarda.)
 * - Categorias: esconde grupos de canais ao vivo da lista de categorias (AppPrefs).
 * - Reprodução: áudio e legenda preferidos e qualidade máxima (AppPrefs, usados pelo player).
 * - Controle parental: ativar, trocar a senha ou desativar.
 * - Dados e cache: limpa histórico, Minha lista, cache de imagens ou tudo.
 * - Atualização: procurar ao abrir (liga/desliga) e procurar/instalar agora.
 * - Diagnóstico: versão, aparelho, memória, espaço, tela e teste de conexão.
 * - Ajuda: dicas e contato (texto em res/values/asc_support.xml).
 *
 * Foco: seta para a direita numa opção entra no painel (as linhas do painel são botões, chaves
 * LIGADO/DESLIGADO ou escolhas que mudam a cada OK); esquerda volta à opção; esquerda na
 * opção volta ao menu.
 */
public class SettingsActivity extends AscActivity {

    private static final int ROW_LISTS = 0;
    private static final int ROW_CATEGORIES = 1;
    private static final int ROW_PLAYBACK = 2;
    private static final int ROW_PARENTAL = 3;
    private static final int ROW_DATA = 4;
    private static final int ROW_UPDATE = 5;
    private static final int ROW_DIAG = 6;
    private static final int ROW_HELP = 7;
    private static final int ROW_COUNT = 8;

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_NEUTRAL = 0xFFB8C0CC;
    private static final int COLOR_ERROR = 0xFFF28B82;
    private static final int COLOR_OK = 0xFF5AA2FF;

    /** Opção da lista da esquerda. */
    private static final class OptionCard {
        GlassSurface view;
        TextView sub;
        View bar;
    }

    /** Linha do painel da direita (botão, chave ou escolha). */
    private static final class Row {
        GlassSurface view;
        TextView title;
        TextView value;
    }

    private interface BoolSetter {
        void set(boolean on);
    }

    private interface IntSetter {
        void set(int index);
    }

    private SideRail rail;
    private final OptionCard[] cards = new OptionCard[ROW_COUNT];
    private TextView panelTitle;
    private LinearLayout panelBody;
    private ScrollView panelScroll;
    private int currentRow = -1;
    private View lastOptionFocus;

    private PinDialog pinDialog;
    private UpdateDialog updateDialog;

    // Atualização
    private Updater.Info pendingUpdate;
    private boolean checking = false;
    private boolean downloading = false;
    private String updateText = null;
    private int updateColor = COLOR_WHITE;
    private TextView updateStatusTv;
    private TextView updateActionTitle;

    // Diagnóstico
    private TextView diagConnTv;
    private boolean testing = false;

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
            View target = lastOptionFocus != null ? lastOptionFocus : cards[0].view;
            target.requestFocus();
        });

        showPanel(ROW_LISTS, true);
        cards[ROW_LISTS].view.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cards[0] != null) {
            refreshSubtitles();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pinDialog != null) {
            pinDialog.dismiss();
            pinDialog = null;
        }
        if (updateDialog != null) {
            updateDialog.dismiss();
            updateDialog = null;
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
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(GlassKit.dp(this, SideRail.WIDTH_DP), MATCH);
        rl.leftMargin = GlassKit.dp(this, 16);
        rl.topMargin = GlassKit.dp(this, 16);
        rl.bottomMargin = GlassKit.dp(this, 16);
        main.addView(SideRail.host(this, rail), rl);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        column.setClipToPadding(false);
        column.setPadding(GlassKit.dp(this, 16), GlassKit.dp(this, 20),
                GlassKit.dp(this, 24), GlassKit.dp(this, 16));
        main.addView(column, new LinearLayout.LayoutParams(0, MATCH, 1f));

        column.addView(titleRow("Ajustes", 24), new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setClipChildren(false);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(MATCH, 0, 1f);
        bl.topMargin = GlassKit.dp(this, 10);
        column.addView(body, bl);

        // Lista de opções (uma coluna, com títulos de seção)
        ScrollView optScroll = new ScrollView(this);
        optScroll.setFocusable(false);
        optScroll.setVerticalScrollBarEnabled(false);
        optScroll.setClipChildren(false);
        optScroll.setClipToPadding(false);
        optScroll.setPadding(GlassKit.dp(this, 4), GlassKit.dp(this, 4),
                GlassKit.dp(this, 4), GlassKit.dp(this, 8));
        LinearLayout optList = new LinearLayout(this);
        optList.setOrientation(LinearLayout.VERTICAL);
        optList.setClipChildren(false);
        optScroll.addView(optList, new FrameLayout.LayoutParams(MATCH, WRAP));
        body.addView(optScroll, new LinearLayout.LayoutParams(GlassKit.dp(this, 270), MATCH));

        String[] titles = {"Listas", "Categorias", "Reprodução", "Controle parental",
                "Dados e cache", "Atualização", "Diagnóstico", "Ajuda"};
        for (int i = 0; i < ROW_COUNT; i++) {
            if (i == ROW_LISTS) {
                optList.addView(sectionLabel("CONTEÚDO"));
            } else if (i == ROW_PARENTAL) {
                optList.addView(sectionLabel("SEGURANÇA E DADOS"));
            } else if (i == ROW_UPDATE) {
                optList.addView(sectionLabel("APLICATIVO"));
            }
            cards[i] = makeOption(optList, i, titles[i]);
        }
        refreshSubtitles();
        linkOptionCards();

        // Painel de vidro com o conteúdo da opção em foco
        GlassSurface panel = new GlassSurface(this, GlassKit.dp(this, 20), false,
                GlassSurface.STYLE_GLASS, true);
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(0, MATCH, 1f);
        pl.leftMargin = GlassKit.dp(this, 10);
        pl.topMargin = GlassKit.dp(this, 8);
        pl.bottomMargin = GlassKit.dp(this, 8);
        body.addView(panel, pl);

        LinearLayout panelCol = new LinearLayout(this);
        panelCol.setOrientation(LinearLayout.VERTICAL);
        panelCol.setClipChildren(false);
        panelCol.setPadding(GlassKit.dp(this, 20), GlassKit.dp(this, 18),
                GlassKit.dp(this, 20), GlassKit.dp(this, 14));
        panel.addView(panelCol, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout panelHead = titleRow("", 20);
        panelTitle = (TextView) panelHead.getChildAt(1);
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

    /** Título pequeno de seção, em dourado, acima de um grupo de opções. */
    private TextView sectionLabel(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(GlassKit.sp(10));
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        t.setLetterSpacing(0.12f);
        t.setTextColor(GlassKit.alpha(this, R.color.asc_gold, 0.85f));
        t.setPadding(GlassKit.dp(this, 10), GlassKit.dp(this, 10), 0, GlassKit.dp(this, 2));
        t.setLayoutParams(new LinearLayout.LayoutParams(MATCH, WRAP));
        return t;
    }

    private OptionCard makeOption(LinearLayout parent, final int row, String title) {
        OptionCard oc = new OptionCard();
        GlassSurface s = new GlassSurface(this, GlassKit.dp(this, 14), true,
                GlassSurface.STYLE_GLASS, false);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(GlassKit.dp(this, 8), GlassKit.dp(this, 8),
                GlassKit.dp(this, 10), GlassKit.dp(this, 8));
        s.addView(inner, new FrameLayout.LayoutParams(MATCH, WRAP));

        // traço dourado: aparece na opção selecionada
        View bar = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setColor(GlassKit.alpha(this, R.color.asc_gold, 0.95f));
        g.setCornerRadius(GlassKit.dp(this, 2));
        bar.setBackground(g);
        bar.setVisibility(View.INVISIBLE);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                GlassKit.dp(this, 3), GlassKit.dp(this, 26));
        barLp.rightMargin = GlassKit.dp(this, 9);
        inner.addView(bar, barLp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        inner.addView(texts, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(GlassKit.sp(14));
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        t.setTextColor(GlassKit.color(this, R.color.asc_text_primary));
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(t, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView sub = new TextView(this);
        sub.setTextSize(GlassKit.sp(11));
        sub.setTextColor(GlassKit.color(this, R.color.asc_text_secondary));
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(sub, new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = GlassKit.dp(this, 2);
        lp.bottomMargin = GlassKit.dp(this, 2);
        parent.addView(s, lp);

        s.setOnClickListener(v -> onRowClick(row));
        // seta para a direita entra no painel (e não deixa o foco escapar para o lado)
        s.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                    focusFirstInPanel();
                }
                return true;
            }
            return false;
        });
        GlassKit.focusScale(s, 1.03f, (v, hasFocus) -> {
            if (hasFocus) {
                lastOptionFocus = v;
                showPanel(row, false);
            }
        });

        oc.view = s;
        oc.sub = sub;
        oc.bar = bar;
        return oc;
    }

    /** Foco na lista: cima/baixo entre as opções (os extremos param); esquerda volta ao menu. */
    private void linkOptionCards() {
        int railId = GlassKit.ensureId(rail.settings);
        for (int i = 0; i < ROW_COUNT; i++) {
            GlassSurface v = cards[i].view;
            GlassSurface up = i > 0 ? cards[i - 1].view : v;
            GlassSurface down = i < ROW_COUNT - 1 ? cards[i + 1].view : v;
            v.setNextFocusUpId(GlassKit.ensureId(up));
            v.setNextFocusDownId(GlassKit.ensureId(down));
            v.setNextFocusLeftId(railId);
        }
    }

    /** Texto pequeno embaixo do nome de cada opção. */
    private void refreshSubtitles() {
        int n = CredentialStore.lists(this).size();
        cards[ROW_LISTS].sub.setText(n == 1 ? "1 lista salva" : n + " listas salvas");
        int hidden = AppPrefs.hiddenCount(this);
        cards[ROW_CATEGORIES].sub.setText(hidden == 0 ? "Todos os grupos"
                : (hidden == 1 ? "1 grupo oculto" : hidden + " grupos ocultos"));
        cards[ROW_PLAYBACK].sub.setText("Áudio, legenda e qualidade");
        cards[ROW_PARENTAL].sub.setText(ParentalStore.isEnabled(this) ? "Ativo" : "Desativado");
        cards[ROW_DATA].sub.setText("Histórico, lista e cache");
        cards[ROW_UPDATE].sub.setText(pendingUpdate != null ? "Nova versão disponível"
                : (AppPrefs.autoUpdateCheck(this) ? "Automática" : "Só manual"));
        String model = Build.MODEL == null ? "" : Build.MODEL;
        cards[ROW_DIAG].sub.setText(model.isEmpty() ? "Aparelho e conexão" : model);
        cards[ROW_HELP].sub.setText("Dicas e contato");
    }

    // ---------- Painel ----------

    private void showPanel(int row, boolean force) {
        if (row == currentRow && !force) {
            return;
        }
        currentRow = row;
        for (int i = 0; i < ROW_COUNT; i++) {
            if (cards[i] != null) {
                cards[i].view.setSelected(i == row);
                cards[i].bar.setVisibility(i == row ? View.VISIBLE : View.INVISIBLE);
            }
        }
        panelBody.removeAllViews();
        updateStatusTv = null;
        updateActionTitle = null;
        diagConnTv = null;
        panelScroll.scrollTo(0, 0);
        switch (row) {
            case ROW_LISTS:
                panelTitle.setText("Listas");
                buildLists();
                break;
            case ROW_CATEGORIES:
                panelTitle.setText("Categorias");
                buildCategories();
                break;
            case ROW_PLAYBACK:
                panelTitle.setText("Reprodução");
                buildPlayback();
                break;
            case ROW_PARENTAL:
                panelTitle.setText("Controle parental");
                buildParental();
                break;
            case ROW_DATA:
                panelTitle.setText("Dados e cache");
                buildData();
                break;
            case ROW_UPDATE:
                panelTitle.setText("Atualização");
                buildUpdate();
                break;
            case ROW_DIAG:
                panelTitle.setText("Diagnóstico");
                buildDiag();
                break;
            default:
                panelTitle.setText("Ajuda");
                buildHelp();
                break;
        }
    }

    private TextView addNote(String text, int color, float sizeSp) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(GlassKit.sp(sizeSp));
        tv.setLineSpacing(0f, 1.15f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = GlassKit.dp(this, 4);
        lp.bottomMargin = GlassKit.dp(this, 6);
        panelBody.addView(tv, lp);
        return tv;
    }

    private TextView addNote(String text) {
        return addNote(text, GlassKit.color(this, R.color.asc_text_secondary), 13);
    }

    /** Linha do painel: título, texto de apoio e (opcional) um valor à direita. */
    private Row newRow(String title, String desc) {
        final Row r = new Row();
        GlassSurface s = new GlassSurface(this, GlassKit.dp(this, 14), true,
                GlassSurface.STYLE_GLASS, false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(GlassKit.dp(this, 14), GlassKit.dp(this, 10),
                GlassKit.dp(this, 14), GlassKit.dp(this, 10));
        s.addView(row, new FrameLayout.LayoutParams(MATCH, WRAP));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        row.addView(texts, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(GlassKit.sp(15));
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        t.setTextColor(GlassKit.color(this, R.color.asc_text_primary));
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(t, new LinearLayout.LayoutParams(MATCH, WRAP));
        r.title = t;

        if (desc != null && !desc.isEmpty()) {
            TextView d = new TextView(this);
            d.setText(desc);
            d.setTextSize(GlassKit.sp(12));
            d.setTextColor(GlassKit.color(this, R.color.asc_text_secondary));
            d.setMaxLines(2);
            d.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(MATCH, WRAP);
            dl.topMargin = GlassKit.dp(this, 2);
            texts.addView(d, dl);
        }

        TextView v = new TextView(this);
        v.setTextSize(GlassKit.sp(12));
        v.setTypeface(v.getTypeface(), Typeface.BOLD);
        v.setSingleLine(true);
        v.setPadding(GlassKit.dp(this, 10), GlassKit.dp(this, 4),
                GlassKit.dp(this, 10), GlassKit.dp(this, 4));
        v.setVisibility(View.GONE);
        LinearLayout.LayoutParams vl = new LinearLayout.LayoutParams(WRAP, WRAP);
        vl.leftMargin = GlassKit.dp(this, 10);
        row.addView(v, vl);
        r.value = v;

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = GlassKit.dp(this, 4);
        lp.bottomMargin = GlassKit.dp(this, 4);
        panelBody.addView(s, lp);
        GlassKit.focusScale(s, 1.02f, null);
        r.view = s;
        return r;
    }

    private void setValue(Row r, String text, boolean highlighted) {
        r.value.setText(text);
        r.value.setVisibility(View.VISIBLE);
        GlassKit.chip(r.value, GlassKit.dp(this, 10),
                highlighted ? R.color.asc_accent : R.color.asc_text_primary,
                highlighted ? 0.60f : 0.14f, R.color.asc_text_primary);
    }

    /** Chave LIGADO/DESLIGADO: OK alterna. */
    private Row addToggle(String title, String desc, boolean on, final BoolSetter setter) {
        final Row r = newRow(title, desc);
        final boolean[] state = {on};
        setValue(r, on ? "LIGADO" : "DESLIGADO", on);
        r.view.setOnClickListener(v -> {
            state[0] = !state[0];
            setter.set(state[0]);
            setValue(r, state[0] ? "LIGADO" : "DESLIGADO", state[0]);
            refreshSubtitles();
        });
        return r;
    }

    /** Escolha: cada OK passa para a opção seguinte (e volta à primeira no fim). */
    private Row addChoice(String title, String desc, final String[] labels, int current,
                          final IntSetter setter) {
        final Row r = newRow(title, desc);
        final int[] idx = {current};
        setValue(r, labels[current], false);
        r.view.setOnClickListener(v -> {
            idx[0] = (idx[0] + 1) % labels.length;
            setter.set(idx[0]);
            setValue(r, labels[idx[0]], false);
        });
        return r;
    }

    private Row addAction(String title, String desc, final Runnable action) {
        Row r = newRow(title, desc);
        r.view.setOnClickListener(v -> action.run());
        return r;
    }

    // ----- Categorias -----

    private void buildCategories() {
        addNote("Escolha os grupos de canais que aparecem na lista de categorias do Ao vivo. "
                + "Os canais continuam em \"Todas as categorias\". O grupo Adultos é "
                + "controlado pelo controle parental.");
        for (final CategoryGroups.Group g : CategoryGroups.Group.values()) {
            if (g == CategoryGroups.Group.ADULTOS) {
                continue;
            }
            addToggle(g.label, "", !AppPrefs.isGroupHidden(this, g),
                    on -> AppPrefs.setGroupHidden(SettingsActivity.this, g, !on));
        }
        linkPanelCards();
    }

    // ----- Reprodução -----

    private void buildPlayback() {
        addNote("Vale na próxima vez que você abrir um canal, filme ou episódio. "
                + "O áudio e a legenda só mudam quando o conteúdo traz essa opção.");
        addChoice("Áudio preferido", "", new String[]{"Automático", "Português", "Inglês"},
                AppPrefs.audioChoice(this),
                i -> AppPrefs.setAudioChoice(SettingsActivity.this, i));
        addChoice("Legenda preferida", "", new String[]{"Automático", "Português"},
                AppPrefs.textChoice(this),
                i -> AppPrefs.setTextChoice(SettingsActivity.this, i));
        addChoice("Qualidade máxima", "Reduza se o vídeo estiver travando",
                new String[]{"Automática", "720p", "480p"},
                AppPrefs.qualityChoice(this),
                i -> AppPrefs.setQualityChoice(SettingsActivity.this, i));
        linkPanelCards();
    }

    // ----- Controle parental -----

    private void buildParental() {
        boolean on = ParentalStore.isEnabled(this);
        addNote(on ? "O controle parental está ATIVO." : "O controle parental está desativado.",
                COLOR_WHITE, 15);
        addAction(on ? "Gerenciar controle parental" : "Ativar controle parental",
                on ? "Trocar a senha ou desativar" : "Tranca o conteúdo adulto com uma senha",
                () -> openParentalMenu());
        linkPanelCards();
    }

    // ----- Dados e cache -----

    private void buildData() {
        addNote("Escolha o que limpar. As listas salvas e o controle parental nunca são apagados.");
        addAction("Limpar histórico e progresso", "Continue assistindo e o que você já viu",
                () -> confirmDo("Apagar o \"Continue assistindo\" e o que você já assistiu?",
                        "Apagar", () -> {
                            ContinueStore.clear(this);
                            PlaybackProgress.clear(this);
                            SeriesProgress.clear(this);
                            Toast.makeText(this, "Histórico apagado.", Toast.LENGTH_SHORT).show();
                        }));
        addAction("Limpar Minha lista", "Os favoritos que você marcou",
                () -> confirmDo("Apagar todos os favoritos da Minha lista?", "Apagar", () -> {
                    FavoritesStore.clearAll(this);
                    Toast.makeText(this, "Minha lista apagada.", Toast.LENGTH_SHORT).show();
                }));
        addAction("Limpar cache", "Imagens e guia de programação (se o app estiver lento)", () -> {
            ImageLoader.clear();
            EpgCache.clear();
            Toast.makeText(this, "Cache limpo.", Toast.LENGTH_SHORT).show();
        });
        addAction("Apagar tudo", "Histórico, Minha lista e cache de uma vez",
                () -> confirmClearData());
        linkPanelCards();
    }

    private void confirmDo(String message, String yes, final Runnable action) {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AscDialog)
                .setMessage(message)
                .setNegativeButton("Cancelar", (d, which) -> d.dismiss())
                .setPositiveButton(yes, (d, which) -> action.run())
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus());
        dialog.show();
    }

    // ----- Atualização -----

    private void buildUpdate() {
        Row action = addAction(
                pendingUpdate != null ? "Instalar versão " + pendingUpdate.name
                        : "Procurar atualização agora",
                "", () -> checkOrInstallUpdate());
        updateActionTitle = action.title;
        addToggle("Procurar ao abrir o app",
                "A tela inicial procura sozinha uma versão nova", AppPrefs.autoUpdateCheck(this),
                on -> AppPrefs.setAutoUpdateCheck(SettingsActivity.this, on));
        updateStatusTv = addNote(updateText != null ? updateText : updateDefaultText(),
                updateText != null ? updateColor
                        : GlassKit.color(this, R.color.asc_text_secondary), 14);
        linkPanelCards();
    }

    private String updateDefaultText() {
        if (pendingUpdate != null) {
            return getString(R.string.update_available, pendingUpdate.name);
        }
        return "Versão instalada: " + installedVersionName();
    }

    // ----- Diagnóstico -----

    private void buildDiag() {
        addAction("Testar conexão", "Confere se o app consegue falar com o servidor de atualização",
                () -> testConnection());
        diagConnTv = addNote("", COLOR_WHITE, 14);
        addNote(appText() + "\n\n" + deviceText(), COLOR_WHITE, 14);
        linkPanelCards();
    }

    private void testConnection() {
        if (testing) {
            return;
        }
        testing = true;
        if (diagConnTv != null) {
            diagConnTv.setText("Testando conexão...");
        }
        Updater.checkAsync(this, new Updater.CheckListener() {
            @Override
            public void onUpdateAvailable(Updater.Info info) {
                testing = false;
                pendingUpdate = info;
                showConn("Conexão OK. Há uma versão nova: " + info.name + ".");
            }

            @Override
            public void onNoUpdate() {
                testing = false;
                showConn(Updater.lastCheckFailed()
                        ? "Sem conexão com o servidor de atualização. Confira a internet."
                        : "Conexão OK.");
            }
        });
    }

    private void showConn(String text) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        refreshSubtitles();
        if (currentRow == ROW_DIAG && diagConnTv != null) {
            diagConnTv.setText(text);
        }
    }

    // ----- Ajuda -----

    private void buildHelp() {
        addNote(getString(R.string.support_contact), COLOR_WHITE, 15);
        addNote("Dicas rápidas", GlassKit.color(this, R.color.asc_gold), 14);
        addNote("• Vídeo travando: em Reprodução, reduza a Qualidade máxima para 720p ou 480p.\n"
                + "• App lento: em Dados e cache, use Limpar cache.\n"
                + "• Não aparece atualização: em Atualização, use Procurar atualização agora "
                + "e, se falhar, o Diagnóstico mostra se a conexão está funcionando.\n"
                + "• Trocar de lista: em Listas, aperte OK na lista e escolha Usar esta lista.\n"
                + "• Canal ou categoria no grupo errado: no Ao vivo, na lista de categorias, "
                + "mantenha OK apertado para mover.", COLOR_WHITE, 13);
        linkPanelCards();
    }

    private void onRowClick(int row) {
        focusFirstInPanel();
    }

    private String installedVersionName() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pi.versionName == null ? "desconhecida" : pi.versionName;
        } catch (Exception e) {
            return "desconhecida";
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
        int listsId = GlassKit.ensureId(cards[currentRow >= 0 ? currentRow : ROW_LISTS].view);
        for (int i = 0; i < items.size(); i++) {
            View v = items.get(i);
            View up = i > 0 ? items.get(i - 1) : v;
            View down = i < items.size() - 1 ? items.get(i + 1) : v;
            v.setNextFocusUpId(GlassKit.ensureId(up));
            v.setNextFocusDownId(GlassKit.ensureId(down));
            v.setNextFocusLeftId(listsId);
            v.setNextFocusRightId(GlassKit.ensureId(v));
        }
        // (a pessoa entra nos cards do painel apertando OK em "Listas")
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
        FavoritesStore.clearAll(this);
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
        if (currentRow == ROW_UPDATE && updateStatusTv != null) {
            updateStatusTv.setText(text);
            updateStatusTv.setTextColor(color);
        }
        if (updateActionTitle != null) {
            updateActionTitle.setText(pendingUpdate != null
                    ? "Instalar versão " + pendingUpdate.name : "Procurar atualização agora");
        }
    }

    private void checkOrInstallUpdate() {
        if (checking || downloading) {
            return;
        }
        if (pendingUpdate != null) {
            showUpdateDialog();
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
                showUpdate(getString(R.string.update_available, info.name), COLOR_OK);
                showUpdateDialog();
            }

            @Override
            public void onNoUpdate() {
                checking = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (Updater.lastCheckFailed()) {
                    showUpdate("Não consegui verificar agora. Confira a internet e tente de novo.",
                            COLOR_WHITE);
                } else {
                    showUpdate("Você já está na versão mais nova.", COLOR_WHITE);
                }
            }
        });
    }

    /** Janela no meio da tela (igual à "Deseja sair?"): Atualizar agora ou Depois. */
    private void showUpdateDialog() {
        if (pendingUpdate == null || (updateDialog != null && updateDialog.isShowing())) {
            return;
        }
        updateDialog = UpdateDialog.show(this, pendingUpdate.name, () -> startUpdate(), () -> {
        });
    }

    private void startUpdate() {
        if (downloading || pendingUpdate == null || updateDialog == null) {
            return;
        }
        downloading = true;
        updateDialog.setStatus(getString(R.string.msg_downloading_unknown), true);

        Updater.downloadAndInstallAsync(this, pendingUpdate, new Updater.InstallListener() {
            @Override
            public void onProgress(int percent) {
                if (isFinishing() || isDestroyed() || updateDialog == null) {
                    return;
                }
                updateDialog.setStatus(percent >= 0
                        ? getString(R.string.msg_downloading, percent)
                        : getString(R.string.msg_downloading_unknown), true);
            }

            @Override
            public void onInstallerOpened() {
                downloading = false;
                if (isFinishing() || isDestroyed() || updateDialog == null) {
                    return;
                }
                updateDialog.setStatus(getString(R.string.msg_installer_opened), false);
            }

            @Override
            public void onFailed(String message) {
                downloading = false;
                if (isFinishing() || isDestroyed() || updateDialog == null) {
                    return;
                }
                updateDialog.setStatus(message, false);
                updateDialog.focusNow();
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
        if (currentRow == ROW_PARENTAL) {
            showPanel(ROW_PARENTAL, true);
            focusFirstInPanel();
        }
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
