# Aparelhos-alvo (TV boxes antigos)

## Aparelho principal de teste
- Modelo: TX9
- Chip: Amlogic S905W
- Versão real do Android: 7.1 (as configurações mostram "10.1", mas isso é só um número de enfeite do fabricante)
- Memória RAM: 1 GB
- WebView (navegador interno do Android): Chrome 52 (de 2016)

## Ainda não sabemos
- Armazenamento livre:
- Resolução da TV:
- Controle remoto (só setas/OK? tem mouse?):
- Tem acesso à Play Store? (sim/não):

## Como o app ASC falha
O app instala normalmente, mas dá "erro de sintaxe" ao iniciar.
Causa: o app ASC usa JavaScript moderno que o WebView Chrome 52 não entende.

## Regras obrigatórias para o app novo
- Escrever em JavaScript antigo (ES5, com no máximo ES6 básico). Evitar: async/await, spread de objetos ({...obj}), optional chaining (?.), nullish coalescing (??), class fields.
- Evitar CSS Grid (só existe a partir do Chrome 57). Usar flexbox.
- Não usar frameworks ou bibliotecas pesadas: o aparelho tem só 1 GB de RAM.
- Testar sempre no TX9 real, não só no computador.
