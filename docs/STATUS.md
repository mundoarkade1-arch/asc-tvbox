# Status do projeto

Última atualização: 24/10/2026

## Fase atual
Todas as etapas de código anteriores estão feitas e VERDES no Actions, mas o roteiro de teste no TX9 ainda NÃO foi feito:
- Etapa 3: passar sozinho para a próxima temporada (servidor e lista M3U).
- Etapa 1: lembrar o episódio onde a pessoa parou (servidor e lista M3U).
- Etapa 4: busca por nome, LIGADA (SearchActivity registrada no AndroidManifest.xml, botão "Buscar" na página inicial).
- Conserto: no "Ver mais" da lista M3U, tocar numa série abre a M3uSeriesActivity.
O guia de programação (EPG) da lista M3U ficou de fora por decisão do dono (ver DECISIONS.md).

FASE ATUAL (decidida com o dono em 24/10/2026): MUDAR TODA A APARÊNCIA E A NAVEGAÇÃO DO APP PRIMEIRO. Os testes no TX9 ficam para DEPOIS da nova aparência (as telas e funções vão mudar, por exemplo a busca passa a ser por aba, então testar agora seria perder tempo). As ideias estão na seção "Nova interface (a fazer)" abaixo. NADA dela foi feito ainda. O dono ainda vai lembrar de mais coisas; se falar, anotar aqui e no DECISIONS.md.

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
- Busca por nome: botão "Buscar" na página inicial, menu (Canais, Filmes, Séries), teclado de letras, tela de resultados. Servidor e lista M3U.
- "Ver mais" da lista M3U: série abre a M3uSeriesActivity (antes mostrava um aviso).

## Nova interface (a fazer) - pedidos do dono em 24/10/2026
Nada disto foi feito. Ainda vão chegar mais pedidos. Ordem e detalhes a definir com o dono, um passo de cada vez.

### Menu e telas principais
- Menu vertical no lado ESQUERDO, só com ícones: Tela inicial, Canais, Filmes, Séries e Ajustes.
- A Tela inicial é sempre a que abre com o app. Na parte grande da direita mostra as indicações: 10 primeiros canais, 10 primeiros filmes, 10 primeiras séries.
- Cada aba (Canais, Filmes, Séries) tem a sua barra de pesquisa, e a busca mostra SÓ aquele tipo (no lugar do botão "Buscar" e do menu de tipo de hoje).
- Ícone de atualizar na tela principal, para recarregar a lista.

### Aba Canais
- Mantém a grade ao lado, com a barra de pesquisa e a linha "Agora: ..." embaixo de cada canal (como já é).
- Barra embaixo da tela, no canal que está sendo assistido: "Agora" com o programa que passa e, do lado direito, o que vai passar depois.
- Tecla de "três setinhas" (como botão de lista), apertada sobre um canal: abre a programação, com o que está passando e os próximos 5 programas. SÓ carrega quando a tecla é apertada, nunca antes.
- A CONFIRMAR com o dono: o controle do TX9 não tem tecla de menu ou lista. Em qual teclado ou controle fica essa tecla das três setinhas? (Pode chegar ao app como tecla de menu do Android.) Combinar também uma alternativa para o controle do TX9 (por exemplo, OK longo no canal).

### Página do filme
- Ao escolher um filme, abre uma página com a capa e a sinopse.
- Botão de tocar e botão "Continuar de onde parei". Se já assistiu parte, o "Continuar" é a opção já pronta para apertar (foco nele).
- Isso pede lembrar o MINUTO exato (hoje o app só lembra o episódio, não o minuto).

### Página da série
- Ao escolher uma série, abre uma página com a capa e a sinopse.
- Botão para escolher a temporada e, embaixo, a lista de episódios.
- Episódios já assistidos ficam marcados com outra cor. O episódio visto pela metade fica com a cor só até a metade.
- Botão "Continuar de onde parei", que vai direto ao episódio certo, sem escolher.

### Player
- Botões do player semi-transparentes (dá para ver, mas são transparentes).
- A barra do player demora muito para sumir: deve sumir mais rápido.
- Hoje o foco sempre cai na opção de configurações. O dono quer o foco livre para avançar e voltar o vídeo com as setas.

