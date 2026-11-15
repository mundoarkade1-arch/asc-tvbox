package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Context;

/**
 * Base de TODAS as telas do app. Faz a tela se ajustar sozinha a qualquer tamanho e
 * resolução (ver ScreenScale). Toda tela nova deve estender AscActivity, não Activity.
 */
public abstract class AscActivity extends Activity {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(ScreenScale.wrap(base));
    }
}
