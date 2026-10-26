## 2026-10-24: Nova interface, parte (b): abas com pesquisa própria

- Decisão: cada aba (Canais, Filmes, Séries) tem um botão "Pesquisar canais", "Pesquisar filmes" ou "Pesquisar séries" no alto da tela, ao lado do título. OK no botão abre o teclado de letras (SearchKeyboard) e, ao buscar, abre a tela de resultados (SearchActivity) SÓ com o tipo daquela aba.
- Motivo: o controle remoto não tem teclado, então a "barra de pesquisa" é um botão que abre o teclado na tela, igual ao que já existia. Reaproveita SearchKeyboard, SearchActivity, XtreamApi.search e M3uApi.search, sem código novo de busca. A busca por tipo também é mais rápida (lê só a lista do tipo).
- Decisão: a aba é a mesma tela do "Ver mais" (BrowseActivity no servidor e M3uBrowseActivity na lista M3U). Não foi criada tela nova.
- Motivo: essas telas já funcionam e já tratam categorias, páginas, controle parental e guia de programação. Menos código e menos risco no aparelho fraco (1 GB de RAM).
- Decisão: o menu à esquerda ficou guardado num arquivo só (layout_nav.xml), e uma classe pequena (NavBar.java) liga os botões. As telas de aba incluem o menu, escondido por padrão, e só mostram quando chamam o NavBar.
- Motivo: copiar o menu em cada tela deixaria o código repetido. Escondido por padrão porque a SearchActivity reaproveita o mesmo layout (activity_browse.xml) e não deve mudar.
- Decisão: a página inicial (activity_home.xml) NÃO usa o layout_nav.xml. Ela continua com o seu menu próprio, que já foi feito na parte (a).
- Motivo: não mexer numa tela que já funciona. O custo é um menu duplicado em dois arquivos (anotado em "Problemas em aberto" do STATUS.md).
- Decisão: trocar de aba pelo menu abre a aba nova e FECHA a de antes, sem animação. Se a pessoa já está na aba, nada acontece. Tela inicial volta à HomeActivity sem abrir outra por cima.
- Motivo: sem fechar a aba de antes, cada troca empilharia uma tela nova na memória e o botão Voltar andaria por todas elas.
- Decisão: o ícone Atualizar dentro de uma aba recarrega a categoria aberta e limpa o cache do guia (EpgCache).
- Decisão: a ordem de trabalho foi: ícones, depois layout, depois código. Arquivos novos (layout_nav.xml e NavBar.java) foram criados ANTES de serem usados, para o Actions não ficar vermelho.
- Falta fazer na parte (b): ligar o menu na M3uBrowseActivity, tirar o botão "Buscar" com menu de tipo da página inicial (código primeiro, layout depois) e decidir com o dono se a SearchActivity terá o menu.
- Dependências: nenhuma nova.

## 2026-10-24: Nova interface, parte (b) concluída: menu na lista M3U e busca só nas abas

- Decisão: a aba da lista M3U (M3uBrowseActivity) passou a ter o mesmo menu à esquerda da aba do servidor, ligado pelo NavBar. O ícone Atualizar recarrega a categoria aberta.
- Motivo: as duas abas precisam ter a mesma aparência e o mesmo comportamento. A lista M3U não tem guia de programação (decisão anterior do dono), então não há cache para limpar ao atualizar.
- Decisão: o botão "Buscar" e o menu de tipo (Canais, Filmes, Séries) saíram da página inicial. A busca agora só existe dentro de cada aba, pelo botão "Pesquisar ...".
- Motivo: cada aba pesquisa só o seu tipo, então o menu de tipo da página inicial ficou sem uso. Menos botões no alto e menos código na tela inicial.
- Decisão: a tela de resultados da busca (SearchActivity) fica SEM o menu à esquerda. Escolha do dono.
- Motivo: a busca sempre começa dentro de uma aba, e o Voltar leva de volta a ela. A tela de resultados é de passagem. Também evita mexer no layout compartilhado (activity_browse.xml).
- Decisão: a ordem de trabalho foi: primeiro o código Java (HomeActivity.java sem a busca), depois o layout (activity_home.xml sem o botão), para o Actions não ficar vermelho.

- ## 2026-10-24: Nova interface, parte (c): aba Ajustes

