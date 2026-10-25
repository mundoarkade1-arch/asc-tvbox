# Status do projeto

Última atualização: 25/10/2026 (R3 pronta)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
A nova aparência (partes a a f) está pronta e VERDE no Actions. O dono fez o PRIMEIRO TESTE no TX9 em 25/10/2026 e aprovou a barra Agora / A seguir do canal. Ele pediu 4 ajustes (Rodada 1) e depois mais 2 (Rodada 2). R1, R2 e R3 estão prontas e verdes. R4, R5 e R6 não foram começadas.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial com 3 faixas, "Ver mais" por categorias, séries (próxima temporada sozinha, onde parou), busca por nome, EPG por canal (só servidor).
- (a) menu à esquerda e tela inicial. (b) abas com pesquisa própria. (c) aba Ajustes. (d) páginas de filme e série com capa e sinopse. (e) player: setas pulam 10 s, barra some em 3 s, controles semi-transparentes; "Continuar de onde parei"; episódios vistos em verde. (f) janela da programação.
- R1 (pronta, verde): menu com ícone + NOME, 200dp (layout_nav.xml e activity_home.xml). Larguras do include do menu também ajustadas para 200dp em activity_browse.xml e activity_settings.xml (R3). Ainda NÃO testada no TX9.
- R2 (pronta, verde): tela inicial sem a caixinha de prévia. Variável focusedLive só para a tecla de menu e o OK segurado. "Agora: ..." no cartão FICA. Ainda NÃO testada no TX9.
- R3 (pronta, verde): nas abas, esquerda na primeira coluna dos itens leva o foco às categorias (dispatchKeyEvent + goToCategories em BrowseActivity.java e M3uBrowseActivity.java). Séries e Ajustes já tinham a ordem certa e não mudaram. Ainda NÃO testada no TX9.

## Respostas do dono (25/10/2026)
- Manter a linha "Agora: ..." sob o cartão do canal na tela inicial. Tirar só a caixinha de prévia.
- Seta para cima e para baixo no player: segue a ORDEM das categorias, como uma lista só, começando no canal atual. Ao acabar a categoria, passa sozinho para a próxima.
- A tecla das três listras: ainda não testada. O dono testa depois das melhorias.

## A fazer, UMA por vez, nesta ordem
R4. Player de canal ao vivo: trocar de canal com as setas
- Cima = canal anterior, baixo = próximo. OK mostra a barra Agora / A seguir. Hoje, com a barra escondida, nada funciona.
- Regra do dono: a lista é TODAS as categorias em ordem, começando no canal atual. Ao fim de uma categoria, passa para a próxima.
- Plano: quem abre o canal manda ao player a lista (ids, nomes e fim de cada categoria, máx. uns 500, no estilo dos extras da fila de séries). Em lista M3U o canal vem com endereço (url), não com id. Pensar no que fazer no último canal da última categoria (sugestão: voltar ao primeiro).
- Atenção: as telas só têm em memória os canais da categoria aberta (páginas de 40, máx. 400). "Todas as categorias" exige buscar as categorias seguintes. Definir com o dono se basta a lista da categoria aberta mais as seguintes, carregadas aos poucos.
- Arquivos: MainActivity.java, activity_main.xml, HomeActivity.java, BrowseActivity.java, M3uBrowseActivity.java, SearchActivity.java.

R5. Tela inicial: as 3 faixas (canais, filmes, séries) INTEIRAS na tela, SEM rolagem. Os cartões podem ficar menores. Arquivos prováveis: activity_home.xml, HomeActivity.java, item_card.xml.

R6. Na faixa, ao chegar no cartão "Ver mais", direita de novo NÃO faz nada (o foco não pula para a faixa de baixo). Para mudar de faixa, só cima e baixo. Arquivo provável: HomeActivity.java.

## Ideias que precisam do OK do dono
- Fazer o Actions ignorar mudanças só em textos (.md): hoje cada texto salvo gera um APK e uma versão nova, e o TX9 oferece "atualizar" sem mudança no app.

## Conferir no TX9 depois das rodadas
- Menu: texto e foco visíveis em todas as telas (inicial, abas, Ajustes), sem nome cortado; Atualizar funciona; o app não fecha sozinho ao abrir cada tela.
- Tela inicial: sem caixinha; "Agora: ..." no cartão; tecla das três listras ou OK segurado abre a programação (anotar qual); OK normal toca.
- Ordem do foco nas abas (servidor e lista M3U): menu, categorias, itens; esquerda na primeira coluna dos itens vai às categorias; da categoria, esquerda vai ao menu.
- Canal ao vivo: cima e baixo trocam de canal, passam de categoria sozinhos; OK mostra a barra.
- Tela inicial inteira sem rolagem; "Ver mais" trava a direita.
- Filme, série, player, busca e Ajustes: roteiro completo será reescrito no fim. Tudo também na lista M3U.

## Próximos passos
1. R4, R5 e R6, uma por vez, com STATUS e DECISIONS ao fim de cada uma.
2. Reescrever o roteiro de teste e o dono testar de novo no TX9.
