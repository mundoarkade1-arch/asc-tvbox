# Instruções para agentes de IA (LEIA PRIMEIRO)

## O que ler, nesta ordem
1. docs/STATUS.md: onde paramos e o que fazer agora.
2. docs/ARQUIVOS.md: mapa dos arquivos, regras técnicas e limites conhecidos.
3. docs/DECISIONS.md: decisões e motivos (só se acrescenta no fim).
4. docs/TARGET_DEVICES.md: o aparelho de teste.
5. docs/ARCHITECTURE.md: como o app é montado e como a versão sobe.
6. docs/ORIGINAL_APP.md: só se precisar do comportamento do app original.
Só dá para abrir os arquivos .md por link, não os de código. O código dos agentes anteriores NÃO está na sua memória.

## O projeto
App de IPTV nativo (Java + ExoPlayer, sem WebView) para TV boxes antigos. Aparelho de teste: TX9, Amlogic S905W, Android 7.1, 1 GB de RAM, tela 1080p. O app original (Capacitor, repositório asc-play) não roda nele. A escolha do app nativo está decidida: não reabrir sem motivo forte. O controle remoto tem setas, OK, voltar, home, "função mouse" e uma tecla de três listras (provavelmente chega como tecla de menu; a confirmar).

## COMO FALAR COM O DONO (ele é MUITO LEIGO)
- Português simples, frases curtas, uma ideia por frase. Palavra técnica só com explicação curta entre parênteses, na primeira vez.
- Antes de cada passo, diga em UMA frase o que ele faz e o que vai mudar na tela do app. Depois de cada passo, diga o que ele deve ver (ex.: "no GitHub, uma bolinha verde ao lado do arquivo significa que deu certo").
- Ações pequenas e numeradas, com link direto. Nunca peça para ele procurar pasta ou trecho. Peça sempre o arquivo INTEIRO (Ctrl+A, Ctrl+C).
- Antes de ele colar um código, lembre-o de conferir o nome do arquivo na barra do endereço do GitHub (já aconteceu de colar no arquivo errado).
- Faça UMA pergunta por vez. Quando der, ofereça respostas numeradas para ele responder só com o número.
- No teste no TX9, diga exatamente quais teclas apertar e o que deve aparecer. Ele responde "funcionou" ou "não funcionou" e conta o que viu.
- Se algo der errado, não culpe e não complique: diga o que mandar (print ou texto do erro do Actions) e conserte.
- Não despeje muita coisa de uma vez. Passos curtos. Se ele parecer perdido, repita de outro jeito, mais simples.
- Ele fala por voz (ditado): se uma frase não fizer sentido, pergunte.
- Tudo é feito pelo site do GitHub. Nada de terminal, zip ou programas. O APK sai do GitHub Actions e chega ao TX9 pela atualização automática do app.
- Nunca peça senha, chave de assinatura ou o link da lista M3U dele (pode ter senha).

## Links diretos
- ver ou editar: https://github.com/mundoarkade1-arch/asc-tvbox/edit/main/CAMINHO
- criar: https://github.com/mundoarkade1-arch/asc-tvbox/new/main/PASTA?filename=NOME
- Java: app/src/main/java/com/ascplay/tvbox/ ; layouts: app/src/main/res/layout/
- Mostrar um arquivo: abre o link, Ctrl+A, Ctrl+C, cola na conversa, sem Commit.
- Salvar: Ctrl+A, Delete, colar o código completo, Commit changes e, na janelinha, Commit changes de novo.
- Espere "feito" ou "verde" antes do próximo passo. Se ele não disser se o Actions ficou verde, pergunte.

## Regras de código
- ANTES de mexer em um arquivo de código, peça o atual e devolva o arquivo COMPLETO. Nunca reescreva de memória. O GitHub é a verdade; o STATUS pode estar errado.
- Mande UM arquivo por vez, com nome e link claros antes do bloco, e espere o "verde".
- Se uma colagem for cortada (erro "reached end of file while parsing"), peça só as últimas linhas e mande SÓ o pedaço que falta.
- Código que deixa de usar algo primeiro, layout depois. Classe nova primeiro, registro no AndroidManifest.xml depois, botões por último.
- Tela nova do servidor: crie tela irmã para a lista M3U.
- Não adicione dependências sem justificar em docs/DECISIONS.md. Não use nome de classe interna igual ao do Java (ex.: Entry).
- App leve: 1 GB de RAM. Peça só o que está na tela, nunca guarde listas inteiras; leia a lista M3U aos poucos.
- Tudo navegável só com setas, OK e Voltar (e modo mouse), com foco bem visível.
- Cada arquivo salvo na main gera um APK e uma versão nova (inclusive os .md). Não é erro.

## FIM DE CADA PASSO (obrigatório, sem o dono pedir)
Quando o dono disser que o passo ficou verde, entregue, nesta ordem, cada um com link e passo a passo para salvar:
1. docs/STATUS.md COMPLETO e CURTO (no máximo uns 80 linhas): fase atual, o que acabou de ser feito, o que falta, perguntas abertas. O que já foi feito vira UMA linha. Não copie para o STATUS o que já está no ARQUIVOS.md.
2. Trecho novo do docs/DECISIONS.md, só o que é novo (decisão e motivo, em linhas curtas). Para salvar: Ctrl+End, Enter duas vezes, colar.
3. docs/ARQUIVOS.md completo, SÓ se mudou arquivo, regra ou limite.
4. O PROMPT PARA O PRÓXIMO AGENTE (modelo abaixo), atualizado.
Atualize a data do STATUS. Se a conversa já teve muitos arquivos colados ou estiver longa, avise e faça isso JÁ, antes de o espaço acabar. Se o dono pedir "passagem", faça na hora.

## PROMPT PARA O PRÓXIMO AGENTE: como escrever
O prompt deve funcionar mesmo se os links não abrirem, e deixar o próximo agente 100% situado. Escreva em português simples, num bloco de código para copiar, com ESTAS seções, sempre completas e atualizadas:
1. Links dos arquivos a ler (AGENTS.md primeiro, depois os da pasta docs) e o aviso de que o GitHub demora uns minutos para mostrar a versão nova; se algo não abrir, o dono cola o texto.
2. O projeto em detalhe: o que é, por que existe, o aparelho, o controle, o que o app faz hoje.
3. Histórico: o que já foi feito, por fases, e o que já foi testado ou não no TX9.
4. Onde estamos agora: a etapa atual, o que está feito e VERDE, o que está em andamento (nome dos arquivos) e o que falta.
5. Pedidos do dono ainda não feitos, com as palavras dele, e as perguntas em aberto.
6. Como me ajudar: o dono é muito leigo (resuma a seção "COMO FALAR COM O DONO" e o fluxo de links, ver, salvar e esperar "verde").
7. Lições aprendidas e regras técnicas importantes.
8. A entrega obrigatória no fim de cada passo (STATUS curto, trecho do DECISIONS, ARQUIVOS se mudou, novo prompt).
9. Primeiras tarefas do próximo agente, em ordem.
Mantenha o mesmo nível de detalhe a cada passagem. O prompt NÃO deve repetir listas longas que já estão no ARQUIVOS.md ou no DECISIONS.md: resuma e aponte o arquivo.
