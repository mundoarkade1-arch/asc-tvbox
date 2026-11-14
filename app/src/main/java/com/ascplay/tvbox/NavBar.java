package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * Menu à esquerda (só ícones) das telas de abas (Canais, Filmes, Séries e Ajustes).
 * O desenho do menu está em layout_nav.xml. Esta classe só liga os botões.
 *
 * - Tela inicial: volta para a HomeActivity.
 * - Canais, Filmes, Séries: troca de aba (abre a tela do servidor ou da lista M3U,
 *   conforme o login, e fecha a aba de antes). Se já está na aba, não faz nada.
 * - Ajustes: abre a SettingsActivity e fecha a aba de antes. Se já está em Ajustes,
 *   não faz nada.
 * - Atualizar: roda o que a tela mandar (recarregar a lista da aba).
 */
final class NavBar {

    static final int HOME = 0;
    static final int LIVE = 1;
    static final int MOVIE = 2;
    static final int SERIES = 3;
    static final int SETTINGS = 4;

    private NavBar() {
    }

    /**
     * Mostra o menu na tela e liga os botões. "current" é a aba em que a tela está.
     * O menu é o MESMO da tela inicial (SideRail, cápsula de vidro flutuante com borda dourada):
     * troca o menu antigo do layout (group_nav) por ele, no mesmo lugar.
     */
    static void bind(final Activity activity, final int current, final Runnable onRefresh) {
        View old = activity.findViewById(R.id.group_nav);
        if (old == null || !(old.getParent() instanceof ViewGroup)) {
            return;
        }
        ViewGroup parent = (ViewGroup) old.getParent();
        int index = parent.indexOfChild(old);
        parent.removeView(old);

        SideRail rail = new SideRail(activity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                GlassKit.dp(activity, 76), LinearLayout.LayoutParams.MATCH_PARENT);
        lp.leftMargin = GlassKit.dp(activity, 16);
        lp.topMargin = GlassKit.dp(activity, 16);
        lp.bottomMargin = GlassKit.dp(activity, 16);
        parent.addView(rail, index, lp);

        rail.home.setSelected(false);
        GlassSurface selected;
        switch (current) {
            case HOME:
                selected = rail.home;
                break;
            case LIVE:
                selected = rail.live;
                break;
            case MOVIE:
                selected = rail.movie;
                break;
            case SERIES:
                selected = rail.series;
                break;
            case SETTINGS:
                selected = rail.settings;
                break;
            default:
                selected = null;
                break;
        }
        if (selected != null) {
            selected.setSelected(true);
        }

        rail.home.setOnClickListener(v -> goHome(activity));
        rail.live.setOnClickListener(v -> goTab(activity, current, LIVE, XtreamApi.Kind.LIVE));
        rail.movie.setOnClickListener(v -> goTab(activity, current, MOVIE, XtreamApi.Kind.MOVIE));
        rail.series.setOnClickListener(
                v -> goTab(activity, current, SERIES, XtreamApi.Kind.SERIES));
        rail.settings.setOnClickListener(v -> goSettings(activity, current));
        rail.refresh.setOnClickListener(v -> {
            if (onRefresh != null) {
                Toast.makeText(activity, "Atualizando a lista...", Toast.LENGTH_SHORT).show();
                onRefresh.run();
            }
        });
    }

    /**
     * Liga os itens do menu flutuante (SideRail) das telas montadas em código (Ajustes).
     * "current" é a aba em que a tela está (a própria aba não faz nada).
     */
    static void bindRail(final Activity activity, SideRail rail, final int current) {
        rail.home.setOnClickListener(v -> goHome(activity));
        rail.live.setOnClickListener(v -> goTab(activity, current, LIVE, XtreamApi.Kind.LIVE));
        rail.movie.setOnClickListener(v -> goTab(activity, current, MOVIE, XtreamApi.Kind.MOVIE));
        rail.series.setOnClickListener(
                v -> goTab(activity, current, SERIES, XtreamApi.Kind.SERIES));
        rail.settings.setOnClickListener(v -> goSettings(activity, current));
    }

    private static void click(Activity activity, int id, View.OnClickListener listener) {
        View v = activity.findViewById(id);
        if (v != null) {
            v.setOnClickListener(listener);
        }
    }

    private static void goHome(Activity activity) {
        Intent i = new Intent(activity, HomeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(i);
    }

    private static void goTab(Activity activity, int current, int target, XtreamApi.Kind kind) {
        if (current == target) {
            return;
        }
        CredentialStore.Credentials creds = CredentialStore.load(activity);
        boolean m3u = creds != null && CredentialStore.MODE_M3U.equals(creds.mode);
        Intent i = new Intent(activity, m3u ? M3uBrowseActivity.class : BrowseActivity.class);
        i.putExtra(HomeActivity.EXTRA_KIND, kind.name());
        activity.startActivity(i);
        activity.finish();
        activity.overridePendingTransition(0, 0);
    }

    private static void goSettings(Activity activity, int current) {
        if (current == SETTINGS) {
            return;
        }
        activity.startActivity(new Intent(activity, SettingsActivity.class));
        activity.finish();
        activity.overridePendingTransition(0, 0);
    }
}
