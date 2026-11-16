package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.GridView;

/**
 * Responsividade do app: faz TODA tela se comportar como a tela de referência (1280x720 dp,
 * a do TX9, onde o visual foi testado), seja qual for a resolução e a densidade do aparelho.
 *
 * COMO FUNCIONA: cada Activity (AscActivity) chama wrap() ao nascer. O wrap() calcula uma
 * densidade nova a partir do tamanho REAL da tela em pixels, de modo que a tela sempre tenha
 * cerca de 1280x720 dp (o lado que sobrar fica maior, nunca menor). Como todos os layouts e
 * todo o código usam dp/sp, tudo escala junto: cards, menu, letras, janelas, player.
 *
 * Exemplos (tela -> densidade usada): 1280x720 -> 160 (igual a antes); 1920x1080 -> 240;
 * 3840x2160 -> 480; 1024x768 -> 128 (fica 1280x960 dp, sobra altura).
 *
 * Para deixar a interface toda maior ou menor, mude só UI_ZOOM (1.0 = tamanho de referência).
 */
final class ScreenScale {

    /** Largura e altura (dp) da tela de referência. */
    static final float REF_LONG_DP = 1280f;
    static final float REF_SHORT_DP = 720f;

    /** 1.0 = referência. Maior = tudo maior (cabe menos coisa). Menor = tudo menor. */
    static final float UI_ZOOM = 1.0f;

    // ---------- Quantas colunas (IGUAL em qualquer TV) ----------
    // Para mudar, mude SÓ estes números: vale para todas as telas e tamanhos de TV.

    /** Colunas de cards na aba Canais (e na busca de canais). */
    static final int COLS_LIVE = 3;
    /** Colunas de cards nas abas Filmes e Séries (e na busca de filmes/séries). */
    static final int COLS_POSTER = 4;
    /** Colunas de botões de episódios. */
    static final int COLS_EPISODES = 15;

    private static final int MIN_DPI = 80;
    private static final int MAX_DPI = 960;

    private ScreenScale() {
    }

    // ---------- Cálculo da densidade ----------

    /** Densidade (dpi) que faz esta tela ter ~1280x720 dp. 0 se não deu para medir. */
    static int targetDpi(Context base) {
        // 1) Medida REAL da janela, guardada pela calibração (ver calibrate): é a que manda.
        try {
            SharedPreferences p = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            int l = p.getInt(K_LONG, 0);
            int sh = p.getInt(K_SHORT, 0);
            if (l > 0 && sh > 0) {
                return dpiFor(l, sh);
            }
        } catch (Throwable e) {
            // sem calibração: usa a medida do sistema, abaixo
        }
        try {
            DisplayManager dm = (DisplayManager) base.getSystemService(Context.DISPLAY_SERVICE);
            Display display = dm == null ? null : dm.getDisplay(Display.DEFAULT_DISPLAY);
            if (display == null) {
                return 0;
            }
            DisplayMetrics m = new DisplayMetrics();
            display.getRealMetrics(m);
            // Usa o MENOR entre a tela real e a área que o app realmente recebe (sem barras do
            // sistema/recortes), para nada ficar escondido.
            DisplayMetrics a = base.getResources().getDisplayMetrics();
            int longPx = Math.max(m.widthPixels, m.heightPixels);
            int shortPx = Math.min(m.widthPixels, m.heightPixels);
            int aLong = Math.max(a.widthPixels, a.heightPixels);
            int aShort = Math.min(a.widthPixels, a.heightPixels);
            if (aLong > 0 && aShort > 0) {
                longPx = Math.min(longPx, aLong);
                shortPx = Math.min(shortPx, aShort);
            }
            if (longPx <= 0 || shortPx <= 0) {
                return 0;
            }
            return dpiFor(longPx, shortPx);
        } catch (Throwable e) {
            return 0;
        }
    }

    /** Cabe inteira: usa o lado que mais limita (a tela nunca fica menor que a referência). */
    private static int dpiFor(int longPx, int shortPx) {
        float scale = Math.min(longPx / REF_LONG_DP, shortPx / REF_SHORT_DP) / UI_ZOOM;
        int dpi = Math.round(DisplayMetrics.DENSITY_DEFAULT * scale);
        return Math.max(MIN_DPI, Math.min(MAX_DPI, dpi));
    }

    // ---------- Calibração: confere o tamanho REAL da janela ----------
    // Em algumas TVs (ex.: 50") o sistema informa um tamanho de tela diferente do real, ou não
    // aplica a densidade pedida. Aí o app ficava com tudo maior e só 3 cards por fileira.
    // A calibração mede a janela de verdade, guarda o tamanho e refaz a tela uma vez.

    private static final String PREFS = "asc_scale";
    private static final String K_LONG = "long";
    private static final String K_SHORT = "short";
    private static String lastKey = null;

