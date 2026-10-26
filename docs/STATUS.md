# Status do projeto

Última atualização: 26/10/2026 (tudo VERDE no Actions; pedido novo M13c ainda não feito)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Tudo está VERDE. O MainActivity.java tinha sido salvo cortado (erro "unclosed comment"). Foi refeito inteiro, com M13 e CONTROLLER_ALPHA = 0.25f, e o Actions ficou verde. O dono mudou o pedido do player (M13c, abaixo). Nada em andamento no código.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base, nova aparência, Rodadas 1 a 6, M6, R4-1, R4-2, páginas de filme e série: testadas no TX9.
- M9 (menu estreito e degradê), M10 (uma entrada por série), M12 (teto de 5000 itens): testadas, ok.
- M11 (pesquisa lenta): resolvido no servidor com SearchIndex (índice em arquivo, 2 h). Testado, "ficou bom". Primeira pesquisa de cada tipo ainda demora.
- Teclado de pesquisa QWERTY brasileiro com Ç e acentos (segurar OK): testado, "ficou bom".
- M13 (verde, SEM teste): player mostra "Temporada N - Episódio N" no alto à esquerda por 5 s.
- M13b (verde, SEM teste): todos os botões do player com CONTROLLER_ALPHA = 0.25f. SUBSTITUÍDO pelo M13c.

## METAS (pedidos do dono ainda não feitos)
- M13c (PRIORIDADE, substitui o M13b): no player de filme e episódio, SÓ o botão de play/pause aparece, meio transparente. O resto (voltar, avançar, barra de tempo, números de tempo) NÃO aparece. Palavras do dono: "decide somente o play pause mostrando meio transparente, o resto não apareça".
  - Os botões vêm da biblioteca media3 (PlayerView), não do activity_main.xml. Ideias: no MainActivity (makeControllerTransparent), esconder exo_rew, exo_ffwd, exo_prev, exo_next, exo_progress, exo_position, exo_duration etc. e deixar só exo_play_pause com alpha perto de 0.5; ou usar atributos do PlayerView no activity_main.xml (show_rewind_button, show_fastforward_button, show_previous_button, show_next_button). Conferir a versão do media3 em docs/ARCHITECTURE.md.
  - Manter: setas esquerda/direita pulam 10 s sem botões; OK pausa pelo dispatchKeyEvent; foco não pode ficar preso nos botões; texto do M13 continua.
- Testes pendentes no TX9: M13 (episódio mostra "Temporada N - Episódio N" no alto à esquerda por 5 s; OK mostra de novo) e M13c depois de feito.
- M11 melhorias (só se o dono pedir): montar o índice em segundo plano ao abrir o app; contagem em "Buscando...". Lista M3U não usa o índice (o dono não usa M3U).
- ADIADOS: fundo com imagem e ícone novo (imagens grandes; reduzir no squoosh.app e enviar pelo GitHub; o agente não envia imagens); otimização geral sem perder funções.

## Perguntas e ideias abertas
- Tela de login com degradê?
- Fim da lista na troca de canal dá a volta (escolha do agente).
- Ideia que precisa do OK do dono: o Actions ignorar mudanças só em .md.
- "Trocar de lista" ainda não chama SearchIndex.clear (opcional).
- Limpeza opcional: tirar lv_seasons e lv_episodes do activity_series.xml.

## Pontos a conferir
- SearchIndex: limite de 200000 itens e 60 MB; se passar, a busca cai na antiga.
- O dono prefere o arquivo INTEIRO num bloco só. Antes de salvar arquivo grande: Ctrl+End e conferir que a última linha é "}". Conferir o nome do arquivo no endereço (já se colou XML dentro de um .java).

## Próximos passos
1. Fazer o M13c: pedir o MainActivity.java atual (e o activity_main.xml se precisar), devolver INTEIRO.
2. Pedir ao dono o teste de M13 e M13c no TX9 (uma pergunta, respostas numeradas).
3. Perguntar o que mais o incomoda.
