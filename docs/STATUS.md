# Status do projeto

Última atualização: 26/10/2026 (M6: faixa "Continue assistindo" pronta na tela inicial; falta "Ver mais" e a limpeza em Trocar de lista)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Nova aparência (a a f) pronta. Rodadas 1 e 2 (R1 a R6) testadas no TX9 e funcionam. A Rodada 3 e a faixa "Continue assistindo" estão prontas e VERDES no Actions, mas AINDA NÃO foram testadas no TX9. Falta o "Ver mais" da faixa, a limpeza em "Trocar de lista" e o teste do dono (M7).

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial, "Ver mais", séries (próxima temporada, onde parou), busca, EPG por canal (só servidor).
- (a) menu e tela inicial. (b) abas com pesquisa. (c) Ajustes. (d) página de filme e série. (e) player (continuar de onde parei, vistos em verde). (f) janela da programação.
- R1 a R6 (testadas, funcionam).
- Rodada 3, verde, NÃO testada no TX9: player (OK pausa, seta segurada, botão de pular); abas com 5 colunas, categorias 190dp, cartão 130dp; "Pesquisar" na 1ª linha das categorias; M3 (foco do menu); capa do filme 340x510dp; capa da série 160x240dp; ícone do app.
- M6 parte 1 (verde): ContinueStore.java (25 itens, criptografada); MainActivity anota; MovieActivity, SeriesActivity e M3uSeriesActivity mandam capa e nome "Série - ep. N".
- M6 parte 2 (verde, NÃO testada no TX9):
  - activity_home.xml: nova faixa group_continue / rail_continue, escondida até ter item, ACIMA de Canais ao vivo.
  - HomeActivity.java: lê a ContinueStore fora da thread principal (loadContinue, também no onResume e no Atualizar), mostra até 10 cartões item_card_small com "Parou em M:SS". Esconde itens adultos com parental ligado.
  - Clique (decisão do dono): filme abre a MovieActivity; episódio abre a SeriesActivity (servidor, número da série tirado da chave guardada) ou a M3uSeriesActivity (lista M3U, nome tirado de "Série - ep. N"). Lá o dono clica em "Continuar". O próximo episódio toca sozinho, pois a fila vem da página da série.
  - Último cartão da faixa: seta direita não faz nada (como no "Ver mais").

## METAS (pedidos do dono ainda não feitos). Apague a meta daqui quando ficar pronta e verde.
- M6 (fim):
  1. "Ver mais" da faixa Continue assistindo: o dono pediu "os 10 últimos, depois ver mais". DECIDIR com o dono (tela simples com os 25 itens; talvez reaproveitar activity_browse.xml ou uma tela nova). Hoje a faixa NÃO tem esse cartão.
  2. Limpar a ContinueStore em "Trocar de lista" (SettingsActivity.java: pedir o arquivo antes; chamar ContinueStore.clear).
- M7. Dono testa no TX9 tudo da Rodada 3, o ícone, a faixa nova (parar um filme no meio, voltar à tela inicial, ver o cartão, abrir e clicar em "Continuar") e a tecla das três listras (ou OK segurado) da programação; anotar qual funciona.

## Perguntas e ideias abertas
- Fim da lista na troca de canal: hoje dá a volta (escolha do agente). O dono pede mudança se não gostar.
- Ideia que precisa do OK do dono: o Actions ignorar mudanças só em .md.
- Ícone: se o TX9 mostrar o app sem imagem na lista de apps, criar o banner (android:banner).
- Nome do episódio na faixa é "Série - ep. N" (posição na fila). Se o dono quiser "T2 E3", mudar depois.

## Pontos a conferir no teste (riscos conhecidos)
- 5 colunas: se a 5ª cortar ou o nome das categorias apertar, ajustar columnWidth e lv_categories em activity_browse.xml.
- Cartão com imagem de 130dp: se alto demais, voltar para cerca de 110dp.
- Capa do filme 340x510dp: se cortar embaixo, diminuir. Capa da série 160x240dp: se pequena, aumentar.
- M3: descendo de Ajustes o foco deve ir a Atualizar em todas as telas.
- R5 e M6: com a 4ª faixa aparecendo, a tela inicial pode cortar embaixo. Se cortar, diminuir item_card_small.xml e os espaços do activity_home.xml.
- Botão de pular episódio: aparece faltando 2 minutos; OK pula.
- Ícone aparece na lista de apps?

## Próximos passos
1. Perguntar ao dono sobre o "Ver mais" da faixa e fazer (um arquivo por vez; código antes do layout).
2. SettingsActivity: limpar a ContinueStore em "Trocar de lista".
3. Dono testa no TX9 (M7) e corrigimos o que aparecer.
