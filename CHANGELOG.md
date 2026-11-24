# Changelog

## Segurança (23/11/2026)
- Updater: receiver do resultado da instalação NÃO exportado, confere a sessão e recusa intent do próprio app (V-01).
- DeviceLink: diagnóstico sem tamanho de usuário/senha (V-04).
- Supabase (supabase/hardening.sql): limite e validação em register_device, permissões extras removidas de devices, funções internas fechadas (V-09, V-10, V-11).
- Novos documentos: SECURITY.md, SECURITY_AUDIT.md, SECURITY_CHECKLIST.md, SECURITY_TESTS.md.
