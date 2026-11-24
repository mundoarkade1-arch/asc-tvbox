package com.ascplay.tvbox;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Organização única das categorias de CANAIS: descobre a qual dos 15 grupos padrão
 * (Esportes, Reality Shows, Canais Abertos, Filmes...) uma categoria do servidor pertence, qualquer que seja o
 * nome que o servidor usa. Só lê o nome: nunca muda nome, id nem endereço do servidor.
 *
 * Classe pura (sem Android), para poder ser testada fora do aparelho. As correções feitas à
 * mão ficam em CategoryOverrides.
 *
 * Limite conhecido: classifica pelo NOME DA CATEGORIA. Olhar canal por canal exigiria carregar
 * a lista inteira (o app é feito para 1 GB de RAM e pede só o que está na tela).
 */
public final class CategoryGroups {

    /**
     * Os 15 grupos, na ordem em que aparecem na lista de canais ao vivo (pedido do dono, 23/11/2026):
     * jogos do dia e canal do cliente, esportes, abertos, documentários, filmes, séries,
     * religiosos, variedades (música, TNT...), depois o que sobrar (notícias, infantil, reality,
     * outros), canais 24 horas, internacionais e, por último, adultos.
     */
    public enum Group {
        INICIO("Início / Destaques", 0),
        ESPORTES("Esportes", 1),
        ABERTOS("Canais Abertos", 2),
        DOCUMENTARIOS("Documentários", 5),
        FILMES("Filmes e Entretenimento", 3),
        SERIES("Séries", 4),
        RELIGIOSOS("Religiosos", 14),
        VARIEDADES("Variedades", 8),
        NOTICIAS("Notícias", 6),
        INFANTIL("Infantil", 7),
        REALITY("Reality Shows", 13),
        OUTROS("Outros Canais", 10),
        H24("Canais 24 Horas", 9),
        INTERNACIONAIS("Internacionais", 11),
        ADULTOS("Adultos", 12);

        public final String label;
        /**
         * Número FIXO do grupo, guardado nas correções manuais (CategoryOverrides). Nunca mude
         * o número de um grupo existente, senão as correções guardadas apontam para o grupo errado.
         * (Antes era a posição na lista; os 13 primeiros mantêm o número que já tinham.)
         */
        public final int code;

        Group(String label, int code) {
            this.label = label;
            this.code = code;
        }

        /** 1 a 15: quanto menor, mais acima. */
        public int order() {
            return ordinal() + 1;
        }
    }

    private static final Pattern INICIO = Pattern.compile(
            "\\bjogos? (do dia|de hoje|hoje|ao vivo|da rodada|da semana|da noite|da tarde|de amanha)\\b"
                    + "|\\beventos? (do dia|de hoje|ao vivo)\\b"
                    + "|\\bprogramacao especial\\b|\\bdestaques?\\b|\\bfavorit\\w*"
                    + "|\\brecomenda\\w*|\\bmais assistid\\w*|\\brecentemente\\b"
                    + "|\\bcana(l|is) (do |dos )?clientes?\\b|\\bclientes?\\b");

    /** Marca de conteúdo brasileiro: não vira Internacional. */
    private static final Pattern BRASIL = Pattern.compile(
            "\\b(brasil|brazil|brasileir\\w*|br|nacionais?)\\b");

    /** País, região ou idioma estrangeiro. */
    private static final Pattern ESTRANGEIRO = Pattern.compile(
            "\\b(usa|eua|us|uk|latinos?|latinas?|latin|latam|portugal|portugues|portuguese"
                    + "|espanha|spain|espanol|spanish|argentina|mexico|chile|colombia|peru"
                    + "|americanos?|americana|america|english|ingles|france|frances|italia"
                    + "|italy|germany|alemanha|arabic|arabe|turkey|turquia|india|hindi|russia"
                    + "|canada|asia|europa|europe)\\b");

    /** Só a palavra "internacional": vale se nenhum gênero for achado. */
    private static final Pattern INTERNACIONAL_GENERICO = Pattern.compile(
            "\\b(internacional|internacionais|international)\\b");

    private static final Pattern H24 = Pattern.compile(
            "\\b24 ?(h|hs|hr|hrs|horas|hours)\\b|\\b24 ?(x|por) ?7\\b|\\b24 7\\b|\\bh24\\b"
                    + "|\\bnon ?stop\\b|\\b24 ?horas?\\b");

