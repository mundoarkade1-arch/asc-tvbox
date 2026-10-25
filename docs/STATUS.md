# Status do projeto

Última atualização: 25/10/2026

## Fase atual
FASE ATUAL (decidida com o dono em 24/10/2026): MUDAR TODA A APARÊNCIA E A NAVEGAÇÃO DO APP PRIMEIRO. Os testes no TX9 ficam para DEPOIS da nova aparência (as telas e funções vão mudar, então testar agora seria perder tempo). As ideias estão na seção "Nova interface" abaixo. O dono ainda pode lembrar de mais pedidos; se falar, anotar aqui e no DECISIONS.md.

Andamento da nova aparência (uma parte de cada vez):
- (a) Menu à esquerda + Tela inicial: FEITA (verde no Actions, ainda NÃO testada no TX9).
- (b) Abas Canais, Filmes e Séries com busca própria: FEITA (verde, NÃO testada no TX9). Veja "Parte (b)" abaixo.
- (c) Aba Ajustes: FEITA (verde, NÃO testada no TX9). Veja "Parte (c)" abaixo.
- (d) Página do filme e da série com capa e sinopse: FEITA (verde, NÃO testada no TX9). Veja "Parte (d)" abaixo.
- (e) Player (botões transparentes, barra que some rápido, setas livres) e "Continuar de onde parei" com minuto exato, nas páginas do filme e da série: FEITA (verde, NÃO testada no TX9). Veja "Parte (e)" abaixo.
- (f) Barra Agora / A seguir no canal e a programação com a tecla das três listras: a fazer. PRÓXIMA PARTE.

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
- Lembrar onde parou: ao começar a tocar um episódio, o player anota a posição dele na fila (SeriesProgress). Ao abrir a série de novo, a tela vai para a temporada certa e deixa o episódio selecionado. Guarda no máximo 50 séries.
- Busca por nome: botão "Pesquisar ..." em cada aba, teclado de letras, tela de resultados. Servidor e lista M3U.
- "Ver mais" da lista M3U: série abre a M3uSeriesActivity (antes mostrava um aviso).
- Parte (a) da nova aparência: menu vertical à esquerda na página inicial, só com ícones.
- Parte (b) da nova aparência: botão "Pesquisar ..." nas telas de aba (servidor e lista M3U), menu à esquerda dentro das duas abas e botão "Buscar" removido da página inicial.
- Parte (c) da nova aparência: aba Ajustes (SettingsActivity) com controle parental, trocar de lista, sobre o dispositivo, sobre o aplicativo e verificar atualização. Botões "Controle parental" e "Trocar login" removidos da página inicial.
- Parte (d) da nova aparência: página do filme (MovieActivity) com capa, título, informações, sinopse e botão Tocar; página da série com capa, título, informações e sinopse no alto (servidor) ou capa do item e "Sem sinopse nesta lista." (lista M3U). Tudo ligado nos lugares que abrem filme e série (página inicial, abas e busca).
- Parte (e) da nova aparência: o player anota o minuto exato de filmes e episódios (PlaybackProgress); botão "Continuar de onde parei" nas páginas do filme e da série (servidor e lista M3U); episódios vistos em verde e vistos pela metade em verde só até a metade; setas esquerda e direita pulam 10 segundos no player; barra do player some em 3 segundos; controles do player semi-transparentes.

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

## Parte (d): página do filme e da série (FEITA, verde, NÃO testada no TX9)
DECIDIDO com o dono em 24/10/2026: a página é a MESMA nos dois logins (opção A). Na lista M3U, onde iria a sinopse aparece "Sem sinopse nesta lista"; a capa é a imagem do item (item.icon). Veja DECISIONS.md.
O "Continuar de onde parei" e as cores de episódio visto (tempo 2) foram ligados na parte (e).

O que existe:
- Página do filme (MovieActivity, layout activity_movie.xml): capa à esquerda; à direita título, linha de informações (escondida se vazia), botão "Continuar de onde parei" (escondido se não há o que continuar), botão Tocar e a sinopse numa área com rolagem (recebe foco com a seta para baixo; setas para cima e para baixo rolam o texto). SEM menu à esquerda (é tela de passagem, como a de resultados da busca). Recebe por Intent: tipo, id, extensão, nome, capa (MovieActivity.EXTRA_ICON) e, na lista M3U, o endereço (MainActivity.EXTRA_URL).
  - Servidor: mostra a capa do item na hora e pede XtreamApi.details UMA vez, numa thread; depois preenche capa do servidor, informações e sinopse. Se falhar ou vier vazio: "Sem sinopse para este filme." (o filme continua tocando).
  - Lista M3U: não pede nada; mostra a capa do item e "Sem sinopse nesta lista.".