### Aba Ajustes
- Controle parental.
- Mudar de lista / gerenciar lista.
- Sobre o dispositivo.
- Sobre o aplicativo.
- Botão para verificar se há atualização.

### Pontos a conferir antes de fazer (limites técnicos)
- Capa e sinopse: o servidor Xtream tem pedidos próprios para isso (get_vod_info para filme e get_series_info para série, um item por vez). A lista M3U em geral NÃO traz sinopse; só a imagem do item. Falar com o dono antes de decidir o que mostrar na lista M3U.
- "Continuar de onde parei" com minuto exato: exige guardar o minuto de cada filme e episódio (arquivo pequeno, com limite de itens, e chave em resumo SHA-1 como no SeriesProgress).
- Episódio "visto" e "visto pela metade": exige guardar, por episódio, o minuto e a duração.
- Memória: o aparelho tem só 1 GB de RAM. Pedir só o que está na tela e nunca guardar listas inteiras.
- Foco bem visível, tudo navegável só com setas, OK e Voltar, e também com o modo mouse.

## Roteiro de teste no TX9 (ainda NÃO feito)
DECIDIDO pelo dono em 24/10/2026: os testes ficam para DEPOIS da nova aparência. No fim da nova aparência, este roteiro será reescrito para as telas novas. Os passos abaixo são do app ANTES da nova aparência e vão mudar (por exemplo, a busca vai ser por aba).
1. Próxima temporada (servidor): abrir uma série com 2 ou mais temporadas, tocar o ÚLTIMO episódio da Temporada 1, avançar o vídeo até o fim com a seta para a direita. Deve começar sozinho o 1º episódio da Temporada 2. No último episódio da série deve aparecer "Fim da série.".
2. Próxima temporada (lista M3U): o mesmo teste, numa série da lista M3U.
3. Onde parou (servidor): tocar o episódio 3 da Temporada 2, apertar Voltar até sair, abrir a mesma série. Deve abrir na Temporada 2 com o episódio 3 já selecionado.
4. Onde parou (lista M3U): o mesmo teste.
5. Busca (servidor): na página inicial apertar "Buscar", escolher Filmes, digitar duas letras ou mais e apertar "Buscar". Devem aparecer cartões com nomes parecidos. Escolher um e conferir que toca. Repetir com Séries (abre a tela da série) e com Canais.
6. Busca (lista M3U): o mesmo, e conferir que demora alguns segundos (esperado).
7. Busca com acento: digitar "acao" deve achar nomes com "Ação".
8. Busca com controle parental ligado: itens com nome adulto não devem aparecer.
9. Busca sem resultado: digitar letras sem sentido deve mostrar "Nada encontrado com esse nome.".
10. "Ver mais" da lista M3U: entrar em Séries, escolher uma série. Deve abrir a tela da série (temporadas e episódios), sem aviso.
11. Conferir que o que já funcionava continua: tocar um canal, um filme e abrir o "Ver mais".

## Arquivos do EPG
- XtreamApi.java: função epg(...) que pede ao servidor a programação de UM canal (get_short_epg, até 4 programas) e devolve o programa de agora e o "a seguir". Horários em milissegundos.
- EpgCache.java: guarda na memória até 80 canais por 1 minuto cada; no máximo 2 pedidos ao servidor ao mesmo tempo; avisa sempre na thread principal
- item_card.xml: tem a linha tv_now (usada para o "Agora: ...")
- activity_home.xml: tem os botões "Buscar" (btn_search), "Controle parental" e "Trocar login" no alto e a caixinha de prévia fixa embaixo (group_preview)
- HomeActivity.java: liga a linha tv_now e a caixinha de prévia ao EpgCache (espera 0,5 segundo parado no canal); tem o menu do controle parental e esconde itens adultos das faixas; tem o botão "Buscar" (menu Canais/Filmes/Séries, depois SearchKeyboard e, no onSearch, abre a SearchActivity com EXTRA_KIND e EXTRA_QUERY); no servidor, série abre a SeriesActivity e "Ver mais" abre a BrowseActivity; na lista M3U carrega as faixas pelo M3uApi, manda o endereço do vídeo ao player, abre a M3uSeriesActivity nas séries e a M3uBrowseActivity no "Ver mais"
- BrowseActivity.java: "Ver mais" do servidor, liga a linha tv_now ao EpgCache; cadeado e senha nas categorias adultas; série abre a SeriesActivity
- activity_main.xml: tem a barra Agora / A seguir (group_nowbar) e a mensagem de status no alto da tela
- MainActivity.java: ver "Arquivos dos episódios" abaixo

