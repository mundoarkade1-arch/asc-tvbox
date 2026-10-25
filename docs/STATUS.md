Olá! Vou te passar a continuação de um projeto que já está bem adiantado. Antes de responder qualquer coisa, leia estes arquivos do meu repositório (público), nesta ordem:

1. https://raw.githubusercontent.com/mundoarkade1-arch/asc-tvbox/main/docs/STATUS.md
2. https://raw.githubusercontent.com/mundoarkade1-arch/asc-tvbox/main/docs/ARQUIVOS.md
3. https://raw.githubusercontent.com/mundoarkade1-arch/asc-tvbox/main/docs/DECISIONS.md
4. https://raw.githubusercontent.com/mundoarkade1-arch/asc-tvbox/main/AGENTS.md
5. https://raw.githubusercontent.com/mundoarkade1-arch/asc-tvbox/main/docs/TARGET_DEVICES.md
6. https://raw.githubusercontent.com/mundoarkade1-arch/asc-tvbox/main/docs/ORIGINAL_APP.md

Se algum link não abrir, ou se o texto parecer antigo (o STATUS.md deve ter uma seção "Rodada 1: ajustes pedidos pelo dono após o 1º teste" e o DECISIONS.md deve ter uma entrada "2026-10-25: Primeiro teste no TX9 e Rodada 1 de ajustes"), me avise que eu colo o texto aqui. O GitHub às vezes demora uns minutos para mostrar a versão nova. Você só consegue abrir os arquivos .md por link, não os de código; por isso, antes de editar qualquer arquivo de código, peça que eu cole o conteúdo atual dele e devolva o arquivo COMPLETO. Nunca reescreva um arquivo de código "de memória". Os arquivos de código que os agentes anteriores escreveram NÃO estão na sua memória: sempre peça o arquivo atual.

## Resumo do projeto
Meu app de IPTV (ASC play, antigo, em Capacitor/WebView, repositório https://github.com/mundoarkade1-arch/asc-play) não roda no meu TV box antigo. Criamos um app NOVO, nativo em Java com ExoPlayer, leve, no repositório asc-tvbox (https://github.com/mundoarkade1-arch/asc-tvbox). A decisão por app nativo já foi tomada; não reabra sem motivo forte. O comportamento do app original está em docs/ORIGINAL_APP.md.
- Aparelho de teste: TX9, Amlogic S905W, Android 7.1, 1 GB de RAM, tela 1080p.
- Controle remoto só com setas, OK, voltar, home, "função mouse" e uma tecla de três listras (chega como tecla de menu, a confirmar).
- O app tem: login por lista M3U ou servidor (DNS, usuário, senha), página inicial, abas Canais/Filmes/Séries com busca própria, páginas de filme e série, "Continuar de onde parei", EPG (só servidor), controle parental, Ajustes e atualização automática.

## Onde paramos
A nova aparência (partes a a f) está feita e verde. Eu fiz o primeiro teste no TX9 e pedi 4 ajustes (R1 a R4 no STATUS.md). NADA deles foi começado. Faça uma por vez, na ordem.

## Como vamos trabalhar (importante)
- Eu sou LEIGO em programação. Explique em português simples e MUITO FÁCIL, com frases curtas. Não use palavras técnicas sem explicar. Não me mande procurar pasta nem trecho de arquivo; prefiro COPIAR E COLAR o arquivo inteiro. Não coloque blocos de exemplo que pareçam campos para eu preencher.
- Eu não uso terminal nem instalo programas. Tudo é feito pelo site do GitHub. O APK é gerado pelo GitHub Actions e chega ao TX9 pela atualização automática do app.
- Me guie UM PASSO POR VEZ, numerando cada ação pequena. Para cada arquivo me dê um LINK DIRETO:
  - para LER/EDITAR: https://github.com/mundoarkade1-arch/asc-tvbox/edit/main/<caminho do arquivo>
  - para CRIAR: https://github.com/mundoarkade1-arch/asc-tvbox/new/main/<pasta>?filename=<NomeDoArquivo>
  - código Java: app/src/main/java/com/ascplay/tvbox/ ; layouts: app/src/main/res/layout/
- Para eu te mostrar um arquivo: abro o link de edição, Ctrl+A, Ctrl+C e colo aqui, sem clicar em Commit.
- Para eu salvar um arquivo: Ctrl+A, Delete, colo o código completo (pelo botão Copy do bloco), clico em Commit changes e, na janelinha, em Commit changes de novo.
- Espere eu responder "feito" ou "verde" (e se o Actions ficou verde) antes do próximo passo. Se eu não disser se ficou verde, pergunte.
- Quando houver erro no build, eu mando um print ou o texto do erro do Actions. Você corrige.
- Eu falo por voz (ditado), então o que eu escrevo pode ter palavras trocadas. Se algo não fizer sentido, me pergunte.
- Não me mande zip nem comandos no terminal. Nunca peça senhas nem a chave de assinatura. Não me peça para colar o meu link de lista M3U (pode ter senha).
- Cada arquivo de código deve ser completo. Não adicione dependências sem justificar em docs/DECISIONS.md.
- Ao final de cada etapa, me dê o STATUS.md atualizado (arquivo inteiro, mantenha CURTO como está) e o trecho novo do DECISIONS.md (só acrescentado no final: Ctrl+End, Enter duas vezes, colar). Se mudar algo do mapa de arquivos ou dos limites, me dê também o docs/ARQUIVOS.md atualizado. Não repita no STATUS.md o que já está no ARQUIVOS.md.

## Lições já aprendidas (evite repetir)
- Arquivos grandes às vezes são cortados na colagem (erro "reached end of file while parsing"). Se acontecer, peça só as últimas linhas e mande SÓ o pedaço que falta, para colar no final (Ctrl+End, Enter, colar).
- Já colei o código de um arquivo no lugar do outro (erro "class X is public, should be declared in a file named X.java"). Antes de cada bloco, deixe claro o nome do arquivo e o link, e mande UM de cada vez, esperando o "verde".
- Ao mudar código e layout juntos: primeiro o código que deixa de usar o item, depois o layout. Classe nova primeiro, registro no AndroidManifest.xml depois, e só então ligue os botões.
- Não use nome de classe interna igual a classes do Java (ex.: "Entry").
- Android 7.1 não tem PBKDF2 com SHA-256 (já resolvido no ParentalStore).
- Ao mexer em tela que funciona no login por servidor, crie tela irmã para a lista M3U.
- Sempre ler a lista M3U aos poucos, nunca guardar a lista inteira.
- O STATUS.md pode estar certo e o arquivo no GitHub desatualizado, ou o contrário: antes de mexer em código, peça o arquivo atual.

## Primeiras tarefas, nesta ordem
1. Confirme em poucas linhas que leu tudo e repita, em frases curtas, os 4 ajustes (R1 a R4).
2. Me faça as duas perguntas em aberto do STATUS.md (uma por vez): (a) manter a linha "Agora: ..." embaixo do cartão do canal na tela inicial? (b) no player, a seta para cima e para baixo passa só pelos canais da categoria aberta ou por todos? Pergunte também se lembrei de mais pedidos.
3. Comece pela R1 (menu com texto), UM ARQUIVO POR VEZ, pedindo o arquivo atual antes de cada um (layout_nav.xml e depois activity_home.xml).
4. Depois R2, R3 e R4, uma por vez, com STATUS.md e DECISIONS.md no fim de cada uma.
5. No fim da Rodada 1, reescreva o roteiro de teste curto para o TX9 e eu respondo em palavras simples.

Quando eu responder "feito" ou "verde" a cada passo, siga para o próximo.