- Página da série (activity_series.xml, usado pela SeriesActivity do servidor e pela M3uSeriesActivity da lista M3U): no alto uma faixa com capa pequena (100x150dp), título, informações e sinopse (no máximo 4 linhas, só para ler, SEM foco, para não prender as setas); logo abaixo o botão "Continuar de onde parei" (escondido se não há o que continuar); embaixo, temporadas à esquerda e episódios à direita.
  - Servidor (SeriesActivity): mostra a capa do item e pede XtreamApi.details uma vez (thread separada da lista de episódios). Se falhar: "Sem sinopse para esta série." (os episódios abrem normalmente).
  - Lista M3U (M3uSeriesActivity): capa do item e "Sem sinopse nesta lista.". Não pede nada novo.
- Lugares que abrem a página do filme: HomeActivity.openItem, BrowseActivity.openItem, M3uBrowseActivity.openItem e SearchActivity.openItem. Canal ao vivo continua tocando direto. Esses mesmos quatro lugares mandam a capa do item (MovieActivity.EXTRA_ICON) também ao abrir uma série.
- MovieActivity registrada no AndroidManifest.xml.
- Controle parental: as duas páginas só abrem a partir de itens que já passaram pelo filtro (não pedem a senha de novo).

Notas técnicas de XtreamApi.details:
- Filme: get_vod_info com vod_id. Série: get_series_info com series_id. Lê só o bloco "info" e PARA (não lê a lista de episódios, se "info" vier antes dela).
- Aceita os nomes de campo mais comuns: capa (movie_image, cover_big, cover; só http/https), sinopse (plot, description; máximo 1200 letras), genre, releasedate (ou releaseDate, release_date; mostra só o ano), duration e rating (mostra "Nota X"; ignora zeros). A linha de informações junta o que existir separado por " • ".
- Devolve um Details vazio (sem erro) se o servidor não informar nada, se o id não for número ou se o tipo não for filme nem série. Erros de rede viram IOException: a tela trata e mostra a página sem sinopse.
- Faz acesso à rede: NUNCA chamar na thread principal.

## Parte (e): player e "Continuar de onde parei" (FEITA, verde, NÃO testada no TX9)
- PlaybackProgress.java (arquivo novo): guarda, por filme e por episódio, o minuto onde parou e a duração (SharedPreferences "playback_progress"). A chave é um SHA-1 (nunca texto, porque o endereço da lista pode ter senha). Máximo de 300 itens; os mais antigos são esquecidos. Só guarda se passou de 5 segundos e a duração é conhecida. Classe interna Mark (positionMs, durationMs, fraction(), isFinished() = 95% ou mais, isPartial() = viu 5 segundos ou mais e não terminou). Chaves: movieKey(servidor, usuário, id) e episodeKey(servidor, usuário, id) no servidor; urlKey(endereço do vídeo) na lista M3U. O player e as páginas usam sempre as mesmas funções de chave.
- MainActivity.java (player):
  - Filme e episódio: anota o minuto a cada 10 segundos, ao sair da tela (onStop) e, ao terminar, marca como visto (posição = duração). Canal ao vivo não anota nada.
  - EXTRA_START_MS: o primeiro vídeo começa naquele minuto (entre 5 segundos e 100 horas). Vale uma vez só. Se a pessoa apertar Home e voltar ao player, continua do mesmo minuto.
  - Setas esquerda e direita em filme e episódio: voltam e avançam 10 segundos direto (dispatchKeyEvent), com intervalo mínimo de 120 ms entre pulos, e mostram a barra. Canal ao vivo não mudou.
  - Controles semi-transparentes: setAlpha(0.65) na barra de controles do ExoPlayer (procura o id exo_controller; se não achar, não faz nada).
