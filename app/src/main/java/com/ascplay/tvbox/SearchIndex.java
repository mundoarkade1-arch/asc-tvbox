package com.ascplay.tvbox;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * M11c: índice de busca em ARQUIVO (servidor Xtream).
 *
 * A primeira busca de um tipo (canais, filmes ou séries) lê a lista do servidor uma vez
 * e grava um arquivo de texto no armazenamento do app (pasta de cache), uma linha por item.
 * As buscas seguintes leem só esse arquivo, sem baixar nada. Depois de TTL_MS o arquivo
 * é refeito sozinho.
 *
 * Memória: nada fica na RAM; cada linha é escrita ou lida e esquecida.
 * Segurança: o nome do arquivo é o SHA-1 de servidor + usuário + tipo (a senha não entra e
 * nenhum endereço aparece em texto). O arquivo guarda só nome, capa, ids e extensão.
 *
 * Faz acesso a disco e rede: NUNCA chamar na thread principal.
 */
public final class SearchIndex {

    /** Dá para pedir o cancelamento enquanto o índice é montado. */
    public interface Cancel {
        boolean isCancelled();
    }

    /** Validade do índice: 2 horas. Para mudar, mude só este número. */
    private static final long TTL_MS = 2L * 60L * 60L * 1000L;
    /** Teto de itens e de tamanho do arquivo. */
    private static final int MAX_ITEMS = 200000;
    private static final long MAX_FILE_BYTES = 60L * 1024L * 1024L;
    private static final String DIR = "search_index";
    private static final String SEP = "\t";

    private static final Object BUILD_LOCK = new Object();

    private SearchIndex() {
    }

    /**
     * Busca por nome no índice (montando-o antes, se não existir ou estiver velho).
     * Avisa cada resultado pelo listener; para ao juntar "limit" itens ou quando o
     * listener devolver false. A regra de comparação é a mesma da busca antiga.
     */
    public static void search(Context ctx, String server, String user, String pass,
                              XtreamApi.Kind kind, String query, boolean skipAdult,
                              int limit, Cancel cancel, XtreamApi.ResultListener listener)
            throws IOException {
        String[] words = XtreamApi.searchWords(query);
        if (words.length == 0) {
            return;
        }
        File file = fileFor(ctx, server, user, kind);
        if (!isFresh(file)) {
            build(file, server, user, pass, kind, cancel);
        }
        if (cancel != null && cancel.isCancelled()) {
            return;
        }
        if (!file.exists()) {
            return;
        }
        BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), 65536);
        try {
            int found = 0;
            int count = 0;
            String line;
            while (found < limit && (line = in.readLine()) != null) {
                count++;
                if ((count & 1023) == 0 && cancel != null && cancel.isCancelled()) {
                    return;
                }
                String[] f = line.split(SEP, -1);
                if (f.length < 6) {
                    continue;
                }
                String name = f[5];
                if (!XtreamApi.matchesWords(name, words)) {
                    continue;
                }
                if (skipAdult && ParentalStore.isAdultName(name)) {
                    continue;
                }
                XtreamApi.Item item = new XtreamApi.Item(kind, f[0], name, f[1], f[2], f[3], f[4]);
                found++;
                if (!listener.onItem(item)) {
                    return;
                }
            }
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // nada a fazer
            }
        }
    }

    /** Apaga todos os índices (para usar, por exemplo, em "Trocar de lista"). */
    public static void clear(Context ctx) {
        File dir = new File(ctx.getApplicationContext().getCacheDir(), DIR);
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            f.delete();
        }
    }

    // ---------- Montagem do índice ----------

    private static boolean isFresh(File file) {
        if (!file.exists() || file.length() == 0) {
            return false;
        }
        long age = System.currentTimeMillis() - file.lastModified();
        return age >= 0 && age < TTL_MS;
    }

    private static void build(File file, String server, String user, String pass,
                              XtreamApi.Kind kind, final Cancel cancel) throws IOException {
        synchronized (BUILD_LOCK) {
            // Outra busca pode ter montado o índice enquanto esta esperava.
            if (isFresh(file)) {
                return;
            }
            File dir = file.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs() && !dir.exists()) {
                throw new IOException("sem pasta");
            }
            final File tmp = new File(file.getPath() + ".tmp");
            final BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8),
                    65536);
            boolean done = false;
            try {
                final int[] count = {0};
                final long[] bytes = {0L};
                final boolean[] stopped = {false};
                XtreamApi.streamAll(server, user, pass, kind, new XtreamApi.ResultListener() {
                    @Override
                    public boolean onItem(XtreamApi.Item item) {
                        if (cancel != null && cancel.isCancelled()) {
                            stopped[0] = true;
                            return false;
                        }
                        String line = item.id + SEP + clean(item.icon) + SEP
                                + clean(item.categoryId) + SEP + clean(item.epgId) + SEP
                                + clean(item.ext) + SEP + clean(item.name) + "\n";
                        try {
                            out.write(line);
                        } catch (IOException e) {
                            stopped[0] = true;
                            return false;
                        }
                        count[0]++;
                        bytes[0] += line.length();
                        if (count[0] >= MAX_ITEMS || bytes[0] > MAX_FILE_BYTES) {
                            stopped[0] = true;
                            return false;
                        }
                        return true;
                    }
                });
                out.flush();
                boolean cancelled = cancel != null && cancel.isCancelled();
                if (stopped[0] && !cancelled) {
                    // Parou por erro de disco ou por passar do limite: não usar índice pela metade.
                    throw new IOException("lista grande demais");
                }
                if (cancelled) {
                    return;
                }
                done = true;
            } finally {
                try {
                    out.close();
                } catch (IOException ignored) {
                    // nada a fazer
                }
                if (done) {
                    file.delete();
                    if (!tmp.renameTo(file)) {
                        tmp.delete();
                        throw new IOException("nao salvou");
                    }
                } else {
                    tmp.delete();
                }
            }
        }
    }

    /** Tira tabulação e quebra de linha, que quebrariam o formato do arquivo. */
    private static String clean(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    private static File fileFor(Context ctx, String server, String user, XtreamApi.Kind kind) {
        File dir = new File(ctx.getApplicationContext().getCacheDir(), DIR);
        return new File(dir, sha1(server + "|" + user + "|" + kind.name()) + ".idx");
    }

    private static String sha1(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(text.hashCode());
        }
    }
}
