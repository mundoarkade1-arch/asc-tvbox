# Status do projeto

Última atualização: 26/10/2026 (M6 em andamento: guardar e anotar prontos; falta a faixa na tela inicial)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Nova aparência (a a f) pronta. Rodadas 1 e 2 (R1 a R6) testadas no TX9 e funcionam. A Rodada 3 está pronta e VERDE no Actions, mas AINDA NÃO foi testada no TX9. Falta terminar a M6 e o dono testar (M7).

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial com 3 faixas, "Ver mais", séries (próxima temporada, onde parou), busca, EPG por canal (só servidor).
- (a) menu e tela inicial. (b) abas com pesquisa. (c) Ajustes. (d) página de filme e série. (e) player (continuar de onde parei, vistos em verde). (f) janela da programação.
- R1 a R6 (testadas, funcionam).
- Rodada 3, verde, NÃO testada no TX9: player (OK pausa, seta segurada, botão de pular); abas com 5 colunas, categorias 190dp, cartão 130dp; "Pesquisar" na 1ª linha das categorias; M3 (foco do menu); capa do filme 340x510dp; capa da série 160x240dp; ícone do app (ic_app_icon.xml no AndroidManifest.xml).
- M6, partes prontas e verdes (ainda sem efeito visível na tela):
  - ContinueStore.java (NOVA): lista de até 25 itens, criptografada (AES-GCM, mesma chave do CredentialStore). Funções: save, list, remove, clear.
  - MainActivity.java: novo EXTRA_ICON; a cada 10 s e ao sair, anota o item na ContinueStore se parou entre 5 s e 95%; ao chegar a 95% ou terminar, tira da lista.
  - MovieActivity, SeriesActivity e M3uSeriesActivity: mandam a capa (EXTRA_ICON) ao player. As duas telas de série mandam o nome "Série - ep. N" (N = posição na fila).

## METAS (pedidos do dono ainda não feitos). Apague a meta daqui quando ficar pronta e verde.
- M6 (continuação). Faixa "Continue assistindo" na tela inicial, ACIMA das outras, igual às de Canais, Filmes e Séries: 10 últimos e depois "Ver mais". Falta:
  1. HomeActivity.java (código): ler ContinueStore.list, montar a faixa com item_card_small.xml, cartão "Ver mais" e o clique. Pedir o arquivo ao dono antes.
  2. Clique do cartão: abre o player direto com EXTRA_START_MS = posMs. Servidor: mandar kind, id, ext, nome, capa. Lista M3U: mandar também EXTRA_URL (o campo url guardado). Sem fila de episódios (o próximo episódio não toca sozinho a partir daqui). Decidir com o dono se isso basta.
  3. "Ver mais" da faixa: decidir com o dono (tela com os 25 itens; talvez reaproveitar activity_browse.xml ou uma tela nova simples).
  4. activity_home.xml (layout, por último): 4ª faixa. Conferir se cabe sem rolagem (R5); pode ser preciso diminuir espaços.
  5. Limpar a ContinueStore em "Trocar de lista" (SettingsActivity), para não sobrar itens da lista antiga.
  6. Esconder da faixa itens adultos se o controle parental estiver ligado (conferir pelo nome).
- M7. Dono testa no TX9 tudo da Rodada 3, o ícone, a faixa nova e a tecla das três listras (ou OK segurado) da programação; anotar qual funciona.

## Perguntas e ideias abertas
- Fim da lista na troca de canal: hoje dá a volta (escolha do agente). O dono pede mudança se não gostar.
- Ideia que precisa do OK do dono: o Actions ignorar mudanças só em .md.
- Ícone: se o TX9 mostrar o app sem imagem na lista de apps, criar o banner (android:banner).
- Nome do episódio na faixa é "Série - ep. N" (posição na fila, não temporada e número). Se o dono quiser "T2 E3", mudar depois.

## Pontos a conferir no teste (riscos conhecidos)
- 5 colunas: se a 5ª cortar ou o nome das categorias apertar, ajustar columnWidth e lv_categories em activity_browse.xml.
- Cartão com imagem de 130dp: se alto demais, voltar para cerca de 110dp.
- Capa do filme 340x510dp: se cortar embaixo, diminuir. Capa da série 160x240dp: se pequena, aumentar.
- M3: descendo de Ajustes o foco deve ir a Atualizar em todas as telas.
- R5: se a tela inicial cortar embaixo, diminuir item_card_small.xml e os espaços do activity_home.xml.
- Botão de pular episódio: aparece faltando 2 minutos; OK pula.
- Ícone aparece na lista de apps?

## Próximos passos
1. M6: HomeActivity.java (código), depois activity_home.xml, depois "Ver mais" da faixa e a limpeza em SettingsActivity.
2. Dono testa no TX9 (M7) e corrigimos o que aparecer.
