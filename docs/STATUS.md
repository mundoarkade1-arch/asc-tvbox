# Status do projeto

Última atualização: 24/10/2026

## Fase atual
Primeiro APK mínimo concluído e testado no TX9: instala e toca um vídeo de teste com ExoPlayer. Próximo passo: implementar as funções do app, começando pelo login.

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
- Esqueleto do projeto Android criado (settings.gradle, build.gradle, gradle.properties, app/build.gradle, AndroidManifest.xml, strings.xml, activity_main.xml, MainActivity.java, .gitignore)
- Chave de assinatura PRÓPRIA criada no Windows (pasta chaves-asc-tvbox, alias asc-tvbox) e 4 Secrets cadastrados: KEYSTORE_B64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD
- Workflow .github/workflows/apk.yml criado: compila, assina e publica o APK em Releases
- Primeiro APK com ExoPlayer (stream HLS de teste, mostra "Carregando..." e erros na tela) instalado e TESTADO no TX9: funcionou

## Próximos passos
1. Tela de login: servidor + usuário + senha (DNS/Xtream), depois lista M3U por link; guardar o login no armazenamento privado do app
2. Página inicial com categorias (10 primeiros canais, filmes e séries)
3. EPG com prévia nos canais
4. Controle parental
5. Telas feitas para controle remoto (setas + OK, foco visível, OK longo para opções)

## Problemas em aberto
- Ainda não sabemos: armazenamento livre do TX9 e se tem Play Store
- Guardar uma cópia da pasta chaves-asc-tvbox (e das senhas) em pen drive: sem ela não dá para atualizar o app instalado
- Decidir como carregar listas grandes sem estourar 1 GB de RAM: API JSON do Xtream por categoria e M3U lido em fluxo, em vez de carregar tudo na memória