- activity_main.xml: PlayerView com app:show_timeout="3000" (a barra some em 3 segundos).
- Página do filme (MovieActivity): no onResume confere o PlaybackProgress. Se viu uma parte (e não terminou), mostra "Continuar de onde parei (minuto)" e o foco abre nele; Tocar começa sempre do zero. Se terminou (95% ou mais) ou nunca viu, o botão fica escondido e o foco fica no Tocar.
- Página da série (SeriesActivity e M3uSeriesActivity): botão "Continuar de onde parei (T2 E3, 12:34)". Usa o SeriesProgress para achar o episódio e o PlaybackProgress para o minuto. Se esse episódio já terminou, o botão leva ao próximo, do começo; se era o último, o botão some. Se há o que continuar, o foco abre no botão. Clicar num episódio da lista começa do zero.
- Cores dos episódios: título do episódio visto fica verde (0xFF4ADE80); visto pela metade fica verde só na parte do título proporcional ao que foi visto. A cor é no TEXTO, não no fundo da linha. Atualiza no onResume (ao voltar do player).

## Nova interface - pedidos do dono em 24/10/2026
Ainda vão chegar mais pedidos. Ordem e detalhes combinados com o dono, um passo de cada vez.

### Menu e telas principais
- Menu vertical no lado ESQUERDO, só com ícones: Tela inicial, Canais, Filmes, Séries e Ajustes. (FEITO nas partes a, b e c)
- A Tela inicial é sempre a que abre com o app. Na parte grande da direita mostra as indicações: 10 primeiros canais, 10 primeiros filmes, 10 primeiras séries.
- Cada aba (Canais, Filmes, Séries) tem a sua barra de pesquisa, e a busca mostra SÓ aquele tipo. (FEITO na parte b)
- Ícone de atualizar na tela principal, para recarregar a lista. (FEITO na parte a)

### Aba Canais
- Mantém a grade ao lado, com a barra de pesquisa e a linha "Agora: ..." embaixo de cada canal (como já é).
- Barra embaixo da tela, no canal que está sendo assistido: "Agora" com o programa que passa e, do lado direito, o que vai passar depois. (A FAZER na parte f. Hoje a barra do player mostra o "Agora" e o "A seguir" um embaixo do outro; o dono quer "Agora" à esquerda e "A seguir" à direita.)
- Tecla das "três listras" do controle do TX9 (botão de lista), apertada sobre um canal: abre a programação, com o que está passando e os próximos 5 programas. SÓ carrega quando a tecla é apertada, nunca antes. (A FAZER na parte f)
- RESOLVIDO com o dono: o controle do TX9 TEM um botão com três listras. No Android ela costuma chegar como tecla de menu (KEYCODE_MENU). Segunda via, caso a tecla chegue diferente: segurar o OK sobre o canal também abre a programação. Conferir no TX9 se a tecla funciona.

### Página do filme (FEITA nas partes d e e)
- Ao escolher um filme, abre uma página com a capa e a sinopse.
- Botão de tocar e botão "Continuar de onde parei". Se já assistiu parte, o "Continuar" é a opção já pronta para apertar (foco nele).

### Página da série (FEITA nas partes d e e)
- Ao escolher uma série, abre uma página com a capa e a sinopse.
- Temporadas à esquerda e, ao lado, a lista de episódios. (O pedido original falava de um botão para escolher a temporada, mas a lista de temporadas à esquerda foi mantida.)
- Episódios já assistidos ficam marcados com outra cor. O episódio visto pela metade fica com a cor só até a metade.
- Botão "Continuar de onde parei", que vai direto ao episódio certo, sem escolher.

### Player (FEITO na parte e)
- Botões do player semi-transparentes (dá para ver, mas são transparentes).
- A barra do player demora muito para sumir: deve sumir mais rápido.
- Hoje o foco sempre cai na opção de configurações. O dono quer o foco livre para avançar e voltar o vídeo com as setas.

### Aba Ajustes (FEITA na parte c)
- Controle parental.
- Mudar de lista / gerenciar lista (feito como "Trocar de lista").
- Sobre o dispositivo.
- Sobre o aplicativo.
- Botão para verificar se há atualização.