- Decisão: criar uma tela nova para os Ajustes (SettingsActivity), com menu à esquerda, lista de 5 opções no meio e uma área de texto à direita. As opções são: Controle parental, Trocar de lista, Sobre o dispositivo, Sobre o aplicativo e Verificar atualização.
- Motivo: o dono pediu uma aba Ajustes para tirar os botões do alto da página inicial. Uma tela própria não mexe nas telas que já funcionam. A lista usa o mesmo estilo das listas de categorias (ListView com item_category.xml), então o foco com setas e OK já é conhecido.
- Decisão: o controle parental e a troca de lista passam a viver só nos Ajustes. Os botões "Controle parental" e "Trocar login" saíram do alto da página inicial.
- Motivo: evitar o mesmo comando em dois lugares. O código do menu do controle parental foi movido da HomeActivity para a SettingsActivity, sem mudar as regras (senha de 4 números, trava depois de 5 erros).
- Decisão: ao voltar de Ajustes, a página inicial confere se o controle parental mudou (onResume) e, se mudou, recarrega as faixas sozinha.
- Motivo: antes, a página inicial recarregava as faixas na hora em que a pessoa mexia no controle parental. Agora a mudança acontece em outra tela, então a página inicial precisa perceber ao voltar. Só recarrega quando mudou, para não gastar internet à toa.
- Decisão: "Trocar de lista" abre o login e fecha todas as telas de antes (flags NEW_TASK e CLEAR_TASK).
- Motivo: sem fechar as telas de antes, o botão Voltar do login levaria de volta a telas da lista antiga.
- Decisão: "Sobre o dispositivo" mostra modelo, versão do Android, memória (total e livre), armazenamento (livre e total) e tamanho da tela.
- Motivo: ajuda a conferir o aparelho e responde uma dúvida antiga do projeto (espaço livre do TX9). Só lê informações do sistema, sem permissão nova.
- Decisão: "Verificar atualização" usa as mesmas funções do Updater que a página inicial já usa. Se achar versão nova, a linha vira "Instalar atualização".
- Motivo: reaproveitar o que já funciona, sem código novo de atualização. O aviso automático da página inicial continua como era.
- Decisão: dentro de Ajustes, o ícone Atualizar do menu não faz nada (não há lista para recarregar). Fica anotado em "Problemas em aberto" do STATUS.md para ver no teste.
- Decisão: o ícone Ajustes do NavBar abre a SettingsActivity e fecha a tela de antes, como na troca de abas.
- Decisão: a ordem de trabalho foi: layout novo, depois a classe nova, depois o registro no AndroidManifest.xml, depois o NavBar, depois a HomeActivity (código) e por último o layout da página inicial (sem os dois botões). Assim o Actions ficou verde em todos os passos.
- Dependências: nenhuma nova.
- Ainda NÃO testado no TX9, por decisão do dono: os testes ficam para depois de toda a nova aparência.

- ## 2026-10-24: Nova interface, parte (d): página do filme e da série (início)

- Decisão: a página do filme e da série é a MESMA nos dois logins (servidor e lista M3U). Escolha do dono ("opção A"). Na lista M3U, que quase nunca traz sinopse, a página mostra a imagem do item, o nome, os botões e o texto "Sem sinopse nesta lista".
- Motivo: o visual e o "Continuar de onde parei" ficam iguais nos dois logins. A alternativa (filme da lista M3U tocando direto, sem página) deixaria esses filmes sem o botão "Continuar".
- Decisão: a parte (d) é feita em DOIS TEMPOS. Agora: páginas com capa, sinopse e botão Tocar (filme) e com capa, sinopse, temporadas e episódios (série). Depois, na parte (e): ligar o "Continuar de onde parei" e as cores de episódio visto.
- Motivo: o "Continuar" e as cores precisam do minuto exato, e o arquivo de minutos só nasce na parte (e). Assim as páginas ficam prontas sem esperar o player.
- Decisão: capa e sinopse vêm do servidor com um pedido por item (get_vod_info para filme e get_series_info para série), pela função XtreamApi.details. Lê só o bloco "info" e para.
- Motivo: aparelho com 1 GB de RAM. Nunca baixar a lista inteira só para mostrar uma sinopse.
- Decisão: details() devolve um resultado vazio (sem erro) quando o servidor não informa nada, e a tela mostra a página sem sinopse. Erros de rede viram IOException e a tela os trata.
- Motivo: os servidores Xtream usam nomes de campo um pouco diferentes. A página precisa abrir mesmo sem capa ou sem sinopse.
- Decisão: ordem de trabalho da parte (d): primeiro a função details (nada usa ainda), depois o layout novo, depois a tela nova, depois o registro no AndroidManifest.xml, e só no fim mudar os lugares que tocam o filme direto. A página da série vem depois da do filme.
- Motivo: o Actions não fica vermelho em nenhum passo.
- Dependências: nenhuma nova.
- Ainda NÃO testado no TX9, por decisão do dono: os testes ficam para depois de toda a nova aparência.

- ## 2026-10-24: Nova interface, parte (d) concluída: página do filme e da série

