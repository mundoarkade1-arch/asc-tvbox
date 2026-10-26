# Status do projeto

Última atualização: 25/10/2026 (R1 a R6 prontas no código; falta testar no TX9)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
A nova aparência (partes a a f) está pronta e VERDE. O dono fez o PRIMEIRO TESTE no TX9 em 25/10/2026 e aprovou a barra Agora / A seguir. Pediu 4 ajustes (Rodada 1) e 2 (Rodada 2). TODOS (R1 a R6) estão prontos e VERDES no Actions. Falta o dono testar no TX9.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial com 3 faixas, "Ver mais", séries (próxima temporada, onde parou), busca, EPG por canal (só servidor).
- (a) menu e tela inicial. (b) abas com pesquisa. (c) Ajustes. (d) página de filme e série. (e) player (pulo de 10 s, barra some em 3 s, continuar de onde parei, vistos em verde). (f) janela da programação.
- R1 (verde): menu com ícone + NOME, 200dp.
- R2 (verde): tela inicial sem a caixinha de prévia; "Agora: ..." no cartão FICA; variável focusedLive.
- R3 (verde): nas abas, esquerda na primeira coluna dos itens vai às categorias.
- R4 (verde): canal ao vivo troca com cima (anterior) e baixo (próximo). ChannelQueue + MainActivity; as 4 telas (Home, Browse, M3uBrowse, Search) mandam EXTRA_QUEUE. Dá a volta no fim.
- R5 (verde): item_card_small.xml (novo, 130dp) usado só pela HomeActivity; activity_home.xml sem ScrollView vertical, espaços e títulos menores. item_card.xml (abas) NÃO mudou.
- R6 (verde): no cartão "Ver mais" da tela inicial, direita não faz nada (setNextFocusRightId para ele mesmo). Mudar de faixa só com cima e baixo.
- Nada de R1 a R6 foi testado no TX9 ainda.

## Respostas do dono (25/10/2026)
- Manter "Agora: ..." sob o cartão na tela inicial.
- Player: cima e baixo seguem a ORDEM das categorias, como lista única, a partir do canal atual.
- Fim da lista: ESCOLHA DO AGENTE, dar a volta. Ele testa e pede mudança se quiser.
- Tecla das três listras: ainda não testada.

## Pontos a conferir no teste (riscos conhecidos)
- R5: não sei a altura real da tela do TX9. Se ainda sobrar corte embaixo, diminuir mais item_card_small.xml (68dp de imagem, 13sp de nome) e os espaços do activity_home.xml. Com o aviso de atualização aberto, a tela pode apertar.
- R4: se o canal não estiver nos primeiros 400 da categoria (ou da fila "todas"), as setas ficam desligadas, sem erro. Na lista M3U cada troca relê a lista (alguns segundos).
- R6: confirmar que a direita no "Ver mais" realmente não faz nada e que o foco não some.

## Ideias que precisam do OK do dono
- Actions ignorar mudanças só em .md (hoje cada texto salvo gera versão nova e o TX9 oferece atualizar).

## Roteiro de teste no TX9 (a fazer agora)
- Menu com nome, sem corte, em todas as telas; Atualizar funciona.
- Tela inicial: sem caixinha; "Agora: ..." no cartão; três faixas inteiras sem rolagem; "Ver mais" trava a direita; tecla das três listras ou OK segurado abre a programação (anotar qual).
- Abas (servidor e M3U): foco menu, categorias, itens.
- Canal ao vivo, aberto pela tela inicial, por uma aba e pela busca: cima e baixo trocam, passam de categoria, dão a volta; OK mostra a barra.
- Filme, série, player, busca e Ajustes. Tudo também na lista M3U.

## Próximos passos
1. Entregar os documentos da R5 e R6 (DECISIONS, ARQUIVOS, prompt novo).
2. Roteiro de teste no TX9, passo a passo.
3. Corrigir o que o teste mostrar.
