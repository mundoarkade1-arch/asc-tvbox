# Mapa dos arquivos e limites conhecidos

Este arquivo muda pouco. O andamento do projeto fica em docs/STATUS.md.
Código Java: app/src/main/java/com/ascplay/tvbox/. Layouts: app/src/main/res/layout/.

## Telas principais
- LoginActivity + CredentialStore: login por servidor (DNS, usuário, senha) ou lista M3U (campo m3uUrl, criptografado).
- HomeActivity (activity_home.xml): página inicial, 3 faixas de 10 itens (canais, filmes, séries) e cartão "Ver mais". Tem o menu à esquerda ESCRITO DIRETO no layout (cópia do layout_nav.xml, de propósito). Se mudar um menu, mudar o outro. Tem a caixinha de prévia (group_preview) e o aviso de atualização.
- BrowseActivity (servidor) e M3uBrowseActivity (lista M3U): as abas Canais, Filmes e Séries (categorias + itens, botão "Pesquisar ..."). Usam activity_browse.xml.
- SearchActivity: resultados da busca (até 100). Reaproveita activity_browse.xml, SEM menu (decisão do dono).
- SettingsActivity (activity_settings.xml): Ajustes (controle parental, trocar de lista, sobre o dispositivo, sobre o aplicativo, verificar atualização).
- MovieActivity (activity_movie.xml): página do filme (capa, informações, sinopse, Tocar, Continuar de onde parei).
- SeriesActivity (servidor) e M3uSeriesActivity (lista M3U), layout activity_series.xml: capa, sinopse (4 linhas, sem foco), Continuar, temporadas à esquerda, episódios à direita.
- MainActivity (activity_main.xml): player (ExoPlayer) de canal, filme e episódio. Extras: EXTRA_URL, EXTRA_LIST_URLS, EXTRA_LIST_IDS, EXTRA_LIST_EXTS, EXTRA_LIST_INDEX, EXTRA_SERIES_KEY, EXTRA_START_MS.

## Menu à esquerda
- layout_nav.xml: coluna group_nav (88dp), botões nav_home, nav_live, nav_movie, nav_series, nav_settings e nav_refresh (hoje ImageButton 64dp, só ícone, fundo bg_button). Incluído (escondido por padrão) em activity_browse.xml e activity_settings.xml.
- NavBar.java: NavBar.bind(activity, abaAtual, aoAtualizar). Só usa os ids e o clique, não depende do tipo do botão. Tela inicial volta à HomeActivity; Canais/Filmes/Séries trocam de aba e fecham a de antes; Ajustes abre SettingsActivity.
- Ícones: drawable/ic_nav_home, ic_nav_live, ic_nav_movie, ic_nav_series, ic_nav_settings, ic_nav_refresh (brancos, 32dp).

## Dados e rede
- XtreamApi.java: Item (tem url e icon), Kind (LIVE, MOVIE, SERIES, EPISODE), Episode, Epg, Program, Details. Funções: items, episodes, epg, schedule, details, search, streamUrl. NUNCA chamar na thread principal.
- M3uApi.java: lê a lista linha por linha, sem guardá-la inteira (para em 200 MB ou 1 minuto). Funções: sample, categories, page, episodes, search. Séries agrupadas pelo título.
- EpgCache.java: até 80 canais, 1 minuto cada, no máximo 2 pedidos ao mesmo tempo.
- EpgDialog.java: janela da programação (até 6 programas), pede ao servidor só ao abrir. Fecha com Voltar, botão Fechar ou a tecla de menu de novo.
- ImageLoader.java: carrega capas (tem clear()).
- Updater.java: atualização automática.

## Guardado no aparelho (sempre com chave em SHA-1, nunca texto)
- SeriesProgress: posição do episódio na fila de cada série (máx. 50 séries).
- PlaybackProgress: minuto e duração de filmes e episódios (máx. 300 itens). Visto = 95% ou mais. Parcial = 5 segundos ou mais e não terminou.
- ParentalStore: senha de 4 números (PBKDF2 com SHA-1, pois o Android 7.1 não tem SHA-256 nesse método), trava depois de 5 erros, reconhece adulto só pelo nome (e pela categoria na lista M3U).
- PinDialog (teclado de números) e SearchKeyboard (teclado de letras, 2 a 40 letras): SearchKeyboard.show(activity, título, listener) devolve o texto em onSearch(texto).

## Regras técnicas que valem sempre
- Aparelho de 1 GB de RAM: pedir só o que está na tela, nunca guardar listas inteiras.
- Tudo navegável só com setas, OK e Voltar (e modo mouse), com foco bem visível.
- Telas de lista usam ListView com item_category.xml; a lista controla o foco (linha com setFocusable(false)).
- Lista M3U: o "id" dos itens é só o número de ordem; o player exige um número como id.
- Ao mexer numa tela do servidor, criar tela irmã para a lista M3U (BrowseActivity/M3uBrowseActivity, SeriesActivity/M3uSeriesActivity).
- Não usar nome de classe interna igual ao do Java (ex.: Entry). item_card.xml já tem tv_now.
- Ao mudar layout e código juntos: primeiro o código que deixa de usar o item, depois o layout. Classe nova primeiro, registro no AndroidManifest.xml depois.

## Limites conhecidos (não são erros)
- Lista M3U: sem guia de programação (decisão do dono). Sem sinopse (mostra "Sem sinopse nesta lista."). "Ver mais" e busca em lista grande podem demorar até 1 minuto. Categoria identificada pelo nome. Séries achadas pelo título; episódio sem "S01E03" vai para a temporada 1. Máximo 500 episódios por série. Se a soma dos endereços da fila passar de 100 mil letras, o próximo episódio não toca sozinho.
- Servidor: itens adultos saem DEPOIS de buscar os 10 primeiros (faixa pode ter menos de 10). Resposta de busca limitada a 80 MB. details() lê o bloco "info" e para; se o servidor usar outros nomes de campo, a página fica sem capa ou sem sinopse.
- Onde parou: guarda a posição na fila; se o servidor mudar a ordem dos episódios, pode apontar para outro. Se a lista M3U trocar os endereços, o "visto" deixa de valer.
- Controle parental: abrir Ajustes não pede a senha (só trocar ou desativar). "Trocar de lista" não apaga o login antigo; o novo o substitui.
- Player: setas esquerda e direita pulam 10 segundos só em filme e episódio (intervalo mínimo de 120 ms). Controles semi-transparentes por setAlpha(0.65) no exo_controller (se não aparecer o efeito, trocar por layout próprio). Cores de episódio visto são no texto, não no fundo.
- Programação do canal: abre com a tecla de menu (três listras) ou segurando o OK (o tempo varia por controle). Não usa o EpgCache.
