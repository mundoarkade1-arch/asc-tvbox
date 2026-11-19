package com.ascplay.tvbox;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Organização única das categorias de CANAIS: descobre a qual dos 13 grupos padrão
 * (Esportes, Canais Abertos, Filmes...) uma categoria do servidor pertence, qualquer que seja o
 * nome que o servidor usa. Só lê o nome: nunca muda nome, id nem endereço do servidor.
 *
 * Classe pura (sem Android), para poder ser testada fora do aparelho. As correções feitas à
 * mão ficam em CategoryOverrides.
 *
 * Limite conhecido: classifica pelo NOME DA CATEGORIA. Olhar canal por canal exigiria carregar
 * a lista inteira (o app é feito para 1 GB de RAM e pede só o que está na tela).
 */
public final class CategoryGroups {

    /** Os 13 grupos, na ordem em que aparecem. */
    public enum Group {
        INICIO("Início / Destaques"),
        ESPORTES("Esportes"),
        ABERTOS("Canais Abertos"),
        FILMES("Filmes e Entretenimento"),
        SERIES("Séries"),
        DOCUMENTARIOS("Documentários"),
        NOTICIAS("Notícias"),
        INFANTIL("Infantil"),
        VARIEDADES("Variedades"),
        H24("Canais 24 Horas"),
        OUTROS("Outros Canais"),
        INTERNACIONAIS("Internacionais"),
        ADULTOS("Adultos");

        public final String label;

        Group(String label) {
            this.label = label;
        }

        /** 1 a 13: quanto menor, mais acima. */
        public int order() {
            return ordinal() + 1;
        }
    }

    private static final Pattern INICIO = Pattern.compile(
            "\\bjogos? (do dia|de hoje|hoje)\\b|\\beventos? (do dia|de hoje)\\b"
                    + "|\\bprogramacao especial\\b|\\bdestaques?\\b|\\bfavorit\\w*"
                    + "|\\brecomenda\\w*|\\bmais assistid\\w*|\\brecentemente\\b"
                    + "|\\bcana(l|is) do cliente\\b");

    /** Marca de conteúdo brasileiro: não vira Internacional. */
    private static final Pattern BRASIL = Pattern.compile(
            "\\b(brasil|brazil|brasileir\\w*|br|nacionais?)\\b");

    /** País, região ou idioma estrangeiro. */
    private static final Pattern ESTRANGEIRO = Pattern.compile(
            "\\b(usa|eua|us|uk|latino|latina|latin|latam|portugal|portugues|portuguese"
                    + "|espanha|spain|espanol|spanish|argentina|mexico|chile|colombia|peru"
                    + "|americanos?|americana|america|english|ingles|france|frances|italia"
                    + "|italy|germany|alemanha|arabic|arabe|turkey|turquia|india|hindi|russia"
                    + "|canada|asia|europa|europe)\\b");

    /** Só a palavra "internacional": vale se nenhum gênero for achado. */
    private static final Pattern INTERNACIONAL_GENERICO = Pattern.compile(
            "\\b(internacional|internacionais|international)\\b");

    private static final Pattern H24 = Pattern.compile(
            "\\b24 ?(h|hs|hr|hrs|horas|hours)\\b|\\b24 7\\b");

    private static final Pattern ESPORTES = Pattern.compile(
            "esport|\\bsports?\\b|futebol|football|soccer|\\bpremiere\\b|sportv|\\bespn\\b"
                    + "|combate|\\bufc\\b|\\bnba\\b|\\bnfl\\b|campeonat|libertadores"
                    + "|brasileirao|champions|\\bcopa\\b|\\bf1\\b|formula 1|\\bluta|\\bboxe\\b"
                    + "|\\bmma\\b|\\bdazn\\b|\\bgol\\b");

    private static final Pattern INFANTIL = Pattern.compile(
            "infantil|\\bkids?\\b|desenho|cartoon|crianc|\\bnick\\w*|gloob|\\bbaby\\b"
                    + "|\\bjunior\\b|disney|boomerang|\\banimes?\\b");

    private static final Pattern NOTICIAS = Pattern.compile(
            "notici|\\bnews\\b|jornal|telejornal|globonews|\\bcnn\\b|\\bbbc\\b");

    private static final Pattern DOCUMENTARIOS = Pattern.compile(
            "document|discovery|\\bhistory\\b|\\bhistoria\\b|nat geo|national geographic"
                    + "|animal planet|ciencia|\\bscience\\b");

    private static final Pattern FILMES = Pattern.compile(
            "\\bfilmes?\\b|\\bmovies?\\b|cinema|\\bcine\\b|cinemax|\\bhbo\\b|telecine"
                    + "|megapix|\\btnt\\b|\\bspace\\b|\\bamc\\b|paramount|lancamento");

    private static final Pattern SERIES = Pattern.compile(
            "\\bseries?\\b|seriado|sitcom|\\baxn\\b|\\bwarner\\b|\\bsony\\b|\\btv shows?\\b");

    private static final Pattern ABERTOS = Pattern.compile(
            "\\baberto|\\baberta|\\bglobo\\b|\\brecord\\b|\\bsbt\\b|\\bband\\b|\\bredetv\\b"
                    + "|\\brede tv\\b|\\btv brasil\\b|\\bemissoras?\\b|\\bafiliad\\w*"
                    + "|\\bregional|\\bregionais\\b|\\btv cultura\\b");

    private static final Pattern VARIEDADES = Pattern.compile(
            "variedade|\\bmusic\\w*|culinar|\\bcook\\w*|gastronom|\\bviage\\w*|\\btravel\\b"
                    + "|\\breality\\b|\\bcultura\\b|entretenimento|lifestyle|\\bmtv\\b"
                    + "|multishow|\\bvh1\\b|\\bcomedy\\b|\\bhumor\\b|\\bnovelas?\\b|\\bshows?\\b");

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
        boolean esportes = ESPORTES.matcher(n).find();
        boolean noticias = NOTICIAS.matcher(n).find();
        // "24h" só vale como canal de programação contínua quando não é esporte nem notícia
        // (um canal de notícias 24h continua sendo de Notícias)
        if (H24.matcher(n).find() && !esportes && !noticias) {
            return Group.H24;
        }
        if (esportes) {
            return Group.ESPORTES;
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