- Decisão: a página do filme (MovieActivity, layout activity_movie.xml) mostra a capa à esquerda e, à direita, título, linha de informações, botão Tocar e sinopse. O foco abre no botão Tocar. A sinopse fica numa área com rolagem, que recebe foco com a seta para baixo.
- Motivo: o controle só tem setas e OK. Assim a pessoa toca o filme com um único OK e, se quiser ler um texto longo, desce até a sinopse e rola com as setas.
- Decisão: a página do filme NÃO tem o menu à esquerda. É uma tela de passagem, como a de resultados da busca.
- Motivo: o menu serve para trocar de aba, e a página do filme sempre nasce de uma aba ou da página inicial. O Voltar leva de volta para lá.
- Decisão: a página da série reaproveita o layout activity_series.xml, que já era usado pela SeriesActivity (servidor) e pela M3uSeriesActivity (lista M3U). Ganhou uma faixa no alto com capa pequena, título, informações e sinopse.
- Motivo: as duas telas de série precisam ter a mesma aparência. Um layout só evita diferenças entre os dois logins.
- Decisão: na página da série, a sinopse mostra no máximo 4 linhas e NÃO recebe foco.
- Motivo: se a sinopse recebesse foco, as setas ficariam presas nela e a pessoa teria dificuldade para chegar às temporadas e aos episódios. O custo é que sinopses longas aparecem cortadas (anotado em "Problemas em aberto" do STATUS.md).
- Decisão: a capa do item (a mesma do cartão) viaja pelo Intent (MovieActivity.EXTRA_ICON) e aparece na hora. No servidor, quando a capa do servidor chega (XtreamApi.details), ela substitui a do item. Na lista M3U fica só a capa do item e o texto "Sem sinopse nesta lista.".
- Motivo: a página nunca fica vazia esperando a internet, e na lista M3U não há nada mais para pedir.
- Decisão: na página da série do servidor, o pedido de capa e sinopse roda numa thread separada da dos episódios.
- Motivo: se o pedido da sinopse demorar ou falhar, os episódios continuam aparecendo e tocando normalmente.
- Decisão: quatro lugares passaram a abrir a página do filme em vez de tocar direto: HomeActivity, BrowseActivity, M3uBrowseActivity e SearchActivity. Canal ao vivo continua tocando direto. Esses mesmos quatro lugares mandam a capa também ao abrir uma série.
- Motivo: escolher um filme sempre leva à mesma página, em qualquer tela, nos dois logins.
- Decisão: ao apertar Voltar no player de um filme, a pessoa volta para a página do filme (e não para a lista).
- Motivo: é o comportamento natural de empilhar telas e é onde ficará o botão "Continuar de onde parei" na parte (e). A conferir no teste se o dono gosta.
- Decisão: as duas páginas só abrem a partir de itens que já passaram pelo filtro do controle parental e não pedem a senha de novo.
- Decisão: ordem de trabalho: função details, layout do filme, tela do filme, registro no AndroidManifest.xml, os quatro lugares que abrem filme, layout da série, SeriesActivity, M3uSeriesActivity e, por fim, a capa da série nos quatro lugares. O Actions ficou verde em todos os passos.
- Dependências: nenhuma nova.
- Ainda NÃO testado no TX9, por decisão do dono: os testes ficam para depois de toda a nova aparência.
- Falta para depois (parte e): botão "Continuar de onde parei" nas duas páginas e cores de episódio visto.

- 
## 2026-10-24: EPG por canal, sob demanda, sem baixar o guia inteiro

- Decisão: o guia de programação (EPG) é pedido ao servidor canal por canal (ação get_short_epg da API Xtream), só para os canais que aparecem na tela. Os resultados ficam num cache pequeno na memória (EpgCache): no máximo 80 canais, cada um por 1 minuto, e no máximo 2 pedidos ao servidor ao mesmo tempo.
- Motivo: o app original baixa o guia completo (XMLTV) e guarda tudo na memória. No TX9, com 1 GB de RAM, esse arquivo grande pode travar o aparelho.
- Alternativa descartada: baixar e ler o XMLTV inteiro.
- Canal sem guia mostra "Programação indisponível", sem erro.

## 2026-10-24: Lista M3U sem guia de programação

- Decisão: o login por lista M3U NÃO tem guia de programação (nem a linha "Agora", nem a prévia, nem a janela da programação).
- Motivo: decisão do dono. A lista M3U em geral não traz o guia de um jeito simples de ler, e puxar um XMLTV inteiro pesaria no TX9.

## 2026-10-24: Nova aparência primeiro, testes no TX9 depois

- Decisão: mudar toda a aparência e a navegação antes de testar no TX9: menu vertical só com ícones à esquerda (Tela inicial, Canais, Filmes, Séries, Ajustes e Atualizar), aba Ajustes (controle parental, trocar de lista, sobre o dispositivo, sobre o aplicativo, verificar atualização), busca dentro de cada aba, página do filme e da série e player novo.
- Motivo: as telas e funções iam mudar, então testar antes seria perder tempo.
- A tela de resultados da busca (SearchActivity) fica SEM o menu: a busca sempre nasce de uma aba e o Voltar leva de volta a ela.
- Todo o app continua navegável só com setas, OK e Voltar (e modo mouse), com foco bem visível.

## 2026-10-24: Página do filme e da série igual nos dois logins

- Decisão: a página com capa e sinopse é a mesma no login por servidor e no login por lista M3U. No servidor, a capa e a sinopse vêm de get_vod_info e get_series_info, um item por vez. Na lista M3U, que em geral não traz sinopse, a página mostra a capa do item e "Sem sinopse nesta lista.".
- Motivo: manter o app simples e igual nos dois logins, sem pedir nada novo à lista M3U.

