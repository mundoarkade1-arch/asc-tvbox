# Status do projeto

Última atualização: 26/10/2026 (menu estreito e degradê VERDES; dono achou 3 problemas novos: M10, M11, M12)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Tudo até a Rodada 5 está pronto e TESTADO pelo dono no TX9. Rodada 6 (menu estreito 110dp com botões de 62dp e fundo em degradê bg_gradient.xml) está VERDE no Actions. O dono começou a usar e achou 3 problemas (M10 a M12), ainda NÃO corrigidos. Nada em andamento no código.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial, "Ver mais", séries, busca, EPG por canal (só servidor).
- (a) a (f) nova aparência. R1 a R6, Rodadas 3 a 5, M6, R4-1, R4-2: testadas no TX9.
- Rodada 5 (testada): capa do filme adaptável; página da série nova (Continuar, Assistir, temporadas, grade de 15 colunas só com número).
- Rodada 6 (verde): menu estreito (layout_nav.xml e cópia em activity_home.xml) e degradê em todas as telas menos o player. O dono ainda não confirmou se gostou do visual (M9).

## METAS (pedidos do dono ainda não feitos). Apague a meta daqui quando ficar pronta e verde.
- M10. "Continue assistindo" repetido. Fato (relatado pelo dono): abrir um episódio, sair e abrir outro da mesma série deixa a série DUAS vezes na faixa, com episódios diferentes. Pedido: UMA entrada por série, sempre o ÚLTIMO episódio. Causa provável (suposição, conferir): ContinueStore.save tira repetido pelo endereço do vídeo (key = SHA-1 do endereço); cada episódio tem endereço diferente. Correção provável: ao salvar um item com kind "episode", remover antes os itens com a MESMA seriesKey. Peça ContinueStore.java inteiro antes. Conferir que "Trocar de lista", limite de 25 e criptografia continuam iguais, e que a faixa e a ContinueActivity mostram bem.
- M11. Pesquisa MUITO lenta (canais, filmes e séries). Fato: o dono acha demorada. Causa provável (suposição): XtreamApi.search baixa a lista inteira do tipo (limite 80 MB) e filtra no aparelho; M3uApi.search lê a lista toda (até 1 minuto). Medir antes: perguntar se é pior no servidor ou na lista M3U e em qual aba. Peça XtreamApi.java, M3uApi.java e SearchActivity.java, um por vez. Ideias a avaliar (não decididas): parar de ler ao juntar 100 resultados (limite atual da busca); usar busca por categoria; mostrar resultados aos poucos; cache curto da lista para buscas seguidas (cuidado com memória, 1 GB).
- M12. Categoria com mais de 400 itens mostra só os 400 primeiros. Fato (relatado): numa categoria com "mil e poucos" itens só carregaram 400; o resto não aparece. Causa provável: limite de 400 itens por categoria nas abas, escolhido por causa da memória (1 GB). Pedido do dono: carregar tudo. Correção provável: carregar MAIS itens por páginas quando a seleção chega perto do fim da grade, sem guardar tudo de uma vez. Peça BrowseActivity.java e M3uBrowseActivity.java (e a função que busca as páginas em XtreamApi/M3uApi). Não confundir com o limite de 400 da fila de troca de canal (ChannelQueue), que é outro assunto.
- M9. Dono olha no TX9 o menu novo e o degradê (6 botões aparecem, foco desce até Atualizar, "Tela inicial" cabe, texto legível no vermelho).
- ADIADO: fundo com imagem e ícone novo (imagens grandes; precisam ser reduzidas no squoosh.app antes, nomes bg_app.jpg e ic_app_logo.png; agente não envia imagens). ADIADO: otimização geral sem perder funções (medir antes; M11 é parte disso).
- Rodada 6: o dono pode lembrar de mais pedidos.

## Perguntas e ideias abertas
- Fim da lista na troca de canal: hoje dá a volta (escolha do agente).
- Ideia que precisa do OK do dono: o Actions ignorar mudanças só em .md.
- Ícone provisório; se o TX9 mostrar o app sem imagem, criar o banner.
- Tela de login ainda com o fundo antigo: perguntar se quer o degradê.
- Limpeza opcional: tirar lv_seasons e lv_episodes do activity_series.xml.

## Pontos a conferir no teste (riscos conhecidos)
- Menu: uns 440dp de altura; em tela baixa o último botão pode cortar.
- Degradê: texto cinza pode ficar menos legível sobre o vermelho do alto.

## Próximos passos
1. Perguntar ao dono por onde começar: M10 (mais simples, um arquivo), M12 ou M11.
2. Para cada um: pedir o arquivo atual, explicar o plano em uma frase, entregar o arquivo completo, esperar o "ok".
