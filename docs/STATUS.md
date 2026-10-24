# Status do projeto

Última atualização: 24/10/2026

## Fase atual
Esqueleto do app nativo criado (ainda não compilado). Próximo passo: criar o workflow do GitHub Actions que compila e assina o APK.

## Feito
- Repositório criado e documentação preenchida (README, AGENTS, CLAUDE, llms.txt, docs/*)
- Aparelho de teste identificado e documentado (TX9, S905W, Android 7.1, 1 GB RAM, 1080p, WebView Chrome 52)
- Controle remoto documentado em docs/TARGET_DEVICES.md (só setas, OK, voltar, home e tecla "função mouse")
- Causa da falha do app original identificada: "erro de sintaxe" por JavaScript moderno no WebView antigo
- Decisão tomada: app Android nativo em Java com ExoPlayer (ver docs/DECISIONS.md)
- Código do app original lido (www/app.js, www/index.html, apk.yml). Resumo:
  - Login DNS = padrão Xtream Codes: baixa a lista inteira por get.php (m3u_plus) e trata como M3U; usa player_api.php só para sinopse, séries e vencimento (user_info.exp_date)
  - M3U: lê #EXTINF (tvg-id, tvg-logo, group-title); tipo (canal/filme/série) pelo caminho da URL (/live/, /movie/, /series/) ou pelo nome da categoria; séries agrupadas por S01E01
  - EPG: XMLTV (link informado > xmltv.php do servidor > url-tvg da lista), guarda 12 h, liga ao canal por tvg-id ou nome; mostra "agora/a seguir" no cartão e numa caixinha ao focar
  - Controle parental: senha de 4 números (PBKDF2), detecta categorias adultas por palavras, esconde adulto na busca/início/favoritos, trava após 5 erros
  - Início: favoritos + 3 fileiras (Ao vivo, Filmes, Séries) com 10 itens cada e "Ver mais"
  - Extras: favoritos, continuar assistindo, várias listas, aviso de vencimento, PIN do app, atualização via GitHub Releases
  - Dados sensíveis criptografados com chave no Android Keystore
- Esqueleto do projeto Android criado (9 arquivos): settings.gradle, build.gradle, gradle.properties, app/build.gradle, AndroidManifest.xml, strings.xml, activity_main.xml, MainActivity.java, .gitignore
- Decidido: o asc-tvbox terá chave de assinatura PRÓPRIA (nova), com os Secrets KEYSTORE_B64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD

## Próximos passos
1. Criar a chave de assinatura nova no Windows e cadastrar os 4 Secrets no asc-tvbox
2. Criar o workflow do GitHub Actions (.github/workflows) que compila e assina o APK
3. Primeiro APK mínimo: abrir e tocar um canal de teste com ExoPlayer
4. Testar no TX9 e registrar o resultado aqui
5. Implementar as funções uma a uma: login, página inicial por categorias, EPG, controle parental

## Problemas em aberto
- Ainda não sabemos: armazenamento livre do TX9 e se tem Play Store
- Esqueleto ainda não compilado: pode aparecer erro no primeiro build (colar o texto do erro aqui para corrigir)
- Decidir (mais adiante) como carregar listas grandes sem estourar 1 GB de RAM: API JSON do Xtream por categoria e M3U lido em fluxo, em vez de carregar tudo na memória