## 2026-10-25: Player, "Continuar de onde parei" e programação do canal

- Decisão: em filme e episódio, o player anota o minuto exato (PlaybackProgress, até 300 itens, chave embaralhada com SHA-1 porque o endereço da lista pode ter senha). As setas esquerda e direita pulam 10 segundos, a barra de controles some em 3 segundos e fica semi-transparente.
- Decisão: a tecla das três listras do controle do TX9 (KEYCODE_MENU) abre uma janela com a programação do canal: o que passa agora e os próximos, até 6 programas, pedidos ao servidor SÓ quando a tecla é apertada. Segunda via, caso a tecla chegue com outro código: segurar o OK sobre o canal.
- A barra do canal ao vivo no player tem "Agora" à esquerda e "A seguir" à direita.
- Controle parental: a senha de 4 números é guardada embaralhada com PBKDF2 e SHA-1, porque o Android 7.1 não tem SHA-256 nesse método.
- Nenhuma dependência nova foi adicionada em nenhuma dessas etapas.

- ## 2026-10-24 e 25: registro das decisões do desenvolvimento (junção do que não foi salvo antes)

- Atualização e login: o app se atualiza sozinho pelas Releases do GitHub; o login é conferido antes de salvar (LoginChecker). Motivo: o dono não instala nada no computador.
- EPG por canal (get_short_epg), só dos canais na tela, num cache pequeno (80 canais, 1 minuto, 2 pedidos ao mesmo tempo). Motivo: o guia completo (XMLTV) pesa demais para 1 GB de RAM. Alternativa descartada: baixar e ler o XMLTV inteiro. Canal sem guia mostra "Programação indisponível".
- Lista M3U SEM guia de programação (nem "Agora", nem prévia, nem janela). Escolha do dono; ele usa mais o login por servidor.
- Controle parental: senha de 4 números, PBKDF2 com SHA-1 (o Android 7.1 não tem SHA-256 nesse método), trava depois de 5 erros, adulto reconhecido pelo nome (e pela categoria na lista M3U).
- Séries: o player recebe a fila de TODOS os episódios (temporadas em ordem, "Especiais" no fim) e toca o próximo sozinho. A posição na fila fica em SeriesProgress (50 séries, chave SHA-1 porque o endereço pode ter senha).
- Ordem de trabalho: mudar TODA a aparência primeiro e testar no TX9 depois. Motivo: as telas iam mudar.
- Busca: cada aba (Canais, Filmes, Séries) tem um botão "Pesquisar ..." que abre o teclado de letras e busca SÓ aquele tipo. Reaproveita SearchKeyboard, SearchActivity, XtreamApi.search e M3uApi.search. A tela de resultados fica sem menu (escolha do dono).
- Aba = a mesma tela do "Ver mais" (BrowseActivity e M3uBrowseActivity). Motivo: já tratam categorias, páginas, controle parental e guia; menos código e risco.
- Menu à esquerda guardado num arquivo (layout_nav.xml) ligado pelo NavBar.java. A página inicial tem uma cópia própria (duplicado de propósito, para não mexer em tela que funciona). Trocar de aba fecha a aba de antes, para não empilhar telas na memória.
- Ajustes: tela própria (SettingsActivity) com controle parental, trocar de lista (abre o login e fecha as telas de antes), sobre o dispositivo, sobre o aplicativo e verificar atualização. Ao voltar de Ajustes, a página inicial recarrega as faixas só se o controle parental mudou.
- Página do filme e da série IGUAL nos dois logins. Servidor: capa e sinopse por get_vod_info e get_series_info, um item por vez, lendo só o bloco "info". Lista M3U: capa do item e "Sem sinopse nesta lista.". A página abre mesmo sem capa ou sem sinopse. Página do filme sem menu à esquerda. Sinopse da série: 4 linhas, sem foco (para não prender as setas). Voltar do player leva à página do filme.
- "Continuar de onde parei": o player anota o minuto exato de filmes e episódios (PlaybackProgress, 300 itens, SHA-1). Visto = 95% ou mais. Episódio visto fica verde no texto; visto pela metade fica verde só até a metade. Tocar começa do zero; Continuar começa no minuto.
- Player: setas esquerda e direita pulam 10 s em filme e episódio, a barra some em 3 s, controles semi-transparentes (setAlpha 0.65). Barra do canal ao vivo: Agora à esquerda e A seguir à direita.
- Programação do canal: tecla das três listras (KEYCODE_MENU) ou OK segurado abre uma janela com o que passa agora e os próximos (até 6), pedidos ao servidor SÓ nesse momento.

## 2026-10-25: Primeiro teste no TX9 e Rodada 1 de ajustes

