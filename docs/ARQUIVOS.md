Olá! Vou te passar a continuação de um projeto já bem adiantado. Sou MUITO LEIGO em programação: preciso de ajuda bem simples e passo a passo.

## 1. Como você vai se informar
Depois deste texto eu colo 3 arquivos, um por mensagem: AGENTS.md, STATUS.md e ARQUIVOS.md. Leia com atenção e siga o AGENTS.md. Só comece depois dos três. Se precisar de DECISIONS.md, ARCHITECTURE.md ou TARGET_DEVICES.md, peça que eu cole. (Links, caso abram; o GitHub demora uns minutos para mostrar a versão nova: https://github.com/mundoarkade1-arch/asc-tvbox/blob/main/AGENTS.md e os da pasta docs.) Você NÃO consegue abrir arquivos de código: antes de mexer em qualquer um, peça que eu cole o atual e devolva o arquivo COMPLETO. Nunca reescreva de memória.

## 2. O projeto
Meu app de IPTV antigo (Capacitor/WebView) não roda no meu TV box (navegador de 2016). Criamos um app NOVO, nativo em Java com ExoPlayer, leve, em https://github.com/mundoarkade1-arch/asc-tvbox. App nativo está decidido; não reabra.
- Aparelho: TX9, Amlogic S905W, Android 7.1, 1 GB de RAM, 1080p.
- Controle: setas, OK, voltar, home, "função mouse" e tecla de três listras (chega como tecla de menu e abre a programação). Sem teclado, números ou teclas coloridas.
- O app faz: login por servidor (DNS, usuário, senha) ou lista M3U (eu uso só o servidor); tela inicial adaptável com faixa "Continue assistindo" e faixas de canais, filmes e séries; abas Canais, Filmes, Séries com categorias, botão "Pesquisar" fixo e grade de 5 por linha; página de filme e de série; continuar de onde parei; próxima temporada sozinha; botão de pular episódio; programação (EPG, só servidor); controle parental; Ajustes; atualização automática; troca de canal ao vivo com cima e baixo; pesquisa com índice em arquivo e teclado QWERTY com acentos (segurar OK na vogal).
- Eu faço tudo pelo site do GitHub, sem terminal. O APK sai do GitHub Actions e chega ao TX9 pela atualização do app. Cada arquivo salvo (até .md) gera versão nova; é normal.

## 3. Histórico
- Fases 1 a 3, Rodadas 1 a 6, M6, M9, M10, M12, R4-1, R4-2, páginas de filme e série: prontas e TESTADAS por mim no TX9.
- M11 (pesquisa lenta): RESOLVIDO no servidor com SearchIndex (índice em arquivo, 2 h). A segunda pesquisa é rápida; a primeira demora. Testado, "ficou bom".
- Teclado de pesquisa QWERTY brasileiro com Ç, barra de espaço grande e acentos ao segurar OK: pronto e testado ("ficou bom").
- M13 (texto "Temporada N - Episódio N" no player, no alto à esquerda por 5 s): código VERDE no Actions, mas eu ainda NÃO testei.
- M13b (botões do player quase invisíveis, opacidade 0.25): SALVO CORTADO, ACTIONS VERMELHO.

## 4. Onde estamos agora
- URGENTE: o MainActivity.java no GitHub foi salvo CORTADO (minha colagem não veio inteira). O Actions mostra "MainActivity.java:426: error: unclosed comment" e "MainActivity.java:428: reached end of file while parsing". Conserte primeiro: peça as últimas 15 linhas do arquivo, descubra o que falta e mande SÓ o pedaço final. Se não der, peça o arquivo inteiro e devolva em 2 ou 3 partes (colagem grande é cortada). O conteúdo certo é o MainActivity com M13 (rótulo do episódio, EXTRA_LIST_LABELS) e CONTROLLER_ALPHA = 0.25f. Detalhes no STATUS.md e no trecho final do ARQUIVOS.md.
- Depois: nada mais em andamento. Todo o resto está verde.

## 5. Pedidos do dono ainda não feitos
- M13b: "fica um botão de pause, de passar e voltar 5s e 15s na tela em branco, quero que ele quase não apareça". Plano: opacidade 0.25 (já no arquivo certo). Se ainda aparecer demais, ajustar o número (0.10) ou criar um layout de controle sem fundo (activity_main.xml não tem esses botões; vêm da biblioteca).
- Testes pendentes no TX9: M13 (episódio mostra "Temporada N - Episódio N" no alto à esquerda por 5 s; OK mostra de novo) e M13b (botões quase invisíveis).
- Possíveis melhorias do M11, só se eu pedir: a primeira pesquisa ainda demora (montar índice em segundo plano ao abrir o app; contagem em "Buscando..."). Lista M3U não usa o índice (eu não uso M3U).
- ADIADOS: fundo com imagem e ícone novo (imagens grandes; reduzir no squoosh.app e enviar pelo GitHub; você não envia imagens); otimização geral sem perder funções.
- Perguntas abertas: tela de login com degradê? Fim da lista na troca de canal dá a volta (escolha do agente). Ideia que precisa do meu OK: o Actions ignorar mudanças só em .md. "Trocar de lista" ainda não chama SearchIndex.clear (opcional).

## 6. Como me ajudar
- Português simples, frases curtas. Palavra técnica só com explicação.
- Antes de cada passo, diga em uma frase o que ele faz; depois, o que devo ver (bolinha verde no Actions = deu certo).
- UM passo e UM arquivo por vez, ações numeradas, com LINK DIRETO: ver/editar https://github.com/mundoarkade1-arch/asc-tvbox/edit/main/CAMINHO ; criar .../new/main/PASTA?filename=NOME ; Java em app/src/main/java/com/ascplay/tvbox/ ; layouts em app/src/main/res/layout/ ; desenhos em app/src/main/res/drawable/.
- Prefiro copiar e colar o arquivo INTEIRO. Para mostrar: abro o link, Ctrl+A, Ctrl+C, colo aqui, sem Commit. Para salvar: Ctrl+A, Delete, colo, Commit changes e, na janelinha, Commit changes de novo. Lembre-me de conferir o nome do arquivo no endereço.
- Combinamos: quando eu digo "ok", quer dizer que salvei e ficou verde. Se der erro, mando print ou texto.
- UMA pergunta por vez, com respostas numeradas. No teste no TX9, diga as teclas e o que deve aparecer.
- Eu falo por voz (ditado): se algo não fizer sentido, pergunte. "pk" ou "pl" costumam ser um "ok" digitado errado: confirme.
- Nunca peça senha, chave ou meu link M3U. Nada de zip ou terminal.
- Às vezes digo que só testo no final e que não quero STATUS/DECISIONS a cada passo: respeite, e só faça a passagem quando eu pedir ou terminar.

## 7. Lições e regras técnicas
- Colagem grande às vezes é cortada (aconteceu de novo com o MainActivity): peça só as últimas linhas e mande SÓ o pedaço que falta. Para arquivos muito grandes (MainActivity, XtreamApi, SearchKeyboard), considere devolver em partes.
- Já colei no arquivo errado: diga o nome do arquivo antes de cada bloco.
- Às vezes mando só "ok" sem colar o arquivo: confira se veio o arquivo antes de seguir.
- Código que deixa de usar algo primeiro, layout depois. Classe ou função nova primeiro, quem a usa depois. Classe nova primeiro, registro no Manifest depois, botões por último.
- Antes de mudar um layout que o código preenche, peça o .java para ver se ele fixa tamanhos.
- LISTA e GRADE controlam o foco e o clique; o item da lista/grade NUNCA pode ser focável nem clicável.
- NÃO mostrar resultados da busca um a um (as capas piscam). Juntar e mostrar de uma vez.
- Android 7.1 sem PBKDF2 com SHA-256. Não use nome de classe interna igual ao do Java (ex.: Entry). Em putExtra, não passe null solto.
- Lista M3U: leia aos poucos; o "id" é só o número de ordem. Qualquer cópia da lista em disco precisa de limite de tamanho e de validade (o SearchIndex tem: 2 h, 200000 itens, 60 MB).
- Pares que andam juntos: menu (layout_nav.xml e activity_home.xml); cartões (item_card.xml e item_card_small.xml); clique do Continue assistindo (HomeActivity e ContinueActivity); SeriesActivity e M3uSeriesActivity (gêmeas); BrowseActivity e M3uBrowseActivity (irmãs).
- O menu é transparente: toda tela com menu precisa do fundo bg_gradient. O player fica preto.
- ContinueStore.Saved e a seriesKey podem ter servidor, usuário e senha: nunca mostrar nem registrar em log.
- A SearchActivity reaproveita activity_browse.xml: tudo que entrar nele precisa ser escondido nela.
- Sem dependências novas sem justificar no DECISIONS.md. App leve (1 GB). Não leia nem guarde listas inteiras na memória.

## 8. No fim de CADA passo (sem eu pedir)
Depois do meu "ok": (1) STATUS.md inteiro e curto (até uns 80 linhas); (2) trecho novo do DECISIONS.md (Ctrl+End, Enter duas vezes, colar); (3) ARQUIVOS.md só se mudou arquivo, regra ou limite (o ARQUIVOS.md tem um trecho "ATUALIZAÇÕES DE 26/10/2026" no fim: quando der, junte-o nas seções certas); (4) um PROMPT novo neste estilo. Um arquivo por vez. Se a conversa ficar longa, avise antes. Se eu disser "faça a passagem", faça tudo de uma vez na hora.

## 9. Primeiras tarefas
1. Espere os 3 arquivos. Confirme em poucas linhas o que entendeu e diga o que falta.
2. Conserte o MainActivity.java cortado (seção 4): peça as últimas 15 linhas, mande só o pedaço que falta, espere o verde.
3. Peça (uma pergunta, respostas numeradas) o teste no TX9 de M13 e M13b e ajuste a opacidade se eu pedir.
4. Pergunte o que mais me incomoda e siga a partir daí.

Quando eu disser "ok", siga para o próximo passo.
