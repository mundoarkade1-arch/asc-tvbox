# Status do projeto

Última atualização: 24/10/2026

## Fase atual
Atualização automática do app implementada. Falta testar a atualização de uma versão para a outra no TX9. Depois vem a página inicial por categorias.

## Feito e testado no TX9
- App instala, tela de login abre, setas funcionam com foco visível, teclado na tela funciona
- Login certo abre o player, que toca um vídeo de teste
- Senha errada mostra "Usuário ou senha incorretos." (corrigido com AuthRefusedException no LoginChecker)

## Feito (compilou no GitHub Actions)
- Chave de assinatura própria, com 4 Secrets no repositório (KEYSTORE_B64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD)
- Workflow compila, assina e publica ASC-TVBox.apk e version.json em Releases (versão = número da execução do Actions)
- Atualização automática, passos 1 a 6:
  1. apk.yml gera version.json (code, name, url, sha256)
  2. app/build.gradle com androidx.core:core:1.13.1
  3. res/xml/file_paths.xml (pasta cache/updates)
  4. AndroidManifest.xml com permissão REQUEST_INSTALL_PACKAGES e FileProvider
  5. Updater.java (confere a versão, baixa, confere o SHA-256 e abre o instalador)
  6. Faixa "Atualizar / Depois" na tela de login (LoginActivity, activity_login.xml, strings.xml)

## Próximos passos
1. Testar a atualização no TX9: com a versão com atualizador instalada, publicar uma versão nova e ver se o aviso aparece e instala
2. Página inicial por categorias (10 primeiros canais, filmes e séries), pulando o login quando houver credenciais salvas
3. Decidir como carregar listas grandes sem estourar 1 GB de RAM
4. EPG com prévia nos canais
5. Controle parental

## Problemas em aberto
- Resumo de como o app original faz M3U, Xtream, EPG e controle parental ainda precisa ser escrito em docs/ORIGINAL_APP.md (ler www/app.js do asc-play)
- Ainda não sabemos: armazenamento livre do TX9 e se tem Play Store
