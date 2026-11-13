package com.ascplay.tvbox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/**
 * Fundo da tela inicial: a cor de fundo do app (vem do tema, windowBackground) com 3 manchas
 * grandes e suaves (degradês radiais) nas cores primária e de foco do app, em alpha baixo.
 * Sem imagem, sem blur: 3 círculos com degradê, desenhados uma vez só (a view nunca é
 * invalidada depois). Leve para TV box.
 */
final class GlassBackground extends View {

    private final Paint p1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint p2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint p3 = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float x1;
    private float y1;
    private float r1;
    private float x2;
    private float y2;
    private float r2;
    private float x3;
    private float y3;
    private float r3;

    GlassBackground(Context c) {
        super(c);
        setFocusable(false);
        setClickable(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) {
            return;
        }
        Context c = getContext();
        int accentStrong = GlassKit.alpha(c, R.color.asc_accent, 0.34f);
        int accentEnd = GlassKit.alpha(c, R.color.asc_accent, 0f);
        int focusSoft = GlassKit.alpha(c, R.color.asc_focus, 0.16f);
        int focusEnd = GlassKit.alpha(c, R.color.asc_focus, 0f);
        int accentMid = GlassKit.alpha(c, R.color.asc_accent, 0.22f);

        // mancha 1: canto superior direito
        x1 = w * 0.88f;
        y1 = h * 0.02f;
        r1 = Math.max(w, h) * 0.55f;
        p1.setShader(new RadialGradient(x1, y1, r1, accentStrong, accentEnd, Shader.TileMode.CLAMP));

        // mancha 2: canto inferior esquerdo
        x2 = w * 0.12f;
        y2 = h * 1.0f;
        r2 = Math.max(w, h) * 0.50f;
        p2.setShader(new RadialGradient(x2, y2, r2, focusSoft, focusEnd, Shader.TileMode.CLAMP));

        // mancha 3: meio, um pouco para a esquerda
        x3 = w * 0.45f;
        y3 = h * 0.60f;
        r3 = Math.max(w, h) * 0.38f;
        p3.setShader(new RadialGradient(x3, y3, r3, accentMid, accentEnd, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(x2, y2, r2, p2);
        canvas.drawCircle(x3, y3, r3, p3);
        canvas.drawCircle(x1, y1, r1, p1);
    }
}