## Arquivos do controle parental
- ParentalStore.java: liga e desliga o controle; guarda a senha embaralhada (PBKDF2 com SHA-1, porque o Android 7.1 não tem SHA-256 nesse método) num arquivo próprio, separado do login; confere a senha; trava depois de 5 erros; reconhece categorias e nomes adultos (isAdultCategory, isAdultName)
- PinDialog.java: teclado de números na tela (1 a 9, 0, Apagar e Sair); avisa o app quando o 4º número é digitado

## Arquivos dos episódios de séries
- XtreamApi.java: tipo Kind.EPISODE, classe Episode (id, title, season, number, ext) e função episodes(server, user, pass, seriesId), no máximo 500, lendo aos poucos. streamUrl(...) monta o endereço de um episódio.
- activity_series.xml: layout da tela da série (usado também pela M3uSeriesActivity)
- SeriesActivity.java (servidor): carrega os episódios, ordena por temporada e número, mostra temporadas e episódios. Tem buildQueue() (fila com "Especiais" no fim). Ao escolher um episódio abre o MainActivity levando a fila de TODA a série (EXTRA_LIST_IDS, EXTRA_LIST_EXTS, EXTRA_LIST_INDEX) e a chave da série (MainActivity.EXTRA_SERIES_KEY = "xt|servidor|usuário|idDaSérie"). Ao abrir, lê o SeriesProgress e abre na temporada e no episódio onde parou.
- M3uSeriesActivity.java (lista M3U): igual, mas os episódios vêm do M3uApi.episodes; manda a fila de endereços (MainActivity.EXTRA_LIST_URLS) e a chave "m3u|endereçoDaLista|títuloDaSérie". Se a soma dos endereços passar de 100 mil letras, manda só o episódio escolhido. No lugar do número de identificação manda a posição na fila (+1).
- SeriesProgress.java: guarda a posição do episódio na fila de cada série (SharedPreferences "series_progress"). A chave é um SHA-1 da chave da série (nunca texto, porque o endereço da lista pode ter senha). Máximo de 50 séries; as mais antigas são esquecidas. Funções save(ctx, chave, posição) e load(ctx, chave) (devolve -1 se não há nada).
- MainActivity.java (player): toca canal, filme e episódio. Tem EXTRA_URL, EXTRA_LIST_URLS e EXTRA_SERIES_KEY. Quando o episódio começa a tocar (STATE_READY) anota a posição no SeriesProgress. Ao terminar um episódio toca o próximo da fila (passa para a temporada seguinte); no último mostra "Fim da série." (servidor e lista M3U). Barra Agora / A seguir só em canal ao vivo do servidor.
- AndroidManifest.xml: tem LoginActivity, HomeActivity, BrowseActivity, M3uBrowseActivity, SeriesActivity, M3uSeriesActivity, SearchActivity e MainActivity registradas.

## Arquivos da lista M3U
- M3uApi.java: lê a lista linha por linha direto da internet, sem guardá-la inteira. Para depois de 200 MB ou 1 minuto. Separa canal, filme e série pelas mesmas regras do app original. Séries agrupadas pelo título (sem "S01E03"). Toda a leitura passa por uma função interna (walk). Tem 5 funções públicas:
  - sample(m3uUrl, limite, esconderAdulto): primeiros itens de cada tipo (página inicial)
  - categories(m3uUrl, tipo): nomes das categorias (máximo 3000)
  - page(m3uUrl, tipo, categoria, deslocamento, limite, pularAdulto): uma página de itens
  - episodes(m3uUrl, tituloDaSerie): episódios de UMA série (máximo 500)
  - search(m3uUrl, tipo, texto, pularAdulto, limite): itens do tipo cujo nome tem todas as palavras do texto (sem ligar para maiúsculas e acentos); para ao juntar o limite; séries por título, uma por série
