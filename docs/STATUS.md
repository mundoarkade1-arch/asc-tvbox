# Status do projeto

Última atualização: 31/10/2026 (identidade visual ASC lite implementada; Actions e TX9 pendentes)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Identidade visual ASC lite implementada no código. O nome ASC lite ficou VERDE no Actions; o Actions do visual e o teste no TX9 ainda faltam.

## Já feito
- Base e rodadas anteriores, M6, M9, M10, M12, R4-1, R4-2 e páginas de filme e série: testadas no TX9.
- M11 (pesquisa e teclado), M13 e M13c: testados no TX9.
- Nome ASC lite em strings.xml: VERDE no Actions.
- Nova paleta, fundos, menu, ícones e banners ASC lite implementados. Foram usados somente os quatro arquivos autorizados do asc-play; as imagens ainda dizem "PLAY".

## Falta
- Conferir o Actions do visual e corrigir qualquer erro de build.
- Atualizar automaticamente e testar a aparência no TX9: ícone/banner, login, menu, foco, cartões e fluidez.
- Opcional, somente depois do Actions verde: avaliar trocar "PLAY" por "LITE" nas imagens.
- Investigar a imagem pixelada de filmes e séries. O diagnóstico temporário continua no MainActivity; removê-lo depois de resolver ou encerrar essa investigação.

## Perguntas abertas
- Qual é o nome do outro app em que a imagem parece melhor?
- O dono quer avaliar a troca opcional de "PLAY" por "LITE"?

## Próximos passos
1. Conferir o Actions do commit visual.
2. No TX9, verificar ícone/banner, login, menu, foco e cartões; confirmar se as telas continuam fluidas.
3. Retomar a investigação da imagem pixelada e remover o diagnóstico temporário ao final.
