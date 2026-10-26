# Status do projeto

Última atualização: 26/10/2026 (R4-2, tela inicial adaptável, pronta e VERDE; falta ver no TX9. Próximo: R4-1, "Pesquisar" fixo)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Nova aparência (a a f) pronta. Rodadas 1 e 2 (R1 a R6) testadas no TX9 e funcionam. Rodada 3 e M6 (Continue assistindo) VERDES no Actions. O dono começou a testar no TX9 e só relatou 2 problemas. Um deles (tela inicial cortando as séries) acabou de ser consertado no código (R4-2, verde), mas AINDA NÃO foi visto no TX9. O outro (Pesquisar some) é o R4-1. O resto do roteiro de teste NÃO foi confirmado pelo dono: perguntar.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial, "Ver mais", séries (próxima temporada, onde parou), busca, EPG por canal (só servidor).
- (a) menu e tela inicial. (b) abas com pesquisa. (c) Ajustes. (d) página de filme e série. (e) player. (f) janela da programação.
- R1 a R6 (testadas, funcionam).
- Rodada 3 (verde): player (OK pausa, seta segurada, botão de pular); abas com 5 colunas; "Pesquisar" na 1ª linha das categorias; foco do menu; capas maiores; ícone do app.
- M6 (verde): ContinueStore, faixa "Continue assistindo", "Ver mais" com ContinueActivity, limpeza em "Trocar de lista".
- R4-2 (verde, a ver no TX9): tela inicial adaptável. activity_home.xml: cada faixa (3 ou 4) é um bloco de altura 0dp com peso 1, então todas dividem a altura por igual; títulos 18sp, avisos 14sp, espaços menores. item_card_small.xml: altura match_parent, imagem com peso (pega o que sobra), nome em 1 linha. HomeActivity.java NÃO mudou.

## METAS (pedidos do dono ainda não feitos). Apague a meta daqui quando ficar pronta e verde.
- R4-1. "Pesquisar" SEMPRE visível nas abas. Dono: "o botão de pesquisa some quando vou pra categorias, quero que ele sempre apareça". Causa: hoje é a linha 0 da lista de categorias e some ao rolar. Plano: botão fixo FORA da lista, no alto da coluna das categorias (activity_browse.xml). Cuidados: a SearchActivity reaproveita activity_browse.xml e esconde a lista de categorias (esconder o botão também); a regra "posição da lista = índice + 1" deixa de valer (volta a posição = índice) em BrowseActivity e M3uBrowseActivity (goToCategories, cliques, seleção); o foco deve ir menu, Pesquisar, categorias, itens. Ordem: layout com o botão escondido por padrão (nada usa), depois as duas Activities, depois mostrar o botão. Pedir cada arquivo ao dono antes.
- M7. Dono termina de testar no TX9: tela inicial nova (R4-2), restante da Rodada 3, ícone, faixa Continue assistindo, tela "Ver mais", Trocar de lista esvazia a faixa, tecla das três listras (ou OK segurado) para a programação.
- Adaptar ao tamanho da tela (pedido do dono): conferir também abas e páginas de filme e série (capas de altura fixa 340x510dp e 160x240dp, cartão de 130dp nas abas).
- Rodada 4: o dono pode lembrar de mais pedidos.

## Perguntas e ideias abertas
- Fim da lista na troca de canal: hoje dá a volta (escolha do agente).
- Ideia que precisa do OK do dono: o Actions ignorar mudanças só em .md.
- Ícone: se o TX9 mostrar o app sem imagem na lista de apps, criar o banner (android:banner).
- Nome do episódio na faixa é "Série - ep. N". Se o dono quiser "T2 E3", mudar depois.
- Não há botão para apagar um item só do Continue assistindo (só se o dono pedir).

## Pontos a conferir no teste (riscos conhecidos)
- Tela inicial nova: se a imagem do cartão ficar pequena demais (tela baixa, 4 faixas, aviso de atualização aberto), diminuir títulos e espaços ou tirar a linha "Carregando..." quando a faixa carregar.
- 5 colunas: se a 5ª cortar ou o nome das categorias apertar, ajustar columnWidth e lv_categories em activity_browse.xml.
- Cartão com imagem de 130dp nas abas: se alto demais, voltar para cerca de 110dp.
- Capa do filme 340x510dp: se cortar embaixo, diminuir. Capa da série 160x240dp: se pequena, aumentar.
- M3: descendo de Ajustes o foco deve ir a Atualizar em todas as telas.

## Próximos passos
1. R4-1 ("Pesquisar" fixo): pedir activity_browse.xml (layout primeiro, botão escondido), depois BrowseActivity e M3uBrowseActivity, depois mostrar o botão.
2. Dono vê a tela inicial nova no TX9 e termina o teste (M7); corrigimos o que aparecer.
3. Conferir se abas e páginas de filme e série se adaptam à tela.
