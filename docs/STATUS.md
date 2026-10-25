# Status do projeto

Última atualização: 24/10/2026

## Fase atual
FASE ATUAL (decidida com o dono em 24/10/2026): MUDAR TODA A APARÊNCIA E A NAVEGAÇÃO DO APP PRIMEIRO. Os testes no TX9 ficam para DEPOIS da nova aparência (as telas e funções vão mudar, então testar agora seria perder tempo). As ideias estão na seção "Nova interface" abaixo. O dono ainda pode lembrar de mais pedidos; se falar, anotar aqui e no DECISIONS.md.

Andamento da nova aparência (uma parte de cada vez):
- (a) Menu à esquerda + Tela inicial: FEITA (verde no Actions, ainda NÃO testada no TX9).
- (b) Abas Canais, Filmes e Séries com busca própria: FEITA (verde, NÃO testada no TX9). Veja "Parte (b)" abaixo.
- (c) Aba Ajustes: FEITA (verde, NÃO testada no TX9). Veja "Parte (c)" abaixo.
- (d) Página do filme e da série com capa e sinopse: EM ANDAMENTO. Veja "Parte (d)" abaixo. Passo 1 feito (XtreamApi.details). PRÓXIMO PASSO: criar o layout novo activity_movie.xml.
- (e) Player (botões transparentes, barra que some rápido, foco livre) e "continuar de onde parei" com minuto exato: a fazer
- (f) Barra Agora / A seguir e a programação com a tecla das três listras: a fazer

Antes da nova aparência, todas as etapas de código estavam VERDES no Actions, mas o roteiro de teste no TX9 ainda NÃO foi feito:
- Etapa 3: passar sozinho para a próxima temporada (servidor e lista M3U).
- Etapa 1: lembrar o episódio onde a pessoa parou (servidor e lista M3U).
- Etapa 4: busca por nome, LIGADA (SearchActivity registrada no AndroidManifest.xml).
- Conserto: no "Ver mais" da lista M3U, tocar numa série abre a M3uSeriesActivity.
O guia de programação (EPG) da lista M3U ficou de fora por decisão do dono (ver DECISIONS.md).

