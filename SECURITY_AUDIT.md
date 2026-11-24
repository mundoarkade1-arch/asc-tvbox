# Auditoria de segurança (23/11/2026)

Método: leitura manual do código + varredura por padrões (grep). Ferramentas automáticas (Semgrep, Gitleaks, Trivy, CodeQL) NÃO foram executadas. Histórico do Git NÃO foi analisado (o zip não o continha).

## Inventário
App Android nativo (Java, ExoPlayer), 61 classes, 11 activities (só LoginActivity exportada), sem serviços, sem WebView, sem backend no repositório. Dependências: media3 1.4.1, androidx.core 1.13.1. Serviços externos: servidor IPTV do usuário, Supabase (DeviceLink), GitHub Releases/Actions.

## Achados
| ID | Gravidade | Status | Descrição |
|---|---|---|---|
| V-01 | Média | CORRIGIDA no código (teste real pendente) | Receiver INSTALL_RESULT exportado em Android < 13 permitia broadcast forjado com intent embutido (Updater.java) |
| V-02 | Baixa | Aceita | usesCleartextTraffic=true (necessário para servidores IPTV em HTTP) |
| V-03 | Baixa | Pendente | Android 5.x (API 21-22) guarda login só em Base64 (sem Keystore) |
| V-04 | Baixa | CORRIGIDA | DeviceLink mostrava o tamanho do usuário e da senha na tela |
| V-05 | Baixa | Pendente | CI: actions por tag (não por SHA) e contents:write no job todo |
| V-06 | Info | Pendente | minifyEnabled false |
| V-07 | Info | Aceita | PIN de 4 dígitos; PBKDF2-SHA1 10.000 iterações |
| V-08 | Info | Aceita | SHA-256 e APK vêm do mesmo release; a assinatura do Android é a defesa final |
| V-09 | Não verificado | Pendente | Backend Supabase (RPC register_device/fetch_playlist, RLS, GRANTs) fora do repositório |

## Pontos positivos verificados
Sem bypass de TLS; sem Log.*; sem WebView; sem segredos no código/docs/CI; allowBackup=false; FileProvider não exportado; limites de tamanho e tempo em todas as leituras de rede; PIN exigido para trocar/desativar o controle parental.

## Correções feitas
- Updater.java: receiver não exportado (ContextCompat, RECEIVER_NOT_EXPORTED), confere o EXTRA_SESSION_ID da sessão, e recusa intent que aponte para o próprio app.
- DeviceLink.java: mensagem de diagnóstico sem tamanhos de usuário/senha.

## Segredos a rotacionar
Nenhum encontrado. Se a chave de assinatura (.jks) já esteve em algum commit público, ela precisa ser trocada.
