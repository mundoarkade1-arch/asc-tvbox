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
- Dependências: nenhuma nova.
- Ainda NÃO testado no TX9, por decisão do dono: os testes ficam para depois de toda a nova aparência.