## Feito (testado no TX9)
- Repositório criado e documentação preenchida (README, AGENTS, CLAUDE, llms.txt, docs/*)
- Aparelho de teste identificado e documentado (TX9, S905W, Android 7.1, 1 GB RAM, tela 1080p)
- Decisão tomada: app Android nativo em Java com ExoPlayer, sem WebView (ver docs/DECISIONS.md)
- Esqueleto do app nativo e workflow do GitHub Actions que gera o APK
- Login por servidor (DNS, usuário e senha) e por lista M3U, com mensagens de erro corretas
- Atualização automática do app (aviso na página inicial, instalação pelo próprio app)
- Abrir direto na página inicial quando há login salvo
- Página inicial com 3 faixas (Canais ao vivo, Filmes, Séries) de 10 itens cada, nos dois logins
- Tocar canal e filme (servidor e lista M3U)
- "Ver mais" por categorias nos dois logins (BrowseActivity e M3uBrowseActivity)
- Linha "Agora: ..." nos canais, caixinha de prévia na página inicial e barra Agora / A seguir no player (só no login por servidor)
- Controle parental completo (senha de 4 números, cadeado nas categorias adultas, itens adultos escondidos, trava depois de 5 erros)
- Séries com episódios nos dois logins: temporadas à esquerda, episódios à direita, próximo episódio da temporada toca sozinho

## Feito (verde no Actions, ainda NÃO testado no TX9)
- Próxima temporada sozinha: a tela da série manda ao player a fila de TODOS os episódios da série (temporadas normais em ordem, "Especiais" = temporada 0 no fim). No último episódio de todos o player mostra "Fim da série." (servidor e lista M3U).
- Lembrar onde parou: ao começar a tocar um episódio, o player anota a posição dele na fila (SeriesProgress). Ao abrir a série de novo, a tela vai para a temporada certa e deixa o episódio selecionado. Guarda no máximo 50 séries. Guarda só o episódio, não o minuto.
- Busca por nome: botão "Pesquisar ..." em cada aba, teclado de letras, tela de resultados. Servidor e lista M3U.
- "Ver mais" da lista M3U: série abre a M3uSeriesActivity (antes mostrava um aviso).
- Parte (a) da nova aparência: menu vertical à esquerda na página inicial, só com ícones.
- Parte (b) da nova aparência: botão "Pesquisar ..." nas telas de aba (servidor e lista M3U), menu à esquerda dentro das duas abas e botão "Buscar" removido da página inicial.
- Parte (c) da nova aparência: aba Ajustes (SettingsActivity) com controle parental, trocar de lista, sobre o dispositivo, sobre o aplicativo e verificar atualização. Botões "Controle parental" e "Trocar login" removidos da página inicial.

## Feito (salvo pelo dono em 24/10/2026; o Actions NÃO foi confirmado como verde na conversa; CONFERIR)
- Parte (d), passo 1: XtreamApi.java ganhou a classe Details (capa, sinopse, linha de informações) e a função details(server, user, pass, kind, id). Nada usa a função ainda.

## Parte (a): menu à esquerda na página inicial (feita, verde, NÃO testada no TX9)
- Menu vertical à esquerda, com 6 ícones: Tela inicial, Canais, Filmes, Séries, Ajustes e (embaixo) Atualizar.
- Tela inicial: leva o foco para o primeiro canal (ou filme, ou série) já carregado.
- Canais, Filmes e Séries: abrem a aba (tela BrowseActivity ou M3uBrowseActivity).
- Ajustes: abre a SettingsActivity (a partir da parte c).
- Atualizar: recarrega as três faixas.
- A caixinha de prévia do canal continua embaixo, só na parte da direita (ao lado do menu).
- Os botões do menu usam o fundo bg_button, que já existia. A CONFERIR no TX9: se o foco (item selecionado) aparece bem visível nos ícones.

## Parte (b): abas com busca própria (FEITA, verde, NÃO testada no TX9)
- As telas de aba (BrowseActivity do servidor e M3uBrowseActivity da lista M3U) têm um botão "Pesquisar canais", "Pesquisar filmes" ou "Pesquisar séries" no alto, ao lado do título. OK nele abre o teclado de letras (SearchKeyboard) e, ao buscar, abre a SearchActivity SÓ com aquele tipo.
- O menu à esquerda aparece dentro das DUAS abas (servidor e lista M3U), ligado pelo NavBar.java. Dentro da aba: Tela inicial volta à página inicial; Canais/Filmes/Séries trocam de aba (a aba de antes fecha); Ajustes abre a tela Ajustes (a aba de antes fecha); Atualizar recarrega a categoria aberta. No servidor também limpa o cache do guia; na lista M3U não há guia para limpar.
- O botão "Buscar" e o menu de tipo (Canais, Filmes, Séries) saíram da página inicial (HomeActivity.java e activity_home.xml).
- DECIDIDO com o dono: a tela de resultados da busca (SearchActivity) fica SEM o menu à esquerda. A busca sempre nasce de uma aba e o Voltar leva de volta a ela.

## Parte (c): aba Ajustes (FEITA, verde, NÃO testada no TX9)
- Tela nova SettingsActivity (layout activity_settings.xml): menu à esquerda, lista de 5 opções no meio (ListView, escolhe com setas e OK) e, à direita, uma área de texto (tv_detail) que mostra as informações.
- Opções: (1) Controle parental: ativar, trocar a senha ou desativar (mesmas telas e regras de antes, com senha de 4 números); (2) Trocar de lista: abre o login e fecha todas as telas de antes; (3) Sobre o dispositivo: modelo, versão do Android, memória (total e livre agora), armazenamento (livre e total) e tamanho da tela; (4) Sobre o aplicativo: versão instalada; (5) Verificar atualização: procura versão nova; se achar, a linha vira "Instalar atualização" e OK instala.
- Abre pelo ícone Ajustes do menu (página inicial e abas). Dentro de Ajustes, o ícone Ajustes não faz nada.
- A página inicial não tem mais os botões "Controle parental" e "Trocar login". Quando a pessoa volta de Ajustes, a página inicial confere se o controle parental mudou e, se mudou, recarrega as faixas sozinha (onResume).
- O aviso "Login não reconhecido" da página inicial agora manda a pessoa para Ajustes, em "Trocar de lista".
- O aviso de atualização na página inicial (Instalar agora / Depois) continua como era.
- A resposta de "Sobre o dispositivo" vai dizer quanto espaço livre o TX9 tem. Quando testar, anotar em TARGET_DEVICES.md ("Armazenamento livre").

## Parte (d): página do filme e da série (EM ANDAMENTO)
DECIDIDO com o dono em 24/10/2026: a página é a MESMA nos dois logins (opção A). Na lista M3U, onde iria a sinopse aparece "Sem sinopse nesta lista"; a capa é a imagem do item (item.icon). Veja DECISIONS.md.
Feito em DOIS TEMPOS:
- Tempo 1 (parte d): páginas com capa, sinopse e botão Tocar (filme) e com capa, sinopse, temporadas e episódios (série). SEM "Continuar de onde parei" e SEM cores de episódio visto.
- Tempo 2 (parte e): quando existir o arquivo de minutos, voltar nas duas páginas e ligar o "Continuar de onde parei" (foco nele se já assistiu parte) e as cores de episódio visto / visto pela metade.
Ordem dos passos (para o Actions não ficar vermelho; um arquivo de cada vez, esperando o "verde"):
1. FEITO (verde a conferir): XtreamApi.java, função details(...) e classe Details.
2. Criar activity_movie.xml (layout novo): capa, título, linha de informações (meta), sinopse (com rolagem pelas setas se for longa) e botão Tocar. O botão "Continuar de onde parei" entra no tempo 2.
3. Criar MovieActivity.java (tela nova). Recebe por Intent: tipo, id, extensão, nome, capa (nova EXTRA_ICON) e, na lista M3U, o endereço (MainActivity.EXTRA_URL). No servidor pede XtreamApi.details numa thread, uma vez, e preenche capa (ImageLoader), meta e sinopse. Na lista M3U não pede nada: mostra a capa do item e "Sem sinopse nesta lista". O botão Tocar abre o MainActivity como o filme abria antes. Respeitar controle parental como as outras telas (só abre a partir de itens já filtrados).
4. Registrar MovieActivity no AndroidManifest.xml (só depois de a classe existir).
5. Mudar os lugares que tocavam o filme direto para abrir a MovieActivity: HomeActivity.openItem, BrowseActivity, M3uBrowseActivity e SearchActivity (canal ao vivo continua tocando direto). Pedir cada arquivo atual ao dono antes.
6. Página da série: mostrar capa e sinopse no alto da tela da série (SeriesActivity do servidor e M3uSeriesActivity da lista M3U; as duas usam activity_series.xml e precisam receber as MESMAS mudanças de aparência). Na lista M3U: capa do item e "Sem sinopse nesta lista". Pedir activity_series.xml, SeriesActivity.java e M3uSeriesActivity.java atuais ao dono antes. Cuidado com o foco: a lista de temporadas e a de episódios não podem ficar presas atrás da sinopse.
Notas técnicas de XtreamApi.details:
- Filme: get_vod_info com vod_id. Série: get_series_info com series_id. Lê só o bloco "info" e PARA (não lê a lista de episódios, se "info" vier antes dela).
- Aceita os nomes de campo mais comuns: capa (movie_image, cover_big, cover; só http/https), sinopse (plot, description; máximo 1200 letras), genre, releasedate (ou releaseDate, release_date; mostra só o ano), duration e rating (mostra "Nota X"; ignora zeros). A linha de informações junta o que existir separado por " • ".
- Devolve um Details vazio (sem erro) se o servidor não informar nada, se o id não for número ou se o tipo não for filme nem série. Erros de rede viram IOException: a tela deve tratar e mostrar a página sem sinopse.
- Faz acesso à rede: NUNCA chamar na thread principal.

## Nova interface - pedidos do dono em 24/10/2026
Ainda vão chegar mais pedidos. Ordem e detalhes combinados com o dono, um passo de cada vez.

### Menu e telas principais
- Menu vertical no lado ESQUERDO, só com ícones: Tela inicial, Canais, Filmes, Séries e Ajustes. (FEITO nas partes a, b e c)
- A Tela inicial é sempre a que abre com o app. Na parte grande da direita mostra as indicações: 10 primeiros canais, 10 primeiros filmes, 10 primeiras séries.
- Cada aba (Canais, Filmes, Séries) tem a sua barra de pesquisa, e a busca mostra SÓ aquele tipo. (FEITO na parte b)
- Ícone de atualizar na tela principal, para recarregar a lista. (FEITO na parte a)

### Aba Canais
- Mantém a grade ao lado, com a barra de pesquisa e a linha "Agora: ..." embaixo de cada canal (como já é).
- Barra embaixo da tela, no canal que está sendo assistido: "Agora" com o programa que passa e, do lado direito, o que vai passar depois.
- Tecla das "três listras" do controle do TX9 (botão de lista), apertada sobre um canal: abre a programação, com o que está passando e os próximos 5 programas. SÓ carrega quando a tecla é apertada, nunca antes.
- RESOLVIDO com o dono: o controle do TX9 TEM um botão com três listras. No Android ela costuma chegar como tecla de menu (KEYCODE_MENU). Segunda via, caso a tecla chegue diferente: segurar o OK sobre o canal também abre a programação. Conferir no TX9 se a tecla funciona.

### Página do filme
- Ao escolher um filme, abre uma página com a capa e a sinopse. (EM ANDAMENTO na parte d)
- Botão de tocar e botão "Continuar de onde parei". Se já assistiu parte, o "Continuar" é a opção já pronta para apertar (foco nele). (o "Continuar" fica para o tempo 2, na parte e)
- Isso pede lembrar o MINUTO exato (hoje o app só lembra o episódio, não o minuto).

### Página da série
- Ao escolher uma série, abre uma página com a capa e a sinopse.
- Botão para escolher a temporada e, embaixo, a lista de episódios.
- Episódios já assistidos ficam marcados com outra cor. O episódio visto pela metade fica com a cor só até a metade. (tempo 2, parte e)
- Botão "Continuar de onde parei", que vai direto ao episódio certo, sem escolher. (tempo 2, parte e)

### Player
- Botões do player semi-transparentes (dá para ver, mas são transparentes).
- A barra do player demora muito para sumir: deve sumir mais rápido.
- Hoje o foco sempre cai na opção de configurações. O dono quer o foco livre para avançar e voltar o vídeo com as setas.

### Aba Ajustes (FEITA na parte c)
- Controle parental.
- Mudar de lista / gerenciar lista (feito como "Trocar de lista").
- Sobre o dispositivo.
- Sobre o aplicativo.
- Botão para verificar se há atualização.

### Pontos a conferir antes de fazer (limites técnicos)
- Capa e sinopse: o servidor Xtream tem pedidos próprios para isso (get_vod_info para filme e get_series_info para série, um item por vez). A lista M3U em geral NÃO traz sinopse; só a imagem do item. RESOLVIDO com o dono: opção A (mesma página, "Sem sinopse nesta lista").
- "Continuar de onde parei" com minuto exato: exige guardar o minuto de cada filme e episódio (arquivo pequeno, com limite de itens, e chave em resumo SHA-1 como no SeriesProgress).
- Episódio "visto" e "visto pela metade": exige guardar, por episódio, o minuto e a duração.
- Memória: o aparelho tem só 1 GB de RAM. Pedir só o que está na tela e nunca guardar listas inteiras.
- Foco bem visível, tudo navegável só com setas, OK e Voltar, e também com o modo mouse.

## Roteiro de teste no TX9 (ainda NÃO feito)
DECIDIDO pelo dono em 24/10/2026: os testes ficam para DEPOIS da nova aparência. No fim da nova aparência, este roteiro será reescrito para as telas novas. Os passos numerados abaixo são do app ANTES da nova aparência e vão mudar (por exemplo, a busca agora é por aba). Os itens da parte (d) (página do filme e da série) serão acrescentados quando as telas existirem.
Itens novos da parte (a) para incluir no roteiro: (i) o menu à esquerda aparece com os 6 ícones; (ii) o foco nos ícones é bem visível; (iii) ir com a seta para a esquerda a partir de um canal chega ao menu, e para a direita volta ao conteúdo; (iv) o ícone da Tela inicial leva ao primeiro canal; (v) Canais, Filmes e Séries abrem a aba; (vi) Ajustes abre a tela Ajustes; (vii) Atualizar recarrega as faixas.
Itens novos da parte (b) para incluir no roteiro: (viii) dentro de uma aba, o botão "Pesquisar ..." aparece no alto e se chega nele com a seta para cima; (ix) OK no botão abre o teclado de letras e o resultado mostra SÓ o tipo da aba; (x) dentro da aba, a seta para a esquerda a partir da lista de categorias chega ao menu; (xi) trocar de aba pelo menu funciona e Voltar não empilha abas repetidas; (xii) o ícone Atualizar dentro da aba recarrega a categoria aberta; (xiii) o ícone Tela inicial volta à página inicial; (xiv) o mesmo na lista M3U (menu já ligado na M3uBrowseActivity); (xv) a página inicial não tem mais o botão "Buscar"; (xvi) a tela de resultados da busca não tem menu (esperado).
Itens novos da parte (c) para incluir no roteiro: (xvii) a página inicial não tem mais os botões "Controle parental" e "Trocar login"; (xviii) o ícone Ajustes abre a tela Ajustes, tanto na página inicial quanto nas abas; (xix) a lista de 5 opções se escolhe com setas e OK, e a seta para a esquerda chega ao menu; (xx) Controle parental: ativar, trocar a senha e desativar funcionam, e ao voltar para a página inicial as faixas recarregam sozinhas (itens adultos somem ou voltam); (xxi) "Trocar de lista" abre o login; (xxii) "Sobre o dispositivo" mostra modelo, Android, memória, armazenamento e tela (anotar o espaço livre em TARGET_DEVICES.md); (xxiii) "Sobre o aplicativo" mostra a versão; (xxiv) "Verificar atualização" mostra "Nenhuma atualização encontrada agora." ou muda a linha para "Instalar atualização"; (xxv) dentro de Ajustes, o ícone Atualizar não faz nada (esperado) e o ícone Tela inicial volta à página inicial.
1. Próxima temporada (servidor): abrir uma série com 2 ou mais temporadas, tocar o ÚLTIMO episódio da Temporada 1, avançar o vídeo até o fim com a seta para a direita. Deve começar sozinho o 1º episódio da Temporada 2. No último episódio da série deve aparecer "Fim da série.".
2. Próxima temporada (lista M3U): o mesmo teste, numa série da lista M3U.
3. Onde parou (servidor): tocar o episódio 3 da Temporada 2, apertar Voltar até sair, abrir a mesma série. Deve abrir na Temporada 2 com o episódio 3 já selecionado.
4. Onde parou (lista M3U): o mesmo teste.
5. Busca (servidor): em uma aba (por exemplo Filmes), apertar "Pesquisar filmes", digitar duas letras ou mais e apertar "Buscar". Devem aparecer cartões com nomes parecidos, só filmes. Escolher um e conferir que toca. Repetir na aba Séries (abre a tela da série) e na aba Canais.
6. Busca (lista M3U): o mesmo, e conferir que demora alguns segundos (esperado).
7. Busca com acento: digitar "acao" deve achar nomes com "Ação".
8. Busca com controle parental ligado: itens com nome adulto não devem aparecer.
9. Busca sem resultado: digitar letras sem sentido deve mostrar "Nada encontrado com esse nome.".
10. "Ver mais" da lista M3U: entrar em Séries, escolher uma série. Deve abrir a tela da série (temporadas e episódios), sem aviso.
11. Conferir que o que já funcionava continua: tocar um canal, um filme e abrir as abas.

## Arquivos do menu à esquerda (partes a, b e c)
- Ícones (pasta app/src/main/res/drawable/): ic_nav_home.xml, ic_nav_live.xml, ic_nav_movie.xml, ic_nav_series.xml, ic_nav_settings.xml, ic_nav_refresh.xml. Cada um é um desenho branco de 32dp.
- layout_nav.xml (pasta layout): o menu guardado num arquivo só. Coluna de 88dp com id group_nav e os botões nav_home, nav_live, nav_movie, nav_series, nav_settings e, embaixo, nav_refresh (ImageButton, 64dp, fundo bg_button).
- activity_home.xml: tem o MESMO menu escrito direto dentro dele (não usa o layout_nav.xml, de propósito, para não mexer numa tela que já funciona). Duas colunas: menu à esquerda, conteúdo à direita. No alto só o título "ASC TV Box" (os botões "Buscar", "Controle parental" e "Trocar login" saíram). Se mudar um menu, mudar o outro.
- activity_browse.xml: tem o layout_nav.xml incluído (include, id group_nav) ESCONDIDO (gone) por padrão. Só as telas que chamam o NavBar mostram o menu. A SearchActivity reaproveita este layout e por isso o menu fica escondido nela (de propósito). Tem também o botão btn_search_bar (escondido por padrão; as telas de aba mostram).
- activity_settings.xml: layout da aba Ajustes. Inclui o layout_nav.xml (escondido por padrão; a SettingsActivity mostra pelo NavBar). Lista lv_settings (340dp) e área de texto tv_detail.
- NavBar.java: classe pequena que mostra o menu e liga os 6 botões. NavBar.bind(activity, abaAtual, aoAtualizar). Constantes HOME, LIVE, MOVIE, SERIES, SETTINGS. Tela inicial volta à HomeActivity (CLEAR_TOP). Canais/Filmes/Séries trocam de aba: abre a BrowseActivity (servidor) ou a M3uBrowseActivity (lista M3U) conforme o login e fecha a aba de antes, sem animação; se já está na aba, não faz nada. Ajustes abre a SettingsActivity e fecha a tela de antes, sem animação; se já está em Ajustes, não faz nada. Atualizar roda o que a tela mandar (se não mandar nada, não faz nada).
- HomeActivity.java: liga os 6 botões do menu da própria página inicial (goToFirstCard, openSection, Ajustes abre a SettingsActivity, reloadRails). Não tem mais código de busca, de controle parental nem de trocar login. No onResume recarrega as faixas se o controle parental mudou.
- BrowseActivity.java: chama NavBar.bind(this, navCurrent(), () -> refreshCurrent()); refreshCurrent() limpa o EpgCache e recarrega a categoria aberta.
- M3uBrowseActivity.java: chama NavBar.bind(this, navCurrent(), () -> refreshCurrent()); refreshCurrent() recarrega a categoria aberta (selectCategory na posição atual). Sem cache de guia para limpar.
- SettingsActivity.java: aba Ajustes. Chama NavBar.bind(this, NavBar.SETTINGS, null). Lista de 5 linhas (RowAdapter com item_category.xml, setFocusable(false), como as outras listas). Controle parental usa ParentalStore e PinDialog (mesmas regras de antes). Trocar de lista abre a LoginActivity com EXTRA_SHOW_LOGIN e as flags NEW_TASK + CLEAR_TASK. Sobre o dispositivo usa Build, ActivityManager.MemoryInfo, StatFs e DisplayMetrics. Sobre o aplicativo usa PackageManager.getPackageInfo. Atualização usa Updater.checkAsync e Updater.downloadAndInstallAsync (mesmas funções da página inicial).

## Arquivos do EPG
- XtreamApi.java: função epg(...) que pede ao servidor a programação de UM canal (get_short_epg, até 4 programas) e devolve o programa de agora e o "a seguir". Horários em milissegundos.
- EpgCache.java: guarda na memória até 80 canais por 1 minuto cada; no máximo 2 pedidos ao servidor ao mesmo tempo; avisa sempre na thread principal. Tem clear().
- item_card.xml: tem a linha tv_now (usada para o "Agora: ...")
- HomeActivity.java: liga a linha tv_now e a caixinha de prévia ao EpgCache (espera 0,5 segundo parado no canal); esconde itens adultos das faixas; no servidor, série abre a SeriesActivity e "Ver mais" abre a BrowseActivity; na lista M3U carrega as faixas pelo M3uApi, manda o endereço do vídeo ao player, abre a M3uSeriesActivity nas séries e a M3uBrowseActivity no "Ver mais"; tem o menu à esquerda
- BrowseActivity.java: aba/"Ver mais" do servidor, liga a linha tv_now ao EpgCache; cadeado e senha nas categorias adultas; série abre a SeriesActivity; botão Pesquisar do tipo da tela; menu à esquerda (NavBar)
- activity_main.xml: tem a barra Agora / A seguir (group_nowbar) e a mensagem de status no alto da tela
- MainActivity.java: ver "Arquivos dos episódios" abaixo

## Arquivos do controle parental
- ParentalStore.java: liga e desliga o controle; guarda a senha embaralhada (PBKDF2 com SHA-1, porque o Android 7.1 não tem SHA-256 nesse método) num arquivo próprio, separado do login; confere a senha; trava depois de 5 erros; reconhece categorias e nomes adultos (isAdultCategory, isAdultName)
- PinDialog.java: teclado de números na tela (1 a 9, 0, Apagar e Sair); avisa o app quando o 4º número é digitado
- O menu do controle parental (ativar, trocar senha, desativar) fica na SettingsActivity (antes ficava na HomeActivity).

## Arquivos dos episódios de séries
- XtreamApi.java: tipo Kind.EPISODE, classe Episode (id, title, season, number, ext) e função episodes(server, user, pass, seriesId), no máximo 500, lendo aos poucos. streamUrl(...) monta o endereço de um episódio. Também tem a classe Details e a função details(...) (capa e sinopse, parte d).
- activity_series.xml: layout da tela da série (usado também pela M3uSeriesActivity)
- SeriesActivity.java (servidor): carrega os episódios, ordena por temporada e número, mostra temporadas e episódios. Tem buildQueue() (fila com "Especiais" no fim). Ao escolher um episódio abre o MainActivity levando a fila de TODA a série (EXTRA_LIST_IDS, EXTRA_LIST_EXTS, EXTRA_LIST_INDEX) e a chave da série (MainActivity.EXTRA_SERIES_KEY = "xt|servidor|usuário|idDaSérie"). Ao abrir, lê o SeriesProgress e abre na temporada e no episódio onde parou.
- M3uSeriesActivity.java (lista M3U): igual, mas os episódios vêm do M3uApi.episodes; manda a fila de endereços (MainActivity.EXTRA_LIST_URLS) e a chave "m3u|endereçoDaLista|títuloDaSérie". Se a soma dos endereços passar de 100 mil letras, manda só o episódio escolhido. No lugar do número de identificação manda a posição na fila (+1).
- SeriesProgress.java: guarda a posição do episódio na fila de cada série (SharedPreferences "series_progress"). A chave é um SHA-1 da chave da série (nunca texto, porque o endereço da lista pode ter senha). Máximo de 50 séries; as mais antigas são esquecidas. Funções save(ctx, chave, posição) e load(ctx, chave) (devolve -1 se não há nada).
- MainActivity.java (player): toca canal, filme e episódio. Tem EXTRA_URL, EXTRA_LIST_URLS e EXTRA_SERIES_KEY. Quando o episódio começa a tocar (STATE_READY) anota a posição no SeriesProgress. Ao terminar um episódio toca o próximo da fila (passa para a temporada seguinte); no último mostra "Fim da série." (servidor e lista M3U). Barra Agora / A seguir só em canal ao vivo do servidor.
- AndroidManifest.xml: tem LoginActivity, HomeActivity, BrowseActivity, M3uBrowseActivity, SettingsActivity, SeriesActivity, M3uSeriesActivity, SearchActivity e MainActivity registradas. (MovieActivity entra no passo 4 da parte d.)

## Arquivos da lista M3U
- M3uApi.java: lê a lista linha por linha direto da internet, sem guardá-la inteira. Para depois de 200 MB ou 1 minuto. Separa canal, filme e série pelas mesmas regras do app original. Séries agrupadas pelo título (sem "S01E03"). Toda a leitura passa por uma função interna (walk). Tem 5 funções públicas:
  - sample(m3uUrl, limite, esconderAdulto): primeiros itens de cada tipo (página inicial)
  - categories(m3uUrl, tipo): nomes das categorias (máximo 3000)
  - page(m3uUrl, tipo, categoria, deslocamento, limite, pularAdulto): uma página de itens
  - episodes(m3uUrl, tituloDaSerie): episódios de UMA série (máximo 500)
  - search(m3uUrl, tipo, texto, pularAdulto, limite): itens do tipo cujo nome tem todas as palavras do texto (sem ligar para maiúsculas e acentos); para ao juntar o limite; séries por título, uma por série
- M3uBrowseActivity.java: aba/"Ver mais" da lista M3U (categorias à esquerda, itens à direita, páginas de 40, máximo 400). Ao tocar numa série, abre a M3uSeriesActivity (EXTRA_ID e EXTRA_NAME), como a HomeActivity. Tem o botão Pesquisar do tipo da tela e o menu à esquerda (NavBar).
- XtreamApi.Item tem o campo "url" (endereço direto do vídeo). Vazio nos itens do servidor. Também tem o campo "icon" (imagem do item; vira a capa da lista M3U na parte d).
- Nos itens da lista M3U, o "id" é só o número de ordem do item na leitura.
- LoginActivity.java e CredentialStore.java: guardam o login por lista M3U (campo m3uUrl, criptografado)

## Arquivos da busca por nome
- SearchKeyboard.java: teclado de letras na tela (A a Z, 0 a 9, Espaço, Apagar, Buscar, Sair), com setas e OK. Máximo 40 letras, mínimo 2. SearchKeyboard.show(activity, título, listener); o listener tem onSearch(texto) e onCancel(). "Buscar" fecha o teclado e entrega o texto.
- XtreamApi.search(server, user, pass, tipo, texto, pularAdulto, limite): lê get_live_streams / get_vod_streams / get_series aos poucos e guarda só os itens cujo nome combina. Também tem searchWords(...) e matchesWords(...) (sem acento, sem maiúsculas, todas as palavras), usadas pelo M3uApi.
- SearchActivity.java: tela de resultados. Recebe HomeActivity.EXTRA_KIND (LIVE, MOVIE ou SERIES) e SearchActivity.EXTRA_QUERY. Reaproveita activity_browse.xml e esconde a lista de categorias por código. Até 100 resultados. Canal e filmes tocam; série abre a SeriesActivity ou a M3uSeriesActivity. Aberta só pelo botão Pesquisar das abas. SEM menu à esquerda (decisão do dono).

## Próximos passos (nesta ordem)
1. Conferir no Actions que o passo 1 da parte (d) (XtreamApi.java com details) ficou verde.
2. Parte (d), passos 2 a 6 (veja a seção "Parte (d)"): activity_movie.xml, MovieActivity.java, registrar no AndroidManifest.xml, ligar os lugares que tocam filme, e a página da série com capa e sinopse.
3. Parte (e): player (botões transparentes, barra que some rápido, foco livre) e "continuar de onde parei" com minuto exato (arquivo de minutos). Depois, voltar nas páginas do filme e da série para ligar o "Continuar de onde parei" e as cores de episódio visto.
4. Parte (f): barra Agora / A seguir e a programação com a tecla das três listras (e OK longo como segunda via).
5. No fim da nova aparência: reescrever o roteiro de teste para as telas novas e testar tudo no TX9.

## Problemas em aberto
- Armazenamento livre do TX9 e acesso à Play Store: ainda não sabemos. A tela Ajustes > "Sobre o dispositivo" vai mostrar o espaço livre; anotar em TARGET_DEVICES.md depois do teste.
- Etapas 3, 1 e 4 (e o conserto do "Ver mais" da lista M3U) ainda sem teste no TX9 (os testes ficam para depois da nova aparência)
- Tecla das três listras: o dono confirmou que o controle do TX9 tem esse botão. Falta conferir no TX9 se ele chega ao app como tecla de menu. Segunda via prevista: OK longo no canal.
- Menu à esquerda: usa o fundo bg_button; falta conferir no TX9 se o foco nos ícones fica bem visível, e se a seta para a esquerda chega ao menu a partir da lista de categorias (ListView) das abas e da lista de opções de Ajustes
- Menu à esquerda: o menu da página inicial é uma cópia do layout_nav.xml (duplicado de propósito). Se um dia mudar um, mudar o outro
- Parte (b): trocar de aba fecha a aba de antes. Voltar sai para a tela anterior à primeira aba (normalmente a página inicial)
- Parte (c): dentro de Ajustes, o ícone Atualizar do menu não faz nada (não há lista para recarregar). A conferir no teste se o dono estranha; se estranhar, esconder o ícone ou mostrar um aviso.
- Parte (c): "Trocar de lista" abre o login sem apagar o login salvo; o login novo substitui o antigo. A conferir no teste que Voltar no login não deixa o app numa tela quebrada.
- Parte (c): o menu do controle parental (ativar, trocar senha, desativar) não pede a senha para ABRIR Ajustes, só para trocar ou desativar (igual era na página inicial).
- Parte (d): os servidores Xtream usam nomes de campo um pouco diferentes em get_vod_info e get_series_info. details() aceita os mais comuns; se o servidor do dono usar outros, a página fica sem capa ou sem sinopse (esperado; conferir no teste e ajustar).
- Parte (d): em get_series_info, se o servidor mandar "episodes" ANTES de "info", details() lê a resposta até achar o "info" (continua limitada a 80 MB e a 15 segundos por leitura).
- Lista M3U: não tem guia de programação, por decisão do dono (ver DECISIONS.md)
- Lista M3U: a página inicial mostra os 10 primeiros itens na ordem da lista; se a lista for gigante e tiver poucos itens de um tipo no começo, a leitura pode demorar até 1 minuto
- Lista M3U, "Ver mais": cada página nova lê a lista de novo desde o começo, então páginas do fim de uma lista grande demoram mais. Se bater no limite de 200 MB ou 1 minuto, a página pode vir menor e a categoria é tratada como terminada
- Lista M3U, "Ver mais": a categoria é identificada pelo nome (group-title); duas categorias com o mesmo nome aparecem como uma só
- Lista M3U, "Atualizar" dentro da aba: recarrega a categoria aberta lendo a lista de novo; em lista grande pode levar alguns segundos
- Lista M3U, séries: achadas pelo título (sem "S01E03"); títulos escritos de jeitos diferentes podem aparecer separados ou misturados. A tela leva alguns segundos para abrir. No máximo 500 episódios por série. Episódio sem "S01E03" no nome vai para a temporada 1, sem número
- Onde parou: guarda a posição do episódio na fila. Se o servidor ou a lista mudar a quantidade ou a ordem dos episódios, a posição pode apontar para outro episódio. Guarda só 50 séries.
- Onde parou, lista M3U: a chave usa o endereço da lista; se o dono trocar a lista, o que foi anotado deixa de valer (esperado)
- Fila da série toda: no máximo 500 episódios. Na lista M3U, se a soma dos endereços passar de 100 mil letras, o próximo episódio NÃO toca sozinho
- Busca: lê a lista do tipo escolhido até achar 100 itens; em listas grandes pode levar de alguns segundos a 1 minuto (na lista M3U). No servidor a resposta é limitada a 80 MB
- Busca no servidor: o filtro adulto é só pelo nome (o servidor não manda a categoria nesse pedido, e a busca não olha categoria). Na lista M3U filtra também pela categoria.
- Telas da série (servidor e M3U): não pedem a senha de novo; só abrem a partir de itens que já passaram pelo filtro do controle parental. A SearchActivity também filtra pelo controle parental ao buscar.
- Canais sem guia no servidor mostram "Programação indisponível" (esperado)
- Controle parental: no servidor, os itens adultos são tirados DEPOIS de buscar os 10 primeiros, então uma faixa pode mostrar menos de 10 cartões (esperado)
- Controle parental: o reconhecimento de conteúdo adulto é só pelo nome (categoria ou título)