### Pontos a conferir (limites técnicos)
- Capa e sinopse: get_vod_info para filme e get_series_info para série, um item por vez. A lista M3U em geral NÃO traz sinopse; só a imagem do item. RESOLVIDO com o dono: opção A. FEITO na parte d.
- Memória: o aparelho tem só 1 GB de RAM. Pedir só o que está na tela e nunca guardar listas inteiras.
- Foco bem visível, tudo navegável só com setas, OK e Voltar, e também com o modo mouse.

## Roteiro de teste no TX9 (ainda NÃO feito)
DECIDIDO pelo dono em 24/10/2026: os testes ficam para DEPOIS da nova aparência. No fim da nova aparência (depois da parte f), este roteiro será reescrito para as telas novas. Os passos numerados no fim são do app ANTES da nova aparência e vão mudar (por exemplo, a busca agora é por aba, e filme e série agora abrem uma página antes de tocar).
Itens novos da parte (a) para incluir no roteiro: (i) o menu à esquerda aparece com os 6 ícones; (ii) o foco nos ícones é bem visível; (iii) ir com a seta para a esquerda a partir de um canal chega ao menu, e para a direita volta ao conteúdo; (iv) o ícone da Tela inicial leva ao primeiro canal; (v) Canais, Filmes e Séries abrem a aba; (vi) Ajustes abre a tela Ajustes; (vii) Atualizar recarrega as faixas.
Itens novos da parte (b): (viii) dentro de uma aba, o botão "Pesquisar ..." aparece no alto e se chega nele com a seta para cima; (ix) OK no botão abre o teclado de letras e o resultado mostra SÓ o tipo da aba; (x) dentro da aba, a seta para a esquerda a partir da lista de categorias chega ao menu; (xi) trocar de aba pelo menu funciona e Voltar não empilha abas repetidas; (xii) o ícone Atualizar dentro da aba recarrega a categoria aberta; (xiii) o ícone Tela inicial volta à página inicial; (xiv) o mesmo na lista M3U; (xv) a página inicial não tem mais o botão "Buscar"; (xvi) a tela de resultados da busca não tem menu (esperado).
Itens novos da parte (c): (xvii) a página inicial não tem mais os botões "Controle parental" e "Trocar login"; (xviii) o ícone Ajustes abre a tela Ajustes, tanto na página inicial quanto nas abas; (xix) a lista de 5 opções se escolhe com setas e OK, e a seta para a esquerda chega ao menu; (xx) Controle parental: ativar, trocar a senha e desativar funcionam, e ao voltar para a página inicial as faixas recarregam sozinhas; (xxi) "Trocar de lista" abre o login; (xxii) "Sobre o dispositivo" mostra modelo, Android, memória, armazenamento e tela (anotar o espaço livre em TARGET_DEVICES.md); (xxiii) "Sobre o aplicativo" mostra a versão; (xxiv) "Verificar atualização" mostra "Nenhuma atualização encontrada agora." ou muda a linha para "Instalar atualização"; (xxv) dentro de Ajustes, o ícone Atualizar não faz nada (esperado) e o ícone Tela inicial volta à página inicial.
Itens novos da parte (d): (xxvi) escolher um filme (página inicial, aba e busca) abre a página do filme, com o foco no botão Tocar (ou no Continuar, se já viu parte); (xxvii) a capa e a sinopse aparecem (servidor); se o servidor do dono usar outros nomes de campo, a página fica sem capa ou sem sinopse (anotar quais campos faltam); (xxviii) seta para baixo no Tocar leva à sinopse e as setas rolam o texto; (xxix) OK em Tocar toca o filme, e Voltar do player volta à página do filme; (xxx) na lista M3U, a página do filme mostra a capa do item e "Sem sinopse nesta lista."; (xxxi) escolher um canal continua tocando direto; (xxxii) escolher uma série (página inicial, aba e busca) abre a tela da série com capa, título, informações e sinopse no alto; (xxxiii) as setas chegam às temporadas e aos episódios sem ficar presas na sinopse; (xxxiv) a sinopse de série longa aparece cortada em 4 linhas (esperado); (xxxv) na lista M3U, a série mostra a capa do item e "Sem sinopse nesta lista."; (xxxvi) com o controle parental ligado, filmes e séries adultos não aparecem para abrir.
Itens novos da parte (e): (xxxvii) filme: assistir uns 2 minutos e apertar Voltar até a página do filme; deve aparecer "Continuar de onde parei (minuto)" com o foco nele; (xxxviii) OK no Continuar começa naquele minuto; OK no Tocar começa do zero; (xxxix) filme visto até o fim (95% ou mais) não mostra o botão Continuar; (xl) série: o botão Continuar mostra o episódio e o minuto e leva direto a ele; se o episódio já terminou, leva ao próximo, do começo; (xli) cores: episódio visto fica verde; visto pela metade fica verde só até a metade do título; (xlii) player: seta esquerda e direita voltam e avançam 10 segundos em filme e episódio, sem o foco cair em configurações; segurar a seta não pode pular rápido demais; (xliii) a barra do player some em cerca de 3 segundos; (xliv) os controles do player ficam semi-transparentes (conferir se o efeito aparece); (xlv) canal ao vivo: as setas continuam como antes e OK mostra a barra Agora / A seguir; (xlvi) sair com Home e voltar ao filme continua do mesmo minuto; (xlvii) tudo isso também na lista M3U.
Itens de teste antigos (vão mudar):
1. Próxima temporada (servidor): abrir uma série com 2 ou mais temporadas, tocar o ÚLTIMO episódio da Temporada 1, avançar o vídeo até o fim. Deve começar sozinho o 1º episódio da Temporada 2. No último episódio da série deve aparecer "Fim da série.".
2. Próxima temporada (lista M3U): o mesmo teste, numa série da lista M3U.
3. Onde parou (servidor): tocar o episódio 3 da Temporada 2, apertar Voltar até sair, abrir a mesma série. Deve abrir na Temporada 2 com o episódio 3 já selecionado.
4. Onde parou (lista M3U): o mesmo teste.
5. Busca (servidor): em uma aba, apertar "Pesquisar ...", digitar duas letras ou mais e apertar "Buscar". Devem aparecer cartões com nomes parecidos, só do tipo da aba. Escolher um e conferir que abre a página (filme e série) ou toca (canal).
6. Busca (lista M3U): o mesmo, e conferir que demora alguns segundos (esperado).
7. Busca com acento: digitar "acao" deve achar nomes com "Ação".
8. Busca com controle parental ligado: itens com nome adulto não devem aparecer.
9. Busca sem resultado: digitar letras sem sentido deve mostrar "Nada encontrado com esse nome.".
10. "Ver mais" da lista M3U: entrar em Séries, escolher uma série. Deve abrir a tela da série, sem aviso.
11. Conferir que o que já funcionava continua: tocar um canal, abrir um filme (página do filme e tocar) e abrir as abas.

