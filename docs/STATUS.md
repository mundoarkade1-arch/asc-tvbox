# Status do projeto

Última atualização: 26/10/2026 (menu estreito e fundo em degradê: VERDES no Actions; AINDA NÃO vistos no TX9)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Tudo até a Rodada 5 está pronto e TESTADO pelo dono no TX9 (tela inicial adaptável, Pesquisar fixo, página do filme, página da série nova, Continue assistindo etc.). Agora é a Rodada 6 (visual): menu da esquerda estreito e fundo em degradê vermelho para preto. Código VERDE. Falta o dono ver no TX9.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial, "Ver mais", séries, busca, EPG por canal (só servidor).
- (a) a (f) nova aparência. R1 a R6, Rodadas 3 a 5, M6, R4-1, R4-2: testadas no TX9, funcionam.
- Rodada 5 (testada): abas conferidas e NÃO mudadas; capa do filme adaptável; página da série nova (Continuar, Assistir, botão de temporada, grade de 15 colunas só com o número; cores vermelho/verde/branco).
- Rodada 6 (verde, a ver no TX9), pedido do dono:
  - Menu da esquerda: 110dp de largura, ícone em cima e nome embaixo (drawableTop, 14sp), 6 botões de 62dp fixos agrupados no alto, coluna transparente. Em layout_nav.xml E na cópia dentro de activity_home.xml. As larguras do include em activity_browse.xml e activity_settings.xml também são 110dp.
  - Fundo: arquivo novo res/drawable/bg_gradient.xml (degradê vertical: #8A1220 no alto, #3A0A12 no meio, #0C0A0B embaixo). Usado como fundo em activity_home, activity_browse (e portanto a busca), activity_settings, activity_movie, activity_series e activity_continue. O player (activity_main.xml) continua preto, de propósito.

## METAS (pedidos do dono ainda não feitos)
- M9. Dono olha no TX9: menu estreito com botões menores (todos os 6 aparecem, foco desce até Atualizar, "Tela inicial" cabe sem "..."); degradê em todas as telas menos o player; texto legível. Se o vermelho do alto ficar forte ou fraco demais, ajustar só o bg_gradient.xml.
- Fundo com imagem e ícone novo do app: ADIADOS pelo dono. O dono tem 2 imagens (fundo largo com logo ASC play em chamas e ondas vermelhas; logo quadrado). Para o TX9 (1 GB) elas precisam ser reduzidas ANTES (fundo JPG ~1280 de largura, qualidade ~70; ícone PNG 512x512), por exemplo no squoosh.app, com nomes bg_app.jpg e ic_app_logo.png, enviadas pelo GitHub (Add file, Upload files). O agente NÃO consegue enviar imagens. O logo no centro do fundo atrapalharia a leitura: precisaria de camada escura por cima.
- Otimização (velocidade, memória, carregamento) SEM perder funções: pedida pelo dono, adiada. Exige ver os arquivos pesados (ImageLoader, XtreamApi, M3uApi, HomeActivity, MainActivity), um por vez, com teste no TX9 a cada mudança. Antes, perguntar onde ele sente lentidão.
- Rodada 6: o dono pode lembrar de mais pedidos.

## Perguntas e ideias abertas
- Fim da lista na troca de canal: hoje dá a volta (escolha do agente).
- Ideia que precisa do OK do dono: o Actions ignorar mudanças só em .md.
- Ícone: simples e provisório (quadrado vermelho com play). Se o TX9 mostrar o app sem imagem, criar o banner.
- Nome do episódio no Continue assistindo é "Série - ep. N".
- Não há botão para apagar um item só do Continue assistindo.
- Se na tela de resultados da busca sobrar espaço vazio à esquerda, pedir SearchActivity.java.
- Limpeza opcional: tirar lv_seasons e lv_episodes do activity_series.xml (hoje escondidas pelo código).
- A tela de login (activity_login.xml) NÃO foi mudada: continua com o fundo antigo. Perguntar se o dono quer o degradê nela.

## Pontos a conferir no teste (riscos conhecidos)
- Menu: 6 botões x 70dp + 20dp = uns 440dp de altura. Em tela baixa pode cortar o último (Atualizar).
- Menu transparente: telas novas que incluírem o menu precisam de fundo escuro ou degradê.
- Degradê: o texto cinza (#A39A9D) sobre o vermelho do alto pode ficar menos legível.

## Próximos passos
1. Perguntar ao dono o que ele viu (M9) e corrigir, um arquivo por vez.
2. Depois, atender o que ele escolher: imagens (fundo e ícone), otimização ou novos pedidos.
