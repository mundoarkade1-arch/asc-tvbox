# Aparelhos-alvo (TV boxes antigos)

## Aparelho principal de teste
- Modelo: TX9
- Chip: Amlogic S905W
- Versão real do Android: 7.1 (as configurações mostram "10.1", mas isso é só um número de enfeite do fabricante)
- Memória RAM: 1 GB
- Resolução da TV: 1080p (Full HD)
- WebView (navegador interno do Android): Chrome 52 (de 2016)

## Controle remoto
Teclas que existem: ligar/desligar, mudo, OK, setas (cima, baixo, esquerda, direita), volume + e -, home, voltar, uma tecla de "função mouse" (liga um cursor controlado pelas setas) e uma tecla com TRÊS LISTRAS (botão de lista/menu).
Teclas que NÃO existem: canal +/-, números, teclas coloridas, play/pause.

Sobre a tecla das três listras (corrigido em 24/10/2026, a pedido do dono: antes este arquivo dizia que não havia tecla de menu):
- No Android, ela costuma chegar ao app como tecla de menu (KEYCODE_MENU). Ainda NÃO conferimos no TX9 se é isso mesmo.
- Uso previsto: abrir a programação do canal (o que está passando e os próximos 5 programas).
- Segunda via, caso ela chegue diferente: segurar o OK sobre o canal faz a mesma coisa.

Regras que isso impõe ao app:
- Toda a navegação deve funcionar só com setas, OK e Voltar. A tecla das três listras é um atalho extra, nunca a única forma de fazer algo.
- Funções extras (opções de um canal, favoritar etc.) devem usar a tecla das três listras, OK longo (segurar) ou itens na própria tela, nunca teclas que não existem.
- O foco (item selecionado) deve ser sempre bem visível.
- O app deve funcionar também com o modo mouse ligado (itens clicáveis com toque/clique) sem quebrar a navegação por setas.
- Volume e mudo são tratados pelo sistema; o app não precisa implementá-los.

## Ainda não sabemos
- Armazenamento livre:
- Tem acesso à Play Store? (sim/não):
- A tecla das três listras chega ao app como tecla de menu? (conferir no TX9)

## Como o app ASC original falha
O app instala normalmente, mas dá "erro de sintaxe" ao iniciar.
Causa: o app ASC usa JavaScript moderno que o WebView Chrome 52 não entende.

## Regras obrigatórias para o app novo
- App Android nativo em Java com ExoPlayer, sem WebView (ver docs/DECISIONS.md).
- Compatível com Android 7.1 (API 25) ou inferior.
- Poucas dependências e baixo uso de memória: o aparelho tem só 1 GB de RAM.
- Testar sempre no TX9 real, não só no computador.