## Arquivos do menu à esquerda (partes a, b e c)
- Ícones (pasta app/src/main/res/drawable/): ic_nav_home.xml, ic_nav_live.xml, ic_nav_movie.xml, ic_nav_series.xml, ic_nav_settings.xml, ic_nav_refresh.xml. Cada um é um desenho branco de 32dp.
- layout_nav.xml (pasta layout): o menu guardado num arquivo só. Coluna de 88dp com id group_nav e os botões nav_home, nav_live, nav_movie, nav_series, nav_settings e, embaixo, nav_refresh (ImageButton, 64dp, fundo bg_button).
- activity_home.xml: tem o MESMO menu escrito direto dentro dele (não usa o layout_nav.xml, de propósito, para não mexer numa tela que já funciona). Duas colunas: menu à esquerda, conteúdo à direita. Se mudar um menu, mudar o outro.
- activity_browse.xml: tem o layout_nav.xml incluído (include, id group_nav) ESCONDIDO (gone) por padrão. Só as telas que chamam o NavBar mostram o menu. A SearchActivity reaproveita este layout e por isso o menu fica escondido nela (de propósito). Tem também o botão btn_search_bar (escondido por padrão; as telas de aba mostram).
- activity_settings.xml: layout da aba Ajustes. Inclui o layout_nav.xml (escondido por padrão; a SettingsActivity mostra pelo NavBar). Lista lv_settings (340dp) e área de texto tv_detail.
- NavBar.java: classe pequena que mostra o menu e liga os 6 botões. NavBar.bind(activity, abaAtual, aoAtualizar). Constantes HOME, LIVE, MOVIE, SERIES, SETTINGS. Tela inicial volta à HomeActivity (CLEAR_TOP). Canais/Filmes/Séries trocam de aba conforme o login e fecham a aba de antes, sem animação; se já está na aba, não faz nada. Ajustes abre a SettingsActivity e fecha a tela de antes; se já está em Ajustes, não faz nada. Atualizar roda o que a tela mandar.
- HomeActivity.java: liga os 6 botões do menu da própria página inicial. No onResume recarrega as faixas se o controle parental mudou. openItem: série abre a SeriesActivity ou M3uSeriesActivity (com a capa do item); filme abre a MovieActivity; canal toca direto.
- BrowseActivity.java: NavBar.bind(this, navCurrent(), () -> refreshCurrent()); refreshCurrent() limpa o EpgCache e recarrega a categoria aberta. openItem como na HomeActivity.
- M3uBrowseActivity.java: igual, sem cache de guia. openItem como na HomeActivity (filme leva também o endereço do vídeo).
- SettingsActivity.java: aba Ajustes. Lista de 5 linhas (RowAdapter com item_category.xml, setFocusable(false)). Usa ParentalStore, PinDialog, Updater, Build, ActivityManager.MemoryInfo, StatFs e DisplayMetrics.

