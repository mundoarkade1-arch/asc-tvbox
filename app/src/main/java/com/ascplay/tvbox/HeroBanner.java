package com.ascplay.tvbox;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Banner de destaque (hero) no estilo "imagem grande de fundo": a imagem larga (backdrop) do
 * filme ou da série ocupa o banner todo, com um escurecimento à esquerda para o texto ler bem;
 * à esquerda ficam o selo EM DESTAQUE, o nome e os botões "Assistir" e "+ Minha lista"; à direita,
 * a capa 2:3 INTEIRA em destaque; embaixo à esquerda, as bolinhas do carrossel (qual destaque
 * está passando). Se o servidor não mandar a imagem larga, o fundo é o degradê do app.
 *
 * O banner sobe até o topo da tela, atrás da barra de busca e do relógio (que ficam por cima),
 * e vai até a borda direita: cantos da direita retos e os da esquerda arredondados.
 *
 * Esta classe só desenha. Quem escolhe o destaque, gira a cada 8 s e abre as telas é a
 * HomeActivity. Trocar de destaque NUNCA mexe no foco.
 */
final class HeroBanner extends FrameLayout {

    /** Altura total (dp), do topo da tela até o começo das linhas de baixo. */
    static final int HEIGHT_DP = 232;
    /** Espaço de cima ocupado pela barra de busca e pelo relógio (a capa começa abaixo dele). */
    private static final int TOP_BAR_DP = 58;
    private static final int POSTER_BOTTOM_DP = 14;
    private static final int MAX_DOTS = 8;

    private final ImageView backdrop;
    private final ImageView art;
    private final LinearLayout textCol;
    private final FrameLayout skeleton;
    private final FrameLayout posterCard;
    private final TextView chipKind;
    private final TextView chipMeta;
    private final TextView chipRating;
    private final TextView title;
    private final GlassSurface.Button playButton;
    private final GlassSurface.Button listButton;
    private final LinearLayout dots;

