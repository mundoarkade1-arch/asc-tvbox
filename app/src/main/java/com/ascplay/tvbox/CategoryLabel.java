package com.ascplay.tvbox;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/**
 * Linha da lista de categorias: NOME primeiro, ícone (ou tarja 18+) DEPOIS, colado no fim do texto.
 *
 * Como funciona: o espaço do ícone é reservado no padding do fim; assim o nome nunca passa por
 * cima dele (nome comprido quebra em 2 linhas ou termina em "..."). O ícone é desenhado logo
 * depois da última linha do nome, centralizado na vertical. Não usa emoji.
 * Tem que ser pública: o LayoutInflater cria a classe pelo nome no XML.
 */
public class CategoryLabel extends TextView {

    private static final float ICON_DP = 26f;
    private static final float GAP_DP = 10f;
    private static final float LOCK_DP = 18f;
    private static final float BADGE_TEXT_SP = 12f;
    private static final float BADGE_PAD_H_DP = 7f;
    private static final float BADGE_PAD_V_DP = 3f;
    private static final float BADGE_RADIUS_DP = 5f;
    private static final int BADGE_BG = 0xFFA11A1A;      // vermelho escuro
    private static final int BADGE_STROKE = 0xFFE05A5A;  // borda clara: destaca em fundo escuro
    private static final int BADGE_FG = 0xFFFFFFFF;

    private final float density;
    private final int baseEnd;
    private final int baseTop;
    private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgeStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgeText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private Drawable icon;
    private Drawable lock;
    private boolean badge;
    private float badgeW;
    private float badgeH;
    private int reserved;

    public CategoryLabel(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        baseEnd = getPaddingEnd();
        baseTop = getPaddingTop();

        badgePaint.setStyle(Paint.Style.FILL);
        badgePaint.setColor(BADGE_BG);
        badgeStroke.setStyle(Paint.Style.STROKE);
        badgeStroke.setStrokeWidth(density * 1.2f);
        badgeStroke.setColor(BADGE_STROKE);
        badgeText.setColor(BADGE_FG);
        badgeText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        badgeText.setTextAlign(Paint.Align.CENTER);
        badgeText.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                BADGE_TEXT_SP, context.getResources().getDisplayMetrics()));
    }

    /**
     * Define o que aparece DEPOIS do nome. iconRes = 0: nada; "badge18" põe a tarja 18+
     * (no lugar do ícone); "locked" põe um cadeado depois de tudo. Tem que redefinir tudo
     * a cada chamada, porque a linha da lista é reaproveitada.
     */
    void setTrailing(int iconRes, boolean badge18, boolean locked, int extraTopPx) {
        Resources res = getResources();
        icon = null;
        lock = null;
        badge = badge18;
        if (!badge18 && iconRes != 0) {
            Drawable d = ContextCompat.getDrawable(getContext(), iconRes);
            if (d != null) {
                icon = d.mutate();
                int s = Math.round(ICON_DP * density);
                icon.setBounds(0, 0, s, s);
            }
        }
        if (locked) {
            Drawable d = ContextCompat.getDrawable(getContext(), R.drawable.ic_cat_lock);
            if (d != null) {
                lock = d.mutate();
                int s = Math.round(LOCK_DP * density);
                lock.setBounds(0, 0, s, s);
            }
        }
        if (badge) {
            Paint.FontMetrics fm = badgeText.getFontMetrics();
            badgeW = badgeText.measureText("18+") + 2 * BADGE_PAD_H_DP * density;
            badgeH = (fm.descent - fm.ascent) + 2 * BADGE_PAD_V_DP * density;
        }
        float w = 0f;
        if (icon != null) {
            w += icon.getBounds().width();
        }
        if (badge) {
            w += badgeW;
        }
        if (lock != null) {
            w += (w > 0 ? GAP_DP * density * 0.6f : 0f) + lock.getBounds().width();
        }
        reserved = w > 0 ? Math.round(w + GAP_DP * density) : 0;
        setPaddingRelative(getPaddingStart(), baseTop + extraTopPx, baseEnd + reserved,
                getPaddingBottom());
        refreshIconState();
        invalidate();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        refreshIconState();
    }

    private void refreshIconState() {
        int[] st = getDrawableState();
        if (icon != null) {
            icon.setState(st);
        }
        if (lock != null) {
            lock.setState(st);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Layout layout = getLayout();
        if (layout == null || (icon == null && !badge && lock == null)) {
            return;
        }
        int last = layout.getLineCount() - 1;
        if (last < 0) {
            return;
        }
        // Onde o texto começa na vertical (a gravidade centraliza o bloco de texto).
        int avail = getHeight() - getCompoundPaddingTop() - getCompoundPaddingBottom();
        int offset = Math.max(0, (avail - layout.getHeight()) / 2);
        float lineCenter = getCompoundPaddingTop() + offset
                + (layout.getLineTop(last) + layout.getLineBottom(last)) / 2f;

        // Colado no fim do nome, mas nunca depois do espaço reservado.
        float x = getCompoundPaddingStart() + layout.getLineRight(last) + GAP_DP * density;
        float maxX = getWidth() - baseEnd - reserved + GAP_DP * density;
        if (x > maxX) {
            x = maxX;
        }

        if (icon != null) {
            drawIcon(canvas, icon, x, lineCenter);
            x += icon.getBounds().width();
        }
        if (badge) {
            float top = lineCenter - badgeH / 2f;
            rect.set(x, top, x + badgeW, top + badgeH);
            float r = BADGE_RADIUS_DP * density;
            canvas.drawRoundRect(rect, r, r, badgePaint);
            canvas.drawRoundRect(rect, r, r, badgeStroke);
            Paint.FontMetrics fm = badgeText.getFontMetrics();
            float baseline = lineCenter - (fm.ascent + fm.descent) / 2f;
            canvas.drawText("18+", x + badgeW / 2f, baseline, badgeText);
            x += badgeW;
        }
        if (lock != null) {
            x += GAP_DP * density * 0.6f;
            drawIcon(canvas, lock, x, lineCenter);
        }
    }

    private static void drawIcon(Canvas canvas, Drawable d, float x, float centerY) {
        int h = d.getBounds().height();
        canvas.save();
        canvas.translate(x, centerY - h / 2f);
        d.draw(canvas);
        canvas.restore();
    }
}
