package com.ascplay.tvbox;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Frases para o cliente quando o app não consegue falar com o servidor.
 *
 * Regra do dono (19/11/2026): problema de conexão ou servidor fora do ar NÃO mostra código de
 * erro. Mostra "Servidor em manutenção". Se o aparelho está sem internet, diz isso. Erro do
 * próprio app (código, tela, player) continua mostrando o código, para o dono conseguir corrigir.
 */
final class ServerMessage {

    static final String MAINTENANCE = "Servidor em manutenção. Tente de novo em alguns minutos.";
    static final String NO_INTERNET = "Sem internet. Confira a conexão do aparelho.";

    private ServerMessage() {
    }

    /** Frase certa para uma falha de conexão: sem internet no aparelho, ou servidor em manutenção. */
    static String connection(Context context) {
        return hasNetwork(context) ? MAINTENANCE : NO_INTERNET;
    }

    /** true se o aparelho tem rede. Se não der para saber, diz que tem (aí vale "manutenção"). */
    @SuppressWarnings("deprecation")
    static boolean hasNetwork(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager)
                    context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return true;
            }
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            return true;
        }
    }
}