    HeroBanner(Context c) {
        super(c);
        final int corner = GlassKit.d(c, 40);
        setClipChildren(false);
        // só o canto de baixo à esquerda é arredondado: em cima o banner encosta no topo da
        // tela e à direita vai até a borda (os outros cantos ficam fora da view)
        setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(-corner, -corner, view.getWidth() + corner,
                        view.getHeight(), corner);
            }
        });
        setClipToOutline(true);
        setBackground(GlassKit.placeholder(c, corner, true));

        // imagem larga de fundo (cortada para preencher; é só ambiente, a capa aparece inteira)
        backdrop = new ImageView(c);
        backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backdrop.setContentDescription(null);
        addView(backdrop, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // escurecimento: esquerda (texto) e embaixo
        int dark = R.color.asc_bg_scrim_dialog;
        View sideScrim = new View(c);
        sideScrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{GlassKit.alpha(c, dark, 0.92f), GlassKit.alpha(c, dark, 0.62f),
                        GlassKit.alpha(c, dark, 0.08f)}));
        addView(sideScrim, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        View bottomScrim = new View(c);
        bottomScrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{GlassKit.alpha(c, dark, 0.7f), GlassKit.alpha(c, dark, 0.0f)}));
        addView(bottomScrim, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, GlassKit.dp(c, 90), Gravity.BOTTOM));
        // escurecimento no topo: a barra de busca e o relógio ficam legíveis
        View topScrim = new View(c);
        topScrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{GlassKit.alpha(c, dark, 0.6f), GlassKit.alpha(c, dark, 0.0f)}));
        addView(topScrim, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, GlassKit.dp(c, 90), Gravity.TOP));

        // coluna do texto (esquerda, embaixo)
        LinearLayout pc = new LinearLayout(c);
        textCol = pc;
        pc.setOrientation(LinearLayout.VERTICAL);
        pc.setClipChildren(false);
        pc.setClipToPadding(false);

        LinearLayout chips = new LinearLayout(c);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setGravity(Gravity.CENTER_VERTICAL);
        TextView chipFeatured = makeChip(c, "EM DESTAQUE");
        GlassKit.chip(chipFeatured, GlassKit.dp(c, 10), R.color.asc_text_primary, 0.14f,
                R.color.asc_gold);
        chips.addView(chipFeatured, chipParams(c));
        chipKind = makeChip(c, "");
        chips.addView(chipKind, chipParams(c));
        chipMeta = makeChip(c, "");
        chipMeta.setMaxWidth(GlassKit.dp(c, 100));
        chipMeta.setEllipsize(TextUtils.TruncateAt.END);
        chips.addView(chipMeta, chipParams(c));
        chipRating = makeChip(c, "");
        chips.addView(chipRating, chipParams(c));
        pc.addView(chips, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        title = new TextView(c);
        title.setTextSize(GlassKit.sp(28));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setTextColor(GlassKit.color(c, R.color.asc_text_primary));
        title.setShadowLayer(GlassKit.dp(c, 4), 0, GlassKit.dp(c, 1), 0xCC000000);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tl.topMargin = GlassKit.dp(c, 6);
        pc.addView(title, tl);

        LinearLayout buttons = new LinearLayout(c);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setClipChildren(false);
        buttons.setClipToPadding(false);
        buttons.setPadding(GlassKit.dp(c, 4), GlassKit.dp(c, 4), GlassKit.dp(c, 4), GlassKit.dp(c, 4));
        playButton = new GlassSurface.Button(c, "\u25B6  Assistir", true);
        listButton = new GlassSurface.Button(c, "+ Minha lista",
                GlassSurface.STYLE_SEE_THROUGH);
        // largura pelo texto
        buttons.addView(playButton, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, GlassKit.dp(c, 38)));
        LinearLayout.LayoutParams lb = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, GlassKit.dp(c, 38));
        lb.leftMargin = GlassKit.dp(c, 10);
        buttons.addView(listButton, lb);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        bl.topMargin = GlassKit.dp(c, 6);
        pc.addView(buttons, bl);

        // bolinhas do carrossel
        dots = new LinearLayout(c);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        dl.topMargin = GlassKit.dp(c, 6);
        dl.leftMargin = GlassKit.dp(c, 4);
        pc.addView(dots, dl);

        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(
                GlassKit.dp(c, 400), LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.BOTTOM);
        pp.leftMargin = GlassKit.dp(c, 16);
        pp.bottomMargin = GlassKit.dp(c, 12);
        addView(pc, pp);

        // capa 2:3 inteira, à direita, ABAIXO da barra de busca/relógio (não passa por trás)
        art = new ImageView(c);
        art.setScaleType(ImageView.ScaleType.FIT_CENTER); // inteira, nunca cortada
        art.setContentDescription(null);
        posterCard = new FrameLayout(c);
        final int cardCorner = GlassKit.dp(c, 14);
        posterCard.setBackground(GlassKit.placeholder(c, cardCorner));
        GlassKit.roundClip(posterCard, cardCorner);
        posterCard.addView(art, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        int cardH = GlassKit.dp(c, HEIGHT_DP - TOP_BAR_DP - POSTER_BOTTOM_DP);
        int cardW = Math.round(cardH * 2f / 3f);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(cardW, cardH,
                Gravity.END | Gravity.BOTTOM);
        cp.rightMargin = GlassKit.dp(c, 40); // margem de segurança da TV (overscan)
        cp.bottomMargin = GlassKit.dp(c, POSTER_BOTTOM_DP);
        addView(posterCard, cp);

        // esqueleto de carregamento: barras no lugar do texto e um bloco no lugar da capa
        skeleton = new FrameLayout(c);
        skeleton.setClipChildren(false);
        LinearLayout sk = new LinearLayout(c);
        sk.setOrientation(LinearLayout.VERTICAL);
        sk.addView(Skeleton.block(c, 96, 20, 10));
        Skeleton t1 = Skeleton.block(c, 240, 26, 8);
        ((LinearLayout.LayoutParams) t1.getLayoutParams()).topMargin = GlassKit.dp(c, 12);
        sk.addView(t1);
        Skeleton t2 = Skeleton.block(c, 160, 26, 8);
        ((LinearLayout.LayoutParams) t2.getLayoutParams()).topMargin = GlassKit.dp(c, 8);
        sk.addView(t2);
        LinearLayout skBtns = new LinearLayout(c);
        skBtns.setOrientation(LinearLayout.HORIZONTAL);
        skBtns.addView(Skeleton.block(c, 112, 38, 16));
        Skeleton b2 = Skeleton.block(c, 124, 38, 16);
        ((LinearLayout.LayoutParams) b2.getLayoutParams()).leftMargin = GlassKit.dp(c, 10);
        skBtns.addView(b2);
        LinearLayout.LayoutParams sbl = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        sbl.topMargin = GlassKit.dp(c, 14);
        sk.addView(skBtns, sbl);
        FrameLayout.LayoutParams skp = new FrameLayout.LayoutParams(
                GlassKit.dp(c, 400), LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.BOTTOM);
        skp.leftMargin = GlassKit.dp(c, 20);
        skp.bottomMargin = GlassKit.dp(c, 20);
        skeleton.addView(sk, skp);
        FrameLayout.LayoutParams skPoster = new FrameLayout.LayoutParams(cardW, cardH,
                Gravity.END | Gravity.BOTTOM);
        skPoster.rightMargin = GlassKit.dp(c, 40);
        skPoster.bottomMargin = GlassKit.dp(c, POSTER_BOTTOM_DP);
        skeleton.addView(new Skeleton(c, GlassKit.dp(c, 14)), skPoster);
        addView(skeleton, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        showLoading();
    }

    /** rightBleedPx: quanto o banner passa da margem direita da coluna, até a borda da tela. */
    static FrameLayout.LayoutParams params(Context c, int rightBleedPx) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, GlassKit.dp(c, HEIGHT_DP));
        lp.rightMargin = -rightBleedPx;
        return lp;
    }

    private static TextView makeChip(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(GlassKit.sp(12));
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        t.setSingleLine(true);
        t.setPadding(GlassKit.dp(c, 8), GlassKit.dp(c, 3), GlassKit.dp(c, 8), GlassKit.dp(c, 3));
        GlassKit.chip(t, GlassKit.dp(c, 10), R.color.asc_text_primary, 0.14f,
                R.color.asc_text_primary);
        t.setShadowLayer(GlassKit.dp(c, 3), 0, GlassKit.dp(c, 1), 0xCC000000);
        return t;
    }

    private static LinearLayout.LayoutParams chipParams(Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.rightMargin = GlassKit.dp(c, 6);
        return lp;
    }

    // ---------- Acesso para a HomeActivity ----------

    GlassSurface.Button playButton() {
        return playButton;
    }

    GlassSurface.Button listButton() {
        return listButton;
    }

    boolean hasFocusInside() {
        return playButton.hasFocus() || listButton.hasFocus();
    }

    // ---------- Conteúdo ----------

    /** Antes de ter um destaque: título de espera, sem botões, capa e fundo vazios. */
    void showLoading() {
        skeleton.setVisibility(VISIBLE);
        textCol.setVisibility(GONE);
        chipKind.setVisibility(GONE);
        chipMeta.setVisibility(GONE);
        chipRating.setVisibility(GONE);
        title.setText("ASC Play");
        playButton.setVisibility(INVISIBLE);
        listButton.setVisibility(INVISIBLE);
        playButton.setFocusable(false);
        listButton.setFocusable(false);
        art.setImageDrawable(null);
        backdrop.setImageDrawable(null);
        posterCard.setVisibility(INVISIBLE);
        dots.removeAllViews();
    }

    /** Mostra um destaque. Não pede foco. backdropUrl pode ser vazio (fica o degradê do app). */
    void showEntry(String name, String kindLabel, String meta, String rating, String artUrl,
                   String backdropUrl, boolean inList) {
        skeleton.setVisibility(GONE);
        textCol.setVisibility(VISIBLE);
        title.setText(name == null ? "" : name);
        // Só o selo "EM DESTAQUE" (pedido do dono): sem Filme/Série, gênero·ano e nota.
        setChip(chipKind, "");
        setChip(chipMeta, "");
        setChip(chipRating, "");
        posterCard.setVisibility(VISIBLE);
        playButton.setVisibility(VISIBLE);
        listButton.setVisibility(VISIBLE);
        playButton.setFocusable(true);
        listButton.setFocusable(true);
        setInList(inList);
        ImageLoader.load(art, artUrl, 640);
        ImageLoader.load(backdrop, backdropUrl, 768);
    }

    /** Bolinhas do carrossel: a do destaque atual fica dourada e alongada. */
    void setPage(int index, int count) {
        Context c = getContext();
        dots.removeAllViews();
        if (count <= 1) {
            return;
        }
        int n = Math.min(count, MAX_DOTS);
        for (int i = 0; i < n; i++) {
            boolean on = i == index;
            View d = new View(c);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(GlassKit.dp(c, 4));
            g.setColor(on ? GlassKit.color(c, R.color.asc_gold)
                    : GlassKit.alpha(c, R.color.asc_text_primary, 0.5f));
            d.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    GlassKit.dp(c, on ? 18 : 7), GlassKit.dp(c, 7));
            lp.rightMargin = GlassKit.dp(c, 5);
            dots.addView(d, lp);
        }
    }

    private static void setChip(TextView chip, String text) {
        if (text == null || text.isEmpty()) {
            chip.setVisibility(GONE);
        } else {
            chip.setText(text);
            chip.setVisibility(VISIBLE);
        }
    }

    void setInList(boolean inList) {
        listButton.setLabel(inList ? "\u2713 Na lista" : "+ Minha lista");
    }
}