## Arquivos das páginas do filme e da série (partes d e e)
- activity_movie.xml: layout da página do filme (iv_cover, tv_title, tv_meta escondido por padrão, btn_continue escondido por padrão, btn_play, sv_plot com tv_plot). Fundo bg_button nos botões e na área da sinopse.
- MovieActivity.java: página do filme. EXTRA_ICON (capa do item). Usa HomeActivity.EXTRA_KIND, EXTRA_ID, EXTRA_EXT, EXTRA_NAME e MainActivity.EXTRA_URL. play(startMs) abre o player (com EXTRA_START_MS se startMs > 0). refreshContinue() no onResume.
- activity_series.xml: faixa do alto (iv_cover, tv_title, tv_meta escondido por padrão, tv_plot sem foco, máximo 4 linhas), btn_continue (escondido por padrão), lv_seasons, lv_episodes e tv_status.
- SeriesActivity.java e M3uSeriesActivity.java: leem EXTRA_ICON para a capa; refreshContinue() e paint() (cores); playEpisode(episódio, minuto). A do servidor chama loadDetails (XtreamApi.details); a da lista M3U escreve "Sem sinopse nesta lista.". Implementam onLowMemory limpando o ImageLoader.
- PlaybackProgress.java: ver "Parte (e)".

## Arquivos do EPG
- XtreamApi.java: função epg(...) que pede ao servidor a programação de UM canal (get_short_epg, até 4 programas) e devolve o programa de agora e o "a seguir". Horários em milissegundos.
- EpgCache.java: guarda na memória até 80 canais por 1 minuto cada; no máximo 2 pedidos ao servidor ao mesmo tempo; avisa sempre na thread principal. Tem clear().
- item_card.xml: tem a linha tv_now (usada para o "Agora: ...")
- HomeActivity.java: liga a linha tv_now e a caixinha de prévia ao EpgCache (espera 0,5 segundo parado no canal).
- BrowseActivity.java: liga a linha tv_now ao EpgCache.
- activity_main.xml: tem a barra Agora / A seguir (group_nowbar) e a mensagem de status no alto da tela.

## Arquivos do controle parental
- ParentalStore.java: liga e desliga o controle; guarda a senha embaralhada (PBKDF2 com SHA-1, porque o Android 7.1 não tem SHA-256 nesse método) num arquivo próprio, separado do login; confere a senha; trava depois de 5 erros; reconhece categorias e nomes adultos (isAdultCategory, isAdultName)
- PinDialog.java: teclado de números na tela (1 a 9, 0, Apagar e Sair); avisa o app quando o 4º número é digitado
- O menu do controle parental (ativar, trocar senha, desativar) fica na SettingsActivity.

