# Status do projeto

Última atualização: 25/10/2026 (R4 pronta no código; faltam R5 e R6)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
A nova aparência (partes a a f) está pronta e VERDE. O dono fez o PRIMEIRO TESTE no TX9 em 25/10/2026 e aprovou a barra Agora / A seguir. Pediu 4 ajustes (Rodada 1) e 2 (Rodada 2). R1, R2, R3 e R4 estão prontas e verdes no Actions. R5 e R6 não começadas.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial com 3 faixas, "Ver mais", séries (próxima temporada, onde parou), busca, EPG por canal (só servidor).
- (a) menu e tela inicial. (b) abas com pesquisa. (c) Ajustes. (d) página de filme e série. (e) player (pulo de 10 s, barra some em 3 s, continuar de onde parei, vistos em verde). (f) janela da programação.
- R1 (verde): menu com ícone + NOME, 200dp.
- R2 (verde): tela inicial sem a caixinha de prévia; "Agora: ..." no cartão FICA; variável focusedLive.
- R3 (verde): nas abas, esquerda na primeira coluna dos itens vai às categorias.
- R4 (verde): trocar de canal ao vivo com cima (anterior) e baixo (próximo) no player. ChannelQueue.java e MainActivity.java prontos. As quatro telas já mandam EXTRA_QUEUE = true:
  - HomeActivity e SearchActivity: sem categoria (= todas).
  - BrowseActivity: categoria = currentCat (nada se for "Todas").
  - M3uBrowseActivity: categoria = currentGroup (nada se for "Todas").
- Nada da nova aparência (R1 a R4) foi testado no TX9 ainda.

## Respostas do dono (25/10/2026)
- Manter "Agora: ..." sob o cartão na tela inicial.
- Player: cima e baixo seguem a ORDEM das categorias, como lista única, a partir do canal atual; ao acabar a categoria passa para a próxima.
- Fim da lista: ele não lembrou; ESCOLHA DO AGENTE: dar a volta (último -> primeiro, primeiro -> último). Ele testa e pede mudança se quiser.
- Tecla das três listras: ainda não testada.

## Cuidados da R4 (para o teste)
- ChannelQueue procura o canal tocado lendo a categoria desde o começo (até 400). Se ele não estiver nesses 400, as setas ficam desligadas (sem erro). Na busca e na tela inicial (fila "todas"), isso pode acontecer com canal muito no fim da lista.
- Na lista M3U cada troca relê a lista (pode levar alguns segundos).

## Rodada 2: AINDA NÃO COMEÇADOS (próximos)
R5. Tela inicial: as 3 faixas (canais, filmes, séries) INTEIRAS na tela, SEM rolagem; cartões podem ficar menores. Arquivos prováveis: activity_home.xml, HomeActivity.java, item_card.xml (cuidado: item_card.xml é usado também nas abas, então não encolher lá sem querer).
R6. Na faixa, no cartão "Ver mais", direita de novo NÃO faz nada (foco não pula de faixa). Mudar de faixa só com cima e baixo. Arquivo provável: HomeActivity.java.

## Ideias que precisam do OK do dono
- Actions ignorar mudanças só em .md (hoje cada texto salvo gera versão nova e o TX9 oferece atualizar).

## Conferir no TX9 depois das rodadas
- Menu com nome, sem corte, em todas as telas; Atualizar funciona.
- Tela inicial: sem caixinha; "Agora: ..." no cartão; tecla das três listras ou OK segurado abre a programação (anotar qual); sem rolagem; "Ver mais" trava a direita.
- Abas (servidor e M3U): foco menu, categorias, itens.
- Canal ao vivo, aberto pela tela inicial, por uma aba e pela busca: cima e baixo trocam, passam de categoria, dão a volta; OK mostra a barra.
- Filme, série, player, busca e Ajustes: roteiro completo será reescrito no fim. Tudo também na lista M3U.

## Próximos passos
1. Entregar os documentos da R4 (DECISIONS, ARQUIVOS, prompt novo).
2. R5 e R6, uma por vez.
3. Roteiro de teste no TX9.
