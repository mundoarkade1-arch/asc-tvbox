# Status do projeto

Última atualização: 26/10/2026 (M6 completa e verde; dono começou o teste no TX9 e fez 2 pedidos novos, ainda não começados)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Nova aparência (a a f) pronta. Rodadas 1 e 2 (R1 a R6) testadas no TX9 e funcionam. Rodada 3 e M6 (Continue assistindo, com faixa, "Ver mais" e limpeza em Trocar de lista) estão VERDES no Actions. O dono começou a testar no TX9 e só relatou 2 problemas (abaixo). O resto do roteiro de teste NÃO foi confirmado por ele: perguntar.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial, "Ver mais", séries (próxima temporada, onde parou), busca, EPG por canal (só servidor).
- (a) menu e tela inicial. (b) abas com pesquisa. (c) Ajustes. (d) página de filme e série. (e) player. (f) janela da programação.
- R1 a R6 (testadas, funcionam).
- Rodada 3 (verde): player (OK pausa, seta segurada, botão de pular); abas com 5 colunas; "Pesquisar" na 1ª linha das categorias; foco do menu; capas maiores; ícone do app.
- M6 (verde): ContinueStore, faixa "Continue assistindo" na tela inicial (clique abre a página do filme ou da série), "Ver mais" com ContinueActivity (só com mais de 10 itens), limpeza em "Trocar de lista".

## METAS (pedidos do dono ainda não feitos). Apague a meta daqui quando ficar pronta e verde.
- R4-1. "Pesquisar" SEMPRE visível nas abas. Dono: "o botão de pesquisa some quando vou pra categorias, quero que ele sempre apareça". Causa: hoje é a linha 0 da lista de categorias e some ao rolar. Plano: botão fixo FORA da lista, no alto da coluna das categorias (activity_browse.xml). Cuidados: a SearchActivity reaproveita activity_browse.xml e esconde a lista de categorias (esconder o botão também); a regra "posição da lista = índice + 1" deixa de valer (volta a posição = índice) em BrowseActivity e M3uBrowseActivity (goToCategories, cliques, seleção); o foco deve ir menu, Pesquisar, categorias, itens. Ordem: layout com o botão escondido por padrão (nada usa), depois as duas Activities, depois mostrar o botão. Pedir cada arquivo ao dono antes.
- R4-2. Tela inicial ADAPTADA à altura da tela, sem rolagem e sem cortar. Dono: "a tela inicial ficou cortando a parte das séries, quero que tudo se adapte ao tamanho da tela, sem rolagem". Causa: cartões de altura fixa. Plano: cada faixa recebe parte igual da altura (layout_height 0dp com peso) e o cartão se ajusta à faixa (item_card_small.xml com altura da imagem por peso, nome e "Agora/Parou em" ocupando o resto; largura proporcional). Tem que funcionar com 3 faixas (sem Continue assistindo) e com 4. Pedir antes: item_card_small.xml (era o passo em andamento) e activity_home.xml. Ver também se o título e o aviso de atualização cabem. Se preciso, HomeActivity.java só muda se houver código ligado a alturas (não parece).
- M7. Dono termina de testar no TX9: restante da Rodada 3, ícone, faixa Continue assistindo, tela "Ver mais", Trocar de lista esvazia a faixa, tecla das três listras (ou OK segurado) para a programação.
- Rodada 4: o dono pode lembrar de mais pedidos.

## Perguntas e ideias abertas
- Fim da lista na troca de canal: hoje dá a volta (escolha do agente).
- Ideia que precisa do OK do dono: o Actions ignorar mudanças só em .md.
- Ícone: se o TX9 mostrar o app sem imagem na lista de apps, criar o banner (android:banner).
- Nome do episódio na faixa é "Série - ep. N". Se o dono quiser "T2 E3", mudar depois.
- Não há botão para apagar um item só do Continue assistindo (só se o dono pedir).

## Pontos a conferir no teste (riscos conhecidos)
- 5 colunas: se a 5ª cortar ou o nome das categorias apertar, ajustar columnWidth e lv_categories em activity_browse.xml.
- Cartão com imagem de 130dp nas abas: se alto demais, voltar para cerca de 110dp.
- Capa do filme 340x510dp: se cortar embaixo, diminuir. Capa da série 160x240dp: se pequena, aumentar.
- M3: descendo de Ajustes o foco deve ir a Atualizar em todas as telas.
- Telas pequenas: as abas e as páginas de filme e série também podem cortar; o dono quer que "tudo se adapte ao tamanho da tela". Depois da tela inicial, conferir as outras.

## Próximos passos
1. R4-2 (tela inicial adaptável): pedir item_card_small.xml, depois activity_home.xml; um arquivo por vez.
2. R4-1 ("Pesquisar" fixo): layout primeiro, depois BrowseActivity e M3uBrowseActivity.
3. Dono termina o teste no TX9 (M7) e corrigimos o que aparecer.