    private static final Pattern ESPORTES = Pattern.compile(
            "esport|(?<!tran)sport|futebol|football|soccer|\\bpremiere\\b|sportv|\\bespn\\b"
                    + "|combate|\\bufc\\b|\\bnba\\b|\\bnfl\\b|campeonat|libertadores"
                    + "|brasileirao|champions|\\bcopa\\b|\\bf1\\b|formula 1|\\bluta|\\bboxe\\b"
                    + "|\\bmma\\b|\\bdazn\\b|\\bgol\\b"
                    // Pedido do dono (19/11/2026): estas contam como esporte nos canais ao vivo
                    + "|\\bprime videos?\\b|\\bamazon prime\\b|\\bgold\\b|\\b(caze|kaze)\\w*"
                    + "|\\bapple tv\\b|\\bparamount(?! channel)\\b"
                    // Pedido do dono (23/11/2026): plataformas que passam jogos e pay-per-view
                    + "|\\bppv\\b|pay ?per ?view|pague ?(para|pra) ?ver|\\bhbo ?max\\b|\\bpeacock\\b"
                    // Jogos e competições (nomes comuns nas listas IPTV)
                    + "|\\bjogos?\\b|\\brodada\\b|\\bpremier league\\b|\\bla ?liga\\b|\\bbundesliga\\b"
                    + "|\\bligue 1\\b|\\bserie [ab]\\b|\\bconmebol\\b|\\buefa\\b|\\bfifa\\b"
                    + "|\\bsul ?americana\\b|\\bcopa do (brasil|mundo)\\b|\\bmundial\\b|\\bolimpiada"
                    + "|\\bwwe\\b|\\bnascar\\b|\\bmotogp\\b|\\bten+is\\b|\\bvolei\\b|\\bbasquete\\b"
                    + "|\\bnbb\\b|\\bbellator\\b|\\bpfl\\b|\\bone championship\\b|\\bpaulistao\\b"
                    + "|\\bcariocao\\b|\\bestaduais\\b|\\bnosso futebol\\b");

    /**
     * Plataformas que o servidor põe como esporte, mas cujo nome perde o \"+\" na limpeza
     * (\"Disney+\" viraria \"disney\", igual ao canal infantil). Por isso olha o nome ORIGINAL.
     */
    private static final Pattern ESPORTES_NOME_ORIGINAL = Pattern.compile(
            "disney ?(\\+|plus|ppv)|\\bstar ?(\\+|plus)|\\bparamount ?\\+|\\bhbo ?max\\b|\\bmax ?\\+");

    /** Canais religiosos. */
    private static final Pattern RELIGIOSOS = Pattern.compile(
            "religio|gospel|\\bigrejas?\\b|catolic|evangel|cristao|crista\\b|cristaos|\\bdeus\\b"
                    + "|\\bpadre\\b|cancao nova|\\bmissa\\b|\\bbiblia\\b|\\bpalavra\\b"
                    + "|aparecida|rede vida|boas novas|\\bfe\\b|\\bmilagre|\\bpastor\\b|\\bvaticano\\b");

    /** Reality shows (só canais ao vivo). */
    private static final Pattern REALITY = Pattern.compile(
            "\\breality\\b|\\bbbb\\b|big brother|\\bfazenda\\b|de volta com o ex"
                    + "|power couple|\\bpeoes\\b|\\bilha record\\b");

    private static final Pattern INFANTIL = Pattern.compile(
            "infantil|\\bkids?\\b|desenho|cartoon|crianc|\\bnick\\w*|gloob|\\bbaby\\b"
                    + "|\\bjunior\\b|disney|boomerang|\\banimes?\\b");

    private static final Pattern NOTICIAS = Pattern.compile(
            "notici|\\bnews\\b|jornal|telejornal|globonews|\\bcnn\\b|\\bbbc\\b");

    private static final Pattern DOCUMENTARIOS = Pattern.compile(
            "document|discovery|\\bhistory\\b|\\bhistoria\\b|nat geo|national geographic"
                    + "|animal planet|ciencia|\\bscience\\b|\\bnatureza\\b|\\bnature\\b"
                    + "|\\bbiografia|\\bcuriosidade");

