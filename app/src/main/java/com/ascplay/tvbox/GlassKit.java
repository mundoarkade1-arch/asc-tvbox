package com.ascplay.tvbox;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

/**
 * Ferramentas do visual "Glass" (vidro) da tela inicial.
 *
 * REGRA DAS CORES: aqui NÃO existe nenhuma cor nova. Tudo vem das cores que o app já define
 * (res/values/asc_glass.xml), só mudando a transparência (alpha) em cada uso.
 *
 * TAMANHOS: o desenho foi feito para uma tela de 1920x1080. Uma tela de TV costuma ter
 * 960x540 dp, então cada "px do desenho" vale DESIGN_TO_DP dp. Para deixar tudo maior ou
 * menor de uma vez, mude só esse número.
 */
final class GlassKit {

    static final float DESIGN_TO_DP = 0.5f;

    /**
     * Aumenta TUDO da tela inicial de uma vez (cards, menu, ícones, textos) para enxergar de
     * longe na TV. 1.0 = tamanho anterior. Para mudar o tamanho geral, mude só este número.
     */
    static final float UI_SCALE = 1.3f;

    private GlassKit() {
    }

    // ---------- Medidas ----------

    /** Pixels reais a partir de uma medida do desenho 1920x1080. */
    static int d(Context c, float designPx) {
        return Math.round(designPx * DESIGN_TO_DP * UI_SCALE * c.getResources().getDisplayMetrics().density);
    }

    /** Pixels reais a partir de dp. */
    static int dp(Context c, float dp) {
        return Math.round(dp * UI_SCALE * c.getResources().getDisplayMetrics().density);
    }

    /** Tamanho de texto (sp) já com o aumento geral. */
    static float sp(float sp) {
        return sp * UI_SCALE;
    }

    // ---------- Cores (sempre as do app, só com alpha diferente) ----------

    static int color(Context c, int resId) {
        return ContextCompat.getColor(c, resId);
    }

    /** Cor do app (resId) com transparência "alpha" de 0 a 1. */
    static int alpha(Context c, int resId, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(255f * alpha)));
        return ColorUtils.setAlphaComponent(ContextCompat.getColor(c, resId), a);
    }

    static ColorStateList navText(Context c) {
        return ContextCompat.getColorStateList(c, R.color.nav_text);
    }

    // ---------- Formas ----------

    /** Recorta a view com cantos arredondados (a imagem filha não vaza dos cantos). */
    static void roundClip(View v, final float radiusPx) {
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radiusPx);
            }
        });
        v.setClipToOutline(true);
    }

    /** Pílula pequena de vidro (chips e notas). Não é focável. */
    static void chip(TextView t, int corner, int bgResId, float bgAlpha, int textResId) {
        Context c = t.getContext();
        GradientDrawable g = new GradientDrawable();
        g.setColor(alpha(c, bgResId, bgAlpha));
        g.setCornerRadius(corner);
        g.setStroke(Math.max(1, dp(c, 1)), alpha(c, R.color.asc_text_primary, 0.18f));
        t.setBackground(g);
        t.setTextColor(color(c, textResId));
    }

    // ---------- Foco ----------

    /** Garante que a view tem um id (para ligar o foco entre elas). */
    static int ensureId(View v) {
        if (v.getId() == View.NO_ID) {
            v.setId(View.generateViewId());
        }
        return v.getId();
    }

    /**
     * Foco de TV: ao ganhar foco a view cresce (animação curta) e sobe na ordem de desenho
     * (translationZ), para não ficar escondida atrás do vizinho. "extra" é opcional.
     */
    static void focusScale(final View v, final float scale, final View.OnFocusChangeListener extra) {
        final float lift = dp(v.getContext(), 6);
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                view.animate().cancel();
                view.animate()
                        .scaleX(hasFocus ? scale : 1f)
                        .scaleY(hasFocus ? scale : 1f)
                        .translationZ(hasFocus ? lift : 0f)
                        .setDuration(120)
                        .start();
                if (extra != null) {
                    extra.onFocusChange(view, hasFocus);
                }
            }
        });
    }

    // ---------- Barra fina de progresso ----------

    /** Barra de progresso bem fina (sem ProgressBar, para ser leve). Valor de 0 a 1. */
    static final class Bar extends View {
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private float fraction = 0f;

        Bar(Context c) {
            super(c);
            track.setColor(alpha(c, R.color.asc_text_primary, 0.18f));
            fill.setColor(color(c, R.color.asc_accent));
        }

        void setFraction(float f) {
            float v = Math.max(0f, Math.min(1f, f));
            if (v != fraction) {
                fraction = v;
                invalidate();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            float r = h / 2f;
            rect.set(0, 0, w, h);
            canvas.drawRoundRect(rect, r, r, track);
            if (fraction > 0f) {
                rect.set(0, 0, Math.max(h, w * fraction), h);
                canvas.drawRoundRect(rect, r, r, fill);
            }
        }
    }

    /** Letra inicial do nome (placeholder quando não há capa). */
    static String firstLetter(String name) {
        String s = name == null ? "" : name.trim();
        if (s.isEmpty()) {
            return "?";
        }
        return new String(Character.toChars(s.codePointAt(0))).toUpperCase();
    }

    /** Fundo do placeholder sem capa: degradê sutil com as cores do app. */
    static GradientDrawable placeholder(Context c, int corner) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{alpha(c, R.color.asc_accent, 0.55f), alpha(c, R.color.asc_glass_light, 0.95f)});
        g.setCornerRadius(corner);
        return g;
    }
}
