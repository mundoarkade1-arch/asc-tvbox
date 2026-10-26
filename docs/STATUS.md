# Status do projeto

Última atualização: 26/10/2026 (ACTIONS VERMELHO: MainActivity.java foi salvo cortado)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## PRIMEIRO PASSO DO PRÓXIMO AGENTE (urgente)
O último salvamento do MainActivity.java (M13b, opacidade 0.25) saiu CORTADO. O Actions deu erro "unclosed comment" na linha 426 e "reached end of file while parsing" na 428. O arquivo no GitHub termina no meio de um comentário /** ... */.
Como consertar: (1) peça ao dono as ÚLTIMAS 15 linhas do MainActivity.java (abrir o link de edição, Ctrl+End, copiar, colar, sem Commit); (2) descubra qual método ficou cortado e mande SÓ o pedaço que falta, para colar no fim do arquivo; (3) se não der, peça o arquivo atual inteiro e devolva-o em DUAS ou TRES partes (colagem grande é cortada). Depois, confira se o Actions ficou verde.
Conteúdo correto do arquivo: o MainActivity com M13 (texto "Temporada N - Episódio N", EXTRA_LIST_LABELS) e CONTROLLER_ALPHA = 0.25f (era 0.65f). Tudo o mais igual.

## Fase atual
Tudo até a Rodada 6 está pronto e TESTADO pelo dono no TX9 (ele disse "tudo deu certo": M9, M10, M12 ok). Nesta sessão:
- M11 (pesquisa lenta) RESOLVIDO no servidor: SearchIndex.java (índice em arquivo, 2 h de validade). Segunda pesquisa fica rápida ("ficou bom"). A PRIMEIRA pesquisa de cada tipo demora até um pouco mais que antes (baixa a lista e grava o arquivo).
- Teclado de pesquisa refeito (QWERTY brasileiro, Ç, barra de espaço grande, segurar OK numa vogal ou no N abre os acentos). Testado, "ficou bom".
- M13 (verde, SEM teste): player mostra "Temporada N - Episódio N" no alto à esquerda por 5 s. SeriesActivity e M3uSeriesActivity mandam EXTRA_LIST_LABELS.
- M13b (VERMELHO, cortado): botões do player quase invisíveis (opacidade 0.25).

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base, nova aparência, R1 a R6, Rodadas 3 a 6, M6, R4-1, R4-2, página do filme e da série: testadas.
- M9, M10, M12: testadas pelo dono, ok.
- M11 servidor: XtreamApi.streamAll + SearchIndex + SearchActivity (usa o índice; se falhar e nada foi achado, cai na busca antiga).
- Teclado QWERTY com acentos: SearchKeyboard.java.
- M13: MainActivity (rótulo), SeriesActivity e M3uSeriesActivity (mandam os textos).

## METAS (pedidos do dono ainda não feitos)
- M13b: botões do player quase invisíveis. Pedido do dono: "fica um botão de pause, de passar e voltar 5s e 15s na tela em branco, quero que ele quase não apareça". Plano: CONTROLLER_ALPHA 0.25 (no MainActivity, só destravar o arquivo cortado). Se ainda aparecer demais, baixar para 0.10 ou criar um layout de controle sem fundo (activity_main.xml não tem os botões; vêm da biblioteca media3).
- Testes pendentes no TX9: M13 (abrir um episódio: texto no alto à esquerda por 5 s; OK mostra de novo) e M13b (botões quase invisíveis).
- M11 melhorias possíveis (só se o dono pedir): a primeira pesquisa ainda demora. Ideias: montar o índice em segundo plano ao abrir o app; mostrar contagem em "Buscando...". Lista M3U ainda usa a busca antiga (o dono não usa M3U).
- ADIADOS: fundo com imagem e ícone novo (imagens grandes; reduzir no squoosh.app antes); otimização geral sem perder funções.

## Perguntas e ideias abertas
- Tela de login com degradê? Fim da lista na troca de canal dá a volta (escolha do agente).
- Ideia que precisa do OK do dono: o Actions ignorar mudanças só em .md.
- "Trocar de lista" ainda NÃO chama SearchIndex.clear(ctx) (o índice é por servidor e usuário, então não quebra, só sobra arquivo no cache). Opcional: chamar no SettingsActivity.changeLogin.
- Limpeza opcional: tirar lv_seasons e lv_episodes do activity_series.xml.

## Pontos a conferir
- SearchIndex: limite de 200000 itens e 60 MB por arquivo; se passar, a busca cai na antiga.
- Índice fica em getCacheDir()/search_index (o Android pode apagar; ele se refaz sozinho).

## Próximos passos
1. Consertar o MainActivity.java (ver acima) e esperar o Actions verde.
2. Pedir ao dono os testes de M13 e M13b no TX9.
3. Perguntar o que mais o incomoda.
