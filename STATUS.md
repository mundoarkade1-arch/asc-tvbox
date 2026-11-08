# Status do projeto

Última atualização: 08/11/2026 (menu compacto colorido, foco inicial da Home e teclado do login: aguardam Actions e TX9). Antes: 03/11/2026 (foco arredondado aplicado na Home; Actions e TX9 pendentes)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
O dono testou a identidade ASC lite no TX9 e disse: "tudo funcionando perfeito". A borda de foco dos cards da Home foi alterada para arredondada e para fora do cartão; falta Actions e teste no TX9. Ajustes ganhou confirmação para apagar dados de uso, preservando login e PIN parental. Build, lint e teste no TX9 pendentes; o ambiente local não tem Android SDK.

## Já feito
- Base e rodadas anteriores, M6, M9, M10, M12, R4-1, R4-2 e páginas de filme e série: testadas no TX9.
- M11 (pesquisa e teclado), M13 e M13c: testados no TX9.
- Nome ASC lite em strings.xml: VERDE no Actions.
- Nova paleta, fundos, menu, ícones e banners ASC lite: testados no TX9, tudo funcionando. As imagens ainda dizem "PLAY".
- Player: seleção limitada a 1080p/60 para reduzir carga de fluxos adaptativos. Diagnóstico de FPS agora mede quadros processados pelo decoder e mostra o FPS do formato como alternativa.
- Página inicial: pôsteres maiores para filmes, séries e Continue; canais com logo ampliado; foco ampliado; barra de progresso em Continue; rolagem vertical.
- Home: borda de foco retangular nos cards de canais, filmes, séries e Continue. Ajustes: botão “Apagar dados” limpa progresso e caches; a conta e o PIN parental ficam salvos.
- Validação local do layout: XML e referências válidos. Build/lint/test bloqueados pela ausência do Android SDK; não há testes locais no app.

## Falta
- Conferir o Actions desta alteração.
- No TX9, confirmar que a borda quadrada envolve capa e título, e testar “Apagar dados” com Cancelar e Apagar.
- Conferir o Actions do diagnóstico de FPS.
- No TX9, testar a Home: tamanho e corte das capas, foco D-pad, rolagem horizontal/vertical e "Ver mais".
- Atualizar automaticamente e testar um canal ao vivo; conferir o FPS medido e se o movimento melhorou.
- Testar também filme e série para confirmar que a nova seleção não piorou a reprodução.
- Opcional, somente depois do Actions verde: avaliar trocar "PLAY" por "LITE" nas imagens.
- Investigar a imagem pixelada de filmes e séries. O diagnóstico temporário continua no MainActivity; removê-lo depois de resolver ou encerrar essa investigação.

## Perguntas abertas
- Se o diagnóstico mostrar 25/30 fps, verificar se o próprio canal é a origem da sensação de arrasto.

## Próximos passos
1. Conferir o Actions do diagnóstico de FPS.
2. No TX9, testar a Home: tamanho e corte das capas, foco D-pad, rolagem horizontal/vertical e "Ver mais".
3. No TX9, testar canal ao vivo, filme e episódio; observar o FPS e a fluidez.
4. Decidir, pelo resultado, se é necessário alterar a tela ou encerrar o diagnóstico temporário.
