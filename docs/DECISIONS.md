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

## 2026-10-24: Prévia do EPG (caixinha na página inicial e barra no player)

- Decisão: a caixinha de prévia (programa de agora, barra de progresso, horários e "A seguir") aparece só na página inicial, numa faixa fixa embaixo da tela. No "Ver mais" fica apenas a linha "Agora: ..." em cada canal.
- Motivo: o dono achou a caixinha desnecessária no "Ver mais", porque a linha "Agora" já mostra a programação. Menos código e menos pedidos ao servidor no aparelho fraco.
- Decisão: a caixinha espera 0,5 segundo com o foco parado no canal antes de pedir o guia ao servidor.
- Motivo: evita pedidos à toa quando o dono passa rápido pelos canais. Economiza rede e memória no TX9 (1 GB de RAM).
- Decisão: no player, a barra Agora / A seguir aparece só em canais ao vivo. Ela surge quando o canal começa a tocar, some depois de 6 segundos e volta ao apertar OK. A mensagem de status ("Carregando...") passou para o alto da tela para não ficar sobre a barra.
- Motivo: o controle remoto não tem tecla de informação, então o OK é o único jeito de chamar a barra. Filmes não têm guia de programação.
- Dependências: nenhuma nova foi adicionada. Os horários vêm em milissegundos do XtreamApi.Epg e a barra de progresso usa só o ProgressBar do Android.

## 2026-10-24: Controle parental

- Decisão: senha de 4 números, igual ao app original. Fica guardada só como "resumo embaralhado" (PBKDF2 com sal), nunca como texto, num arquivo próprio (ParentalStore.java), separado do login.
- Motivo: se ficasse junto do login, o botão "Trocar login" apagaria a senha. Separada, o controle parental continua ligado ao trocar de login.
- Decisão: usar PBKDF2WithHmacSHA1 com 10 mil repetições.
- Motivo: o Android 7.1 (API 25) do TX9 não tem a versão SHA-256 do PBKDF2 (ela só existe a partir do Android 8). 10 mil repetições mantêm a conferência rápida num aparelho fraco, sem travar a tela.
- Limite conhecido: uma senha de 4 números é proteção para crianças, não segurança forte. Quem tiver acesso aos arquivos do aparelho conseguiria descobrir a senha testando as 10 mil combinações. Por isso a trava de tentativas (abaixo) é a defesa principal.
- Decisão: depois de 5 senhas erradas seguidas, o app trava por um tempo (30 segundos, dobrando a cada erro seguinte, no máximo 1 hora). A contagem é zerada ao acertar.
- Decisão: a senha é digitada num teclado de números desenhado na tela (PinDialog.java), escolhido com setas e OK. Ele avisa o app assim que o 4º número é digitado, sem botão de confirmar.
- Motivo: o controle remoto do TX9 não tem teclas de número (ver docs/TARGET_DEVICES.md). O teclado é montado por código, sem arquivo de layout, e é usado em todos os lugares: criar, trocar, desativar e abrir categoria adulta.
- Decisão: o reconhecimento de conteúdo adulto é só pelo nome, com as mesmas palavras do app original: uma lista mais ampla para nomes de categoria e uma lista mais curta para títulos de canal, filme e série (para evitar falsos alarmes).
- Limite conhecido: conteúdo adulto com nome comum pode passar, e categorias adultas com nome normal não ganham cadeado. Para consertar isso seria preciso o dono marcar categorias à mão (possível melhoria futura).
- Decisão: no "Ver mais", categorias adultas ficam com cadeado e pedem a senha. A liberação vale só para aquela categoria, enquanto a pessoa está nela. Ao trocar de categoria ou sair da tela, tranca de novo. Em "Todas as categorias", itens adultos não aparecem.
- Motivo: é o mesmo comportamento do app original e evita deixar a senha "aberta" por esquecimento.
- Decisão: na página inicial, os itens adultos são tirados depois de buscar os 10 primeiros de cada faixa, então uma faixa pode mostrar menos de 10 cartões.
- Motivo: evita pedir mais itens ao servidor só para completar a faixa. Menos rede e memória no TX9 (1 GB de RAM). Já no "Ver mais", se uma página inteira for escondida, o app busca a próxima sozinho.
- Dependências: nenhuma nova foi adicionada. Tudo usa classes que já vêm no Android.

## 2026-10-24: Episódios de séries (parte de dados)

