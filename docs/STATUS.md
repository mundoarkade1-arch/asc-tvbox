# Status do projeto

Última atualização: 08/11/2026 (rodada R5: cards grandes, categorias, Jogos do dia, diagnóstico do login remoto; Actions verde em todos os arquivos, falta teste no TX9)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Rodada R5 salva e VERDE no Actions. Nada dela foi visto no TX9 ainda, exceto a primeira versão dos cards (o dono mandou fotos e pediu capa cheia, já feita). O ambiente local não tem Android SDK: nada foi compilado fora do Actions.

## Já feito
- Rodadas anteriores (base, M6 a M13c, R4, identidade ASC lite, Home, player 1080p/60, Ajustes com "Apagar dados"): testadas no TX9.
- R5 cards: canais com 3 por fileira (item_card_live.xml, nome 22sp, "Agora" e "A seguir"); filmes e séries com 4 por fileira, capa cheia 260dp centerCrop, nome 18sp; Home com cards maiores e faixas mais altas; Continue com lista maior.
- R5 categorias: A a Z em Canais, Filmes e Séries; adultas no fim.
- R5 Home: faixa de canais vira "Jogos do dia" quando o servidor tem essa categoria (até 20 itens).
- R5 login remoto: a lista que chega pelo Supabase agora é testada antes de entrar; a tela de login mostra uma frase com o motivo do erro.

## Falta
- No TX9: conferir cards (3 e 4 por fileira, capa cheia, nomes), "A seguir" nos canais, categorias em ordem, "Jogos do dia" na Home.
- PROBLEMA ABERTO (o mais importante): depois da mudança de segurança no Supabase / app ASC Admin (feita FORA deste repositório), a lista enviada à distância não entra direto. O dono quer colocar a lista UMA vez à distância e ela ficar salva no aparelho. Esperando a foto da tela de login com a frase nova (ex.: "Erro 401 em fetch_playlist", "Servidor ok, usuário N letras, senha N letras", "o teste falhou (AUTH)").
- Favoritos: o app não tem (pedido do dono só citou os cards; ofereci criar).
- Lista M3U: faixa "Jogos do dia" não existe.

## Perguntas abertas
- Qual é a frase na tela de login? O que mudou na segurança do Supabase (função fetch_playlist, permissões, formato de senha)?
- Qual é o nome exato da categoria de jogos no servidor do dono (se não tiver "jogos do dia")?

## Próximos passos
1. Pedir a foto da tela de login e descobrir pela frase o que o Supabase manda de errado.
2. Pedir ao dono para testar no TX9 a lista da rodada R5.
3. Só então decidir favoritos ou "Jogos do dia" na lista M3U.
