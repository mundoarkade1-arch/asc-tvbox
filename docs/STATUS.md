# Status do projeto

Última atualização: 26/10/2026 (tudo VERDE no Actions)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Tudo está VERDE. Nada em andamento no código. O M13c (player só com play/pause meio transparente) foi feito em duas rodadas e está verde, mas o dono ainda NÃO disse como ficou no TX9. Foi acrescentado um texto de DIAGNÓSTICO da imagem (temporário) no player.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base, nova aparência, Rodadas 1 a 6, M6, M9, M10, M12, R4-1, R4-2, páginas de filme e série: testadas no TX9.
- M11 (pesquisa lenta): SearchIndex (índice em arquivo, 2 h). Testado, "ficou bom".
- Teclado de pesquisa QWERTY brasileiro com Ç e acentos: testado.
- M13 (texto "Temporada N - Episódio N" no player): verde, SEM teste.
- M13c (verde, SEM teste): botões do player escondidos (voltar, avançar, anterior, próximo e extras); barra de progresso, números de tempo e texto do episódio CONTINUAM aparecendo; play/pause com alpha 0.5. Feito no activity_main.xml (4 atributos show_*_button="false") e no MainActivity (makeControllerTransparent). O texto do episódio fica na tela enquanto a barra de controles está visível.
- Diagnóstico da imagem (verde, TEMPORÁRIO): OK em filme/episódio mostra 6 s, no alto à direita: tamanho do vídeo, tipo, decodificador, tela.

## Achado do diagnóstico (queixa: "imagem pixelada em filmes e séries; em outro app é melhor")
- Vídeo do filme testado: 1280x720, AVC (H264). Decodificador: OMX.amlogic.avc.decoder.awesome (por hardware, é o melhor). Tela do Android: 1280x720.
- Conclusão: o decodificador não é o problema. O arquivo chega em 720p do servidor e a saída do TX9 está em 720p (o aparelho aceita 1080p).
- Dica dada ao dono: nas Configurações do TX9, mudar a saída/resolução para 1080p e repetir o OK no filme (a linha "Tela" deve mostrar 1920x1080). RESPOSTA DO DONO: ainda não veio.
- Ainda não verificado: se o outro app (que o dono acha melhor) pega uma versão melhor do mesmo filme. Perguntar o nome do outro app se a mudança para 1080p não resolver.

## METAS (pedidos do dono ainda não feitos)
- Imagem pixelada nos filmes e séries: esperar o resultado do teste de 1080p. Se não resolver, investigar com o dono (outro app, versão do arquivo, escala do vídeo). Depois de resolver, REMOVER o diagnóstico do MainActivity (createDiagLabel, showDiag, decoderName, AnalyticsListener, hideDiag, DIAG_HIDE_MS e a chamada showDiag() no OK).
- Testes pendentes no TX9: M13 (episódio mostra "Temporada N - Episódio N" no alto à esquerda por 5 s e junto com a barra) e M13c (OK em filme/episódio: pausa e aparecem só barra de progresso, números e play/pause meio transparente, sem botões de voltar/avançar).
- M11 melhorias (só se o dono pedir): montar o índice em segundo plano ao abrir o app; contagem em "Buscando...". Lista M3U não usa o índice (o dono não usa M3U).
- ADIADOS: fundo com imagem e ícone novo (imagens grandes; reduzir no squoosh.app e enviar pelo GitHub; o agente não envia imagens); otimização geral sem perder funções.

## Perguntas e ideias abertas
- Tela de login com degradê?
- Fim da lista na troca de canal dá a volta (escolha do agente).
- Ideia que precisa do OK do dono: o Actions ignorar mudanças só em .md.
- "Trocar de lista" ainda não chama SearchIndex.clear (opcional).
- Limpeza opcional: tirar lv_seasons e lv_episodes do activity_series.xml.

## Pontos a conferir
- Se o activity_main.xml reclamar de show_rewind_button etc., a versão do media3 não aceita; o Actions ficou verde, então aceita.
- O dono prefere o arquivo INTEIRO num bloco só. Antes de salvar arquivo grande: Ctrl+End e conferir que a última linha é "}". Conferir o nome do arquivo no endereço.

## Próximos passos
1. Perguntar o resultado do 1080p (respostas numeradas) e do teste de M13/M13c.
2. Se a imagem melhorou: remover o diagnóstico do MainActivity (pedir o arquivo atual, devolver INTEIRO).
3. Se não melhorou: investigar a diferença para o outro app.
4. Perguntar o que mais incomoda o dono.
