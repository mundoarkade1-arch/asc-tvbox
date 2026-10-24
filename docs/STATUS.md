# Status do projeto

Última atualização: 24/10/2026

## Fase atual
Repositório organizado e decisão de tecnologia tomada. Próximo passo: criar o esqueleto do app nativo e o workflow que gera o APK.

## Feito
- Repositório criado e documentação preenchida (README, AGENTS, CLAUDE, llms.txt, docs/*)
- Aparelho de teste identificado e documentado (TX9, S905W, Android 7.1, 1 GB RAM, WebView Chrome 52)
- Causa da falha do app original identificada: "erro de sintaxe" por JavaScript moderno no WebView antigo
- Decisão tomada: app Android nativo em Java com ExoPlayer (ver docs/DECISIONS.md)

## Próximos passos
1. Ler o código do app original (https://github.com/mundoarkade1-arch/asc-play, pasta www/) para extrair a lógica: leitura de M3U, login por DNS (usuário/senha), EPG, controle parental
2. Criar o esqueleto do projeto Android nativo neste repositório
3. Criar o workflow do GitHub Actions que gera o APK (.github/workflows)
4. Primeiro APK mínimo: abrir e tocar um canal de teste
5. Testar no TX9 e registrar o resultado aqui
6. Implementar as funções uma a uma: login, página inicial por categorias, EPG, controle parental

## Problemas em aberto
- Ainda não sabemos: armazenamento livre, resolução da TV, tipo de controle remoto
- Ainda não sabemos como o app original autentica por DNS (provavelmente API Xtream Codes; confirmar lendo o código)
