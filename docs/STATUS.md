# Status do projeto

Última atualização: 25/10/2026 (R4 em andamento)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
A nova aparência (partes a a f) está pronta e VERDE. O dono fez o PRIMEIRO TESTE no TX9 em 25/10/2026 e aprovou a barra Agora / A seguir. Pediu 4 ajustes (Rodada 1) e 2 (Rodada 2). R1, R2 e R3 prontas e verdes. R4 está pela metade. R5 e R6 não começadas.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial com 3 faixas, "Ver mais", séries (próxima temporada, onde parou), busca, EPG por canal (só servidor).
- (a) menu e tela inicial. (b) abas com pesquisa. (c) Ajustes. (d) página de filme e série. (e) player (pulo de 10 s, barra some em 3 s, continuar de onde parei, vistos em verde). (f) janela da programação.
- R1 (verde): menu com ícone + NOME, 200dp.
- R2 (verde): tela inicial sem a caixinha de prévia; "Agora: ..." no cartão FICA; variável focusedLive.
- R3 (verde): nas abas, esquerda na primeira coluna dos itens vai às categorias; include do menu com 200dp em activity_browse.xml e activity_settings.xml.
- Nada disso foi testado no TX9 ainda.

## Respostas do dono (25/10/2026)
- Manter "Agora: ..." sob o cartão na tela inicial.
- Player: cima e baixo seguem a ORDEM das categorias, como lista única, a partir do canal atual; ao acabar a categoria passa para a próxima.
- Fim da lista: ele não lembrou; ESCOLHA DO AGENTE: dar a volta (último -> primeiro, primeiro -> último). Ele testa e pede mudança se quiser.
- Tecla das três listras: ainda não testada.

## R4 (trocar de canal com as setas): ONDE PARAMOS
FEITO e salvo (confirmar se o Actions ficou verde):
- ChannelQueue.java (arquivo NOVO): fila de canais por categoria, páginas de 40, máx. 400 por categoria, dá a volta no fim, pula categoria adulta com parental ligado.
- MainActivity.java: cima = canal anterior, baixo = próximo (só canal ao vivo, só se a tela mandar EXTRA_QUEUE = true). Depois da troca: barra Agora / A seguir (servidor) ou o nome do canal (lista M3U).
FALTA (UM arquivo por vez, peça o atual antes de cada um):
1. HomeActivity.java: em openItem, para canal ao vivo (kind LIVE), acrescentar putExtra(MainActivity.EXTRA_QUEUE, true) e putExtra(MainActivity.EXTRA_QUEUE_CAT, null). Na tela inicial a categoria é nula = "Todas as categorias" (uma fila só, máx. 400).
2. BrowseActivity.java: em openItem, canal ao vivo: EXTRA_QUEUE true e EXTRA_QUEUE_CAT = currentCat (número da categoria; nulo se "Todas").
3. M3uBrowseActivity.java: igual, EXTRA_QUEUE_CAT = currentGroup (nome do grupo; nulo se "Todas").
4. SearchActivity.java: pedir o arquivo. Provável: EXTRA_QUEUE true e EXTRA_QUEUE_CAT nulo.
Depois: dono testa no TX9 (cima e baixo trocam de canal; passa de categoria; dá a volta; OK mostra a barra).
Cuidados: ChannelQueue procura o canal tocado lendo a categoria desde o começo (até 400); se ele não estiver nesses 400, as setas ficam desligadas (sem erro). Na lista M3U cada troca relê a lista (pode levar alguns segundos).

## Rodada 2: ainda não começados
R5. Tela inicial: as 3 faixas (canais, filmes, séries) INTEIRAS na tela, SEM rolagem; cartões podem ficar menores. Arquivos prováveis: activity_home.xml, HomeActivity.java, item_card.xml (cuidado: item_card.xml é usado também nas abas).
R6. Na faixa, no cartão "Ver mais", direita de novo NÃO faz nada (foco não pula de faixa). Mudar de faixa só com cima e baixo. Arquivo provável: HomeActivity.java.

## Ideias que precisam do OK do dono
- Actions ignorar mudanças só em .md (hoje cada texto salvo gera versão nova e o TX9 oferece atualizar).

## Conferir no TX9 depois das rodadas
- Menu com nome, sem corte, em todas as telas; Atualizar funciona.
- Tela inicial: sem caixinha; "Agora: ..." no cartão; tecla das três listras ou OK segurado abre a programação (anotar qual); sem rolagem; "Ver mais" trava a direita.
- Abas (servidor e M3U): foco menu, categorias, itens.
- Canal ao vivo: cima e baixo trocam, passam de categoria, dão a volta; OK mostra a barra.
- Filme, série, player, busca e Ajustes: roteiro completo será reescrito no fim. Tudo também na lista M3U.

## Próximos passos
1. Confirmar verde do MainActivity; terminar R4 (4 arquivos acima).
2. R5 e R6.
3. Roteiro de teste no TX9.