- Fato: o dono testou a nova aparência (partes a a f) e pediu 4 ajustes. Nada começado: (R1) menu com texto, (R2) tela inicial sem a caixinha de prévia, (R3) ordem do foco menu, categorias, itens, (R4) trocar de canal com as setas no player.
- Decisão: o menu da esquerda mostra ícone e texto. Motivo: só ícone não deixava claro o que cada botão faz.
- Decisão: a tela inicial NÃO mostra a caixinha de prévia. A janela da programação continua e não pode depender dela.
- Decisão: nas abas, o foco anda menu, categorias, itens; a seta esquerda a partir dos itens para nas categorias.
- Decisão: no player de canal ao vivo, seta para cima volta, seta para baixo avança e OK mostra a barra. Suposição a confirmar: só os canais da lista de onde ele veio.
- Decisão (documentos): o STATUS.md ficou curto; o mapa de arquivos e os limites foram para ARQUIVOS.md; as regras de trabalho e o prompt FIXO do próximo agente foram para o AGENTS.md; ARCHITECTURE.md foi preenchido (build, versão e dependências). Motivo: o STATUS era regerado inteiro, estava pesado e os agentes não conseguiam fazer a passagem.
- Dependências: nenhuma nova.

- ## 25/10/2026 - Rodada 1, R1 (menu com texto) e respostas do dono

- Menu da esquerda: os botões deixaram de ser ImageButton e viraram TextView com ícone à esquerda (drawableLeft) e o nome ao lado. Motivo: o dono quer ler o nome de cada botão. NavBar.java não mudou, pois só usa os ids e o clique.
- Coluna do menu: de 88dp para 200dp. O menu está em layout_nav.xml e há uma cópia em activity_home.xml. As duas devem ficar iguais.
- Foi acrescentado o botão com o nome "Atualizar" (antes só tinha o ícone).
- Tela inicial: a linha "Agora: ..." sob o cartão do canal FICA. O dono quer tirar só a caixinha de prévia (programa, barra de progresso e "A seguir").
- Player de canal ao vivo, cima e baixo: a lista de canais é TODAS as categorias em ordem, como uma lista só, começando no canal atual. Ao acabar uma categoria, passa para a próxima. Motivo: pedido do dono. Isso substitui a suposição antiga de passar só pela categoria aberta.
- Novos pedidos (Rodada 2): R5 (as 3 faixas da tela inicial inteiras na tela, sem rolagem) e R6 (no cartão "Ver mais", direita de novo não faz nada; para mudar de faixa, só cima e baixo).
- A tecla das três listras ainda não foi testada no TX9. O dono testa depois das melhorias.
- Sem dependências novas.

- ## 25/10/2026 - Rodada 1, R2 (tela inicial sem a caixinha de prévia)

- Tela inicial: a caixinha de prévia (programa de agora, barra de progresso, horários e "A seguir") foi removida. Motivo: pedido do dono. A linha "Agora: ..." no cartão do canal continua.
- HomeActivity.java deixou de usar schedulePreview, fetchPreview, showPreview, hidePreview, o Handler e o atraso de prévia. Também saíram os imports que só serviam para isso.
- A variável previewItem foi trocada por focusedLive (canal ao vivo do servidor que está com o foco). Motivo: previewItem também decidia se a tecla de menu e o OK segurado abriam a programação (EpgDialog). Assim essa função continua funcionando. focusedLive fica vazia quando o foco vai para outro lugar (menu, botões de atualização, filmes, séries, "Ver mais").
- group_preview e seus textos e barra de progresso saíram do activity_home.xml. Ordem seguida: código primeiro, layout depois.
- Sem dependências novas.

- ## 25/10/2026 - Rodada 1, R3 (ordem do foco nas abas)

- Causa do pulo: a grade de itens não tinha regra para a seta esquerda. O Android escolhia sozinho o vizinho e acabava indo ao menu, passando por cima das categorias.
- Correção: em BrowseActivity.java e M3uBrowseActivity.java, o dispatchKeyEvent trata a seta esquerda com o foco na grade. Se o item selecionado está na primeira coluna (posição dividida pelo número de colunas dá resto 0), o foco vai para a lista de categorias (goToCategories), já na categoria aberta. Nas outras colunas, a seta esquerda funciona como antes. Da lista de categorias, a seta esquerda continua indo ao menu.
- Motivo de usar o código e não nextFocusLeft no layout: a grade e a lista são ListView e GridView, e as linhas não recebem foco (a lista controla a seleção). Uma regra no layout não sabe em que coluna o foco está.
- Séries (activity_series.xml) e Ajustes (activity_settings.xml): a ordem já estava certa (temporadas e episódios lado a lado; opções e detalhe sem foco). Sem mudança de foco.
- Correção achada junto: o include do menu em activity_browse.xml e activity_settings.xml ainda estava com 88dp. Passou para 200dp, igual ao menu novo da R1. Sem isso, os nomes ficariam cortados.
- Sem dependências novas.

- ## 25/10/2026 - Rodada 1, R4 (trocar de canal com as setas), parte 1

