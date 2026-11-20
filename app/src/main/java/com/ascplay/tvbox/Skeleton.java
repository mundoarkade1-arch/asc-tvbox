package com.ascplay.tvbox;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Esqueleto de carregamento (skeleton loader): blocos de vidro com um brilho que passa da
 * esquerda para a direita, no lugar do conteúdo que ainda está chegando.
 *
 * Leve para TV box de 1 GB: um único relógio (ValueAnimator) move o brilho de TODOS os blocos
 * que estão na tela e ele só roda enquanto existir algum bloco visível. Sem imagens e sem
 * camadas extras: cada bloco é só um retângulo arredondado com um degradê.
 */
final class Skeleton extends View {

    private static final List<Skeleton> ACTIVE = new ArrayList<>();
    private static ValueAnimator clock;
    private static float phase = 0f;

    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Matrix matrix = new Matrix();
    private final float corner;
    private final int shineColor;
    private LinearGradient shader;
    private int shaderBand = -1;

    Skeleton(Context c, float cornerPx) {
        super(c);
        corner = cornerPx;
        basePaint.setColor(GlassKit.alpha(c, R.color.asc_text_primary, 0.09f));
        shineColor = GlassKit.alpha(c, R.color.asc_text_primary, 0.20f);
        setFocusable(false);
        setClickable(false);
    }

    // ---------- Relógio único do brilho ----------

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ACTIVE.add(this);
        startClock();
    }

    @Override
    protected void onDetachedFromWindow() {
        ACTIVE.remove(this);
        if (ACTIVE.isEmpty()) {
            stopClock();
        }
        super.onDetachedFromWindow();
    }

    private static void startClock() {
        if (clock != null) {
            return;
        }
        clock = ValueAnimator.ofFloat(0f, 1f);
        clock.setDuration(1300);
        clock.setRepeatCount(ValueAnimator.INFINITE);
        clock.setInterpolator(new LinearInterpolator());
        clock.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                phase = (Float) a.getAnimatedValue();
                for (int i = 0; i < ACTIVE.size(); i++) {
                    Skeleton s = ACTIVE.get(i);
                    if (s.isShown()) {
                        s.invalidate();
                    }
                }
            }
        });
        clock.start();
    }

    private static void stopClock() {
        if (clock != null) {
            clock.cancel();
            clock = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, corner, corner, basePaint);

        int band = Math.max(1, Math.round(w * 0.55f));
        if (shader == null || shaderBand != band) {
            int edge = shineColor & 0x00FFFFFF;
            shader = new LinearGradient(0, 0, band, 0, new int[]{edge, shineColor, edge},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
            shinePaint.setShader(shader);
            shaderBand = band;
        }
        matrix.setTranslate(-band + phase * (w + band), 0);
        shader.setLocalMatrix(matrix);
        canvas.drawRoundRect(rect, corner, corner, shinePaint);
    }

    // ---------- Construtores prontos ----------

    /** Bloco de esqueleto. Largura negativa = ocupa toda a largura. Usa LayoutParams de LinearLayout. */
    static Skeleton block(Context c, int widthDp, int heightDp, int cornerDp) {
        Skeleton s = new Skeleton(c, GlassKit.dp(c, cornerDp));
        s.setLayoutParams(new LinearLayout.LayoutParams(
                widthDp < 0 ? ViewGroup.LayoutParams.MATCH_PARENT : GlassKit.dp(c, widthDp),
                GlassKit.dp(c, heightDp)));
        return s;
    }

    /**
     * Grade de esqueleto para as telas de lista (Filmes, Séries, Ao vivo): "rows" fileiras de
     * "cols" células iguais, cada uma com um bloco do cartão e (opcional) uma linha de nome.
     */
    static View grid(Context c, int cols, int rows, int cellHeightDp, boolean caption) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(GlassKit.dp(c, 9), GlassKit.dp(c, 9), GlassKit.dp(c, 9), 0);
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = 0; i < cols; i++) {
                LinearLayout cell = new LinearLayout(c);
                cell.setOrientation(LinearLayout.VERTICAL);
                Skeleton card = block(c, -1, cellHeightDp, 14);
                cell.addView(card);
                if (caption) {
                    LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                            GlassKit.dp(c, 110), GlassKit.dp(c, 12));
                    cl.topMargin = GlassKit.dp(c, 8);
                    cl.leftMargin = GlassKit.dp(c, 4);
                    cell.addView(new Skeleton(c, GlassKit.dp(c, 6)), cl);
                }
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lp.leftMargin = GlassKit.dp(c, 5);
                lp.rightMargin = GlassKit.dp(c, 5);
                row.addView(cell, lp);
            }
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rl.bottomMargin = GlassKit.dp(c, 12);
            col.addView(row, rl);
        }
        FrameLayout box = new FrameLayout(c);
        box.setClipChildren(false);
        box.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }

    /**
     * Sinopse carregando: no lugar do texto "Carregando..." aparecem 3 linhas de esqueleto. Assim
     * que o texto receber qualquer conteúdo (a sinopse, ou "Sem sinopse..."), as linhas somem e
     * o texto volta. O texto precisa estar dentro de um ScrollView ou de outro grupo.
     */
    static void loadingText(final TextView tv) {
        final Context c = tv.getContext();
        ViewGroup parent = (ViewGroup) tv.getParent();
        if (parent == null) {
            tv.setText("Carregando...");
            return;
        }
        final LinearLayout box;
        if ("asc_plot_box".equals(parent.getTag())) {
            box = (LinearLayout) parent;
        } else {
            int idx = parent.indexOfChild(tv);
            ViewGroup.LayoutParams lp = tv.getLayoutParams();
            parent.removeView(tv);
            box = new LinearLayout(c);
            box.setTag("asc_plot_box");
            box.setOrientation(LinearLayout.VERTICAL);
            box.addView(tv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            parent.addView(box, idx, lp);
        }
        final LinearLayout bars = new LinearLayout(c);
        bars.setOrientation(LinearLayout.VERTICAL);
        int[] widths = {-1, -1, -1, 320};
        for (int w : widths) {
            Skeleton b = block(c, w, 16, 8);
            ((LinearLayout.LayoutParams) b.getLayoutParams()).bottomMargin = GlassKit.dp(c, 10);
            bars.addView(b);
        }
        box.addView(bars, 0, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setText("");
        tv.setVisibility(View.GONE);
        tv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() > 0) {
                    box.removeView(bars);
                    tv.setVisibility(View.VISIBLE);
                    tv.removeTextChangedListener(this);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}
