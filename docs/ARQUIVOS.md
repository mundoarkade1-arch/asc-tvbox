# Mapa dos arquivos e limites conhecidos

Muda pouco. O andamento fica em docs/STATUS.md. Montagem e versão: docs/ARCHITECTURE.md.
Java: app/src/main/java/com/ascplay/tvbox/. Layouts: app/src/main/res/layout/. Ícones e fundos: app/src/main/res/drawable/.
Outros: res/values/strings.xml, res/xml/file_paths.xml, AndroidManifest.xml, app/build.gradle, build.gradle, settings.gradle, gradle.properties, .github/workflows/apk.yml. Textos: README.md, CLAUDE.md, llms.txt, AGENTS.md, docs/*.

## Telas
- LoginActivity (activity_login.xml) + LoginChecker + CredentialStore: login por servidor (DNS, usuário, senha) ou lista M3U (campo m3uUrl, criptografado). LoginChecker confere antes de salvar: servidor via player_api.php (conta ativa e não vencida) e M3U lendo só o começo (precisa começar com #EXTM3U ou #EXTINF). Segue até 5 redirecionamentos, espera 15 s, não registra senha em log. Roda fora da thread principal.
- HomeActivity (activity_home.xml): página inicial, 3 faixas de 10 itens (canais, filmes, séries) e cartão "Ver mais". O menu à esquerda está ESCRITO DIRETO neste layout (cópia do layout_nav.xml, de propósito): mudou um, mude o outro. Tem o aviso de atualização. NÃO tem caixinha de prévia (removida na R2). A variável focusedLive guarda o canal ao vivo (servidor) com o foco, só para a tecla de menu e o OK segurado abrirem a programação. Ao abrir canal ao vivo, manda EXTRA_QUEUE = true, sem categoria (R4).
- BrowseActivity (servidor) e M3uBrowseActivity (lista M3U), activity_browse.xml: abas Canais, Filmes e Séries (categorias + itens, botão "Pesquisar ..."). Ordem do foco: menu, categorias, itens. No dispatchKeyEvent, seta esquerda na primeira coluna da grade chama goToCategories (R3). Da lista de categorias, esquerda vai ao menu. Ao abrir canal ao vivo, mandam EXTRA_QUEUE = true e a categoria aberta (BrowseActivity: currentCat; M3uBrowseActivity: currentGroup); em "Todas as categorias" não mandam categoria (R4).
- SearchActivity: resultados da busca (até 100). Reaproveita activity_browse.xml, SEM menu (decisão do dono). Ao abrir canal ao vivo, manda EXTRA_QUEUE = true, sem categoria (R4).
- SettingsActivity (activity_settings.xml): controle parental, trocar de lista, sobre o dispositivo, sobre o aplicativo, verificar atualização.
- MovieActivity (activity_movie.xml): capa, informações, sinopse, Tocar e Continuar de onde parei.
- SeriesActivity (servidor) e M3uSeriesActivity (lista M3U), activity_series.xml: capa, sinopse (4 linhas, sem foco), Continuar, temporadas à esquerda, episódios à direita. Sem menu à esquerda.
- MainActivity (activity_main.xml): player ExoPlayer de canal, filme e episódio. Extras: EXTRA_URL, EXTRA_LIST_URLS, EXTRA_LIST_IDS, EXTRA_LIST_EXTS, EXTRA_LIST_INDEX, EXTRA_SERIES_KEY, EXTRA_START_MS, EXTRA_QUEUE (true = canal ao vivo troca de canal com cima e baixo), EXTRA_QUEUE_CAT (categoria de origem; ausente ou nulo = todas). Barra Agora (esquerda) e A seguir (direita) só em canal ao vivo do servidor.
- ChannelQueue.java (R4): fila de canais ao vivo usada pelo MainActivity. Categorias em ordem, uma por vez na memória (páginas de 40, máx. 400), dá a volta no fim. Servidor: categoria = número; M3U: categoria = nome do grupo; nula = todas. Pula categorias adultas com parental ligado. Roda fora da thread principal; responde pelo Listener (onChannel, onFail).
- item_card.xml (cartão, tem tv_now), item_category.xml (linha de lista).

## Menu à esquerda
- layout_nav.xml: coluna group_nav (200dp), botões nav_home, nav_live, nav_movie, nav_series, nav_settings e nav_refresh. Cada botão é um TextView (56dp de altura, fundo bg_button, clickable e focusable) com o ícone à esquerda (drawableLeft) e o nome ao lado: Tela inicial, Canais, Filmes, Séries, Ajustes, Atualizar. Incluído (escondido por padrão, também com 200dp no include) em activity_browse.xml e activity_settings.xml.
- NavBar.java: NavBar.bind(activity, abaAtual, aoAtualizar). Só usa os ids e o clique, não o tipo do botão. Tela inicial volta à HomeActivity; Canais, Filmes e Séries trocam de aba e fecham a de antes; Ajustes abre a SettingsActivity.
- Ícones: ic_nav_home, ic_nav_live, ic_nav_movie, ic_nav_series, ic_nav_settings, ic_nav_refresh (brancos, 32dp). Fundos: bg_button, bg_card, bg_field.

## Dados e rede
- XtreamApi: Item (tem url e icon), Kind (LIVE, MOVIE, SERIES, EPISODE), Episode, Epg, Program, Details. Funções: items, episodes, epg, schedule, details, search, streamUrl, categories. NUNCA chamar na thread principal.
- M3uApi: lê a lista linha por linha, sem guardá-la inteira (para em 200 MB ou 1 minuto). Funções: sample, categories, page, episodes, search. Séries agrupadas pelo título.
- EpgCache: até 80 canais, 1 minuto cada, 2 pedidos ao mesmo tempo. EpgDialog: janela da programação (até 6), pede ao servidor só ao abrir, não usa o cache. Fecha com Voltar, botão Fechar ou a tecla de menu de novo.
- ImageLoader: capas (tem clear()). Updater: atualização automática.

## Guardado no aparelho (chave sempre em SHA-1, nunca texto)
- SeriesProgress: posição do episódio na fila de cada série (máx. 50).
- PlaybackProgress: minuto e duração de filmes e episódios (máx. 300). Visto = 95% ou mais; parcial = 5 s ou mais e não terminou.
- ParentalStore: senha de 4 números (PBKDF2 com SHA-1, pois o Android 7.1 não tem SHA-256 nesse método), trava depois de 5 erros, reconhece adulto pelo nome (e pela categoria na lista M3U).
- PinDialog (teclado de números). SearchKeyboard (letras, 2 a 40): SearchKeyboard.show(activity, título, listener) devolve o texto em onSearch(texto).

## Regras técnicas que valem sempre
- Lista M3U: o "id" dos itens é só o número de ordem; o player exige um número como id.
- Telas de lista usam ListView com item_category.xml; a lista controla o foco (linha com setFocusable(false)). Por isso a ordem do foco entre lista e grade é tratada no código (dispatchKeyEvent), não no layout.
- Ao abrir canal ao vivo, a tela manda EXTRA_QUEUE = true. A categoria só é mandada quando existe (não mandar nada = todas; evita putExtra com null solto).
- Ver também as regras de código no AGENTS.md.

## Limites conhecidos (não são erros)
- Lista M3U: sem guia de programação (decisão do dono); sem sinopse ("Sem sinopse nesta lista."); "Ver mais" e busca em lista grande podem levar até 1 minuto; categoria identificada pelo nome; séries achadas pelo título (episódio sem "S01E03" vai para a temporada 1); máx. 500 episódios por série; se a soma dos endereços da fila passar de 100 mil letras, o próximo episódio não toca sozinho.
- Servidor: itens adultos saem DEPOIS de buscar os 10 primeiros (faixa pode ter menos de 10); resposta da busca limitada a 80 MB; details() lê só o bloco "info" e, se o servidor usar outros nomes de campo, a página fica sem capa ou sem sinopse.
- Onde parou guarda a posição na fila: se o servidor mudar a ordem dos episódios, pode apontar para outro. Na lista M3U, se os endereços mudarem, o "visto" deixa de valer.
- Controle parental: abrir Ajustes não pede a senha (só trocar ou desativar). "Trocar de lista" não apaga o login antigo; o novo o substitui.
- Player: setas esquerda e direita pulam 10 s só em filme e episódio (mínimo 120 ms entre pulos). Controles semi-transparentes com setAlpha(0.65) no exo_controller. Cores de episódio visto são no texto, não no fundo.
- Troca de canal (R4): cima/baixo só funcionam se a tela que abriu o canal mandar EXTRA_QUEUE. Hoje as quatro telas (Home, Browse, M3uBrowse, Search) mandam. Se o canal tocado não estiver nos primeiros 400 da categoria (ou da fila "todas"), as setas ficam desligadas, sem erro. Na lista M3U cada troca relê a lista (pode levar alguns segundos). Fim da lista: dá a volta (escolha do agente, a testar).
- Programação do canal: abre com a tecla de menu (três listras) ou segurando o OK (o tempo varia por controle).
- Menu com texto (R1), tela inicial sem caixinha (R2), ordem do foco nas abas (R3) e troca de canal (R4): ainda não testados no TX9. Se algum nome do menu não couber nos 200dp, reduzir a letra ou alargar a coluna.
- Ordem do foco: a seta esquerda na primeira coluna da grade vai às categorias (usa getNumColumns).
