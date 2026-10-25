package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
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

    /** Mostra o menu na tela e liga os botões. "current" é a aba em que a tela está. */
    static void bind(final Activity activity, final int current, final Runnable onRefresh) {
        View nav = activity.findViewById(R.id.group_nav);
        if (nav == null) {
            return;
        }
        nav.setVisibility(View.VISIBLE);

        click(activity, R.id.nav_home, v -> goHome(activity));
        click(activity, R.id.nav_live, v -> goTab(activity, current, LIVE, XtreamApi.Kind.LIVE));
        click(activity, R.id.nav_movie, v -> goTab(activity, current, MOVIE, XtreamApi.Kind.MOVIE));
        click(activity, R.id.nav_series,
                v -> goTab(activity, current, SERIES, XtreamApi.Kind.SERIES));
        click(activity, R.id.nav_settings, v -> goSettings(activity, current));
        click(activity, R.id.nav_refresh, v -> {
            if (onRefresh != null) {
                Toast.makeText(activity, "Atualizando a lista...", Toast.LENGTH_SHORT).show();
                onRefresh.run();
            }
        });
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