- M3uBrowseActivity.java: "Ver mais" da lista M3U (categorias à esquerda, itens à direita, páginas de 40, máximo 400). Ao tocar numa série, abre a M3uSeriesActivity (EXTRA_ID e EXTRA_NAME), como a HomeActivity.
- XtreamApi.Item tem o campo "url" (endereço direto do vídeo). Vazio nos itens do servidor.
- Nos itens da lista M3U, o "id" é só o número de ordem do item na leitura.
- LoginActivity.java e CredentialStore.java: guardam o login por lista M3U (campo m3uUrl, criptografado)

## Arquivos da busca por nome
- SearchKeyboard.java: teclado de letras na tela (A a Z, 0 a 9, Espaço, Apagar, Buscar, Sair), com setas e OK. Máximo 40 letras, mínimo 2. SearchKeyboard.show(activity, título, listener); o listener tem onSearch(texto) e onCancel(). "Buscar" fecha o teclado e entrega o texto.
- XtreamApi.search(server, user, pass, tipo, texto, pularAdulto, limite): lê get_live_streams / get_vod_streams / get_series aos poucos e guarda só os itens cujo nome combina. Também tem searchWords(...) e matchesWords(...) (sem acento, sem maiúsculas, todas as palavras), usadas pelo M3uApi.
- SearchActivity.java: tela de resultados. Recebe HomeActivity.EXTRA_KIND (LIVE, MOVIE ou SERIES) e SearchActivity.EXTRA_QUERY. Reaproveita activity_browse.xml e esconde a lista de categorias por código. Até 100 resultados. Canal e filme tocam; série abre a SeriesActivity ou a M3uSeriesActivity. Registrada no AndroidManifest.xml e aberta pelo botão "Buscar" da HomeActivity.

## Próximos passos (nesta ordem)
1. Registrar a decisão da nova interface no DECISIONS.md (próximo passo).
2. Perguntar ao dono em qual teclado ou controle fica a tecla das três setinhas e combinar uma alternativa para o controle do TX9.
3. Combinar com o dono a ORDEM da nova interface (sugestão: a) menu à esquerda e tela inicial; b) abas Canais, Filmes e Séries com busca própria; c) aba Ajustes e ícone de atualizar; d) página do filme e da série com capa e sinopse; e) player e "continuar de onde parei" com minuto exato; f) barra Agora / A seguir e a programação com as três setinhas).
4. Fazer cada parte UM ARQUIVO POR VEZ, pedindo o arquivo atual antes (começar pelo activity_home.xml e pelo HomeActivity.java).
5. No fim da nova aparência: reescrever o roteiro de teste para as telas novas e testar tudo no TX9.

## Problemas em aberto
- Armazenamento livre do TX9 e acesso à Play Store: ainda não sabemos
- Etapas 3, 1 e 4 (e o conserto do "Ver mais" da lista M3U) ainda sem teste no TX9 (os testes ficam para depois da nova aparência)
- Tecla das três setinhas: o controle do TX9 não tem tecla de menu ou lista; falta saber em qual teclado ou controle ela fica
- Lista M3U: não tem guia de programação, por decisão do dono (ver DECISIONS.md)
- Lista M3U: a página inicial mostra os 10 primeiros itens na ordem da lista; se a lista for gigante e tiver poucos itens de um tipo no começo, a leitura pode demorar até 1 minuto
- Lista M3U, "Ver mais": cada página nova lê a lista de novo desde o começo, então páginas do fim de uma lista grande demoram mais. Se bater no limite de 200 MB ou 1 minuto, a página pode vir menor e a categoria é tratada como terminada
- Lista M3U, "Ver mais": a categoria é identificada pelo nome (group-title); duas categorias com o mesmo nome aparecem como uma só
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
