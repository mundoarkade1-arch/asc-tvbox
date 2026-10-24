# Decisões

Registre aqui cada decisão importante: o que foi decidido e por quê.

## 2026-10-24: App Android nativo (Java) com ExoPlayer, sem WebView

- Decisão: construir o app como Android nativo em Java, usando ExoPlayer para tocar os vídeos. Não usar Capacitor nem site dentro de WebView.
- Motivo: o TV box de teste (TX9, Android 7.1, 1 GB de RAM) tem WebView Chrome 52 (2016). O app original (asc-play, Capacitor) dá "erro de sintaxe" ao iniciar. O app nativo não depende do WebView, é mais leve e toca vídeo melhor em aparelho fraco.
- Alternativa descartada: reescrever o site em JavaScript antigo (ES5) e continuar com Capacitor. Descartada porque o WebView de 2016 é lento para vídeo, e as versões atuais do Capacitor exigem um WebView mais novo.
- Como gerar o APK: GitHub Actions, sem precisar instalar nada no computador (mesmo método do asc-play).
- Requisitos mínimos do app: Android 7.1 (API 25) ou menor, funcionar com 1 GB de RAM.

## 2026-10-24: Assinatura própria e versão automática

- Decisão: o APK é assinado com uma chave própria, guardada só nos Secrets do GitHub (KEYSTORE_B64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD). Nunca no repositório.
- Decisão: o número da versão (versionCode) é o número da execução do Actions, e o nome é 1.0.N. Assim cada publicação é sempre maior que a anterior.
- Motivo: o Android só instala uma atualização por cima se a assinatura for a mesma e o número da versão for maior. Sem isso, o usuário teria que desinstalar e perderia o login salvo.
- Se o Secret de chave não existir, o workflow gera um APK só de teste e NÃO publica em Releases.

## 2026-10-24: Atualização automática pelo GitHub Releases

- Decisão: o app confere o arquivo version.json da última versão em Releases (campos: code, name, url, sha256). Se o code for maior que o instalado, mostra a faixa "Atualizar / Depois" na tela de login e na página inicial.
- Segurança: só usa https; só aceita baixar APK de https://github.com/mundoarkade1-arch/asc-tvbox/releases/download/; confere o SHA-256 do arquivo baixado e apaga se não bater; o Android ainda confere a assinatura antes de instalar. Falhas na verificação ficam em silêncio (sem internet, o app segue normal).
- Versão instalada: lida com PackageManager, porque o BuildConfig não é gerado neste projeto.
- Motivo: o TV box não tem Play Store garantida, e o dono instala por pen drive. Com o atualizador, só a primeira instalação é manual.
- Dependência nova: androidx.core:core:1.13.1, usada só para o FileProvider (entregar o APK baixado ao instalador do Android). É a forma oficial e segura; fazer isso à mão exigiria expor o arquivo por caminho direto, o que o Android 7 em diante bloqueia.

## 2026-10-24: Login e mensagens de erro