## Arquivos dos episódios de séries
- XtreamApi.java: tipo Kind.EPISODE, classe Episode (id, title, season, number, ext) e função episodes(server, user, pass, seriesId), no máximo 500, lendo aos poucos. streamUrl(...) monta o endereço de um episódio. Também tem a classe Details e a função details(...).
- SeriesActivity.java (servidor): carrega os episódios, ordena por temporada e número, mostra temporadas e episódios. Tem buildQueue() (fila com "Especiais" no fim). Ao escolher um episódio abre o MainActivity levando a fila de TODA a série (EXTRA_LIST_IDS, EXTRA_LIST_EXTS, EXTRA_LIST_INDEX) e a chave da série (MainActivity.EXTRA_SERIES_KEY = "xt|servidor|usuário|idDaSérie"). Ao abrir, lê o SeriesProgress e abre na temporada e no episódio onde parou.
- M3uSeriesActivity.java (lista M3U): igual, mas os episódios vêm do M3uApi.episodes; manda a fila de endereços (MainActivity.EXTRA_LIST_URLS) e a chave "m3u|endereçoDaLista|títuloDaSérie". Se a soma dos endereços passar de 100 mil letras, manda só o episódio escolhido. No lugar do número de identificação manda a posição na fila (+1).
- SeriesProgress.java: guarda a posição do episódio na fila de cada série (SharedPreferences "series_progress"). A chave é um SHA-1 da chave da série. Máximo de 50 séries. Funções save(ctx, chave, posição) e load(ctx, chave) (devolve -1 se não há nada).
- MainActivity.java (player): toca canal, filme e episódio. Tem EXTRA_URL, EXTRA_LIST_URLS, EXTRA_SERIES_KEY e EXTRA_START_MS. Quando o episódio começa a tocar (STATE_READY) anota a posição no SeriesProgress. Ao terminar um episódio toca o próximo da fila; no último mostra "Fim da série.". Barra Agora / A seguir só em canal ao vivo do servidor. Ver também "Parte (e)".
- AndroidManifest.xml: tem LoginActivity, HomeActivity, BrowseActivity, M3uBrowseActivity, SettingsActivity, SeriesActivity, M3uSeriesActivity, MovieActivity, SearchActivity e MainActivity registradas.

## Arquivos da lista M3U
- M3uApi.java: lê a lista linha por linha direto da internet, sem guardá-la inteira. Para depois de 200 MB ou 1 minuto. Separa canal, filme e série pelas mesmas regras do app original. Séries agrupadas pelo título (sem "S01E03"). Funções públicas: sample, categories, page, episodes e search.
- XtreamApi.Item tem o campo "url" (endereço direto do vídeo). Vazio nos itens do servidor. Também tem o campo "icon" (imagem do item; é a capa da lista M3U).
- Nos itens da lista M3U, o "id" é só o número de ordem do item na leitura.
- LoginActivity.java e CredentialStore.java: guardam o login por lista M3U (campo m3uUrl, criptografado)

## Arquivos da busca por nome
- SearchKeyboard.java: teclado de letras na tela, com setas e OK. Máximo 40 letras, mínimo 2. SearchKeyboard.show(activity, título, listener); o listener tem onSearch(texto) e onCancel().
- XtreamApi.search(...), searchWords(...) e matchesWords(...) (sem acento, sem maiúsculas, todas as palavras), usadas também pelo M3uApi.
- SearchActivity.java: tela de resultados. Reaproveita activity_browse.xml. Até 100 resultados. Canal toca direto; filme abre a MovieActivity; série abre a SeriesActivity ou a M3uSeriesActivity. SEM menu à esquerda (decisão do dono).

## Próximos passos (nesta ordem)
1. Parte (f): (a) barra embaixo do player no canal ao vivo, com "Agora" à esquerda e "A seguir" à direita; (b) tecla das três listras (KEYCODE_MENU) e OK longo (segunda via) sobre um canal abrem a programação: o que passa agora e os próximos 5 programas, SÓ carregando quando a tecla é apertada. Provavelmente precisa de: função nova no XtreamApi (programação com até 6 programas), uma tela ou caixa nova para mostrar a lista, e o tratamento das teclas na HomeActivity, BrowseActivity e MainActivity. Pedir cada arquivo atual ao dono antes de mexer.
2. No fim da nova aparência: reescrever o roteiro de teste para as telas novas e testar tudo no TX9.