- Nova classe ChannelQueue.java: fila de canais ao vivo. Ordem: todas as categorias, uma depois da outra, a partir do canal que toca. Só UMA categoria fica na memória por vez (páginas de 40, máx. 400). Motivo: 1 GB de RAM e a regra do dono (lista única passando de categoria sozinha).
- Servidor: categoria = número; lista M3U: categoria = nome do grupo; nula = "Todas as categorias" (fila única).
- Fim da lista: ESCOLHA DO AGENTE, o dono não lembrou: dá a volta (último -> primeiro e primeiro -> último). Ele testa e pode pedir "parar".
- Parental ligado: categorias adultas são puladas e canais com nome adulto não entram, menos na categoria de onde a pessoa saiu.
- MainActivity.java: cima/baixo só em canal ao vivo e só se a tela mandar EXTRA_QUEUE (true) e EXTRA_QUEUE_CAT. Sem EXTRA_QUEUE nada muda. Motivo: classe e player primeiro, telas depois (código antes de quem usa).
- Depois de trocar: servidor mostra a barra Agora / A seguir; lista M3U mostra o nome do canal (Toast), pois não tem guia.
- Limite: se o canal tocado não estiver nos primeiros 400 da categoria, as setas ficam desligadas (sem erro).
- Falta: HomeActivity, BrowseActivity, M3uBrowseActivity e SearchActivity mandarem EXTRA_QUEUE e EXTRA_QUEUE_CAT.
- Sem dependências novas.

- ## 25/10/2026 - Rodada 1, R4 concluída: as quatro telas mandam a fila de canais

- Decisão: HomeActivity, BrowseActivity, M3uBrowseActivity e SearchActivity passam a mandar MainActivity.EXTRA_QUEUE = true ao abrir um canal ao vivo. Filmes, séries e episódios não mudam.
- Decisão: a categoria de origem (EXTRA_QUEUE_CAT) só é mandada pelas abas. BrowseActivity manda currentCat (número) e M3uBrowseActivity manda currentGroup (nome do grupo). Em "Todas as categorias", nada é mandado, e isso quer dizer "todas".
- Motivo: no Java, passar null direto em putExtra pode dar erro de compilação por ser ambíguo. Não mandar nada tem o mesmo efeito e é mais seguro.
- Decisão: HomeActivity e SearchActivity não mandam categoria (fila "todas").
- Motivo: a tela inicial junta canais de várias categorias, e a busca junta resultados soltos. Não existe uma categoria única de origem.
- Limite aceito: na fila "todas", se o canal tocado não estiver entre os primeiros 400, as setas cima e baixo ficam desligadas, sem erro. Pode acontecer na busca com canal muito no fim da lista. O dono decide depois do teste se isso precisa mudar.
- Fim da lista: continua a escolha do agente (dar a volta). O dono ainda vai testar.
- Ordem de trabalho: ChannelQueue e MainActivity primeiro (nada usava), depois as quatro telas, uma por vez. O Actions ficou verde em todos os passos.
- Sem dependências novas.
- Ainda NÃO testado no TX9, por decisão do dono: os testes ficam para o fim da rodada.

- ## 25/10/2026 - Rodada 2, R5 (tela inicial sem rolagem) e R6 ("Ver mais" trava a direita)

- Decisão (R5): criar um cartão menor só para a tela inicial, o item_card_small.xml (130dp de largura, imagem de 68dp, nome em 13sp). O item_card.xml (170dp) NÃO foi mudado e continua nas abas, na busca e nas telas de "Ver mais".
- Motivo: o item_card.xml é usado em várias telas. Encolher ele estragaria as abas, que têm espaço de sobra. Um arquivo novo é mais seguro e não afeta nada que já funciona.
- Decisão (R5): a HomeActivity passou a usar item_card_small nos cartões das faixas e no cartão "Ver mais". Os ids internos (tv_initial, iv_logo, tv_name, tv_now) são os mesmos, então o resto do código não mudou.
- Decisão (R5): no activity_home.xml, a ScrollView vertical foi tirada. O título "ASC TV Box" ficou com 24sp, os títulos das faixas com 20sp, os avisos com 16sp, e os espaços entre as faixas ficaram menores (8dp).
- Motivo: o pedido do dono é ver as três faixas inteiras, sem rolagem. As faixas continuam rolando para o lado, porque cada uma tem 11 cartões.
- Risco aceito: a altura real da tela do TX9 não é conhecida. O dono confere no teste. Se sobrar corte, diminuir mais o cartão pequeno e os espaços. Com o aviso de atualização aberto, a tela pode apertar um pouco.
- Decisão (R6): no cartão "Ver mais" da tela inicial, a seta para a direita não faz nada. O código dá ao cartão um id próprio (View.generateViewId) e manda o foco da direita para ele mesmo (setNextFocusRightId). Mudar de faixa é só com cima e baixo.
- Motivo: pedido do dono. Sem essa regra, a direita pulava para a faixa de baixo.
- Ordem de trabalho: item_card_small.xml (arquivo novo, nada usava), depois HomeActivity.java, depois activity_home.xml, depois a regra da R6 na HomeActivity. O Actions ficou verde em todos os passos.
- Sem dependências novas.
- Ainda NÃO testado no TX9, por decisão do dono: os testes são o próximo passo.
- Dependências: nenhuma nova.

