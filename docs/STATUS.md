# Status do projeto

Última atualização: 24/10/2026

## Fase atual
Etapa "EPG com prévia nos canais" em andamento. Feita a linha "Agora: ..." nos cartões de canais (página inicial e "Ver mais"). Faltam a caixinha de prévia ao focar num canal e a barra Agora/A seguir no player.

## Feito (testado no TX9)
- Repositório criado e documentação preenchida (README, AGENTS, CLAUDE, llms.txt, docs/*)
- Aparelho de teste identificado e documentado (TX9, S905W, Android 7.1, 1 GB RAM, tela 1080p)
- Decisão tomada: app Android nativo em Java com ExoPlayer, sem WebView (ver docs/DECISIONS.md)
- Esqueleto do app nativo e workflow do GitHub Actions que gera o APK
- Login por servidor (DNS, usuário e senha), com mensagens de erro corretas
- Atualização automática do app (aviso na página inicial, instalação pelo próprio app)
- Abrir direto na página inicial quando há login salvo
- Página inicial com 3 faixas (Canais ao vivo, Filmes, Séries) de 10 itens cada
- Tocar canal e filme
- "Ver mais": cartão no fim de cada faixa que abre a navegação por categorias (BrowseActivity), com borda vermelha de seleção
- Linha "Agora: ..." nos cartões de canais ao vivo da página inicial (testada no TX9)

## Feito (compila, Actions verde, mas ainda NÃO testado no TX9)
- Linha "Agora: ..." nos cartões de canais dentro do "Ver mais" (BrowseActivity). Teste a fazer: Canais ao vivo > Ver mais, esperar uns 5 segundos, rolar para baixo e trocar de categoria, e conferir se a linha aparece.

## Arquivos do EPG
- XtreamApi.java: função epg(...) que pede ao servidor a programação de UM canal (get_short_epg, até 4 programas) e devolve o programa de agora e o "a seguir"
- EpgCache.java: guarda na memória até 80 canais por 1 minuto cada; no máximo 2 pedidos ao servidor ao mesmo tempo; avisa sempre na thread principal
- item_card.xml: já tinha a linha tv_now (escondida); não foi alterado
- HomeActivity.java e BrowseActivity.java: ligam a linha tv_now ao EpgCache

## Próximos passos (nesta ordem)
1. Testar no TX9 o "Ver mais" com a linha "Agora" (ver acima)
2. Caixinha de prévia ao parar o foco num canal: nome do programa, barra de progresso, horário de início e fim e "A seguir" (usa os campos start, end, nextTitle e nextStart da classe XtreamApi.Epg)
3. Barra Agora / A seguir no player, enquanto o canal toca (MainActivity.java)
4. Controle parental
5. Episódios de séries (hoje o toque numa série só mostra um aviso)
6. Página inicial para login por lista M3U (hoje mostra um aviso de que chega numa etapa própria)

## Problemas em aberto
- Armazenamento livre do TX9 e acesso à Play Store: ainda não sabemos
- Lista M3U ainda não tem página inicial (ver passo 6 acima)
- Canais sem guia no servidor mostram "Programação indisponível" (comportamento esperado, não é erro)
