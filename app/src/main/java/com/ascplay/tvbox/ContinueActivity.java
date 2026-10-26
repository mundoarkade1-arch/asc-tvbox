package com.ascplay.tvbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tela "Continue assistindo" (M6, "Ver mais" da faixa da tela inicial): lista simples com
 * todos os itens guardados na ContinueStore (até 25), do mais novo ao mais antigo.
 * Cada linha mostra a capa pequena, o nome e "Parou em M:SS".
 *
 * Clique (igual ao da faixa da tela inicial): filme abre a página do filme (MovieActivity);
 * episódio abre a página da série (SeriesActivity no servidor, M3uSeriesActivity na lista M3U).
 * Lá a pessoa clica em "Continuar". O próximo episódio toca sozinho, pois a fila vem da série.
 *
 * Lê a ContinueStore fora da thread principal e refaz a lista no onResume (depois do player).
 * Controle parental ligado: itens com nome adulto não aparecem.
 * O endereço do vídeo e a chave da série NUNCA são mostrados nem registrados em log.
 * Sem menu à esquerda (tela de passagem); o Voltar leva à tela inicial.
 */
public class ContinueActivity extends Activity {

    private CredentialStore.Credentials creds;

    private TextView tvStatus;
    private ListView lvContinue;
    private ContinueAdapter adapter;
    private final List<ContinueStore.Saved> shown = new ArrayList<>();

    // Para ignorar respostas antigas
    private int loadGen = 0;
    private boolean firstShow = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        creds = CredentialStore.load(this);
        if (creds == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_continue);
        tvStatus = findViewById(R.id.tv_continue_status);
        lvContinue = findViewById(R.id.lv_continue);

        adapter = new ContinueAdapter();
        lvContinue.setAdapter(adapter);
        lvContinue.setOnItemClickListener((parent, view, position, rowId) -> {
            if (position >= 0 && position < shown.size()) {
                openContinue(shown.get(position));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (creds != null) {
            loadList();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ImageLoader.clear();
    }

    private boolean isXtream() {
        return creds != null && CredentialStore.MODE_XTREAM.equals(creds.mode);
    }

    private boolean isM3u() {
        return creds != null && CredentialStore.MODE_M3U.equals(creds.mode);
    }

    // ---------- Ler a lista ----------

    private void loadList() {
        final int gen = ++loadGen;
        new Thread(() -> {
            List<ContinueStore.Saved> all = null;
            try {
                all = ContinueStore.list(this);
            } catch (Exception e) {
                // não conseguiu ler: mostra a lista vazia
            }
            final List<ContinueStore.Saved> result = all;
            runOnUiThread(() -> {
                if (gen == loadGen) {
                    showList(result);
                }
            });
        }).start();
    }

    private void showList(List<ContinueStore.Saved> list) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        int oldPos = lvContinue.getSelectedItemPosition();

        boolean hideAdult = ParentalStore.isEnabled(this);
        shown.clear();
        if (list != null) {
            for (ContinueStore.Saved s : list) {
                if (s.url.isEmpty()) {
                    continue;
                }
                if (hideAdult && ParentalStore.isAdultName(s.name)) {
                    continue;
                }
                shown.add(s);
            }
        }
        adapter.notifyDataSetChanged();

        if (shown.isEmpty()) {
            tvStatus.setText("Nada para continuar por enquanto.");
            tvStatus.setVisibility(View.VISIBLE);
            return;
        }
        tvStatus.setVisibility(View.GONE);

        lvContinue.requestFocus();
        if (firstShow) {
            firstShow = false;
            lvContinue.setSelection(0);
        } else if (oldPos >= 0 && oldPos < shown.size()) {
            lvContinue.setSelection(oldPos);
        }
    }

    // ---------- Clique ----------

    /**
     * Filme abre a página do filme; episódio abre a página da série. Em ambas, a pessoa
     * clica em "Continuar".
     */
    private void openContinue(ContinueStore.Saved s) {
        if ("movie".equals(s.kind)) {
            Intent i = new Intent(this, MovieActivity.class);
            i.putExtra(HomeActivity.EXTRA_KIND, XtreamApi.Kind.MOVIE.name());
            i.putExtra(HomeActivity.EXTRA_ID, s.id);
            i.putExtra(HomeActivity.EXTRA_EXT, s.ext);
            i.putExtra(HomeActivity.EXTRA_NAME, s.name);
            if (!s.icon.isEmpty()) {
                i.putExtra(MovieActivity.EXTRA_ICON, s.icon);
            }
            if (isM3u()) {
                i.putExtra(MainActivity.EXTRA_URL, s.url);
            }
            startActivity(i);
            return;
        }

        if ("episode".equals(s.kind)) {
            Intent i;
            int q = s.name.lastIndexOf(" - ep. ");
            if (isXtream() && s.seriesKey.startsWith("xt|")) {
                // Chave: xt|servidor|usuário|número da série. O número é a última parte.
                int p = s.seriesKey.lastIndexOf('|');
                String seriesId = p >= 0 ? s.seriesKey.substring(p + 1) : "";
                String seriesName = q > 0 ? s.name.substring(0, q).trim() : s.name;
                if (!seriesId.matches("\\d{1,12}")) {
                    cannotOpen();
                    return;
                }
                i = new Intent(this, SeriesActivity.class);
                i.putExtra(HomeActivity.EXTRA_ID, seriesId);
                i.putExtra(HomeActivity.EXTRA_NAME, seriesName);
            } else if (isM3u() && s.seriesKey.startsWith("m3u|")) {
                // Lista M3U: a série é achada pelo título, que vem antes de " - ep. N"
                String seriesName = q > 0 ? s.name.substring(0, q).trim() : "";
                if (seriesName.isEmpty()) {
                    cannotOpen();
                    return;
                }
                i = new Intent(this, M3uSeriesActivity.class);
                i.putExtra(HomeActivity.EXTRA_NAME, seriesName);
            } else {
                cannotOpen();
                return;
            }
            if (!s.icon.isEmpty()) {
                i.putExtra(MovieActivity.EXTRA_ICON, s.icon);
            }
            startActivity(i);
            return;
        }

        cannotOpen();
    }

    private void cannotOpen() {
        Toast.makeText(this, "Não deu para abrir este item. Ele pode ser de outra lista.",
                Toast.LENGTH_LONG).show();
    }

    /** Minuto em texto: 5:07 ou 1:23:45. */
    private static String clock(long ms) {
        long total = ms / 1000L;
        long h = total / 3600L;
        long m = (total % 3600L) / 60L;
        long sec = total % 60L;
        if (h > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", h, m, sec);
        }
        return String.format(Locale.US, "%d:%02d", m, sec);
    }

    // ---------- Adaptador (reaproveita as linhas ao rolar) ----------

    private final class ContinueAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return shown.size();
        }

        @Override
        public Object getItem(int position) {
            return shown.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(ContinueActivity.this)
                        .inflate(R.layout.item_continue, parent, false);
            }
            ContinueStore.Saved s = shown.get(position);
            ImageView cover = row.findViewById(R.id.iv_cover);
            TextView name = row.findViewById(R.id.tv_name);
            TextView pos = row.findViewById(R.id.tv_pos);

            name.setText(s.name);
            pos.setText("Parou em " + clock(s.posMs));
            cover.setImageDrawable(null);
            if (!s.icon.isEmpty()) {
                ImageLoader.load(cover, s.icon);
            }
            return row;
        }
    }
}
