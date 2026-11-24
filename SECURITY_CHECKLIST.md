# Checklist de segurança

## Antes de publicar uma versão
- [ ] Nenhum segredo/senha/link M3U nos arquivos novos
- [ ] Secrets do GitHub preenchidos (senão o APK não é publicado)
- [ ] Teste de atualização automática no aparelho (Android 7.1 / TX9)

## Supabase (CONFIGURAÇÃO MANUAL, fora do repositório) — V-09
No painel do Supabase, confira:
- [ ] RLS ligado em TODAS as tabelas do schema public
- [ ] Nenhuma policy permite SELECT/UPDATE/DELETE para `anon` direto nas tabelas de aparelhos/listas
- [ ] `anon` só tem EXECUTE em register_device e fetch_playlist (REVOKE no resto)
- [ ] fetch_playlist exige código E secret, compara só o HASH do secret e responde igual para "código não existe" e "secret errado"
- [ ] register_device não revela se o código já existe de forma que permita varrer códigos
- [ ] Quem GRAVA a lista (app de gerenciar) exige login de administrador, nunca só o código do aparelho
- [ ] Limite de tentativas por IP/código (rate limit)
- [ ] A service_role key NÃO está em nenhum app nem repositório

## Pendências recomendadas
- [ ] Fixar as actions do apk.yml por SHA e separar permissão contents:write só no passo de publicar (V-05)
- [ ] Ligar minifyEnabled (testar bem antes) (V-06)
- [ ] Rodar: gitleaks detect, semgrep --config p/android, trivy fs .
