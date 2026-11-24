# Checklist de segurança

## Antes de publicar uma versão
- [ ] Nenhum segredo/senha/link M3U nos arquivos novos
- [ ] Secrets do GitHub preenchidos (senão o APK não é publicado)
- [ ] Teste de atualização automática no aparelho (Android 7.1 / TX9)

## Supabase
- [x] RLS ligado em todas as tabelas do schema public (só existe devices)
- [x] anon sem acesso direto à tabela; só EXECUTE em register_device e fetch_playlist
- [x] fetch_playlist exige código + chave secreta (hash), resposta igual para erro
- [x] Gravar lista exige login do administrador
- [x] Limite de 300 novos aparelhos por hora em register_device (supabase/hardening.sql)
- [ ] MANUAL: Authentication > Sign In / Providers > "Allow new users to sign up" DESLIGADO (V-12)
- [ ] MANUAL: MFA ligado na conta do administrador
- [ ] MANUAL: confirmar que a service_role key não está em nenhum app ou repositório

## Pendências recomendadas
- [ ] Fixar as actions do apk.yml por SHA e separar contents:write só no passo de publicar (V-05)
- [ ] Ligar minifyEnabled (testar bem antes) (V-06)
- [ ] Rodar: gitleaks detect, semgrep --config p/android, trivy fs .
