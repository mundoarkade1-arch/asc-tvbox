# Status do projeto

Última atualização: 25/10/2026 (R1 pronta)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
A nova aparência (partes a a f) está pronta e VERDE no Actions. O dono fez o PRIMEIRO TESTE no TX9 em 25/10/2026 e aprovou a barra Agora / A seguir do canal. Ele pediu 4 ajustes (Rodada 1) e depois mais 2 (Rodada 2). A R1 está pronta e verde. As outras não foram começadas.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial com 3 faixas, "Ver mais" por categorias, séries (próxima temporada sozinha, onde parou), busca por nome, EPG por canal (só servidor).
- (a) menu à esquerda e tela inicial. (b) abas com pesquisa própria. (c) aba Ajustes. (d) páginas de filme e série com capa e sinopse. (e) player: setas pulam 10 s, barra some em 3 s, controles semi-transparentes; "Continuar de onde parei"; episódios vistos em verde. (f) janela da programação.
- R1 (pronta, verde): menu com ícone + NOME. layout_nav.xml e activity_home.xml agora usam TextView com drawableLeft (200dp de largura). Ainda NÃO testada no TX9.

## Respostas do dono (25/10/2026)
- Manter a linha "Agora: ..." sob o cartão do canal na tela inicial. Tirar só a caixinha de prévia.
- Seta para cima e para baixo no player: segue a ORDEM das categorias, como uma lista só, começando no canal atual. Ao acabar a categoria, passa sozinho para a próxima.
- A tecla das três listras: ainda não testada. O dono testa depois das melhorias.

## Rodada 1: a fazer, UMA por vez, nesta ordem
R2. Tela inicial SEM a caixinha de prévia
- Pedido: ao parar o foco num canal da tela inicial, NÃO mostrar programa, barra de progresso nem "A seguir". A linha "Agora: ..." no cartão FICA.
- Plano: HomeActivity.java deixa de chamar schedulePreview, fetchPreview e showPreview. CUIDADO: previewItem também decide se a tecla de menu e o OK segurado abrem a programação (EpgDialog). Trocar por uma variável própria ("canal ao vivo com o foco"). Só depois tirar group_preview do activity_home.xml (código primeiro, layout depois).
- Arquivos: HomeActivity.java, activity_home.xml.

R3. Ordem do foco nas abas: menu, categorias, itens
- Hoje, indo para a esquerda a partir dos itens, o foco PULA as categorias e vai ao menu.
- Causa provável (CONFIRMAR lendo o código): categorias sem foco, ou nextFocusLeft da grade apontando ao menu.
- Valer também para a lista M3U, Séries (temporadas e episódios) e Ajustes.
- Arquivos: activity_browse.xml, BrowseActivity.java, M3uBrowseActivity.java; conferir activity_series.xml e activity_settings.xml.

R4. Player de canal ao vivo: trocar de canal com as setas
- Cima = canal anterior, baixo = próximo. OK mostra a barra Agora / A seguir. Hoje, com a barra escondida, nada funciona.
- Regra nova do dono: a lista é TODAS as categorias em ordem, começando no canal atual. Ao fim de uma categoria, passa para a próxima.
- Plano: quem abre o canal manda ao player a lista (ids, nomes e fim de cada categoria, máx. uns 500, no estilo dos extras da fila de séries). Em lista M3U o canal vem com endereço (url), não com id. Pensar no que fazer ao chegar no último canal da última categoria (sugestão: voltar ao primeiro).
- Arquivos: MainActivity.java, activity_main.xml, HomeActivity.java, BrowseActivity.java, M3uBrowseActivity.java, SearchActivity.java.

## Rodada 2: pedidos novos (ainda não começados)
R5. Tela inicial: as 3 faixas (canais, filmes, séries) INTEIRAS na tela, SEM rolagem. Os cartões podem ficar menores. Arquivos prováveis: activity_home.xml, HomeActivity.java, item_card.xml.
R6. Na faixa, ao chegar no cartão "Ver mais", direita de novo NÃO faz nada (o foco não pula para a faixa de baixo). Para mudar de faixa, só cima e baixo. Arquivo provável: HomeActivity.java.

## Ideias que precisam do OK do dono
- Fazer o Actions ignorar mudanças só em textos (.md): hoje cada texto salvo gera um APK e uma versão nova, e o TX9 oferece "atualizar" sem mudança no app.

## Conferir no TX9 depois da Rodada 1
- Menu: texto e foco visíveis em todas as telas (inicial, abas, Ajustes); Atualizar funciona; o app não fecha sozinho ao abrir cada tela.
- Tela inicial: sem caixinha; tecla das três listras ou OK segurado abre a programação (anotar qual); OK normal toca.
- Ordem do foco: menu, categorias, itens.
- Canal ao vivo: cima e baixo trocam de canal, passam de categoria sozinhos; OK mostra a barra.
- Filme, série, player, busca e Ajustes: roteiro completo será reescrito no fim da Rodada 1. Tudo também na lista M3U.

## Próximos passos
1. R2, R3 e R4, uma por vez, com STATUS e DECISIONS ao fim de cada uma.
2. R5 e R6 (Rodada 2).
3. Reescrever o roteiro de teste e o dono testar de novo no TX9.
