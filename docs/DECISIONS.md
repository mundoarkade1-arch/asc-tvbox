# Decisões

Registre aqui cada decisão importante: o que foi decidido e por quê.

## 2026-10-24: App Android nativo (Java) com ExoPlayer, sem WebView

- Decisão: construir o app como Android nativo em Java, usando ExoPlayer para tocar os vídeos. Não usar Capacitor nem site dentro de WebView.
- Motivo: o TV box de teste (TX9, Android 7.1, 1 GB de RAM) tem WebView Chrome 52 (2016). O app original (asc-play, Capacitor) dá "erro de sintaxe" ao iniciar. O app nativo não depende do WebView, é mais leve e toca vídeo melhor em aparelho fraco.
- Alternativa descartada: reescrever o site em JavaScript antigo (ES5) e continuar com Capacitor. Descartada porque o WebView de 2016 é lento para vídeo, e as versões atuais do Capacitor exigem um WebView mais novo.
- Como gerar o APK: GitHub Actions, sem precisar instalar nada no computador (mesmo método do asc-play).
- Requisitos mínimos do app: Android 7.1 (API 25) ou menor, funcionar com 1 GB de RAM.
