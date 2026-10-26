# Status do projeto

Última atualização: 26/10/2026 (R4-1 e R4-2 prontas e VERDES no Actions; as duas ainda NÃO foram vistas no TX9)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Nova aparência (a a f) pronta. Rodadas 1 e 2 (R1 a R6) testadas no TX9 e funcionam. Rodada 3, M6 (Continue assistindo) e Rodada 4 (R4-1 e R4-2) estão VERDES no Actions. O dono começou a testar no TX9 e só relatou 2 problemas; os dois já foram consertados no código, mas ele ainda NÃO viu o resultado no TX9. O resto do roteiro de teste NÃO foi confirmado: perguntar.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial, "Ver mais", séries (próxima temporada, onde parou), busca, EPG por canal (só servidor).
- (a) menu e tela inicial. (b) abas com pesquisa. (c) Ajustes. (d) página de filme e série. (e) player. (f) janela da programação.
- R1 a R6 (testadas, funcionam).
- Rodada 3 (verde): player (OK pausa, seta segurada, botão de pular); abas com 5 colunas; foco do menu; capas maiores; ícone do app.
- M6 (verde): ContinueStore, faixa "Continue assistindo", "Ver mais" com ContinueActivity, limpeza em "Trocar de lista".
- R4-2 (verde, a ver no TX9): tela inicial adaptável. Cada faixa é um bloco de altura 0dp com peso 1; cartão pequeno com altura match_parent.
- R4-1 (verde, a ver no TX9): "Pesquisar" fixo. Botão btn_search_top (TextView) dentro de group_categories, acima de lv_categories, em activity_browse.xml (escondido por padrão). BrowseActivity e M3uBrowseActivity mostram o botão, ligam o clique e o foco (botão desce à lista, lista sobe ao botão) e voltaram à regra "posição da lista = índice". A SearchActivity NÃO mudou (o botão nasce escondido e ela não o mostra).

## METAS (pedidos do dono ainda não feitos). Apague a meta daqui quando ficar pronta e verde.
- M7. Dono termina de testar no TX9: tela inicial nova (R4-2), "Pesquisar" fixo (R4-1: continua à vista ao descer as categorias; cima na primeira categoria vai ao botão; baixo no botão volta à lista; OK abre o teclado; NÃO aparece na tela de resultados da busca; serve nas abas do servidor e da lista M3U), resto da Rodada 3, ícone, faixa Continue assistindo, tela "Ver mais", Trocar de lista esvazia a faixa, tecla das três listras (ou OK segurado) para a programação.
- Adaptar ao tamanho da tela (pedido do dono): conferir abas e páginas de filme e série (cartão de 150dp com imagem de 130dp nas abas, capa do filme 340x510dp, capa da série 160x240dp são tamanhos fixos). Fazer depois do teste do dono, pedindo cada arquivo antes.
- Rodada 4: o dono pode lembrar de mais pedidos.

## Perguntas e ideias abertas
- Fim da lista na troca de canal: hoje dá a volta (escolha do agente).
- Ideia que precisa do OK do dono: o Actions ignorar mudanças só em .md.
- Ícone: se o TX9 mostrar o app sem imagem na lista de apps, criar o banner (android:banner).
- Nome do episódio na faixa é "Série - ep. N". Se o dono quiser "T2 E3", mudar depois.
- Não há botão para apagar um item só do Continue assistindo (só se o dono pedir).
- Se na tela de resultados da busca sobrar espaço vazio à esquerda (onde ficavam as categorias), pedir SearchActivity.java e ajustar.

## Pontos a conferir no teste (riscos conhecidos)
- Tela inicial nova: se a imagem do cartão ficar pequena demais (4 faixas, aviso de atualização aberto), diminuir títulos e espaços ou tirar a linha "Carregando...".
- 5 colunas: se a 5ª cortar ou o nome das categorias apertar, ajustar columnWidth e lv_categories em activity_browse.xml (o botão Pesquisar também tem 190dp de largura).
- Cartão com imagem de 130dp nas abas: se alto demais, voltar para cerca de 110dp.
- Capa do filme 340x510dp: se cortar embaixo, diminuir. Capa da série 160x240dp: se pequena, aumentar.
- M3: descendo de Ajustes o foco deve ir a Atualizar em todas as telas.

## Próximos passos
1. Perguntar ao dono o que ele já viu no TX9 (tela inicial e Pesquisar fixo) e dar o roteiro do que falta (M7).
2. Corrigir o que o teste mostrar, um arquivo por vez.
3. Conferir se abas e páginas de filme e série se adaptam à tela.
