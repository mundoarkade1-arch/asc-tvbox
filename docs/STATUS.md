# Status do projeto

Última atualização: 26/10/2026 (Rodada 3 em andamento)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Nova aparência (a a f) pronta. Rodadas 1 e 2 (R1 a R6) testadas no TX9 pelo dono em 26/10/2026: funcionaram. O dono fez pedidos novos (Rodada 3). Parte já está pronta e VERDE no Actions; o resto está nas METAS abaixo.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial com 3 faixas, "Ver mais", séries (próxima temporada, onde parou), busca, EPG por canal (só servidor).
- (a) menu e tela inicial. (b) abas com pesquisa. (c) Ajustes. (d) página de filme e série. (e) player (continuar de onde parei, vistos em verde). (f) janela da programação.
- R1 a R6 (testadas, funcionam): menu com nome, tela inicial sem caixinha, foco das abas, troca de canal com cima/baixo (ChannelQueue), tela inicial sem rolagem (item_card_small.xml), "Ver mais" trava a direita.
- Rodada 3, VERDE no Actions, ainda NÃO testado no TX9:
  - MainActivity.java: OK pausa e continua em filme e episódio; seta direita/esquerda: um toque pula 10 s, segurando vai pulando mais rápido (10 s, 30 s, 60 s); em episódio, faltando 2 minutos, botão "Pular para o próximo episódio" (embaixo, à direita; OK pula e marca como visto).
  - item_card.xml: cartão das abas menor (150dp) e activity_browse.xml: 5 colunas, categorias com 190dp.
  - BrowseActivity.java (servidor): "Pesquisar" é a PRIMEIRA linha da lista de categorias (linha 0 = Pesquisar; linha N = categoria N-1).

## METAS (pedidos do dono ainda não feitos). Apague a meta daqui quando ficar pronta e verde.
- M1. Pesquisar como primeira linha também na lista M3U: M3uBrowseActivity.java (copiar a ideia da BrowseActivity: linha 0 = Pesquisar, linha N = categoria N-1, goToCategories com +1). DEPOIS tirar o botão "Pesquisar" do alto em activity_browse.xml (código primeiro, layout depois). Conferir antes se SearchActivity usa o botão (ela reaproveita o layout).
- M2. Ícone do app (hoje o app está sem ícone). Precisa de uma imagem; explicar ao dono com calma como enviar pelo site do GitHub.
- M3. Menu da esquerda: descendo até "Atualizar", o foco vai para "Séries" em vez de ir direto para "Atualizar". Ver layout_nav.xml, o menu copiado em activity_home.xml e NavBar.java. Causa ainda não achada.
- M4. Cartões das grades (Canais, Filmes, Séries) mais ALTOS: a largura está boa, mas parecem "amassados". Mexer em item_card.xml (imagem hoje com 90dp de altura). O item_card_small.xml é só da tela inicial.
- M5. Capas das páginas de filme e série estão pequenas: aumentar em activity_movie.xml e activity_series.xml (a janela da capa pode estar pequena demais).
- M6. Faixa "Continue assistindo" na tela inicial, ACIMA das outras faixas, igual às de Canais, Filmes e Séries: 10 últimos e depois "Ver mais". Entra tudo que o dono começou e não terminou (filme e episódio). Limite de 25 itens: ao passar disso, apagar os mais antigos.
  - Atenção: o PlaybackProgress guarda só chave embaralhada (SHA-1), então não dá para listar nome e capa a partir dele. Será preciso um arquivo novo de guardar com nome, capa, tipo, id, extensão e posição.
  - Atenção: na lista M3U o endereço do vídeo tem senha. Decidir com o dono como guardar sem expor.
  - Atenção: com 4 faixas, a tela inicial sem rolagem (R5) pode não caber. Conferir a altura no TX9.
- M7. Testar no TX9 tudo da Rodada 3 e a tecla das três listras (ou OK segurado) para abrir a programação; anotar qual funciona.

## Perguntas e ideias abertas
- Fim da lista na troca de canal: hoje dá a volta (escolha do agente). O dono pede mudança se não gostar.
- Ideia que precisa do OK do dono: o Actions ignorar mudanças só em .md (hoje cada texto salvo gera versão nova e o TX9 oferece atualizar).

## Pontos a conferir no teste (riscos conhecidos)
- 5 colunas: a largura real da tela do TX9 não é conhecida. Se a 5ª coluna cortar ou o nome das categorias ficar apertado, ajustar columnWidth e a largura de lv_categories em activity_browse.xml.
- R5: se a tela inicial ainda cortar embaixo, diminuir item_card_small.xml e os espaços do activity_home.xml.
- R4: se o canal não estiver nos primeiros 400 da categoria, as setas cima e baixo ficam desligadas, sem erro.
- Botão de pular episódio: confirmar que aparece faltando 2 minutos e que OK pula.

## Próximos passos
1. Entregar o trecho do DECISIONS.md da Rodada 3 (se ainda não foi salvo).
2. M1 (M3uBrowseActivity.java e depois activity_browse.xml), M3, M4, M5, M2, M6.
3. Dono testa no TX9 (M7) e corrigimos o que aparecer.
