# Status do projeto

Última atualização: 24/10/2026

## Fase atual
Etapa "Ver mais" (navegar por categorias) EM ANDAMENTO. Os arquivos da tela já estão no repositório e compilam (Actions verde), mas AINDA NÃO ESTÃO LIGADOS: a tela não está registrada no AndroidManifest e nada a abre.

## Feito e testado no TX9
- App instala, tela de login abre, setas funcionam com foco visível, teclado na tela funciona
- Senha errada mostra "Usuário ou senha incorretos."
- Atualização automática (faixa "Atualizar / Depois", SHA-256, instalador do Android; login mantido). Já entregou versões novas ao TX9 sem pen drive
- Com login salvo, o app abre direto na página inicial
- Página inicial (servidor com login): faixas Canais ao vivo, Filmes e Séries com até 10 itens, logo ou inicial do nome, foco com borda vermelha
- Canal e filme tocam no player (ExoPlayer); Voltar volta para a página inicial
- "Trocar login" abre o login com os dados antigos preenchidos

## Feito, compilando, mas NÃO testado (porque ainda não está ligado)
- XtreamApi.java: agora aceita offset (items(..., categoryId, offset, limit)) para carregar por páginas
- activity_browse.xml: categorias à esquerda (ListView 300dp), grade de itens à direita (GridView), texto de status no centro
- item_category.xml: linha de categoria (usa drawable/bg_card)
- BrowseActivity.java: carrega categorias; "Todas as categorias" é a primeira; itens por páginas de 40, máximo 400 por categoria; ignora respostas atrasadas ao trocar de categoria; canal/filme abre o player; série mostra aviso

## O que falta para fechar "Ver mais" (nesta ordem, um arquivo por vez)
1. drawable/bg_card.xml: acrescentar o estado android:state_selected="true" (com a mesma borda vermelha do foco). Motivo: ListView e GridView marcam o item escolhido como "selected", não como "focused"; sem isso a borda vermelha não aparece em BrowseActivity
2. AndroidManifest.xml: registrar <activity android:name=".BrowseActivity"> (landscape, exported=false, mesmo padrão da HomeActivity)
3. HomeActivity.java: acrescentar um cartão "Ver mais ›" no fim de cada faixa que abre BrowseActivity com EXTRA_KIND (LIVE, MOVIE ou SERIES). Precisa de um layout simples para esse cartão (ou reaproveitar item_card com texto fixo)
4. Testar no TX9 (a atualização automática traz a versão): abrir "Ver mais", navegar categorias com setas, trocar de categoria, rolar a grade até carregar mais páginas, tocar um canal e um filme, Voltar

## Próximos passos depois de "Ver mais"
1. EPG com prévia nos canais (programa de agora no cartão e ao focar; "A seguir"). Original: XMLTV de servidor/xmltv.php?username=..&password=..; liga canal ao guia por epg_channel_id (XtreamApi.Item.epgId já existe), depois por nome
2. Controle parental (senha de 4 números; categorias adultas fora da busca, do início e dos favoritos; bloqueio crescente após 5 erros)
3. Séries: temporadas e episódios, com "continuar de onde parou"
4. Página inicial para login por lista M3U (ler aos poucos, sem carregar tudo na memória)
5. "Ao vivo" da página inicial: colocar primeiro a categoria "Canal do cliente" (como no original)
6. Depois: favoritos, busca, várias listas, aviso de vencimento

## Problemas em aberto
- Série: OK só mostra um aviso
- Lista M3U: o login funciona, mas a página inicial mostra só um aviso
- Ainda não sabemos: armazenamento livre do TX9 e se tem Play Store
