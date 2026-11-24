# Testes de segurança

## Executados (23/11/2026)
| Teste | Resultado |
|---|---|
| Leitura do código de todas as classes sensíveis | Feito |
| grep: Log, WebView, TrustManager, exec, segredos | Nada encontrado |
| Diff das alterações contra o original | Só Updater.java e DeviceLink.java mudaram |
| Supabase: leitura de tabelas, colunas, policies, grants e funções | Feito (somente leitura) |
| Supabase pós-correção: register_device("XX","curto") | "bad" (aprovado) |
| Supabase pós-correção: authenticated tem TRUNCATE em devices | false (aprovado) |
| Supabase pós-correção: anon executa bump_version | false (aprovado) |
| Supabase pós-correção: anon executa register_device / fetch_playlist | true / true (aprovado, o app precisa) |

## NÃO VERIFICADO
- Compilação do app (GitHub Actions deve ficar VERDE)
- V-01 em aparelho real
- Limite de 300/hora em carga real (resposta "busy")
- Cadastro de usuários desligado no Supabase (V-12)
- Ferramentas automáticas (Semgrep, Gitleaks, Trivy, CodeQL)

## Roteiro de teste manual no TX9
1. Instalar a versão nova e abrir o app: entra normal, login salvo.
2. Publicar uma versão mais nova e abrir o app: aparece o aviso de atualização.
3. Aceitar: baixa e abre a confirmação do Android; confirmar e o app atualiza.
4. Reabrir: o login continua salvo.
Se a confirmação NÃO abrir, avisar: o V-01 precisa de ajuste.
