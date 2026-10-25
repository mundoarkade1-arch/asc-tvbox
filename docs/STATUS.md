# Status do projeto

Última atualização: 25/10/2026 (passagem organizada)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
A nova aparência (partes a a f) está pronta no código e VERDE no Actions. O dono fez o PRIMEIRO TESTE no TX9 em 25/10/2026. O que ele aprovou: a barra Agora (esquerda) e A seguir (direita) no player do canal. Ele pediu 4 ajustes (Rodada 1). NENHUM foi começado.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial com 3 faixas, "Ver mais" por categorias, séries (próxima temporada sozinha, onde parou), busca por nome, EPG por canal (só servidor).
- (a) menu à esquerda e tela inicial. (b) abas com pesquisa própria. (c) aba Ajustes. (d) páginas de filme e série com capa e sinopse. (e) player: setas pulam 10 s, barra some em 3 s, controles semi-transparentes; "Continuar de onde parei" com minuto exato; episódios vistos em verde. (f) janela da programação (tecla das três listras ou OK segurado).

## Rodada 1: a fazer, UMA por vez, nesta ordem
R1. Menu da esquerda com TEXTO
- Pedido: ao lado do ícone, escrever Tela inicial, Canais, Filmes, Séries, Ajustes (e Atualizar).
- Plano: trocar os botões por ícone + texto lado a lado e alargar a coluna. NavBar.java não muda. HomeActivity guarda os botões como View, então não depende do tipo.
- Arquivos: layout_nav.xml e depois activity_home.xml (tem a cópia do menu). Conferir a largura nas telas que incluem o layout_nav (abas e Ajustes).

R2. Tela inicial SEM a caixinha de prévia
- Pedido: ao parar o foco num canal da tela inicial, NÃO mostrar programa de agora, barra de progresso nem "A seguir".
- Plano: HomeActivity.java deixa de chamar schedulePreview, fetchPreview e showPreview. CUIDADO: previewItem também decide se a tecla de menu e o OK segurado abrem a programação (EpgDialog). Trocar por uma variável própria ("canal ao vivo com o foco"). Só depois tirar group_preview do activity_home.xml (código primeiro, layout depois).
- Arquivos: HomeActivity.java, activity_home.xml.

R3. Ordem do foco nas abas: menu, categorias, itens
- Pedido: da esquerda para a direita: menu, categorias, canais/itens. Hoje, indo para a esquerda a partir dos itens, o foco PULA as categorias e vai ao menu.
- Causa provável (CONFIRMAR lendo o código): a lista de categorias não recebe o foco, ou a grade de itens aponta a seta esquerda direto ao menu (nextFocusLeft).
- Valer também para a lista M3U, a tela de Séries (temporadas e episódios) e Ajustes.
- Arquivos: activity_browse.xml, BrowseActivity.java, M3uBrowseActivity.java; conferir activity_series.xml e activity_settings.xml.

R4. Player de canal ao vivo: trocar de canal com as setas
- Pedido: seta para CIMA volta ao canal anterior, seta para BAIXO vai ao próximo. OK continua mostrando a barra Agora / A seguir. Hoje, depois que a barra some, nenhuma tecla funciona (só Voltar).
- Causa provável (CONFIRMAR): em canal ao vivo, o MainActivity não trata as setas com os controles escondidos.
- Plano: quem abre o canal manda ao player a lista dos canais ao vivo daquela tela (ids e nomes, máx. uns 500, no estilo dos extras da fila de séries). Ao trocar, atualizar nome e barra Agora / A seguir. Lista M3U: o canal vem com endereço (url), não com id.
- Arquivos: MainActivity.java, activity_main.xml, HomeActivity.java, BrowseActivity.java, M3uBrowseActivity.java, SearchActivity.java.

## Perguntas abertas para o dono (uma por vez)
1. (R2) Manter a linha "Agora: ..." embaixo do cartão do canal na tela inicial? (Na aba Canais ela fica, como ele pediu.)
2. (R4) Seta para cima e para baixo passa só pelos canais da categoria aberta (suposição atual) ou por todos?
3. A tecla das três listras abriu a programação no TX9? (anotar se foi a tecla ou o OK segurado)
4. Ele lembrou de mais pedidos? (anotar como Rodada 2)

## Ideias que precisam do OK do dono
- Fazer o Actions ignorar mudanças só em textos (.md): hoje cada texto salvo gera um APK e uma versão nova, e o TX9 oferece "atualizar" sem mudança no app.

## Conferir no TX9 depois da Rodada 1
- Menu: texto e foco visíveis; ordem menu, categorias, itens; Atualizar funciona.
- Tela inicial: sem caixinha; tecla das três listras ou OK segurado abre a programação; OK normal toca.
- Canal ao vivo: setas cima e baixo trocam de canal; OK mostra a barra.
- Filme: capa e sinopse (anotar campo que faltar), Continuar com foco, minuto certo, visto em 95%.
- Série: capa e sinopse, Continuar no episódio e minuto certos, verde de visto, próxima temporada sozinha, "Fim da série.".
- Player de filme e episódio: setas pulam 10 s, barra some em 3 s, controles semi-transparentes, Home e voltar continua do mesmo minuto.
- Busca por aba (só o tipo da aba, "acao" acha "Ação", sem resultado, controle parental). Ajustes (parental, trocar lista, dispositivo: anotar espaço livre em TARGET_DEVICES.md, aplicativo, atualização). Tudo também na lista M3U.

## Próximos passos
1. Fazer as perguntas abertas, uma por vez.
2. R1, R2, R3 e R4, uma por vez, com STATUS e DECISIONS ao fim de cada uma.
3. Reescrever o roteiro de teste e o dono testar de novo no TX9.
