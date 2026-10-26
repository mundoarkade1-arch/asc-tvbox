# Status do projeto

Última atualização: 26/10/2026 (M2 pronta: ícone do app)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Nova aparência (a a f) pronta. Rodadas 1 e 2 (R1 a R6) testadas no TX9 e funcionam. A Rodada 3 (pedidos novos do dono) está pronta e VERDE no Actions, mas AINDA NÃO foi testada no TX9. Faltam as METAS abaixo.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial com 3 faixas, "Ver mais", séries (próxima temporada, onde parou), busca, EPG por canal (só servidor).
- (a) menu e tela inicial. (b) abas com pesquisa. (c) Ajustes. (d) página de filme e série. (e) player (continuar de onde parei, vistos em verde). (f) janela da programação.
- R1 a R6 (testadas, funcionam): menu com nome, tela inicial sem caixinha, foco das abas, troca de canal (ChannelQueue), tela inicial sem rolagem, "Ver mais" trava a direita.
- Rodada 3, verde, NÃO testada no TX9:
  - MainActivity: OK pausa; seta segurada pula cada vez mais rápido; botão "Pular para o próximo episódio" faltando 2 minutos.
  - Abas: 5 colunas, categorias com 190dp, cartão com imagem de 130dp (item_card.xml).
  - "Pesquisar" é a 1ª linha da lista de categorias (BrowseActivity e M3uBrowseActivity); botão do alto removido do layout.
  - M3: nextFocusDown/Up no menu (layout_nav.xml e cópia em activity_home.xml), para o foco ir de Ajustes a Atualizar. Causa ainda a confirmar.
  - M5 filme: capa em activity_movie.xml com 340x510dp (verde).
  - M5b série: capa em activity_series.xml com 160x240dp (verde). Menor que a do filme de propósito.
  - M2 ícone: ic_app_icon.xml (quadrado vermelho com triângulo de play) ligado em android:icon no AndroidManifest.xml (verde). Ainda não visto no TX9.

## METAS (pedidos do dono ainda não feitos). Apague a meta daqui quando ficar pronta e verde.
- M6. Faixa "Continue assistindo" na tela inicial, ACIMA das outras faixas, igual às de Canais, Filmes e Séries: 10 últimos e depois "Ver mais". Entra tudo que o dono começou e não terminou (filme e episódio). Limite de 25 itens: ao passar disso, apagar os mais antigos.
  - O PlaybackProgress guarda só chave embaralhada (SHA-1): não dá para listar nome e capa. Precisa de um arquivo novo de guardar com nome, capa, tipo, id, extensão e posição, e a hora, para apagar o mais antigo.
  - Na lista M3U o endereço do vídeo tem senha. Decidir com o dono como guardar sem expor (guardar criptografado, como o CredentialStore).
  - Com 4 faixas, a tela inicial sem rolagem (R5) pode não caber. Conferir a altura no TX9; pode ser preciso diminuir os espaços.
  - Ordem sugerida: classe nova de guardar, depois o player anota, depois HomeActivity (código), por último activity_home.xml.
- M7. Testar no TX9 tudo da Rodada 3, o ícone, e a tecla das três listras (ou OK segurado) para abrir a programação; anotar qual funciona.

## Perguntas e ideias abertas
- Fim da lista na troca de canal: hoje dá a volta (escolha do agente). O dono pede mudança se não gostar.
- Ideia que precisa do OK do dono: o Actions ignorar mudanças só em .md (hoje cada texto salvo gera versão nova e o TX9 oferece atualizar).
- Ícone: alguns TV box mostram um "banner" largo na lista de apps. Se o TX9 mostrar o app sem imagem, criar o banner (android:banner).

## Pontos a conferir no teste (riscos conhecidos)
- 5 colunas: se a 5ª coluna cortar ou o nome das categorias ficar apertado, ajustar columnWidth e a largura de lv_categories em activity_browse.xml.
- Cartão com imagem de 130dp: se ficar alto demais, voltar para cerca de 110dp.
- Capa do filme com 340x510dp: se cortar embaixo, diminuir.
- Capa da série com 160x240dp: se parecer pequena, aumentar; se os episódios ficarem apertados, diminuir.
- M3: confirmar que descendo de Ajustes o foco vai a Atualizar, em todas as telas.
- R5: se a tela inicial ainda cortar embaixo, diminuir item_card_small.xml e os espaços do activity_home.xml.
- Botão de pular episódio: confirmar que aparece faltando 2 minutos e que OK pula.
- Ícone: conferir se aparece na lista de apps do TX9.

## Próximos passos
1. M6 (Continue assistindo), um arquivo por vez.
2. Dono testa no TX9 (M7) e corrigimos o que aparecer.
