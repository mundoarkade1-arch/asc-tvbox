# App ASC original

## O que ele faz
App de IPTV. Reproduz canais, filmes e séries a partir de uma lista M3U ou de login por DNS (servidor, usuário e senha). Foco em ser organizado, seguro e leve.

## Código do app original
- Repositório: https://github.com/mundoarkade1-arch/asc-play
- Tecnologia: Capacitor (site HTML/JS empacotado como app Android)
- App web em `www/` (index.html, app.js, hls.min.js)
- Lógica de comportamento: `www/app.js`

## Por que não roda no TV box TX9
O WebView do TX9 é o Chrome 52 (2016) e o app dá "erro de sintaxe" ao iniciar. O app usa JavaScript moderno que esse navegador não entende.

## Como o app original faz cada coisa (resumo do app.js)

### Login e listas
- Três jeitos de adicionar uma lista: servidor + usuário + senha, link M3U, ou arquivo/texto colado.
- Servidor com login (Xtream): baixa a lista inteira por `servidor/get.php?username=...&password=...&type=m3u_plus&output=m3u8`.
- Aceita várias listas salvas (trocar, renomear, atualizar, excluir). Tamanho máximo de uma lista: cerca de 250 MB.
- Tudo que é sensível (listas, logins, favoritos) fica criptografado no aparelho.
- Ao abrir, se já existe uma lista salva, pula o login e entra direto.

### Leitura da lista M3U
- Lê de cada item: nome, logo (tvg-logo), categoria (group-title), id do guia (tvg-id), sinopse e o endereço do vídeo.
- Descobre se o item é canal, filme ou série: primeiro pelo endereço (/live/, /movie/, /series/), depois pela extensão do arquivo (.mp4, .mkv etc. = filme ou série; .m3u8 ou .ts = canal) e por último por palavras na categoria e no nome (ex.: "S01E02", "temporada" = série).
- Séries: os episódios são agrupados pelo título (tirando "S01E02" do nome) e organizados por temporada.
- Guarda o "url-tvg" da lista, se houver, para usar como guia (EPG).

### Página inicial
- Faixas horizontais: Favoritos, Ao vivo, Filmes e Séries, com os 10 primeiros de cada e um botão "Ver mais".
- "Ao vivo" mostra primeiro os canais da categoria "Canal do cliente" (a categoria com "cliente" no nome, ou jogos/eventos/futebol/PPV), e completa até 10 com os outros canais. A categoria pode ser escolhida nos Ajustes.
- Abas: Início, Canais, Filmes, Séries, Favoritos. Em cada aba há seletor de categoria (com a quantidade de itens) e busca por nome.

### EPG (guia de programação)
- Endereço do guia, em ordem de prioridade: link informado pelo usuário, depois `servidor/xmltv.php?username=...&password=...`, depois o url-tvg da lista.
- Lê o guia (XMLTV, pode vir compactado em .gz) e guarda só os programas de agora até 12 horas à frente.
- Liga o canal ao guia por: id do guia (tvg-id), depois pelo nome do canal, depois pelo nome simplificado (sem HD, FHD, 4K etc.).
- Prévia nos canais: cada cartão de canal mostra o programa de agora. Ao focar o canal, aparece um balão com programa atual, barra de progresso, horário e "A seguir".
- No player, uma barra mostra "Agora" e "A seguir" e atualiza a cada 30 segundos. O guia pode ser atualizado à mão nos Ajustes.

### Controle parental
- Senha de 4 números (guardada com hash, não em texto).
- Detecta categorias e itens adultos por palavras no nome da categoria e do item.
- Com o controle ativo: itens adultos não aparecem na busca, no início nem nos favoritos. Só aparecem dentro da categoria adulta, depois de digitar a senha.
- Após 5 senhas erradas, bloqueia por um tempo que vai aumentando (até 1 hora).
- Ativar, trocar e desativar pedem a senha atual.

### Player
- Canais ao vivo: troca para o canal anterior/seguinte pelas setas; se o endereço do canal for .ts, tenta .m3u8.
- Filmes e séries: guarda onde parou ("Continuar de 12:34" ou "Assistir do começo"), marca como assistido ao passar de 92%, e na série indica o próximo episódio.
- Vigia a imagem: se travar, mostra o círculo de carregando e tenta voltar sozinho (no ao vivo, sem desistir).
- Detalhes de filme e série (sinopse, ano, nota) vêm da API do servidor (player_api.php), quando há login.

### Outros
- Favoritos de canais, filmes e séries.
- Aviso de vencimento da lista (7 dias antes, em banner e notificação); o Xtream informa a data, a lista M3U pede a data digitada.
- Bloqueio do app por PIN de 4 a 6 dígitos.
- Atualização automática pelo version.json em Releases.
- Ajustes: gerenciar listas, vencimento, PIN, controle parental, atualização, limpar favoritos, apagar tudo, sobre/versão, modo TV ou celular, canal do cliente, bloquear capturas de tela.

## O que precisa existir no app novo
- Tudo da lista "Funções principais": login por M3U ou DNS, página inicial com 10 itens por categoria, EPG com prévia nos canais, controle parental
- Funcionar no Android 7.1 com 1 GB de RAM (o original carrega a lista inteira na memória; o novo precisa ser mais econômico)

## O que pode ficar de fora (a confirmar com o dono)
- Modo celular (o app novo é só para TV box)
- Bloqueio de capturas de tela
- Notificações de vencimento (o aviso na tela basta)
- Bloqueio do app por PIN (a decidir mais tarde)
