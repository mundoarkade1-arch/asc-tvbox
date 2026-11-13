package com.ascplay.tvbox;

import android.content.Context;
import android.graphics.Canvas;
import android.widget.ImageView;

/**
 * Fundo das telas montadas em código: a imagem de fundo do app (asc_bg) preenchendo a tela.
 * "dim" > 0 escurece essa fração (0.30 = 30%) com a cor de fundo do app, para telas com
 * listas. A tela inicial usa dim = 0 (imagem pura). Nenhuma cor nova.
 */
final class GlassBackground extends ImageView {

    private final int dimColor;

    GlassBackground(Context c) {
        this(c, 0f);
    }

    GlassBackground(Context c, float dim) {
        super(c);
        setScaleType(ScaleType.CENTER_CROP);
        setImageResource(R.drawable.asc_bg);
        setFocusable(false);
        setClickable(false);
        dimColor = dim > 0f ? GlassKit.alpha(c, R.color.asc_bg_scrim_dialog, dim) : 0;
        if (dim > 0f) {
            // a cor do scrim é a do fundo do app; só o alpha muda
            setWillNotDraw(false);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dimColor != 0) {
            canvas.drawColor(dimColor);
        }
    }
}
