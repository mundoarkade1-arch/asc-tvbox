# Segurança

## Como reportar um problema
Não abra um issue público com detalhes de falha. Avise o dono do repositório em particular.

## Regras do projeto
- Nunca colocar no repositório: senha, link M3U, chave de assinatura (.jks/.keystore), tokens.
- A chave de assinatura do APK fica só nos Secrets do GitHub (KEYSTORE_B64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD).
- A chave sb_publishable_... no DeviceLink.java é PÚBLICA por desenho (Supabase). A proteção real fica nas regras do banco (veja SECURITY_CHECKLIST.md).
- Login é guardado cifrado (AES-GCM, Android Keystore). O PIN parental é guardado só como resumo (PBKDF2 + sal).
- O app não grava logs com dados do usuário.

## Atualização automática
Só aceita APK de https://github.com/mundoarkade1-arch/asc-tvbox/releases/download/, confere SHA-256 e o Android exige a mesma assinatura.