- Decisão: os episódios são pedidos ao servidor só quando a pessoa abre uma série (get_series_info, UMA série por vez). Nunca se baixa a lista de todos os episódios de todas as séries.
- Motivo: o TX9 tem só 1 GB de RAM. A resposta é lida aos poucos (streaming) e a leitura para ao chegar em 500 episódios por série.
- Decisão: episódio é um tipo à parte (XtreamApi.Kind.EPISODE e a classe XtreamApi.Episode), que só serve para tocar e não aparece em listas. O endereço de reprodução é /series/usuario/senha/id.extensão.
- Decisão (combinada com o dono): a série abre numa tela própria, com temporadas à esquerda e episódios à direita. Ao terminar um episódio, o próximo da mesma temporada toca sozinho.
- Fora desta etapa: lembrar onde a pessoa parou, capa e sinopse da série. Podem entrar depois, se o dono pedir.
- Dependências: nenhuma nova foi adicionada.

## 2026-10-24: Episódios de séries (tela da série e próximo episódio automático)

- Decisão: ao escolher uma série, abre a SeriesActivity. À esquerda ficam as temporadas, à direita os episódios da temporada escolhida. Ao mexer nas temporadas com as setas, a lista de episódios troca sozinha. OK numa temporada leva o foco para os episódios. OK num episódio abre o player.
- Decisão: os episódios são pedidos ao servidor só quando a pessoa abre a série (get_series_info), no máximo 500, e ficam em ordem de temporada e de número do episódio. Temporada 0 aparece como "Especiais".
- Decisão: a tela da série manda ao player a lista dos episódios da MESMA temporada (números de identificação e extensões) e a posição do episódio escolhido. O player confere tudo (só números e letras, no máximo 500). Se algo vier errado, toca só o episódio escolhido e não passa para o próximo.
- Decisão: quando um episódio termina, o player toca sozinho o próximo da mesma temporada, sem a barra Agora / A seguir. No último episódio da temporada, mostra "Fim da temporada." e para. Não passa sozinho para a temporada seguinte.
- Decisão: a SeriesActivity não pede a senha do controle parental de novo. Ela só abre a partir de itens que já passaram pelo filtro (faixas da página inicial e "Ver mais"), que escondem séries com nome adulto e trancam categorias adultas.
- Motivo: manter a navegação simples só com setas, OK e Voltar, e gastar pouca memória (só os episódios de uma série por vez, sem capa nem sinopse por enquanto).
- Dependências: nenhuma nova.

- ## 2026-10-24: Página inicial para login por lista M3U

- Decisão: a lista M3U é lida AOS POUCOS (linha por linha, direto da internet) pelo novo M3uApi.java, e a leitura PARA assim que acha 10 canais, 10 filmes e 10 séries. A lista inteira nunca fica na memória.
- Motivo: o app original lia a lista inteira de uma vez (até 250 MB), o que não cabe no TX9 (1 GB de RAM).
- Limites de segurança da leitura: para depois de 200 MB lidos ou de 1 minuto, e mostra o que já achou. Se não achou nada, mostra o aviso de erro.
- Decisão: a separação em canal, filme e série usa as mesmas regras do app original: primeiro o caminho do endereço (/live/, /movie/, /series/), depois o tipo de arquivo (.m3u8 e .ts são canal; .mp4, .mkv e parecidos são filme, ou série se o nome parece episódio, como "S01E03"), depois palavras na categoria e no título.
- Decisão: séries da lista M3U são agrupadas pelo título (sem "S01E03"), então a faixa de séries mostra uma série por cartão, e não um cartão por episódio.
- Decisão: o item do app (XtreamApi.Item) ganhou o campo "url" (endereço direto do vídeo). Itens do servidor Xtream deixam vazio e o endereço continua sendo montado como antes. Itens da lista M3U já trazem o endereço pronto, e XtreamApi.streamUrl devolve ele como está.
- Decisão: o player (MainActivity) aceita o endereço direto pela tela anterior (EXTRA_URL) no login por lista M3U, e só toca se o endereço começar com http ou https. O endereço nunca é mostrado nem registrado em log (costuma conter usuário e senha).
- Decisão: no canal ao vivo da lista M3U, o player NÃO tenta trocar .m3u8 por .ts sozinho (o endereço é o que veio na lista). Esse truque só vale para o servidor Xtream.
- Decisão: controle parental também vale na lista M3U: com ele ligado, a leitura já pula itens adultos (pela categoria "group-title" e pelo nome), usando as mesmas palavras do ParentalStore.
- Dependências: nenhuma nova.
- Fora desta etapa (de propósito): "Ver mais" por categorias, episódios das séries e guia de programação (EPG) da lista M3U. Cada um chega numa etapa própria. Hoje, tocar numa série da lista M3U mostra um aviso.
- Teste no TX9: as 3 faixas aparecem, canal e filme tocam, o aviso da série aparece e o controle parental esconde o conteúdo adulto.
- Fora desta etapa (de propósito): lembrar onde a pessoa parou, capa e sinopse dos episódios.
- Teste no TX9: série abre pela página inicial e pelo "Ver mais", temporadas e episódios funcionam com as setas, o episódio toca e o próximo começa sozinho.
