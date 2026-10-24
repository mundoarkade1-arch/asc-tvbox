# Status do projeto

Última atualização: 24/10/2026

## Fase atual
Etapa "Ver mais" concluída e testada no TX9. Próxima etapa: EPG com prévia nos canais.

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
- "Ver mais": cartão no fim de cada faixa que abre a tela de navegação por categorias (BrowseActivity), com borda vermelha de seleção nas listas

## Próximos passos (nesta ordem)
1. EPG com prévia nos canais
2. Controle parental
3. Episódios de séries (hoje o toque numa série só mostra um aviso)
4. Página inicial para login por lista M3U (hoje mostra um aviso de que chega numa etapa própria)

## Problemas em aberto
- Armazenamento livre do TX9 e acesso à Play Store: ainda não sabemos
- Lista M3U ainda não tem página inicial (ver passo 4 acima)
