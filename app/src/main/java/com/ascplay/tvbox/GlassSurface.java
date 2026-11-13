package com.ascplay.tvbox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Superfície de vidro reutilizável (menu, cards, chips, botões).
 *
 * Visual: degradê diagonal translúcido (16% -> 5%), borda de 1dp (22%), linha de brilho no topo
 * e sombra suave. Tudo com as cores atuais do app em alpha diferente.
 *
 * DESEMPENHO (TV box fraca): NÃO usa blur real de fundo. É só translucidez + degradê + borda,
 * desenhados por um Drawable leve (sem bitmap, sem RenderEffect). Este é o "fallback" para
 * todos os aparelhos.
 *
 * Foco: se for focável, ganha um anel de 2,5dp (cor de foco do app) com brilho em volta,
 * desenhado POR CIMA do conteúdo (foreground), e cresce 1.08x (GlassKit.focusScale).
 */
class GlassSurface extends FrameLayout {

    static final int STYLE_GLASS = 0;
    static final int STYLE_FILLED = 1;

    private final Fill fill;

    GlassSurface(Context context, float cornerPx, boolean focusable) {
        this(context, cornerPx, focusable, STYLE_GLASS, false);
    }

    GlassSurface(Context context, float cornerPx, boolean focusable, int style, boolean shadow) {
        super(context);
        fill = new Fill(context, cornerPx, style, shadow);
        setBackground(fill);
        if (focusable) {
            setFocusable(true);
            setClickable(true);
            setForeground(new Ring(context, cornerPx));
        }
    }

    // ---------------------------------------------------------------------------------
    // Botão de vidro (texto centralizado). style FILLED = botão principal (cor primária).
    // ---------------------------------------------------------------------------------
    static final class Button extends GlassSurface {
        private final TextView label;

        Button(Context context, String text, boolean filled) {
            super(context, GlassKit.dp(context, 16), true,
                    filled ? STYLE_FILLED : STYLE_GLASS, false);
            label = new TextView(context);
            label.setText(text);
            label.setTextSize(14);
            label.setSingleLine(true);
            label.setGravity(Gravity.CENTER);
            label.setTextColor(GlassKit.color(context, R.color.asc_text_primary));
            label.setTypeface(label.getTypeface(), android.graphics.Typeface.BOLD);
            label.setPadding(GlassKit.dp(context, 14), 0, GlassKit.dp(context, 14), 0);
            addView(label, new FrameLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, Gravity.CENTER));
            GlassKit.focusScale(this, 1.08f, null);
        }

