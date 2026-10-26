# Status do projeto

Última atualização: 26/10/2026 (tudo VERDE no Actions; MainActivity.java consertado)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Tudo está VERDE. O MainActivity.java tinha sido salvo cortado (erro "unclosed comment"). Foi refeito inteiro, com M13 e CONTROLLER_ALPHA = 0.25f, e o Actions ficou verde. Nada em andamento no código.

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base, nova aparência, Rodadas 1 a 6, M6, R4-1, R4-2, páginas de filme e série: testadas no TX9.
- M9 (menu estreito e degradê), M10 (uma entrada por série), M12 (teto de 5000 itens): testadas, ok.
- M11 (pesquisa lenta): resolvido no servidor com SearchIndex (índice em arquivo, 2 h). Testado, "ficou bom". Primeira pesquisa de cada tipo ainda demora.
- Teclado de pesquisa QWERTY brasileiro com Ç e acentos (segurar OK): testado, "ficou bom".
- M13 (verde, SEM teste): player mostra "Temporada N - Episódio N" no alto à esquerda por 5 s.
- M13b (verde, SEM teste): botões do player quase invisíveis (CONTROLLER_ALPHA = 0.25f).

## METAS (pedidos do dono ainda não feitos)
- Testes pendentes no TX9:
  - M13: abrir um episódio; deve aparecer "Temporada N - Episódio N" no alto à esquerda por 5 s; OK mostra de novo.
  - M13b: apertar OK em filme ou episódio; os botões devem aparecer bem fracos. Se ainda aparecem demais, baixar CONTROLLER_ALPHA para 0.10 ou criar controle sem fundo.
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
- Colagem de arquivo grande pode ser cortada: antes de salvar, Ctrl+End e conferir que a última linha é "}". Conferir também o nome do arquivo no endereço (já se colou XML dentro de um .java).

## Próximos passos
1. Pedir ao dono os testes de M13 e M13b no TX9 (uma pergunta, respostas numeradas).
2. Ajustar a opacidade se ele pedir.
3. Perguntar o que mais o incomoda.
