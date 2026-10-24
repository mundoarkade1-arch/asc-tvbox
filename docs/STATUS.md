# Status do projeto

Última atualização: 24/10/2026

## Fase atual
Etapa "EPG com prévia nos canais" concluída e testada no TX9. Próxima etapa: controle parental.

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
- Linha "Agora: ..." nos cartões de canais ao vivo, na página inicial e no "Ver mais"
- Caixinha de prévia na página inicial: ao parar o foco num canal ao vivo, uma faixa fixa embaixo mostra o programa de agora, a barra de progresso, os horários de início e fim e o "A seguir"
- Barra Agora / A seguir no player (MainActivity): aparece quando o canal ao vivo começa a tocar, some depois de 6 segundos e volta ao apertar OK; não aparece em filmes

## Arquivos do EPG
- XtreamApi.java: função epg(...) que pede ao servidor a programação de UM canal (get_short_epg, até 4 programas) e devolve o programa de agora e o "a seguir". Horários em milissegundos.
- EpgCache.java: guarda na memória até 80 canais por 1 minuto cada; no máximo 2 pedidos ao servidor ao mesmo tempo; avisa sempre na thread principal
- item_card.xml: tem a linha tv_now (usada para o "Agora: ...")
- activity_home.xml: tem a caixinha de prévia fixa embaixo (group_preview)
- HomeActivity.java: liga a linha tv_now e a caixinha de prévia ao EpgCache (espera 0,5 segundo parado no canal antes de pedir o guia)
- BrowseActivity.java: liga a linha tv_now ao EpgCache (sem caixinha de prévia, por decisão do dono)
- activity_main.xml: tem a barra Agora / A seguir (group_nowbar) e a mensagem de status no alto da tela
- MainActivity.java: mostra a barra Agora / A seguir nos canais ao vivo

## Próximos passos (nesta ordem)
1. Controle parental
2. Episódios de séries (hoje o toque numa série só mostra um aviso)
3. Página inicial para login por lista M3U (hoje mostra um aviso de que chega numa etapa própria)

## Problemas em aberto
- Armazenamento livre do TX9 e acesso à Play Store: ainda não sabemos
- Lista M3U ainda não tem página inicial (ver passo 3 acima)
- Canais sem guia no servidor mostram "Programação indisponível" (comportamento esperado, não é erro)
