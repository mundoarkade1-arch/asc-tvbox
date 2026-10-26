# Status do projeto

Última atualização: 26/10/2026 (M6 completa e verde: faixa, "Ver mais" e limpeza em Trocar de lista; falta o teste do dono, M7)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Nova aparência (a a f) pronta. Rodadas 1 e 2 (R1 a R6) testadas no TX9 e funcionam. A Rodada 3 e a M6 inteira (Continue assistindo) estão prontas e VERDES no Actions, mas AINDA NÃO foram testadas no TX9. Falta só o teste do dono (M7) e corrigir o que aparecer.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial, "Ver mais", séries (próxima temporada, onde parou), busca, EPG por canal (só servidor).
- (a) menu e tela inicial. (b) abas com pesquisa. (c) Ajustes. (d) página de filme e série. (e) player (continuar de onde parei, vistos em verde). (f) janela da programação.
- R1 a R6 (testadas, funcionam).
- Rodada 3, verde, NÃO testada no TX9: player (OK pausa, seta segurada, botão de pular); abas com 5 colunas, categorias 190dp, cartão 130dp; "Pesquisar" na 1ª linha das categorias; M3 (foco do menu); capa do filme 340x510dp; capa da série 160x240dp; ícone do app.
- M6 (verde, NÃO testada no TX9):
  - Parte 1: ContinueStore.java (25 itens, criptografada); MainActivity anota; MovieActivity, SeriesActivity e M3uSeriesActivity mandam capa e nome "Série - ep. N".
  - Parte 2: faixa "Continue assistindo" na tela inicial (activity_home.xml + HomeActivity.java), até 10 cartões com "Parou em M:SS". Clique: filme abre MovieActivity; episódio abre SeriesActivity ou M3uSeriesActivity; lá o dono clica em "Continuar" e o próximo episódio toca sozinho.
  - Parte 3: "Ver mais" da faixa (só aparece se há mais de 10 itens) abre ContinueActivity (nova: activity_continue.xml, item_continue.xml, registrada no AndroidManifest.xml), lista simples com até 25 itens, capa pequena, nome e "Parou em ...". Mesmo clique da faixa. Sem menu à esquerda.
  - Parte 4: "Trocar de lista" (SettingsActivity.changeLogin) chama ContinueStore.clear antes de abrir o login.

## METAS (pedidos do dono ainda não feitos). Apague a meta daqui quando ficar pronta e verde.
- M7. Dono testa no TX9 tudo da Rodada 3, o ícone, a faixa nova e a tela "Ver mais" (parar filmes e episódios no meio, voltar à tela inicial, ver os cartões, abrir e clicar em "Continuar"; com mais de 10 itens, ver o "Ver mais") e a tecla das três listras (ou OK segurado) da programação; anotar qual funciona.
- Rodada 4: o dono pode lembrar de mais pedidos depois do teste.

## Perguntas e ideias abertas
- Fim da lista na troca de canal: hoje dá a volta (escolha do agente). O dono pede mudança se não gostar.
- Ideia que precisa do OK do dono: o Actions ignorar mudanças só em .md.
- Ícone: se o TX9 mostrar o app sem imagem na lista de apps, criar o banner (android:banner).
- Nome do episódio na faixa é "Série - ep. N" (posição na fila). Se o dono quiser "T2 E3", mudar depois.
- Tela ContinueActivity: itens com mais de 10 só se veem nela; não há botão para apagar um item só. Se o dono quiser, é pedido novo.

## Pontos a conferir no teste (riscos conhecidos)
- 5 colunas: se a 5ª cortar ou o nome das categorias apertar, ajustar columnWidth e lv_categories em activity_browse.xml.
- Cartão com imagem de 130dp: se alto demais, voltar para cerca de 110dp.
- Capa do filme 340x510dp: se cortar embaixo, diminuir. Capa da série 160x240dp: se pequena, aumentar.
- M3: descendo de Ajustes o foco deve ir a Atualizar em todas as telas.
- R5 e M6: com a 4ª faixa aparecendo, a tela inicial pode cortar embaixo. Se cortar, diminuir item_card_small.xml e os espaços do activity_home.xml.
- ContinueActivity: foco abre no primeiro item; Voltar leva à tela inicial; linha com capa de 60x90dp.
- Botão de pular episódio: aparece faltando 2 minutos; OK pula.
- Ícone aparece na lista de apps?

## Próximos passos
1. Dono testa no TX9 (M7) e corrigimos o que aparecer, um arquivo por vez.
2. Rodada 4, se houver novos pedidos.