- ## 26/10/2026 - Rodada 3 (parte 1): player, grade com 5 colunas e Pesquisar nas categorias

- Fato: o dono testou R1 a R6 no TX9 e disse que funcionam. Fez novos pedidos (Rodada 3), anotados como METAS no STATUS.md.
- Decisão: em filme e episódio, OK pausa e continua o vídeo, mesmo com a barra de controles escondida. Motivo: antes, o OK só mostrava a barra e não pausava.
- Decisão: seta direita/esquerda em filme e episódio: um toque pula 10 segundos; segurando, continua pulando enquanto a tecla está apertada, com passos maiores quanto mais tempo segura (10 s até 2 s segurando, 30 s até 5 s, depois 60 s), um pulo a cada 250 ms. Motivo: pedido do dono (avançar o tanto que segurar). Os passos crescentes evitam que um filme longo demore demais para andar.
- Decisão: em episódio, faltando 2 minutos, aparece um botão pequeno "Pular para o próximo episódio" embaixo, à direita. OK pula, marca o episódio como visto e toca o próximo. Enquanto o botão está na tela, OK pula (não pausa). Só aparece se existe próximo episódio na fila.
- Motivo: o botão é criado dentro do MainActivity.java, sem mexer em layout. Ele não recebe foco; o OK é tratado em dispatchKeyEvent.
- Decisão: o cartão das abas (item_card.xml) passou de 170dp para 150dp de largura (imagem de 110dp para 90dp, nome 14sp), e a grade das abas passou a ter 5 colunas fixas (activity_browse.xml). A coluna de categorias passou de 300dp para 190dp.
- Motivo: pedido do dono (5 por linha e categorias menores, ainda com o nome). Os ids do cartão continuam iguais aos do item_card_small.xml.
- Risco aceito: a largura real da tela do TX9 não é conhecida; se cortar, ajustar columnWidth e a largura da lista de categorias.
- Decisão: "Pesquisar" é a PRIMEIRA linha da lista de categorias (linha 0); a categoria N fica na linha N+1. OK nessa linha abre o teclado de letras, como o botão fazia. Feito na BrowseActivity (servidor). Falta a M3uBrowseActivity e depois tirar o botão do alto do activity_browse.xml.
- Motivo: pedido do dono. A seta esquerda, vinda da grade, volta para a linha da categoria aberta (com +1 por causa da linha do Pesquisar).
- Ordem de trabalho: MainActivity primeiro; item_card.xml antes do activity_browse.xml; código antes do layout. O Actions ficou verde em todos os passos.
- Dependências: nenhuma nova.

- ## 26/10/2026 - Rodada 3 (parte 2): Pesquisar na lista M3U, menu, cartão alto e capa do filme

- Decisão: "Pesquisar" também é a primeira linha da lista de categorias na lista M3U (M3uBrowseActivity). O botão antigo do alto saiu do activity_browse.xml. Motivo: pedido do dono; as duas abas (servidor e lista M3U) ficam iguais. Antes de tirar o botão, conferi que a SearchActivity (que reaproveita o mesmo layout) não usa esse botão.
- Decisão: ordem de trabalho: código das duas telas primeiro, layout por último, para o Actions não ficar vermelho.
- Decisão (M3): cada botão do menu da esquerda ganhou nextFocusDown e nextFocusUp, na ordem Tela inicial, Canais, Filmes, Séries, Ajustes, Atualizar. Feito em layout_nav.xml e na cópia dentro de activity_home.xml. Motivo: descendo de Ajustes, o foco ia para Séries e não chegava em Atualizar, porque o Android escolhe sozinho o vizinho mais perto e o Atualizar fica separado, no pé da coluna. Causa provável, ainda a confirmar no TX9.
- Decisão (M4): a imagem do cartão das abas (item_card.xml) passou de 90dp para 130dp de altura; a largura (150dp) não mudou. Motivo: o dono achou os cartões "amassados". O item_card_small.xml (tela inicial) não mudou. Risco: cabem menos linhas na tela; se ficar alto demais, usar cerca de 110dp.
- Decisão (M5, filme): a capa da página do filme passou de 240x360dp para 340x510dp (activity_movie.xml), mantendo o formato de pôster. Motivo: o dono achou a capa pequena. Risco: a altura real da tela do TX9 não é conhecida; se cortar, diminuir. A capa da página da série (activity_series.xml) ainda falta.
- Decisão: novas metas do dono anotadas no STATUS.md (M2 ícone, M6 "Continue assistindo" na tela inicial), para os próximos agentes irem eliminando.
- Dependências: nenhuma nova.
- Ainda NÃO testado no TX9.

