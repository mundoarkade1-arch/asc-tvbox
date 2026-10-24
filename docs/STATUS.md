# Status do projeto

Última atualização: 24/10/2026

## Fase atual
Etapa "página inicial" CONCLUÍDA e testada no TX9 (login Xtream). Próxima etapa: navegar por categorias (Ver mais), depois EPG, controle parental, episódios de séries e página inicial para lista M3U.

## Feito e testado no TX9
- App instala, tela de login abre, setas funcionam com foco visível, teclado na tela funciona
- Senha errada mostra "Usuário ou senha incorretos."
- Atualização automática: a faixa "Atualizar / Depois" aparece, baixa, confere o SHA-256, abre o instalador, e o login é mantido. A versão com a página inicial chegou ao TX9 por essa atualização, sem pen drive
- Com login salvo, o app abre direto na página inicial (pula o login)
- Página inicial (servidor com login): três faixas (Canais ao vivo, Filmes, Séries) com até 10 itens cada, logo ou inicial do nome, foco com borda vermelha, primeiro cartão já selecionado
- Canal e filme tocam no player (ExoPlayer); Voltar volta para a página inicial
- "Trocar login" abre a tela de login com os dados antigos preenchidos

## Como o projeto está montado
- Chave de assinatura própria, com 4 Secrets no repositório (KEYSTORE_B64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD)
- Workflow (.github/workflows/apk.yml): a cada alteração salva em main, compila, assina e publica ASC-TVBox.apk e version.json em Releases (versão = número da execução do Actions)
- Arquivos em app/src/main/java/com/ascplay/tvbox/:
  - LoginActivity, LoginChecker, CredentialStore: login e login salvo (criptografado)
  - Updater: atualização automática
  - HomeActivity: página inicial (pede só o começo de cada lista ao servidor)
  - XtreamApi: categorias e itens do servidor, lidos aos poucos (nunca a lista inteira na memória)
  - ImageLoader: logos pequenos, cache de cerca de 8 MB, no máximo 2 downloads ao mesmo tempo
  - MainActivity: player (monta o endereço com o login salvo; canal tenta .m3u8 e depois .ts)
- Layouts: activity_login, activity_home, activity_main, item_card; fundo do cartão: drawable/bg_card

## Próximos passos
1. "Ver mais": tela para navegar por categorias de canais, filmes e séries, com lista completa carregada aos poucos (por página)
2. EPG com prévia nos canais (programa de agora no cartão e ao focar; "A seguir")
3. Controle parental (senha de 4 números; categorias adultas fora da busca, do início e dos favoritos)
4. Séries: tela de temporadas e episódios, com "continuar de onde parou"
5. Página inicial para login por lista M3U (ler a lista aos poucos, sem carregar tudo na memória)
6. Depois: favoritos, busca, várias listas, aviso de vencimento

## Problemas em aberto
- Página inicial pede só os 10 primeiros de cada seção: no "Ao vivo" o original coloca primeiro a categoria "Canal do cliente"; ainda não foi feito
- Série: ao apertar OK só aparece um aviso (os episódios vêm numa etapa própria)
- Lista M3U: o login funciona, mas a página inicial mostra só um aviso
- Ainda não sabemos: armazenamento livre do TX9 e se tem Play Store
