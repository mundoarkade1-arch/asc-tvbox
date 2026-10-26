# Status do projeto

Última atualização: 26/10/2026 (página do filme adaptável e página da série nova com grade de 15 colunas: VERDES no Actions; ainda NÃO vistas no TX9 depois da correção)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Nova aparência (a a f), Rodadas 1 a 4 e M6 (Continue assistindo) prontas. O dono testou no TX9 e tudo funcionava (M7 concluída). Rodada 5: adaptar à tela e refazer a página da série. Código VERDE. Falta o dono ver as duas páginas novas no TX9.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial, "Ver mais", séries, busca, EPG por canal (só servidor).
- (a) a (f) nova aparência. R1 a R6, Rodada 3, M6, R4-1 e R4-2: testadas no TX9, funcionam.
- Abas (activity_browse.xml e item_card.xml): conferidas, já se adaptam à altura. NÃO mudaram, por decisão.
- Página do filme (verde, a ver no TX9): activity_movie.xml com capa que usa a altura que sobra (match_parent, largura até 340dp, mínimo 120dp, fitCenter, adjustViewBounds). MovieActivity.java não mudou.
- Página da série NOVA (verde, a ver no TX9), pedido do dono:
  - item_episode.xml: quadradinho de 48dp, SÓ O NÚMERO do episódio, sem foco e sem clique próprios.
  - activity_series.xml: btn_continue (texto "Continuar") e btn_watch ("Assistir") lado a lado; btn_season (abre janelinha com as temporadas; só aparece se há mais de uma); gv_episodes (grade de 15 colunas, altura = o que sobra). As listas antigas lv_seasons e lv_episodes continuam no layout, ESCONDIDAS pelo código.
  - SeriesActivity.java e M3uSeriesActivity.java: usam os itens novos. Quem controla setas, seleção, rolagem e clique é a GRADE (setOnItemClickListener), como nas outras listas. Cores: visto = vermelho (0xFFFF4D5E), atual (o do Continuar) = verde, resto = branco. "Assistir" toca o episódio atual do começo (ou o 1 se nunca viu). Clicar num episódio começa do zero. Sem número no episódio, mostra a posição na temporada.
- Correção: na primeira versão os quadradinhos tinham foco próprio e a grade não rolava (só 18 de 60 episódios aparecem). Foi resolvido com o número pequeno (15 por linha) e o foco na grade.

## METAS (pedidos do dono ainda não feitos)
- M8. Dono testa no TX9 a página do filme e a página da série (servidor e lista M3U, se puder). Ver "Pontos a conferir".
- Rodada 5: o dono pode lembrar de mais pedidos.

## Perguntas e ideias abertas
- Fim da lista na troca de canal: hoje dá a volta (escolha do agente).
- Ideia que precisa do OK do dono: o Actions ignorar mudanças só em .md.
- Ícone: se o TX9 mostrar o app sem imagem, criar o banner (android:banner).
- Nome do episódio no Continue assistindo é "Série - ep. N". Se o dono quiser "T2 E3", mudar depois.
- Não há botão para apagar um item só do Continue assistindo.
- Se na tela de resultados da busca sobrar espaço vazio à esquerda, pedir SearchActivity.java.
- Limpeza opcional: tirar lv_seasons e lv_episodes do activity_series.xml (hoje só escondidas).

## Pontos a conferir no teste (riscos conhecidos)
- Série: o quadradinho SELECIONADO precisa ficar bem visível ao andar com as setas (depende de como o bg_card mostra a seleção; não foi conferido). Se não aparecer, criar um fundo próprio para o item_episode.xml.
- Série: 60 episódios cabem em 4 linhas sem rolar; séries de 100 ou mais rolam com as setas.
- Série: descer do botão de temporada leva à grade; janelinha de temporadas abre com OK e escolher uma leva o foco à grade.
- Série: o título não mostra mais "- Temporada N" (só o botão mostra).
- Série: sem o nome do episódio na grade, só o número. Episódio visto pela metade conta como atual (verde).
- Filme: a capa cabe inteira sem cortar; o texto à direita não fica estranho se a capa for estreita.
- Capa da série ainda 160x240dp, fixa.

## Próximos passos
1. Dono testa M8 e conta o que viu (principalmente se o quadradinho selecionado aparece).
2. Corrigir o que o teste mostrar, um arquivo por vez.
3. Anotar novos pedidos como Rodada 5.
