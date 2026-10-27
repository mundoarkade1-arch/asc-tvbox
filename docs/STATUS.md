# Status do projeto

Última atualização: 26/10/2026 (tudo VERDE no Actions)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Tudo está VERDE. Nada em andamento no código. O dono testou M13 e M13c no TX9: funcionou tudo. Último passo feito: ícone novo do app (só as letras ASC com degradê, fundo branco). Falta o dono VER o ícone no TX9 depois da atualização automática.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base, nova aparência, Rodadas 1 a 6, M6, M9, M10, M12, R4-1, R4-2, páginas de filme e série: testadas no TX9.
- M11 (pesquisa com índice em arquivo, SearchIndex, 2 h): testado, "ficou bom".
- Teclado de pesquisa QWERTY brasileiro com Ç e acentos: testado.
- M13 (texto "Temporada N - Episódio N" no player): testado, funcionou.
- M13c (player só com play/pause meio transparente; barra, tempos e texto do episódio aparecem): testado, funcionou.
- Ícone novo (verde, a ver no TX9): res/drawable/ic_app_icon.xml agora é um quadrado branco de cantos redondos com as letras ASC desenhadas em vetor e preenchidas com degradê vertical (#E0192F em cima, #3A0A12 embaixo). Pedido do dono: "a letra com degradê e o fundo branco". Antes era quadrado vermelho com triângulo de play.
- Diagnóstico da imagem (verde, TEMPORÁRIO, ainda no MainActivity): OK em filme/episódio mostra 6 s, no alto à direita, tamanho do vídeo, decodificador e tela.

## Achado sobre a imagem (queixa: "pixelada em filmes e séries; em outro app é melhor")
- Vídeo do filme testado: 1280x720, AVC. Decodificador: OMX.amlogic.avc.decoder.awesome (hardware, o melhor). Tela do Android: 1280x720 na hora do teste.
- O dono olhou as Configurações do TX9: a saída JÁ ESTAVA em 1080p 60 Hz e ele não mudou nada. Então mudar para 1080p não resolve.
- Hipóteses: o arquivo do servidor é só 720p e é esticado para a tela; ou o outro app pega uma versão melhor do filme ou estica de outro jeito.
- Ainda não verificado: o nome do outro app e se ele mostra a mesma versão do filme.

## METAS (pedidos do dono ainda não feitos)
- Imagem pixelada nos filmes e séries: perguntar o nome do outro app (ou se ele ainda está no TX9). Investigar com o dono. Depois de resolver (ou se o dono desistir), REMOVER o diagnóstico do MainActivity (createDiagLabel, showDiag, diagLabel, decoderName, hideDiag, DIAG_HIDE_MS, AnalyticsListener, imports Format/AnalyticsListener e a chamada showDiag() no OK).
- Ver o ícone novo no TX9. Se o TX9 mostrar o app sem imagem, criar um banner (android:banner).
- M11 melhorias (só se o dono pedir): índice em segundo plano ao abrir o app; contagem em "Buscando...". Lista M3U não usa o índice (o dono não usa M3U).
- ADIADOS: fundo com imagem (imagens grandes; reduzir no squoosh.app e enviar pelo GitHub; o agente não envia imagens); otimização geral sem perder funções.

## Perguntas e ideias abertas
- Tela de login com degradê?
- Fim da lista na troca de canal dá a volta (escolha do agente).
- Ideia que precisa do OK do dono: o Actions ignorar mudanças só em .md.
- "Trocar de lista" ainda não chama SearchIndex.clear (opcional).
- Limpeza opcional: tirar lv_seasons e lv_episodes do activity_series.xml.

## Pontos a conferir
- O dono prefere o arquivo INTEIRO num bloco só. Antes de salvar arquivo grande: Ctrl+End e conferir que a última linha é "}". Conferir o nome do arquivo no endereço.
- Se o Actions reclamar do degradê do ícone (aapt:attr), trocar por uma cor só.

## Próximos passos
1. Perguntar o nome do outro app (imagem) ou se o dono prefere deixar como está e tirar o diagnóstico.
2. Perguntar se o ícone novo apareceu no TX9 e como ficou.
3. Perguntar o que mais incomoda o dono.
