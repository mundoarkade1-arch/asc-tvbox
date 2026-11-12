package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Fundo global do app (imagem + camada escura) em UM só lugar.
 * A imagem é res/drawable-nodpi/asc_bg.jpg e a camada escura é a cor asc_bg_scrim
 * (res/values/asc_glass.xml). Só visual: não mexe em lógica, rede nem navegação.
 */
final class Glass {

    private static final String TAG = "asc_global_bg";

    private Glass() {
    }

    /** Põe o fundo atrás de toda a tela da Activity (uma vez só). O player fica de fora. */
    static void attach(Activity activity) {
        if (activity instanceof MainActivity) {
            return;
        }
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.findViewWithTag(TAG) != null) {
            return;
        }
        content.addView(layer(activity, R.color.asc_bg_scrim), 0,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /** Para telas cheias montadas em código (teclado de busca, PIN): fundo + conteúdo. */
    static View wrap(Context context, View content) {
        FrameLayout root = new FrameLayout(context);
        root.addView(layer(context, R.color.asc_bg_scrim_dialog), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    @SuppressWarnings("deprecation")
    private static View layer(Context context, int scrimColorRes) {
        FrameLayout layer = new FrameLayout(context);
        layer.setTag(TAG);
        layer.setFocusable(false);
        layer.setClickable(false);

        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImageResource(R.drawable.asc_bg);
        image.setFocusable(false);
        layer.addView(image, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View scrim = new View(context);
        scrim.setBackgroundColor(context.getResources().getColor(scrimColorRes));
        scrim.setFocusable(false);
        layer.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return layer;
    }
}