    /**
     * Segunda garantia: se o Resources da Activity ficou com densidade diferente da pedida
     * (o sistema ignorou o wrap), aplica a densidade direto nele. Chamar ANTES do super.onCreate.
     */
    @SuppressWarnings("deprecation")
    static void enforce(Activity a) {
        try {
            int want = targetDpi(a);
            Resources r = a.getResources();
            int have = r.getDisplayMetrics().densityDpi;
            if (want <= 0 || Math.abs(want - have) <= have * 0.03f) {
                return;
            }
            Configuration c = new Configuration(r.getConfiguration());
            c.densityDpi = want;
            c.fontScale = 1f;
            r.updateConfiguration(c, r.getDisplayMetrics());
        } catch (Throwable e) {
            // se não der, segue com o que tem
        }
    }

    /** Liga a calibração à janela da Activity (chamar uma vez, depois do onCreate). */
    static void calibrate(final Activity a) {
        final View decor = a.getWindow().getDecorView();
        final Handler h = new Handler(Looper.getMainLooper());
        final Runnable check = new Runnable() {
            @Override
            public void run() {
                checkWindow(a, decor.getWidth(), decor.getHeight());
            }
        };
        decor.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b,
                    int ol, int ot, int or, int ob) {
                h.removeCallbacks(check);
                h.postDelayed(check, 500); // espera o tamanho assentar
            }
        });
    }

    private static void checkWindow(Activity a, int w, int h) {
        if (a == null || a.isFinishing() || w <= 0 || h <= 0) {
            return;
        }
        float density = a.getResources().getDisplayMetrics().density;
        if (density <= 0f) {
            return;
        }
        int lp = Math.max(w, h);
        int sp = Math.min(w, h);
        // 1.0 = a janela tem 1280x720 dp (ou mais, no lado que sobra), como deve ser
        float k = Math.min(lp / density / REF_LONG_DP, sp / density / REF_SHORT_DP) * UI_ZOOM;
        if (Math.abs(k - 1f) < 0.05f) {
            lastKey = null;
            return;
        }
        String key = lp + "x" + sp + "@" + density;
        if (key.equals(lastKey)) {
            return; // já tentou com esta medida: não fica em laço
        }
        lastKey = key;
        try {
            a.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putInt(K_LONG, lp).putInt(K_SHORT, sp).apply();
        } catch (Throwable e) {
            return;
        }
        a.recreate();
    }

    /** Devolve um Context com a densidade ajustada (ou o próprio, se não for preciso). */
    static Context wrap(Context base) {
        int dpi = targetDpi(base);
        if (dpi <= 0) {
            return base;
        }
        Configuration cur = base.getResources().getConfiguration();
        Configuration c = new Configuration(cur);
        float ratio = cur.densityDpi > 0 ? (float) cur.densityDpi / dpi : 1f;
        c.densityDpi = dpi;
        c.fontScale = 1f; // o tamanho das letras segue a escala acima, não a do sistema
        if (cur.screenWidthDp > 0) {
            c.screenWidthDp = Math.round(cur.screenWidthDp * ratio);
        }
        if (cur.screenHeightDp > 0) {
            c.screenHeightDp = Math.round(cur.screenHeightDp * ratio);
        }
        if (cur.smallestScreenWidthDp > 0) {
            c.smallestScreenWidthDp = Math.round(cur.smallestScreenWidthDp * ratio);
        }
        return base.createConfigurationContext(c);
    }

    // ---------- Segurança: tela mudou depois de aberta ----------

    private static int lastTriedDpi = 0;

    /**
     * Se a tela real mudou (ex.: o box trocou de 720p para 1080p) e a Activity ficou com a
     * densidade velha, refaz a Activity para pegar a densidade certa. Só tenta uma vez por densidade (evita laço).
     */
    static void recheck(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        int want = targetDpi(activity);
        int have = activity.getResources().getConfiguration().densityDpi;
        if (want <= 0) {
            return;
        }
        if (want == have) {
            lastTriedDpi = 0;
            return;
        }
        if (Math.abs(want - have) < have * 0.10f) {
            return; // diferença pequena (barras do sistema etc.): não vale refazer a tela
        }
        if (want == lastTriedDpi) {
            return; // já tentou esta densidade; se o sistema não aceitou, não fica em laço
        }
        lastTriedDpi = want;
        activity.recreate();
    }

    // ---------- Grades com colunas automáticas ----------

    /**
     * Faz a grade escolher sozinha o número de colunas pela largura que tem, para o cartão ficar
     * perto de "cardDp" de largura (já contando o espaço entre colunas). Sem isso, telas largas
     * esticariam os cartões e telas estreitas os espremeriam.
     */
    static void autoColumns(final GridView grid, final float cardDp, final int minCols,
            final int maxCols) {
        grid.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b,
                    int ol, int ot, int or, int ob) {
                int w = (r - l) - grid.getPaddingLeft() - grid.getPaddingRight();
                if (w <= 0) {
                    return;
                }
                float density = grid.getResources().getDisplayMetrics().density;
                int gap = Math.max(0, grid.getHorizontalSpacing());
                int cols = (int) Math.floor((w + gap) / (cardDp * density + gap));
                cols = Math.max(minCols, Math.min(maxCols, cols));
                if (cols != grid.getNumColumns()) {
                    grid.setNumColumns(cols);
                }
            }
        });
    }
}
