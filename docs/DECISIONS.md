# Decisões

Registre aqui cada decisão importante: o que foi decidido e por quê.

## 2026-10-24: App Android nativo (Java) com ExoPlayer, sem WebView

- Decisão: construir o app como Android nativo em Java, usando ExoPlayer para tocar os vídeos. Não usar Capacitor nem site dentro de WebView.
- Motivo: o TV box de teste (TX9, Android 7.1, 1 GB de RAM) tem WebView Chrome 52 (2016). O app original (asc-play, Capacitor) dá "erro de sintaxe" ao iniciar. O app nativo não depende do WebView, é mais leve e toca vídeo melhor em aparelho fraco.
- Alternativa descartada: reescrever o site em JavaScript antigo (ES5) e continuar com Capacitor. Descartada porque o WebView de 2016 é lento para vídeo, e as versões atuais do Capacitor exigem um WebView mais novo.
- Como gerar o APK: GitHub Actions, sem precisar instalar nada no computador (mesmo método do asc-play).
- Requisitos mínimos do app: Android 7.1 (API 25) ou menor, funcionar com 1 GB de RAM.

## 2026-10-24: Chave de assinatura própria para o asc-tvbox

- Decisão: o asc-tvbox terá uma chave de assinatura NOVA, separada da do asc-play. Os Secrets do GitHub usam os mesmos nomes (KEYSTORE_B64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD), mas com valores novos.
- Motivo: são dois apps diferentes (com pacotes diferentes), e o dono quer mantê-los independentes.
- Regra: o arquivo da chave e as senhas ficam só no computador do dono e nos Secrets, nunca no repositório (o .gitignore bloqueia .jks, .keystore e similares).

## 2026-10-24: Configuração base do projeto Android

- Pacote: com.ascplay.tvbox. minSdk 21 (Android 5.0), compileSdk/targetSdk 34.
- Dependências (únicas): androidx.media3 versão 1.4.1, módulos exoplayer, exoplayer-hls e ui. Justificativa: é o ExoPlayer oficial do Android, necessário para tocar canais HLS (.m3u8) e vídeos, e funciona desde o Android 5.0.
- Tráfego HTTP sem criptografia liberado (usesCleartextTraffic=true): muitos servidores IPTV só oferecem http://. O app não guarda nada sensível fora do armazenamento privado.
- Tela fixa na horizontal e sem exigir tela de toque (uses-feature touchscreen required=false), pois o alvo é TV box com controle remoto. O app também aparece no menu de apps de TV (LEANBACK_LAUNCHER).
- Sem gradlew no repositório: o workflow do GitHub Actions instala o Gradle. Isso evita arquivos binários que o dono não conseguiria criar pelo navegador.
- Interface sem bibliotecas extras (só componentes nativos do Android) para economizar memória.
