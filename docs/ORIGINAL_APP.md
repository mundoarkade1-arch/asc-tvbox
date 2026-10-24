# App ASC original

## O que ele faz
App de IPTV. Reproduz canais, filmes e séries a partir de uma lista M3U ou de login por DNS (servidor, usuário e senha). Foco em ser organizado, seguro e leve.

## Funções principais
- Login por lista M3U ou por DNS com usuário e senha
- Página inicial com os 10 primeiros canais, filmes e séries, divididos por categoria
- EPG (guia de programação) com prévia nos canais, para ver o que está passando sem abrir o canal
- Controle parental

## Código do app original
- Repositório: https://github.com/mundoarkade1-arch/asc-play
- Tecnologia: Capacitor (site HTML/JS empacotado como app Android)
- App web em `www/` (index.html, app.js, hls.min.js)
- APK gerado pelo GitHub Actions (`.github/workflows/apk.yml`)

## Por que não roda no TV box TX9
O WebView do TX9 é o Chrome 52 (2016) e o app dá "erro de sintaxe" ao iniciar. O app usa JavaScript moderno que esse navegador não entende.

## O que precisa existir no app novo
- Tudo da lista "Funções principais" acima
- Funcionar no Chrome 52 / Android 7.1 / 1 GB de RAM

## O que pode ficar de fora
(a definir)
