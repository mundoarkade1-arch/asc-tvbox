# Testes de segurança

## Executados (23/11/2026)
| Teste | Resultado |
|---|---|
| Leitura do código de todas as classes sensíveis | Feito |
| grep: Log, WebView, TrustManager, exec, segredos | Nada encontrado |
| Diff das alterações contra o original | Só Updater.java e DeviceLink.java mudaram |

## NÃO VERIFICADO (sem Android SDK nem rede no ambiente da auditoria)
- Compilação do app (o GitHub Actions compila ao subir; precisa ficar VERDE)
- V-01 em aparelho real
- Todos os testes contra o Supabase (V-09)
- Ferramentas automáticas (Semgrep, Gitleaks, Trivy, CodeQL)

## Roteiro de teste manual no TX9 (para o dono)
1. Instalar a versão nova e abrir o app. Deve entrar normal.
2. Publicar uma versão ainda mais nova e abrir o app: deve aparecer o aviso de atualização.
3. Aceitar: deve baixar e abrir a tela de confirmação do Android; confirmar e o app atualiza.
4. Entrar de novo e conferir que o login continua salvo.
Se a atualização NÃO abrir a confirmação, avise: a correção V-01 precisa de ajuste.