    private static final Pattern FILMES = Pattern.compile(
            "\\bfilmes?\\b|\\bmovies?\\b|cinema|\\bcine\\b|cinemax|\\bhbo\\b|telecine"
                    + "|megapix|\\bspace\\b|\\bamc\\b|paramount|lancamento");

    private static final Pattern SERIES = Pattern.compile(
            "\\bseries?\\b|seriado|sitcom|\\baxn\\b|\\bwarner\\b|\\bsony\\b|\\btv shows?\\b");

    private static final Pattern ABERTOS = Pattern.compile(
            "\\baberto|\\baberta|\\bglobo\\b|\\brecord\\b|\\bsbt\\b|\\bband\\b|\\bredetv\\b"
                    + "|\\brede tv\\b|\\btv brasil\\b|\\bemissoras?\\b|\\bafiliad\\w*"
                    + "|\\bregional|\\bregionais\\b|\\btv cultura\\b|\\bgazeta\\b"
                    + "|\\btv (camara|senado|justica|escola)\\b|\\bfutura\\b");

    private static final Pattern VARIEDADES = Pattern.compile(
            "variedade|\\bmusic\\w*|culinar|\\bcook\\w*|gastronom|\\bviage\\w*|\\btravel\\b"
                    + "|\\bcultura\\b|entretenimento|lifestyle|\\bmtv\\b"
                    + "|multishow|\\bvh1\\b|\\bcomedy\\b|\\bhumor\\b|\\bnovelas?\\b|\\bshows?\\b"
                    + "|\\btnt\\b|\\bradio\\b|\\bclipes?\\b|\\bplaylist\\b");

    private CategoryGroups() {
    }

    /** Grupo automático da categoria, pelo nome. Nunca devolve null. */
    public static Group auto(String categoryName) {
        String n = simplify(categoryName);
        if (n.isEmpty()) {
            return Group.OUTROS;
        }
        // Adulto sempre primeiro (mesma regra do controle parental)
        if (ParentalStore.isAdultCategory(categoryName)) {
            return Group.ADULTOS;
        }
        if (INICIO.matcher(n).find()) {
            return Group.INICIO;
        }
        // Estrangeiro não se mistura com o brasileiro, mesmo sendo do mesmo gênero
        if (!BRASIL.matcher(n).find() && ESTRANGEIRO.matcher(n).find()) {
            return Group.INTERNACIONAIS;
        }
        boolean esportes = ESPORTES.matcher(n).find()
                || ESPORTES_NOME_ORIGINAL.matcher(original(categoryName)).find();
        boolean noticias = NOTICIAS.matcher(n).find();
        // "24h" só vale como canal de programação contínua quando não é esporte nem notícia
        // (um canal de notícias 24h continua sendo de Notícias)
        if (H24.matcher(n).find() && !esportes && !noticias) {
            return Group.H24;
        }
        if (esportes) {
            return Group.ESPORTES;
        }
        if (RELIGIOSOS.matcher(n).find()) {
            return Group.RELIGIOSOS;
        }
        if (REALITY.matcher(n).find()) {
            return Group.REALITY;
        }
        if (INFANTIL.matcher(n).find()) {
            return Group.INFANTIL;
        }
        if (noticias) {
            return Group.NOTICIAS;
        }
        if (DOCUMENTARIOS.matcher(n).find()) {
            return Group.DOCUMENTARIOS;
        }
        if (FILMES.matcher(n).find()) {
            return Group.FILMES;
        }
        if (SERIES.matcher(n).find()) {
            return Group.SERIES;
        }
        if (ABERTOS.matcher(n).find()) {
            return Group.ABERTOS;
        }
        if (VARIEDADES.matcher(n).find()) {
            return Group.VARIEDADES;
        }
        if (INTERNACIONAL_GENERICO.matcher(n).find()) {
            return Group.INTERNACIONAIS;
        }
        return Group.OUTROS;
    }

    /** Só tira acentos e deixa minúsculo (mantém \"+\" e outros símbolos). */
    private static String original(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** Tira acentos, deixa minúsculo e troca símbolos por espaço ("|BR| Séries" vira "br series"). */
    static String simplify(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return n.replaceAll("[^a-z0-9]+", " ").trim();
    }
}
