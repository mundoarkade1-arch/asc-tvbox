# Auditoria de segurança (23/11/2026)

Método: leitura manual do código + varredura por padrões (grep) + leitura da estrutura do Supabase (consulta somente leitura feita pelo dono). Ferramentas automáticas (Semgrep, Gitleaks, Trivy, CodeQL) NÃO foram executadas. Histórico do Git NÃO foi analisado.

## Inventário
App Android nativo (Java, ExoPlayer), 61 classes, 11 activities (só LoginActivity exportada), sem serviços, sem WebView. Dependências: media3 1.4.1, androidx.core 1.13.1. Backend: Supabase, tabela public.devices (RLS ligado), funções register_device e fetch_playlist. Outros: servidor IPTV do usuário, GitHub Releases/Actions.

## Achados
| ID | Gravidade | Status | Descrição |
|---|---|---|---|
| V-01 | Média | CORRIGIDA no código (teste real pendente) | Receiver INSTALL_RESULT exportado em Android < 13 (Updater.java) |
| V-02 | Baixa | Aceita | usesCleartextTraffic=true (necessário para servidores IPTV em HTTP) |
| V-03 | Baixa | Pendente | Android 5.x (API 21-22) guarda login só em Base64 |
| V-04 | Baixa | CORRIGIDA | DeviceLink mostrava o tamanho do usuário e da senha |
| V-05 | Baixa | Pendente | CI: actions por tag (não SHA) e contents:write no job todo |
| V-06 | Info | Pendente | minifyEnabled false |
| V-07 | Info | Aceita | PIN de 4 dígitos; PBKDF2-SHA1 10.000 iterações |
| V-08 | Info | Aceita | SHA-256 e APK vêm do mesmo release; a assinatura do Android é a defesa final |
| V-09 | Média | CORRIGIDA e VERIFICADA | register_device sem limite (encher o banco) e sondagem de códigos |
| V-10 | Baixa | CORRIGIDA e VERIFICADA | authenticated tinha TRUNCATE/REFERENCES/TRIGGER em devices |
| V-11 | Info | CORRIGIDA e VERIFICADA | rls_auto_enable e bump_version abertas a PUBLIC |
| V-12 | Baixa | PENDENTE (ação manual) | Regra do dono depende do e-mail: desligar "Allow new users to sign up" |
| V-13 | Info | Aceita | Senha das listas em texto puro na coluna password (o app precisa dela); só o admin lê |

## Backend verificado
- RLS ligado em devices; única policy: owner_all (só o e-mail do dono, role authenticated). Sem acesso direto de anon.
- fetch_playlist exige código + chave secreta (hash SHA-256 da chave de 256 bits) e responde igual para código inexistente e chave errada.
- Ninguém sem login de admin consegue gravar ou trocar a lista de um aparelho.

## Segredos a rotacionar
Nenhum encontrado. Se a chave de assinatura (.jks) já esteve em algum commit público, troque-a.

## Riscos residuais
Conta do administrador (e-mail do dono) controla todas as listas: usar senha forte e MFA. Cadastro de novos usuários no Supabase precisa estar desligado (V-12). Compilação e teste do V-01 em aparelho real pendentes.
