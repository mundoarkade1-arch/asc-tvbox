# Status do projeto

Última atualização: 31/10/2026 (cartões da página inicial ampliados; build e teste pendentes)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
O dono testou a identidade ASC lite no TX9 e disse: "tudo funcionando perfeito". O limite 1080p/60 ficou VERDE no Actions. O diagnóstico foi ajustado para medir FPS processados; os cartões da página inicial foram ampliados e perderam a borda normal, preservando o foco. Novo build e teste no TX9 pendentes.

## Já feito
- Base e rodadas anteriores, M6, M9, M10, M12, R4-1, R4-2 e páginas de filme e série: testadas no TX9.
- M11 (pesquisa e teclado), M13 e M13c: testados no TX9.
- Nome ASC lite em strings.xml: VERDE no Actions.
- Nova paleta, fundos, menu, ícones e banners ASC lite: testados no TX9, tudo funcionando. As imagens ainda dizem "PLAY".
- Player: seleção limitada a 1080p/60 para reduzir carga de fluxos adaptativos. Diagnóstico de FPS agora mede quadros processados pelo decoder e mostra o FPS do formato como alternativa.
- Página inicial: cartões ficaram mais largos, capas maiores, nomes maiores em até duas linhas e sem borda normal; sem rolagem vertical. XML válido; compilação local bloqueada pela falta do Android SDK.

## Falta
- Conferir o Actions do diagnóstico de FPS.
- No TX9, testar a página inicial sem rolagem vertical; conferir capas, nomes, contorno de foco e navegação lateral.
- Atualizar automaticamente e testar um canal ao vivo; conferir o FPS medido e se o movimento melhorou.
- Testar também filme e série para confirmar que a nova seleção não piorou a reprodução.
- Opcional, somente depois do Actions verde: avaliar trocar "PLAY" por "LITE" nas imagens.
- Investigar a imagem pixelada de filmes e séries. O diagnóstico temporário continua no MainActivity; removê-lo depois de resolver ou encerrar essa investigação.

## Perguntas abertas
- Se o diagnóstico mostrar 25/30 fps, verificar se o próprio canal é a origem da sensação de arrasto.

## Próximos passos
1. Conferir o Actions do diagnóstico de FPS.
2. No TX9, testar a página inicial sem rolagem vertical; conferir capas, nomes, contorno de foco e navegação lateral.
3. No TX9, testar canal ao vivo, filme e episódio; observar o FPS e a fluidez.
4. Decidir, pelo resultado, se é necessário alterar a tela ou encerrar o diagnóstico temporário.
