# Status do projeto

Última atualização: 24/10/2026

## Fase atual
Etapa "Controle parental" concluída e testada no TX9. Próxima etapa: episódios de séries.

## Feito (testado no TX9)
- Repositório criado e documentação preenchida (README, AGENTS, CLAUDE, llms.txt, docs/*)
- Aparelho de teste identificado e documentado (TX9, S905W, Android 7.1, 1 GB RAM, tela 1080p)
- Decisão tomada: app Android nativo em Java com ExoPlayer, sem WebView (ver docs/DECISIONS.md)
- Esqueleto do app nativo e workflow do GitHub Actions que gera o APK
- Login por servidor (DNS, usuário e senha), com mensagens de erro corretas
- Atualização automática do app (aviso na página inicial, instalação pelo próprio app)
- Abrir direto na página inicial quando há login salvo
- Página inicial com 3 faixas (Canais ao vivo, Filmes, Séries) de 10 itens cada
- Tocar canal e filme
- "Ver mais": cartão no fim de cada faixa que abre a navegação por categorias (BrowseActivity), com borda vermelha de seleção
- Linha "Agora: ..." nos cartões de canais ao vivo, na página inicial e no "Ver mais"
- Caixinha de prévia na página inicial: ao parar o foco num canal ao vivo, uma faixa fixa embaixo mostra o programa de agora, a barra de progresso, os horários de início e fim e o "A seguir"
- Barra Agora / A seguir no player (MainActivity): aparece quando o canal ao vivo começa a tocar, some depois de 6 segundos e volta ao apertar OK; não aparece em filmes
- Controle parental:
  - Botão "Controle parental" na página inicial, com menu: ativar, trocar senha e desativar
  - Senha de 4 números, digitada num teclado de números na tela (setas e OK)
  - Com o controle ligado, canais, filmes e séries com nome adulto não aparecem nas faixas da página inicial
  - No "Ver mais", categorias adultas ficam com cadeado e pedem a senha; a liberação vale só enquanto a pessoa está naquela categoria
  - Em "Todas as categorias", conteúdo adulto não aparece com o controle ligado
  - Depois de 5 senhas erradas seguidas, o app trava por um tempo antes de aceitar nova tentativa

## Arquivos do EPG
- XtreamApi.java: função epg(...) que pede ao servidor a programação de UM canal (get_short_epg, até 4 programas) e devolve o programa de agora e o "a seguir". Horários em milissegundos.
- EpgCache.java: guarda na memória até 80 canais por 1 minuto cada; no máximo 2 pedidos ao servidor ao mesmo tempo; avisa sempre na thread principal
- item_card.xml: tem a linha tv_now (usada para o "Agora: ...")
- activity_home.xml: tem o botão "Controle parental" no alto e a caixinha de prévia fixa embaixo (group_preview)
- HomeActivity.java: liga a linha tv_now e a caixinha de prévia ao EpgCache (espera 0,5 segundo parado no canal antes de pedir o guia); tem o menu do controle parental e esconde itens adultos das faixas
- BrowseActivity.java: liga a linha tv_now ao EpgCache (sem caixinha de prévia, por decisão do dono); mostra o cadeado e pede a senha nas categorias adultas
- activity_main.xml: tem a barra Agora / A seguir (group_nowbar) e a mensagem de status no alto da tela
- MainActivity.java: mostra a barra Agora / A seguir nos canais ao vivo

## Arquivos do controle parental
- ParentalStore.java: liga e desliga o controle; guarda a senha embaralhada (PBKDF2 com sal) num arquivo próprio, separado do login (o botão "Trocar login" não apaga); confere a senha; trava depois de 5 erros; reconhece categorias e nomes adultos pelas mesmas palavras do app original
- PinDialog.java: teclado de números na tela (1 a 9, 0, Apagar e Sair); avisa o app quando o 4º número é digitado

## Próximos passos (nesta ordem)
1. Episódios de séries (hoje o toque numa série só mostra um aviso). Ao fazer, respeitar o controle parental.
2. Página inicial para login por lista M3U (hoje mostra um aviso de que chega numa etapa própria). Ao fazer, aplicar o controle parental também.

## Problemas em aberto
- Armazenamento livre do TX9 e acesso à Play Store: ainda não sabemos
- Lista M3U ainda não tem página inicial (ver passo 2 acima)
- Canais sem guia no servidor mostram "Programação indisponível" (comportamento esperado, não é erro)
- Controle parental: na página inicial, os itens adultos são tirados DEPOIS de buscar os 10 primeiros, então uma faixa pode mostrar menos de 10 cartões (comportamento esperado, não é erro)
- Controle parental: o reconhecimento de conteúdo adulto é só pelo nome (categoria ou título). Conteúdo adulto com nome comum pode passar; categorias com nome normal não ganham cadeado
