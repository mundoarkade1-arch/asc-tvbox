# Status do projeto

Última atualização: 24/10/2026

## Fase atual
Etapa "Ver mais por categorias na lista M3U" CONCLUÍDA e testada no TX9. Próxima etapa: episódios das séries na lista M3U.

## Feito (testado no TX9)
- Repositório criado e documentação preenchida (README, AGENTS, CLAUDE, llms.txt, docs/*)
- Aparelho de teste identificado e documentado (TX9, S905W, Android 7.1, 1 GB RAM, tela 1080p)
- Decisão tomada: app Android nativo em Java com ExoPlayer, sem WebView (ver docs/DECISIONS.md)
- Esqueleto do app nativo e workflow do GitHub Actions que gera o APK
- Login por servidor (DNS, usuário e senha), com mensagens de erro corretas
- Login por lista M3U (endereço da lista), com mensagens de erro corretas
- Atualização automática do app (aviso na página inicial, instalação pelo próprio app)
- Abrir direto na página inicial quando há login salvo
- Página inicial com 3 faixas (Canais ao vivo, Filmes, Séries) de 10 itens cada, no login por servidor e no login por lista M3U
- Tocar canal e filme (servidor e lista M3U)
- "Ver mais" no login por servidor: cartão no fim de cada faixa que abre a navegação por categorias (BrowseActivity), com borda vermelha de seleção
- "Ver mais" no login por lista M3U: o mesmo cartão, que abre a navegação por categorias da lista (M3uBrowseActivity)
- Linha "Agora: ..." nos cartões de canais ao vivo, na página inicial e no "Ver mais" (só no login por servidor)
- Caixinha de prévia na página inicial: ao parar o foco num canal ao vivo, uma faixa fixa embaixo mostra o programa de agora, a barra de progresso, os horários e o "A seguir" (só no login por servidor)
- Barra Agora / A seguir no player (MainActivity): aparece quando o canal ao vivo começa a tocar, some depois de 6 segundos e volta ao apertar OK; não aparece em filmes nem em episódios; só no login por servidor
- Controle parental:
  - Botão "Controle parental" na página inicial, com menu: ativar, trocar senha e desativar
  - Senha de 4 números, digitada num teclado de números na tela (setas e OK)
  - Com o controle ligado, canais, filmes e séries com nome adulto não aparecem nas faixas da página inicial (servidor e lista M3U)
  - No "Ver mais" (servidor e lista M3U), categorias adultas ficam com cadeado e pedem a senha; a liberação vale só enquanto a pessoa está naquela categoria
  - Em "Todas as categorias", conteúdo adulto não aparece com o controle ligado
  - Depois de 5 senhas erradas seguidas, o app trava por um tempo antes de aceitar nova tentativa
- Episódios de séries (login por servidor):
  - Tocar numa série (na página inicial e no "Ver mais") abre a tela da série
  - Tela da série: temporadas à esquerda, episódios da temporada escolhida à direita; as setas na lista de temporadas trocam os episódios sozinhas; OK numa temporada leva o foco para os episódios; OK num episódio abre o player
  - O player toca o episódio e, quando ele termina, toca sozinho o próximo da mesma temporada; no último mostra "Fim da temporada." e para
- Lista M3U:
  - A página inicial lê a lista aos poucos e para ao achar 10 canais, 10 filmes e 10 séries
  - Canal e filme da lista tocam direto pelo endereço que vem na lista
  - Controle parental esconde itens adultos (pela categoria e pelo nome) já na leitura
  - "Ver mais": categorias achadas lendo a lista, itens por páginas de 40 (máximo 400 por categoria), canal e filme tocam direto
  - Tocar numa série da lista M3U (na página inicial ou no "Ver mais") mostra o aviso "os episódios chegam na próxima etapa" (esperado)

## Arquivos do EPG
- XtreamApi.java: função epg(...) que pede ao servidor a programação de UM canal (get_short_epg, até 4 programas) e devolve o programa de agora e o "a seguir". Horários em milissegundos.
- EpgCache.java: guarda na memória até 80 canais por 1 minuto cada; no máximo 2 pedidos ao servidor ao mesmo tempo; avisa sempre na thread principal
- item_card.xml: tem a linha tv_now (usada para o "Agora: ...")
- activity_home.xml: tem o botão "Controle parental" no alto e a caixinha de prévia fixa embaixo (group_preview)
- HomeActivity.java: liga a linha tv_now e a caixinha de prévia ao EpgCache (espera 0,5 segundo parado no canal antes de pedir o guia); tem o menu do controle parental e esconde itens adultos das faixas; ao escolher uma série abre a SeriesActivity (login por servidor); no login por lista M3U carrega as faixas pelo M3uApi e manda o endereço do vídeo ao player; o cartão "Ver mais" abre a BrowseActivity (servidor) ou a M3uBrowseActivity (lista M3U)
- BrowseActivity.java: liga a linha tv_now ao EpgCache (sem caixinha de prévia, por decisão do dono); mostra o cadeado e pede a senha nas categorias adultas; ao escolher uma série abre a SeriesActivity
- activity_main.xml: tem a barra Agora / A seguir (group_nowbar) e a mensagem de status no alto da tela
- MainActivity.java: mostra a barra Agora / A seguir nos canais ao vivo do servidor; toca episódios e passa para o próximo da temporada; no login por lista M3U toca o endereço direto recebido pela tela anterior (EXTRA_URL)

## Arquivos do controle parental
- ParentalStore.java: liga e desliga o controle; guarda a senha embaralhada (PBKDF2 com SHA-1, porque o Android 7.1 não tem SHA-256 nesse método) num arquivo próprio, separado do login (o botão "Trocar login" não apaga); confere a senha; trava depois de 5 erros; reconhece categorias e nomes adultos pelas mesmas palavras do app original
- PinDialog.java: teclado de números na tela (1 a 9, 0, Apagar e Sair); avisa o app quando o 4º número é digitado

## Arquivos dos episódios de séries (servidor)
- XtreamApi.java: tipo Kind.EPISODE, classe Episode (id, title, season, number, ext) e função episodes(server, user, pass, seriesId), que pede ao servidor (get_series_info) os episódios de UMA série, no máximo 500, lendo aos poucos. streamUrl(...) monta o endereço de um episódio (/series/usuario/senha/id.ext).
- activity_series.xml: layout da tela da série (título no alto, lista de temporadas à esquerda, lista de episódios à direita, mensagem de status no meio)
- SeriesActivity.java: carrega os episódios de uma série, ordena por temporada e número, mostra temporadas e episódios (reaproveita item_category.xml); ao escolher um episódio abre o MainActivity levando a lista de episódios da mesma temporada (ids, extensões e posição). Temporada 0 aparece como "Especiais".
- AndroidManifest.xml: tem a SeriesActivity registrada (mesmo padrão das outras telas)

## Arquivos da lista M3U
- M3uApi.java: lê a lista linha por linha direto da internet, sem guardá-la inteira. Para depois de 200 MB ou 1 minuto. Separa canal, filme e série pelas mesmas regras do app original (caminho do endereço, tipo de arquivo, palavras na categoria e no título). Séries são agrupadas pelo título (sem "S01E03"). Tem 3 funções públicas:
  - sample(m3uUrl, limite, esconderAdulto): os primeiros itens de cada tipo (página inicial); para ao achar o limite de cada tipo
  - categories(m3uUrl, tipo): os nomes das categorias (group-title) que têm itens daquele tipo, na ordem da lista; guarda só os nomes (máximo 3000)
  - page(m3uUrl, tipo, categoria, deslocamento, limite, pularAdulto): uma página de itens de um tipo, de uma categoria ou de todas (categoria null); pula os primeiros "deslocamento" itens; itens adultos pulados não contam no deslocamento; a lista é lida desde o começo a cada chamada, mas só a página fica na memória
- M3uBrowseActivity.java: tela do "Ver mais" da lista M3U. Reaproveita activity_browse.xml, item_category.xml e item_card.xml. Categorias à esquerda (a 1ª é "Todas as categorias"), itens à direita, páginas de 40 (máximo 400). As categorias são lidas uma vez, depois que a primeira página aparece. Cadeado e senha nas categorias adultas (liberação vale só para aquela categoria). Canal e filme tocam direto pelo endereço da lista (EXTRA_URL). Série mostra o aviso. Sem linha "Agora" (a lista M3U ainda não tem guia).
- AndroidManifest.xml: tem a M3uBrowseActivity registrada
- XtreamApi.Item ganhou o campo "url" (endereço direto do vídeo). Vazio nos itens do servidor; preenchido nos canais e filmes da lista M3U. XtreamApi.streamUrl devolve esse endereço como está, quando existe.
- Nos itens da lista M3U, o "id" é só o número de ordem do item na leitura (não é um número do servidor). Itens de série da lista M3U NÃO têm endereço: serão preenchidos na etapa dos episódios, relendo a lista pelo título da série.
- LoginActivity.java e CredentialStore.java: já guardavam o login por lista M3U (campo m3uUrl, criptografado); não mudaram.

## Próximos passos (nesta ordem)
1. Lista M3U: episódios das séries (tela da série com temporadas e episódios, relendo a lista pelo título da série, reaproveitando a SeriesActivity se der; o player toca o próximo episódio sozinho).
2. Lista M3U: guia de programação (EPG). É pesado: o app original baixa um arquivo XMLTV inteiro. Decidir com o dono se vale a pena no TX9 e, se sim, como (por exemplo, ler só os canais da tela, lendo aos poucos).
3. Depois, definir com o dono as próximas etapas. Fora do que já foi feito, só se o dono pedir: lembrar onde parou numa série, capa e sinopse dos episódios, passar sozinho para a próxima temporada.

## Problemas em aberto
- Armazenamento livre do TX9 e acesso à Play Store: ainda não sabemos
- Lista M3U: ainda não tem episódios das séries nem guia de programação (ver passos 1 e 2 acima)
- Lista M3U: a página inicial mostra os 10 primeiros itens na ordem em que aparecem na lista; se a lista for gigante e tiver poucos itens de um tipo no começo, a leitura pode demorar até o limite de 1 minuto
- Lista M3U, "Ver mais": cada página nova lê a lista de novo desde o começo, então páginas mais para o fim de uma lista grande demoram mais. Se a leitura bater no limite de 200 MB ou de 1 minuto, a página pode vir menor e a categoria é tratada como terminada
- Lista M3U, "Ver mais": as categorias chegam alguns segundos depois dos primeiros itens (comportamento esperado)
- Lista M3U, "Ver mais": a categoria é identificada pelo nome (group-title); duas categorias com o mesmo nome aparecem como uma só
- Canais sem guia no servidor mostram "Programação indisponível" (comportamento esperado, não é erro)
- Controle parental: no login por servidor, os itens adultos são tirados DEPOIS de buscar os 10 primeiros, então uma faixa pode mostrar menos de 10 cartões (comportamento esperado, não é erro). Na lista M3U os adultos já são pulados durante a leitura.
- Controle parental: o reconhecimento de conteúdo adulto é só pelo nome (categoria ou título). Conteúdo adulto com nome comum pode passar; categorias com nome normal não ganham cadeado
- Séries (servidor): a tela da série não pede a senha de novo; ela só abre a partir de itens que já passaram pelo filtro do controle parental (decisão registrada em DECISIONS.md). Esse caminho com o controle ligado ainda não foi testado de propósito no TX9
- Séries (servidor): o player só passa para o próximo episódio da MESMA temporada; no último mostra "Fim da temporada." e para
