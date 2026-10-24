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

- Decisão: o app confere o arquivo version.json da última versão em Releases (campos: code, name, url, sha256). Se o code for maior que o instalado, mostra a faixa "Atualizar / Depois" na tela de login.
- Segurança: só usa https; só aceita baixar APK de https://github.com/mundoarkade1-arch/asc-tvbox/releases/download/; confere o SHA-256 do arquivo baixado e apaga se não bater; o Android ainda confere a assinatura antes de instalar. Falhas na verificação ficam em silêncio (sem internet, o app segue normal).
- Versão instalada: lida com PackageManager, porque o BuildConfig não é gerado neste projeto.
- Motivo: o TV box não tem Play Store garantida, e o dono instala por pen drive. Com o atualizador, só a primeira instalação é manual.
- Dependência nova: androidx.core:core:1.13.1, usada só para o FileProvider (entregar o APK baixado ao instalador do Android). É a forma oficial e segura; fazer isso à mão exigiria expor o arquivo por caminho direto, o que o Android 7 em diante bloqueia.

## 2026-10-24: Login e mensagens de erro

- Decisão: a tela de login confere o login de verdade antes de entrar (Xtream: player_api.php; M3U: o início do arquivo precisa ser #EXTM3U). Mostra mensagens separadas para: campos vazios, servidor inválido, sem conexão, usuário/senha errados, conta vencida e lista que não é M3U.
- Motivo: no Android, um 401 sem pedido de autenticação vira erro de rede. Por isso o app trata esse caso como "usuário ou senha incorretos" (AuthRefusedException), em vez de "sem conexão".
- A rede nunca roda na thread principal, e senhas e endereços nunca vão para log.