- Decisão: a tela de login confere o login de verdade antes de entrar (Xtream: player_api.php; M3U: o início do arquivo precisa ser #EXTM3U). Mostra mensagens separadas para: campos vazios, servidor inválido, sem conexão, usuário/senha errados, conta vencida e lista que não é M3U.
- Motivo: no Android, um 401 sem pedido de autenticação vira erro de rede. Por isso o app trata esse caso como "usuário ou senha incorretos" (AuthRefusedException), em vez de "sem conexão".
- A rede nunca roda na thread principal, e senhas e endereços nunca vão para log.

## 2026-10-24: Página inicial sem baixar a lista inteira

- Decisão: com login por servidor (Xtream), o app NUNCA baixa a lista completa. Ele pede à API do servidor (player_api.php) e lê a resposta aos poucos (JsonReader), parando assim que junta os 10 itens da página inicial. Há limite de 80 MB por resposta e IDs só podem ser números.
- Motivo: o app original baixa a lista M3U inteira e a mantém na memória (até 250 MB). No TX9, com 1 GB de RAM, isso estoura. A leitura parcial usa pouca memória, qualquer que seja o tamanho da lista.
- Seções da página inicial: Canais ao vivo, Filmes e Séries, 10 itens cada, como no app original. As categorias são navegadas depois, em "Ver mais" (etapa seguinte).
- Lista M3U (sem servidor): a página inicial ainda não existe para esse tipo de login. Será feita numa etapa própria, lendo a lista aos poucos e sem guardá-la inteira na memória.
- Sem dependência nova: a leitura usa android.util.JsonReader, que já vem no Android.

## 2026-10-24: Logos com carregador próprio (ImageLoader)

- Decisão: um carregador de imagens simples, sem biblioteca externa: cache de cerca de 8 MB, imagem reduzida ao abrir (lado maior de no máximo 256 pixels), no máximo 2 downloads ao mesmo tempo, limite de 2 MB por imagem, só http e https, e endereços que falharam não são tentados de novo na sessão.
- Motivo: bibliotecas como Glide e Picasso são boas, mas trazem muito código e consomem mais memória do que o necessário para cartões pequenos. O AGENTS.md pede poucas dependências, e este carregador resolve com cerca de 200 linhas e controle total da memória.
- Se o logo falhar ou não existir, o cartão mostra a inicial do nome.

## 2026-10-24: Player de canais e filmes

- Decisão: o endereço de reprodução (que contém usuário e senha) é montado dentro do MainActivity, a partir do login salvo. Entre as telas viajam só o tipo, o número do item, a extensão e o nome.
- Canal ao vivo: tenta primeiro HLS (.m3u8); se o servidor não entregar, tenta uma vez o .ts. Filme: usa a extensão informada pelo servidor (ou mp4).
- Mensagens de erro do player mostram só o código do erro, porque a mensagem completa pode conter o endereço com a senha.
- Redirecionamento de http para https é permitido (alguns servidores fazem isso).

## 2026-10-24: Login salvo abre direto na página inicial

- Decisão: ao abrir o app, se há login salvo, ele vai direto para a página inicial. O botão "Trocar login" abre a tela de login com os dados antigos preenchidos, sem apagar nada.
- Motivo: no dia a dia, quem usa TV box quer ligar e assistir, sem digitar nada de novo.

## 2026-10-24: Navegação por categorias ("Ver mais") por páginas

- Decisão: tela BrowseActivity com categorias à esquerda e itens à direita. Os itens carregam por páginas de 40, quando o usuário chega perto do fim da grade, com máximo de 400 por categoria. Cada página é pedida ao servidor com um deslocamento (offset); os itens pulados são lidos e descartados sem ficar na memória.
- Motivo: uma categoria pode ter milhares de itens; no TX9 (1 GB de RAM) não dá para guardar tudo. O limite de 400 protege a memória; busca por nome vem em etapa futura.
- Sem dependência nova: usa ListView e GridView do próprio Android, que reaproveitam as linhas ao rolar.
- Trocar de categoria rápido: respostas atrasadas são ignoradas (contador de geração).


## 2026-10-24: "Ver mais" com navegação por categorias, carregando por partes

- Decisão: a página inicial continua buscando só os 10 primeiros itens de cada faixa. O cartão "Ver mais ›" no fim de cada faixa abre uma tela própria (BrowseActivity) para navegar por categorias, carregando os itens por páginas (a API ganhou o parâmetro de deslocamento, o "offset").
- Motivo: o TX9 tem só 1 GB de RAM. Carregar a lista inteira do servidor de uma vez pode travar o aparelho. Carregar por partes mantém o app leve.
- Nenhuma dependência nova foi adicionada nesta etapa.


## 2026-10-24: EPG por canal, sob demanda, sem baixar o guia inteiro

- Decisão: o guia de programação (EPG) é pedido ao servidor canal por canal (ação get_short_epg da API Xtream, até 4 programas), só para os canais que aparecem na tela. Os resultados ficam num cache pequeno na memória (EpgCache): no máximo 80 canais, cada um por 1 minuto, e no máximo 2 pedidos ao servidor ao mesmo tempo.
- Motivo: o app original baixa o guia completo (XMLTV) e guarda tudo na memória. No TX9, com 1 GB de RAM, esse arquivo grande pode travar o aparelho. Pedir só o que está na tela mantém o app leve.
- Alternativa descartada: baixar e ler o XMLTV inteiro. Pode voltar a ser considerada para login por lista M3U, que não tem a API do Xtream.
- Canal sem guia mostra "Programação indisponível", sem erro.
- Nenhuma dependência nova foi adicionada.
