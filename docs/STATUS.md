# Status do projeto

Última atualização: 24/10/2026

## Fase atual
Tela de login pronta e testada no TX9 (setas, teclado na tela, login correto abre o player de teste). Em andamento: atualização automática do app (passos 1 e 2 de 6 enviados; 3 a 6 pendentes).

## Feito
- Repositório criado e documentação preenchida (README, AGENTS, CLAUDE, llms.txt, docs/*)
- Aparelho de teste documentado (TX9, S905W, Android 7.1, 1 GB RAM, 1080p, WebView Chrome 52) e controle remoto documentado em docs/TARGET_DEVICES.md
- Decisão: app Android nativo em Java com ExoPlayer (docs/DECISIONS.md)
- Código do app original (asc-play) lido. Resumo:
  - Login DNS = Xtream Codes: baixa a lista inteira por get.php (m3u_plus) e trata como M3U; usa player_api.php só para sinopse, séries e vencimento (user_info.exp_date)
  - M3U: lê #EXTINF (tvg-id, tvg-logo, group-title); tipo (canal/filme/série) pelo caminho da URL (/live/, /movie/, /series/) ou pelo nome da categoria; séries agrupadas por S01E01
  - EPG: XMLTV (link informado > xmltv.php do servidor > url-tvg da lista), guarda 12 h, liga ao canal por tvg-id ou nome; mostra "agora/a seguir" no cartão e numa caixinha ao focar
  - Controle parental: senha de 4 números (PBKDF2), detecta categorias adultas por palavras, esconde adulto na busca/início/favoritos, trava após 5 erros
  - Início: favoritos + 3 fileiras (Ao vivo, Filmes, Séries) com 10 itens e "Ver mais"
  - Extras: favoritos, continuar assistindo, várias listas, aviso de vencimento, PIN do app, atualização via GitHub Releases
- Esqueleto Android criado; chave de assinatura PRÓPRIA criada no Windows (pasta chaves-asc-tvbox, alias asc-tvbox, mesma senha para arquivo e chave) e 4 Secrets cadastrados: KEYSTORE_B64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD
- Workflow .github/workflows/apk.yml: compila, assina e publica ASC-TVBox.apk + version.json em Releases (tag v<número da execução>)
- Primeiro APK com ExoPlayer testado no TX9: toca o stream HLS de teste
- Tela de login (LoginActivity, activity_login.xml, bg_field.xml, bg_button.xml, CredentialStore.java, LoginChecker.java) testada no TX9: setas com foco visível OK, teclado na tela OK, login correto OK (abre o player de teste)

## Em andamento: atualização automática pelo app (6 passos, um arquivo por vez)
1. .github/workflows/apk.yml publica version.json junto do APK: FEITO
2. app/build.gradle com androidx.core:core:1.13.1 (FileProvider): ENVIADO, confirmar se o dono salvou
3. app/src/main/res/xml/file_paths.xml (pasta de cache do app liberada ao instalador): pendente
4. AndroidManifest.xml: permissão REQUEST_INSTALL_PACKAGES + provider FileProvider (authority com.ascplay.tvbox.fileprovider): pendente
5. Updater.java: consulta version.json, compara code com o versionCode instalado (PackageManager), aceita só URL de https://github.com/mundoarkade1-arch/asc-tvbox/releases/download/, baixa para o cache privado, confere SHA-256, abre o instalador via FileProvider (Android 8+: checar canRequestPackageInstalls): pendente
6. LoginActivity: checar atualização ao abrir (thread de fundo) e mostrar diálogo Atualizar/Depois navegável por setas: pendente
Depois: a primeira versão com atualizador ainda vai por pen drive; as seguintes chegam pelo próprio app.

## Próximos passos (depois do atualizador)
1. Página inicial com categorias (10 primeiros canais, filmes e séries) e pular o login quando houver credenciais salvas
2. Carregamento de listas sem estourar 1 GB de RAM (ver "Problemas em aberto")
3. EPG com prévia nos canais
4. Controle parental
5. Telas feitas para controle remoto (setas + OK, foco visível, OK longo para opções)
6. Trocar o vídeo de teste da MainActivity pelo player real

## Problemas em aberto
- LoginChecker.java: correção enviada (senha errada mostrava "Sem conexão com o servidor" no TX9 em vez de "Usuário ou senha incorretos."). Confirmar se o dono salvou e retestar no aparelho
- Armazenamento livre do TX9 e Play Store: ainda não sabemos
- Decidir como carregar listas grandes: API JSON do Xtream por categoria (get_live_categories, get_live_streams&category_id=...) e M3U lido em fluxo com banco local, em vez de carregar tudo na memória
- Guardar cópia da pasta chaves-asc-tvbox e das senhas em pen drive: sem a chave não dá para atualizar o app instalado