- ## 26/10/2026 - Rodada 3, M5b: capa maior na página da série

- Decisão: a capa da página da série (activity_series.xml) passou de 100x150dp para 160x240dp, mantendo o formato de pôster. Só mudaram a largura e a altura do ImageView iv_cover. Sinopse (4 linhas, sem foco), botão Continuar e listas de temporadas e episódios não mudaram.
- Motivo: pedido do dono (capa pequena). A capa NÃO ficou do tamanho da do filme (340x510dp) porque, na página da série, ela fica no alto e as listas de temporadas e episódios ficam embaixo. Uma capa muito alta tiraria o espaço dos episódios.
- Risco aceito: a altura real da tela do TX9 não é conhecida. Se a capa ainda parecer pequena, aumentar; se os episódios ficarem apertados, diminuir.
- Capa do filme (activity_movie.xml, 340x510dp) confirmada pelo dono: salva e verde.
- Dependências: nenhuma nova.
- Ainda NÃO testado no TX9.

- ## 26/10/2026 - Rodada 3, M2: ícone do app

- Decisão: o ícone do app é um desenho em texto (vetor), no arquivo res/drawable/ic_app_icon.xml: quadrado vermelho (#FF2A3D, o mesmo vermelho do título) de cantos redondos, com um triângulo branco de "play" no meio. Ligado no AndroidManifest.xml com android:icon="@drawable/ic_app_icon" dentro de <application>.
- Motivo: o dono não tem imagem pronta e não usa terminal. Um desenho em texto se cria só pelo site do GitHub, é bem leve e não precisa de dependência nem de arquivo de imagem.
- Decisão: ordem de trabalho: arquivo novo do desenho primeiro (nada usava), depois a ligação no AndroidManifest.xml. O Actions ficou verde nos dois passos.
- Risco aceito: alguns TV box mostram na lista de apps um "banner" largo (android:banner) e não o ícone quadrado. Só se saberá no teste no TX9. Se aparecer sem imagem, criar o banner depois.
- Limite: o ícone é simples e provisório. O dono pode pedir outro desenho depois.
- Dependências: nenhuma nova.
- Ainda NÃO visto no TX9.

- ## 26/10/2026 - Rodada 3, M6 (parte 1): guardar e anotar o "Continue assistindo"

- Decisão: a lista do "Continue assistindo" fica numa classe nova, ContinueStore.java, com no máximo 25 itens (o mais novo primeiro; ao entrar um 26º, o mais antigo sai; se o mesmo vídeo entra de novo, sobe para o começo). Cada item guarda: tipo (filme ou episódio), nome, capa, endereço do vídeo, id, extensão, chave da série, minuto, duração e hora.
- Motivo: o PlaybackProgress guarda só chave embaralhada (SHA-1), sem nome nem capa, e não dá para montar cartões com ele. Ele NÃO foi mudado.
- Decisão (autorizada pelo dono): a lista inteira é guardada CRIPTOGRAFADA (AES-GCM, chave do Android Keystore, a mesma do CredentialStore, com o mesmo plano B sem criptografia se o Keystore falhar). Motivo: o endereço do vídeo pode ter usuário e senha (servidor e lista M3U). A chave de cada item é o SHA-1 do endereço.
- Decisão: o player (MainActivity) anota na ContinueStore junto com o minuto exato (a cada 10 s e ao sair). Entra se parou entre 5 s e 95%; sai se passou de 95% ou terminou. Canal ao vivo não entra.
- Decisão: a capa chega ao player por um recado novo, MainActivity.EXTRA_ICON (opcional; sem capa, o recado não é mandado, sem null solto). MovieActivity manda a capa do filme (a do servidor, se chegar). SeriesActivity e M3uSeriesActivity mandam a capa da série.
- Decisão: o nome do episódio na lista é "Nome da série - ep. N", em que N é a posição do episódio na fila da série. As duas telas de série mandam esse nome como EXTRA_NAME, e o player usa o mesmo formato ao passar sozinho para o próximo. O player não mostra o nome do episódio em nenhum lugar, então nada muda na tela de reprodução.
- Motivo do formato: simples e igual nos dois logins. Limite aceito: não mostra temporada e número (T2 E3).
- Decisão: ordem de trabalho: ContinueStore (nada usava), MainActivity, MovieActivity, SeriesActivity, M3uSeriesActivity. Falta HomeActivity (código) e depois activity_home.xml. O Actions ficou verde em todos os passos.
- Limites anotados: o clique na faixa (a fazer) abrirá o player direto, sem a fila de episódios, então o próximo episódio não toca sozinho a partir da faixa. "Trocar de lista" ainda não limpa a ContinueStore (a fazer). Se o servidor mudar de endereço, os itens antigos deixam de tocar.
- Dependências: nenhuma nova.
- Ainda NÃO testado no TX9.
- Ainda NÃO testado no TX9.
- Ainda NÃO testado no TX9, por decisão do dono: os testes ficam para depois de toda a nova aparência.
