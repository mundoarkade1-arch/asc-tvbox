# Status do projeto

Última atualização: 24/10/2026

## Fase atual
Etapa de login e atualização automática CONCLUÍDA e testada no TX9. Próxima etapa: página inicial por categorias.

## Feito e testado no TX9
- App instala, tela de login abre, setas funcionam com foco visível, teclado na tela funciona
- Login certo abre o player, que toca um vídeo de teste
- Senha errada mostra "Usuário ou senha incorretos."
- Atualização automática: a faixa "Atualizar / Depois" aparece na tela de login, baixa, confere o SHA-256, abre o instalador do Android, e o login é mantido depois de atualizar

## Como o projeto está montado
- Chave de assinatura própria, com 4 Secrets no repositório (KEYSTORE_B64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD)
- Workflow (.github/workflows/apk.yml): a cada alteração salva em main, compila, assina e publica ASC-TVBox.apk e version.json em Releases (versão = número da execução do Actions)
- Atualizador: Updater.java (verifica, baixa, confere SHA-256, instala), file_paths.xml, FileProvider e permissão REQUEST_INSTALL_PACKAGES no AndroidManifest, faixa de aviso em LoginActivity e activity_login.xml
- Arquivos principais em app/src/main/java/com/ascplay/tvbox/: LoginActivity, LoginChecker, CredentialStore, MainActivity, Updater

## Próximos passos
1. Página inicial por categorias (10 primeiros canais, filmes e séries), pulando o login quando houver credenciais salvas
2. Decidir como carregar listas grandes sem estourar 1 GB de RAM (o app original carrega a lista inteira na memória; no app novo vamos preferir a API do servidor, por categoria, e para M3U ler aos poucos)
3. EPG com prévia nos canais (o app original lê o XMLTV do servidor e mostra o programa atual no foco do canal)
4. Controle parental (o original usa senha de 4 números; categorias adultas ficam fora da busca, do início e dos favoritos)
5. Depois: favoritos, continuar de onde parou, várias listas, aviso de vencimento

## Problemas em aberto
- docs/ORIGINAL_APP.md ainda precisa do resumo de como o app original faz M3U, Xtream, EPG e controle parental (já lido no app.js)
- Ainda não sabemos: armazenamento livre do TX9 e se tem Play Store