        void setLabel(String text) {
            label.setText(text);
        }
    }

    // ---------------------------------------------------------------------------------
    // Fundo de vidro
    // ---------------------------------------------------------------------------------
    static final class Fill extends Drawable {
        private final float corner;
        private final boolean shadow;
        private final int style;
        private final float line;

        private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        private final int glassHi;
        private final int glassLo;
        private final int selHi;
        private final int selLo;
        private final int focusHi;
        private final int focusLo;
        private final int borderNormal;
        private final int borderSelected;
        private final int sheenNormal;
        private final int accent;
        private final int accentTint;

        private boolean selected = false;
        private boolean focused = false;

        Fill(Context c, float corner, int style, boolean shadow) {
            this.corner = corner;
            this.style = style;
            this.shadow = shadow;
            this.line = Math.max(1f, GlassKit.dp(c, 1));

            glassHi = GlassKit.alpha(c, R.color.asc_text_primary, 0.16f);
            glassLo = GlassKit.alpha(c, R.color.asc_text_primary, 0.05f);
            selHi = GlassKit.alpha(c, R.color.asc_text_primary, 0.26f);
            selLo = GlassKit.alpha(c, R.color.asc_text_primary, 0.11f);
            focusHi = GlassKit.alpha(c, R.color.asc_text_primary, 0.24f);
            focusLo = GlassKit.alpha(c, R.color.asc_text_primary, 0.10f);
            borderNormal = GlassKit.alpha(c, R.color.asc_text_primary, 0.22f);
            borderSelected = GlassKit.alpha(c, R.color.asc_focus, 0.70f);
            sheenNormal = GlassKit.alpha(c, R.color.asc_text_primary, 0.38f);
            accent = GlassKit.color(c, R.color.asc_accent);
            accentTint = GlassKit.alpha(c, R.color.asc_accent, 0.30f);

            shadowPaint.setColor(GlassKit.color(c, R.color.asc_glass_shadow));
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(line);
            sheenPaint.setStyle(Paint.Style.STROKE);
            sheenPaint.setStrokeWidth(line);
            sheenPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            boolean s = false;
            boolean f = false;
            for (int st : state) {
                if (st == android.R.attr.state_selected) {
                    s = true;
                } else if (st == android.R.attr.state_focused) {
                    f = true;
                }
            }
            if (s != selected || f != focused) {
                selected = s;
                focused = f;
                rebuildShader();
                invalidateSelf();
                return true;
            }
            return false;
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            rebuildShader();
        }

        private void rebuildShader() {
            Rect b = getBounds();
            if (b.width() <= 0 || b.height() <= 0) {
                return;
            }
            int hi;
            int lo;
            if (style == STYLE_FILLED) {
                hi = accent;
                lo = accent;
            } else if (selected) {
                hi = selHi;
                lo = selLo;
            } else if (focused) {
                hi = focusHi;
                lo = focusLo;
            } else {
                hi = glassHi;
                lo = glassLo;
            }
            fillPaint.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom,
                    hi, lo, Shader.TileMode.CLAMP));
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            if (b.width() <= 0 || b.height() <= 0) {
                return;
            }
            float bottomGap = shadow ? line * 2f : 0f;

            if (shadow) {
                rect.set(b.left, b.top + line * 2f, b.right, b.bottom);
                canvas.drawRoundRect(rect, corner, corner, shadowPaint);
            }

            rect.set(b.left, b.top, b.right, b.bottom - bottomGap);
            canvas.drawRoundRect(rect, corner, corner, fillPaint);

            if (selected && style != STYLE_FILLED) {
                tintPaint.setColor(accentTint);
                canvas.drawRoundRect(rect, corner, corner, tintPaint);
            }

            // borda de 1dp
            borderPaint.setColor(selected ? borderSelected : borderNormal);
            float half = line / 2f;
            rect.set(b.left + half, b.top + half, b.right - half, b.bottom - bottomGap - half);
            canvas.drawRoundRect(rect, Math.max(0f, corner - half), Math.max(0f, corner - half),
                    borderPaint);

            // brilho interno no topo (linha 1dp mais clara)
            sheenPaint.setColor(sheenNormal);
            float y = b.top + line * 1.5f;
            float x1 = b.left + corner * 0.8f;
            float x2 = b.right - corner * 0.8f;
            if (x2 > x1) {
                canvas.drawLine(x1, y, x2, y, sheenPaint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    // ---------------------------------------------------------------------------------
    // Anel de foco (por cima do conteúdo)
    // ---------------------------------------------------------------------------------
    static final class Ring extends Drawable {
        private final float corner;
        private final float ringW;
        private final float glowW;
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Paint idlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean idleBorder;
        private final float idleW;
        private boolean focused = false;

        Ring(Context c, float corner) {
            this(c, corner, false);
        }

        /** idleBorder = true: sem foco ainda desenha a borda fina de vidro (cards com imagem). */
        Ring(Context c, float corner, boolean idleBorder) {
            this.corner = corner;
            this.idleBorder = idleBorder;
            this.idleW = Math.max(1f, GlassKit.dp(c, 1));
            idlePaint.setStyle(Paint.Style.STROKE);
            idlePaint.setStrokeWidth(idleW);
            idlePaint.setColor(GlassKit.alpha(c, R.color.asc_text_primary, 0.22f));
            this.ringW = GlassKit.dp(c, 2.5f);
            this.glowW = GlassKit.dp(c, 6f);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(ringW);
            ringPaint.setColor(GlassKit.color(c, R.color.asc_focus));
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeWidth(glowW);
            glowPaint.setColor(GlassKit.alpha(c, R.color.asc_accent, 0.55f));
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            boolean f = false;
            for (int st : state) {
                if (st == android.R.attr.state_focused) {
                    f = true;
                    break;
                }
            }
            if (f != focused) {
                focused = f;
                invalidateSelf();
                return true;
            }
            return false;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            if (!focused) {
                if (idleBorder) {
                    float h = idleW / 2f;
                    rect.set(b.left + h, b.top + h, b.right - h, b.bottom - h);
                    canvas.drawRoundRect(rect, Math.max(0f, corner - h), Math.max(0f, corner - h),
                            idlePaint);
                }
                return;
            }
            float g = glowW / 2f;
            rect.set(b.left + g, b.top + g, b.right - g, b.bottom - g);
            canvas.drawRoundRect(rect, Math.max(0f, corner - g), Math.max(0f, corner - g), glowPaint);
            float r = ringW / 2f;
            rect.set(b.left + r, b.top + r, b.right - r, b.bottom - r);
            canvas.drawRoundRect(rect, Math.max(0f, corner - r), Math.max(0f, corner - r), ringPaint);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
