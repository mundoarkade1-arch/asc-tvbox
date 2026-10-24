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

## 2026-10-24: Como o APK é gerado (workflow)

- O workflow .github/workflows/apk.yml roda a cada alteração na branch main (e manualmente). Ele instala Java 17 e Gradle 8.9, compila com "gradle assembleRelease", alinha e assina o APK com apksigner usando os Secrets, e publica em Releases (tag v<número da execução>) como ASC-TVBox.apk.
- Plugin do Android Gradle: versão 8.5.2.
- Sem os Secrets, o workflow gera um APK de teste com chave temporária e não publica em Releases.
- versionCode e versionName vêm das variáveis VERSION_CODE e VERSION_NAME (número da execução do workflow), lidas em variáveis simples no app/build.gradle (a primeira forma, direto dentro de defaultConfig, dava erro "Value is null").

## 2026-10-24: Tela de login e guarda das credenciais

- Duas formas de entrar na mesma tela: "Servidor e login" (DNS/Xtream Codes) e "Lista M3U" (link).
- CredentialStore.java guarda o login no armazenamento privado do app (SharedPreferences privado), criptografado com AES-GCM e chave do Android Keystore (Android 6.0+). Se o Keystore falhar no aparelho (comum em TV box barato), guarda em Base64 no mesmo armazenamento privado, em vez de travar. Se não conseguir ler, apaga e pede o login de novo. Nenhuma biblioteca extra.
- LoginChecker.java confere o login antes de salvar. Xtream: consulta player_api.php e exige user_info.auth = 1, status Active e exp_date no futuro. M3U: lê só os primeiros 4 KB e confere se começa com #EXTM3U ou #EXTINF (não baixa a lista inteira). Usa HttpURLConnection (sem OkHttp), segue redirecionamentos, timeout de 15 s e não escreve senhas nem endereços em log.
- Resposta 4xx do servidor é tratada como login recusado ("Usuário ou senha incorretos"). Servidores que respondem 401 sem cabeçalho de autenticação geram IOException no Android ("authentication challenge"); isso também é tratado como login recusado.
- Interface da tela de login: sem bibliotecas de UI, foco visível por selector (borda branca de 4 dp), tudo navegável só com setas e OK. Testado no TX9.
- Por enquanto o app sempre abre pela tela de login e, após sucesso, abre a MainActivity (que ainda toca só um stream HLS de teste). Quando existir a página inicial, o app deve pular o login se houver credenciais salvas.

## 2026-10-24: Atualização automática pelo app (em andamento)

- Motivo: o dono não usa computador para instalar; passar APK por pen drive a cada versão é inviável.
- Como funciona: o workflow publica, junto do APK, um version.json com {code, name, url, sha256}. O app consulta https://github.com/mundoarkade1-arch/asc-tvbox/releases/latest/download/version.json ao abrir, compara "code" com o versionCode instalado e, se for maior, mostra aviso com Atualizar/Depois. Ao aceitar, baixa o APK para o cache privado, confere o SHA-256 e abre o instalador do Android via FileProvider.
- Segurança: só aceita URL https que comece com https://github.com/mundoarkade1-arch/asc-tvbox/releases/download/. O Android ainda confere a assinatura (chave diferente = recusado).
- Limites do Android: o usuário precisa confirmar a instalação com o controle (sem root não dá para instalar sozinho). A primeira versão com o atualizador ainda é instalada por pen drive.
- Única dependência nova: androidx.core:core:1.13.1 (FileProvider). Justificativa: é a forma oficial e compatível com Android 7.1 de entregar o APK ao instalador.
- Ler o versionCode instalado com PackageManager (o BuildConfig não é gerado por padrão no Android Gradle Plugin 8).
