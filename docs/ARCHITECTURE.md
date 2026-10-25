# Arquitetura

## Tecnologia
- Android nativo em Java 8, sem WebView. Pacote com.ascplay.tvbox. minSdk 21, compileSdk e targetSdk 34. Sem minificação.
- Dependências (app/build.gradle): androidx.media3 exoplayer, exoplayer-hls e ui (versão 1.4.1) e androidx.core 1.13.1. Não tem o módulo DASH: só HLS e arquivos diretos. Qualquer dependência nova precisa de justificativa em DECISIONS.md.
- Telas em Activities, listas em ListView, imagens pelo ImageLoader próprio, rede com HttpURLConnection. Sem bibliotecas de terceiros além das acima.

## Como o APK é gerado
- Arquivo .github/workflows/apk.yml ("Gerar APK"). Roda a cada mudança na branch main (qualquer arquivo, inclusive .md) e também à mão (workflow_dispatch).
- Gradle 8.9 e Java 17 no Actions. VERSION_CODE = número da execução (github.run_number) e VERSION_NAME = 1.0.N. O app/build.gradle lê essas duas variáveis.
- O APK é assinado com a chave guardada nos Secrets do GitHub (KEYSTORE_B64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD). NUNCA pedir esses valores ao dono. Sem a chave nos Secrets, o APK é só de teste e NÃO é publicado.
- Com a chave, o Actions cria uma Release "vN" com ASC-TVBOX.apk (ASC-TVBox.apk) e version.json (code, name, url, sha256).

## Atualização automática
- Updater.java confere a versão publicada em Releases e, se for maior que a instalada, oferece instalar (aviso na página inicial e opção em Ajustes). Confira em Updater.java antes de mudar qualquer coisa nessa parte.

## Mapa dos arquivos e regras
Veja docs/ARQUIVOS.md.
