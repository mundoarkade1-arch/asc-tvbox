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

## Como trabalhar com o dono
- O dono é LEIGO. Português simples, frases curtas, sem termo técnico sem explicar. Ele fala por voz (ditado): se algo não fizer sentido, pergunte.
- Tudo é feito pelo site do GitHub. Nada de terminal, zip ou programas. O APK sai do GitHub Actions e chega ao TX9 pela atualização automática do app.
- UM PASSO POR VEZ, com ações pequenas numeradas. Para cada arquivo, dê um LINK DIRETO:
  - ver ou editar: https://github.com/mundoarkade1-arch/asc-tvbox/edit/main/CAMINHO
  - criar: https://github.com/mundoarkade1-arch/asc-tvbox/new/main/PASTA?filename=NOME
  - Java: app/src/main/java/com/ascplay/tvbox/ ; layouts: app/src/main/res/layout/
- Para mostrar um arquivo: abre o link, Ctrl+A, Ctrl+C, cola na conversa, sem Commit.
- Para salvar: Ctrl+A, Delete, colar o código completo, Commit changes e, na janelinha, Commit changes de novo.
- Espere "feito" ou "verde" antes do próximo passo. Se ele não disser se o Actions ficou verde, pergunte. Se der erro, ele manda print ou texto do Actions e você corrige.
- Não mande procurar trecho dentro de arquivo: ele prefere copiar e colar o arquivo INTEIRO. Sem blocos de exemplo que pareçam campos a preencher.
- Nunca peça senha, chave de assinatura ou o link da lista M3U dele (pode ter senha).

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
1. docs/STATUS.md COMPLETO e CURTO (no máximo uns 80 linhas). Conteúdo: fase atual, o que acabou de ser feito, o que falta, perguntas abertas. O que já foi feito vira UMA linha. Não copie para o STATUS o que já está no ARQUIVOS.md.
2. Trecho novo do docs/DECISIONS.md, só o que é novo (decisão, motivo, em linhas curtas). Para salvar: Ctrl+End, Enter duas vezes, colar.
3. docs/ARQUIVOS.md completo, SÓ se mudou arquivo, regra ou limite.
Atualize a data do STATUS. Se a conversa já teve muitos arquivos colados ou estiver longa, avise e faça isso JÁ, antes de o espaço acabar. Se o dono pedir "passagem", faça na hora.

## PROMPT PARA O PRÓXIMO AGENTE
O prompt é FIXO (está abaixo) porque o estado do projeto fica nos arquivos. Entregue o mesmo texto, sem aumentar. Só acrescente linhas se houver algo importante que NÃO está nos arquivos.
---
Leia e siga https://raw.githubusercontent.com/mundoarkade1-arch/asc-tvbox/main/AGENTS.md. Depois leia, nesta ordem, STATUS.md, ARQUIVOS.md e DECISIONS.md da pasta docs (mesmo endereço, trocando AGENTS.md por docs/NOME.md).
Se algum link não abrir ou o STATUS.md parecer antigo (deve ter a data da última passagem), me avise que eu colo o texto. O GitHub demora uns minutos para mostrar a versão nova.
Sou leigo. Siga o AGENTS.md: um passo por vez, arquivo atual antes de mexer, e STATUS e DECISIONS ao fim de cada passo.
Comece confirmando em 3 linhas o que entendeu e faça a primeira pergunta aberta do STATUS.md.
---
