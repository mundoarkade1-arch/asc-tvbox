Olá! Vou te passar a continuação de um projeto já bem adiantado. Sou MUITO LEIGO em programação: preciso de ajuda bem simples e passo a passo.

## 1. Como você vai se informar
Depois deste texto eu colo 3 arquivos, um por mensagem: AGENTS.md, STATUS.md e ARQUIVOS.md. Leia com atenção e siga o AGENTS.md. Só comece depois dos três. Se precisar de DECISIONS.md, ARCHITECTURE.md ou TARGET_DEVICES.md, peça que eu cole. (Links, caso abram; o GitHub demora uns minutos para mostrar a versão nova: https://github.com/mundoarkade1-arch/asc-tvbox/blob/main/AGENTS.md e os da pasta docs.) Você NÃO consegue abrir arquivos de código: antes de mexer em qualquer um, peça que eu cole o atual e devolva o arquivo COMPLETO. Nunca reescreva de memória.

## 2. O projeto
Meu app de IPTV antigo (Capacitor/WebView) não roda no meu TV box (navegador de 2016). Criamos um app NOVO, nativo em Java com ExoPlayer, leve, em https://github.com/mundoarkade1-arch/asc-tvbox. App nativo está decidido; não reabra.
- Aparelho: TX9, Amlogic S905W, Android 7.1, 1 GB de RAM, 1080p.
- Controle: setas, OK, voltar, home, "função mouse" e tecla de três listras (deve chegar como tecla de menu; a confirmar). Sem teclado, números ou teclas coloridas.
- O app faz: login por servidor (DNS, usuário, senha) ou lista M3U; tela inicial com 10 canais, 10 filmes, 10 séries; abas Canais, Filmes, Séries com categorias e busca; página de filme e série com capa e sinopse; continuar de onde parei; episódios vistos em verde; próxima temporada sozinha; programação (EPG, só servidor); controle parental; Ajustes; atualização automática.
- Eu faço tudo pelo site do GitHub, sem terminal. O APK sai do GitHub Actions e chega ao TX9 pela atualização do app. Cada arquivo salvo (até .md) gera versão nova; é normal.

## 3. Histórico
- Fases 1 e 2: testadas/verdes (login, atualização, página inicial, "Ver mais", parental, séries, busca).
- Fase 3, nova aparência (a) a (f): verde. Primeiro teste no TX9 em 25/10/2026: a barra Agora / A seguir ficou certa. O resto ainda NÃO foi testado no TX9.
- Decisões: lista M3U sem guia; filme e série iguais nos dois logins; resultados da busca sem menu.

## 4. Onde estamos agora
- R1 (menu com nome, 200dp), R2 (tela inicial sem caixinha de prévia, mantendo "Agora: ..."), R3 (ordem do foco nas abas): PRONTAS e VERDES.
- R4 (trocar de canal com cima/baixo no player ao vivo) está PELA METADE:
  - FEITO: ChannelQueue.java (novo) e MainActivity.java (cima = anterior, baixo = próximo, só se a tela mandar EXTRA_QUEUE = true; EXTRA_QUEUE_CAT = categoria de origem, nulo = todas). Preciso confirmar se o Actions ficou verde no MainActivity.
  - FALTA, um arquivo por vez: HomeActivity.java (canal ao vivo: EXTRA_QUEUE true, categoria nula), BrowseActivity.java (categoria = currentCat), M3uBrowseActivity.java (categoria = currentGroup), SearchActivity.java (pedir o arquivo; provável categoria nula). Código antes, mas aqui nada de layout.
- Depois: R5 e R6.

## 5. Pedidos do dono ainda não feitos (com as minhas palavras)
- R4: seta para baixo vai ao próximo canal; passando as categorias em ordem, como se fossem todos os canais, a partir de onde estou; ao acabar a categoria, pula sozinho para a próxima. OK mostra a barra com o que passa agora e depois. Fim da lista: eu não lembrei; o agente escolheu dar a volta (último -> primeiro). Vou testar.
- R5: na tela inicial, as três faixas (canais, filmes, séries) devem caber INTEIRAS na tela, SEM rolagem (cartões podem ficar menores). Cuidado: item_card.xml também é usado nas abas.
- R6: numa faixa, ao chegar em "Ver mais", apertar direita de novo NÃO faz nada (não pula para a faixa de baixo). Para mudar de faixa, só cima e baixo.
- Em aberto: testar a tecla das três listras (depois das melhorias). Ideia que precisa do meu OK: o Actions ignorar mudanças só em .md. Posso lembrar de mais pedidos (Rodada 3).

## 6. Como me ajudar
- Português simples, frases curtas. Palavra técnica só com explicação.
- Antes de cada passo, diga em uma frase o que ele faz; depois, o que devo ver (bolinha verde no Actions = deu certo).
- UM passo e UM arquivo por vez, ações numeradas, com LINK DIRETO: ver/editar https://github.com/mundoarkade1-arch/asc-tvbox/edit/main/CAMINHO ; criar .../new/main/PASTA?filename=NOME ; Java em app/src/main/java/com/ascplay/tvbox/ ; layouts em app/src/main/res/layout/.
- Prefiro copiar e colar o arquivo INTEIRO. Para mostrar: abro o link, Ctrl+A, Ctrl+C, colo aqui, sem Commit. Para salvar: Ctrl+A, Delete, colo, Commit changes e, na janelinha, Commit changes de novo. Lembre-me de conferir o nome do arquivo no endereço.
- Combinamos: eu digo "ok" quando salvei e ficou verde; se der erro, mando print ou texto.
- UMA pergunta por vez, com respostas numeradas. No teste no TX9, diga as teclas e o que deve aparecer.
- Eu falo por voz (ditado): se algo não fizer sentido, pergunte.
- Nunca peça senha, chave ou meu link M3U. Nada de zip ou terminal.

## 7. Lições e regras técnicas
- Colagem grande às vezes é cortada: peça só as últimas linhas e mande SÓ o pedaço que falta.
- Já colei no arquivo errado: diga o nome do arquivo antes de cada bloco.
- Código que deixa de usar algo primeiro, layout depois. Classe nova primeiro, registro no Manifest depois, botões por último.
- Android 7.1 sem PBKDF2 com SHA-256. Não use nome de classe interna igual ao do Java (ex.: Entry).
- Lista M3U: leia aos poucos; o "id" é só o número de ordem; o canal vem com endereço (url).
- Tela inicial tem CÓPIA do menu (activity_home.xml); o resto usa layout_nav.xml. Mudou um, mude o outro.
- Sem dependências novas sem justificar no DECISIONS.md. App leve (1 GB).

## 8. No fim de CADA passo (sem eu pedir)
Depois do meu "ok": (1) STATUS.md inteiro e curto (até uns 80 linhas); (2) trecho novo do DECISIONS.md (Ctrl+End, Enter duas vezes, colar); (3) ARQUIVOS.md só se mudou arquivo, regra ou limite; (4) um PROMPT novo neste estilo. Um arquivo por vez. Se a conversa ficar longa, avise antes. Se eu disser "faça a passagem", faça na hora.

## 9. Primeiras tarefas
1. Espere os 3 arquivos. Confirme em poucas linhas o que entendeu e diga o que falta da R4.
2. Pergunte se o Actions ficou verde no MainActivity.java (uma pergunta).
3. Termine a R4: HomeActivity, BrowseActivity, M3uBrowseActivity, SearchActivity, um por vez (peça o atual de cada um).
4. Depois R5 e R6, uma por vez.
5. No fim da Rodada, roteiro curto de teste no TX9 (menu, foco, tela inicial sem rolagem, "Ver mais", troca de canal com cima/baixo, tecla das três listras).

Quando eu disser "ok", siga para o próximo passo.
