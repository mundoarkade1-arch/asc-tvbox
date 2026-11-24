package com.ascplay.tvbox;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Ajuda a ordenar as categorias de CANAIS: as que mostram o que passa no dia
 * (jogos do dia, lutas, UFC, canal do cliente) vão para o topo, na ordem abaixo.
 * As outras ficam em ordem alfabética (feito por quem chama).
 */
public final class CategoryOrder {

    /** Valor das categorias comuns (sem prioridade). */
    public static final int NORMAL = 100;

    private static final Pattern UFC = Pattern.compile("\\bufc\\b");

    private CategoryOrder() {
    }

    /** Quanto menor, mais acima na lista. */
    public static int rank(String name) {
        if (name == null) {
            return NORMAL;
        }
        String n = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        if (n.contains("jogos do dia") || n.contains("jogo do dia")
                || n.contains("jogos de hoje") || n.contains("jogos hoje")
                || n.contains("jogos ao vivo")) {
            return 0;
        }
        if (n.contains("luta") || n.contains("eventos do dia") || n.contains("eventos de hoje")) {
            return 1;
        }
        if (UFC.matcher(n).find()) {
            return 2;
        }
        if (n.contains("cliente")) {
            return 3;
        }
        // Plataformas de jogos e pay-per-view: logo depois, antes do resto do esporte
        if (n.contains("ppv") || n.contains("pay per view") || n.contains("pay-per-view")
                || n.contains("disney") || n.contains("hbo max") || n.contains("paramount")
                || n.contains("prime video") || n.contains("star+") || n.contains("star plus")) {
            return 4;
        }
        return NORMAL;
    }
}