## Problemas em aberto
- Armazenamento livre do TX9 e acesso à Play Store: ainda não sabemos. A tela Ajustes > "Sobre o dispositivo" vai mostrar o espaço livre; anotar em TARGET_DEVICES.md depois do teste.
- Etapas 3, 1 e 4 (e o conserto do "Ver mais" da lista M3U) ainda sem teste no TX9 (os testes ficam para depois da nova aparência)
- Tecla das três listras: o dono confirmou que o controle do TX9 tem esse botão. Falta conferir no TX9 se ele chega ao app como tecla de menu. Segunda via prevista: OK longo no canal.
- Menu à esquerda: usa o fundo bg_button; falta conferir no TX9 se o foco nos ícones fica bem visível, e se a seta para a esquerda chega ao menu a partir da lista de categorias (ListView) das abas e da lista de opções de Ajustes
- Menu à esquerda: o menu da página inicial é uma cópia do layout_nav.xml (duplicado de propósito). Se um dia mudar um, mudar o outro
- Parte (b): trocar de aba fecha a aba de antes. Voltar sai para a tela anterior à primeira aba (normalmente a página inicial)
- Parte (c): dentro de Ajustes, o ícone Atualizar do menu não faz nada. A conferir no teste se o dono estranha.
- Parte (c): "Trocar de lista" abre o login sem apagar o login salvo; o login novo substitui o antigo. A conferir no teste que Voltar no login não deixa o app numa tela quebrada.
- Parte (c): o menu do controle parental não pede a senha para ABRIR Ajustes, só para trocar ou desativar.
- Parte (d): os servidores Xtream usam nomes de campo um pouco diferentes em get_vod_info e get_series_info. details() aceita os mais comuns; se o servidor do dono usar outros, a página fica sem capa ou sem sinopse (conferir no teste e ajustar).
- Parte (d): em get_series_info, se o servidor mandar "episodes" ANTES de "info", details() lê a resposta até achar o "info" (limitada a 80 MB e a 15 segundos por leitura). Na página da série isso soma um segundo pedido ao servidor.
- Parte (d): na página da série a sinopse mostra no máximo 4 linhas e NÃO rola. Sinopses longas aparecem cortadas. A conferir no teste se o dono quer ler o texto todo.
- Parte (d): no servidor, a página abre com a capa do item e depois troca pela capa do servidor, se ela vier. Pode dar um "pulo" visual rápido (esperado).
- Parte (d): ao apertar Voltar no player, a pessoa volta para a página do filme (e não para a lista). Conferir se o dono acha bom.
- Parte (e): a transparência dos controles usa setAlpha(0.65) na barra do ExoPlayer (id exo_controller). Se no TX9 não aparecer o efeito, trocar por um layout próprio dos controles ou ajustar o fundo.
- Parte (e): só as setas esquerda e direita foram liberadas para avançar e voltar. Cima, baixo e OK ainda movem o foco entre os botões do player. Conferir no teste se o foco ainda "cai" em configurações ao apertar cima, baixo ou OK, e se o dono quer mais mudanças.
- Parte (e): o salto de 10 segundos tem intervalo mínimo de 120 ms ao segurar a seta. Pode ficar rápido demais ao segurar. Conferir no teste e aumentar o intervalo se precisar.
- Parte (e): as setas esquerda e direita não funcionam em canal ao vivo (continuam como antes).
- Parte (e): o minuto só é guardado quando o player já sabe a duração do vídeo. Vídeos sem duração (alguns ao vivo disfarçados de filme) não ganham "Continuar".
- Parte (e): guarda no máximo 300 itens (filmes e episódios); os mais antigos são esquecidos e perdem a cor de "visto".
- Parte (e): as cores dos episódios são no texto do título, não no fundo da linha. Se o dono preferir o fundo colorido, trocar (precisa ver o item_category.xml).
- Parte (e): no servidor, a chave do episódio é o número dele (não muda). Na lista M3U a chave é o endereço do vídeo: se a lista trocar os endereços, o que foi visto deixa de valer (esperado).
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
- Busca no servidor: o filtro adulto é só pelo nome. Na lista M3U filtra também pela categoria.
- Telas da série e do filme (servidor e M3U): não pedem a senha de novo; só abrem a partir de itens que já passaram pelo filtro do controle parental. A SearchActivity também filtra pelo controle parental ao buscar.
- Canais sem guia no servidor mostram "Programação indisponível" (esperado)
- Controle parental: no servidor, os itens adultos são tirados DEPOIS de buscar os 10 primeiros, então uma faixa pode mostrar menos de 10 cartões (esperado)
- Controle parental: o reconhecimento de conteúdo adulto é só pelo nome (categoria ou título)
