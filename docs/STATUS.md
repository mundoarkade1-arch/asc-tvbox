# Status do projeto

Última atualização: 19/11/2026 (limpeza dos textos e das cópias soltas; depois, correção do instalador da atualização: Updater.java, aguarda Actions e teste no TX9)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Visual Glass da Home, Ajustes, escala automática e janelas novas estão no GitHub (rodadas de 14 a 16/11/2026). Falta ver tudo no TX9 e na TV de 50". O ambiente local não tem Android SDK: nada é compilado fora do Actions.

## Já feito (uma linha cada)
- Base, M6 a M13c, R4, páginas de filme e série, busca com índice, teclado, Continue assistindo, player 1080p/60: testados no TX9.
- Identidade ASC lite (09/11): testada no TX9, "tudo funcionando perfeito". O nome do app hoje é ASC Play.
- R5 (08/11): cards grandes, categorias A a Z, login remoto com mensagem de erro: Actions verde, sem teste no TX9.
- 14/11: várias listas (CredentialStore), Ajustes em vidro, "Deseja sair?".
- 15/11: escala automática (ScreenScale), menu lateral que sempre cabe, instalador da atualização reforçado.
- 16/11: banner até o topo e mais alto, vidro quase transparente, janela "Atualizar agora / Depois", Ajustes em cards pequenos 2x3 com painel ao lado, colunas fixas (3 canais, 4 pôsteres).
- Minha lista (FavoritesStore) e busca geral (SearchAllActivity) existem na Home.
- 19/11: textos conferidos com o código; cópias soltas da raiz e 5 arquivos sem uso apagados (ver DECISIONS).
- 19/11: Updater.java: instalador abre em tarefa própria e sem janela de escolha (ver DECISIONS). Falta Actions e teste no TX9.
- 19/11 (7): ícones de linha fina nas categorias (ic_cat_*, CategoryIcons); adulto em vermelho e separado. Falta Actions e teste no TX9 (ver se os ícones aparecem e se o texto cabe nos 190dp).
- 19/11 (8): ajuste visual: nome antes do ícone, ícones 26dp vivos, tarja 18+ em Adultos (CategoryLabel). Falta Actions e teste no TX9 (tarja legível, nomes longos, foco).
- 19/11 (9): grupo Reality Shows (depois de Esportes) e Esportes ampliado (Sportnet, Prime Video, Gold, Kazé, Apple TV, Paramount). Falta Actions e teste no TX9. Dúvida aberta: os nomes citados são de CATEGORIA ou de CANAL?
- 19/11 (10): Continue assistindo: some se faltam menos de 3 min (PlaybackProgress.isNearEnd) e os cards têm o tamanho de Filmes/Séries. Falta Actions e teste no TX9.
- 19/11 (11): player com fundo preto e encaixe sem cortar (faixas azuis eram o fundo do layout). Falta Actions e teste no TX9.
- 19/11 (13): banner mais baixo (280) com a capa inteira dentro do mesmo card do nome. Falta Actions e teste no TX9 (ver se o card cabe e se as linhas subiram).
- 19/11 (15): banner sem fundo azul, card de vidro, "Lista atualizada" removida, relógio menor. Falta Actions e teste no TX9.
- 19/11 (14): Updater: desfeito o instalador escolhido sozinho (causava aviso e "assistente" que não instala). Falta Actions e teste no TX9; limpar o padrão salvo no TV box (ver DECISIONS).
- 19/11 (6): categorias de CANAIS na ordem padrão dos 13 grupos (CategoryGroups, CategoryOverrides) + "Mover para" segurando o OK. Falta Actions e teste no TX9. Pendente de decisão do dono: Adultos oculto por padrão, títulos de grupo, juntar categorias.
- 19/11 (5): mensagem "Servidor em manutenção" (ServerMessage) no lugar de erro com código para falha de conexão. Falta Actions e teste no TX9 (desligar a internet e ver "Sem internet"; lista com servidor fora do ar e ver "manutenção").
- 19/11 (4): o APK instalado na TV ainda é o antigo (com o bug) e por isso não acha atualização: instalar o APK novo UMA vez à mão. Updater agora compara o endereço sem diferenciar maiúscula/minúscula.
- 19/11 (3): bug do Updater consertado (a atualização nunca era achada); atualização em janela (Home, login e Ajustes); login mostra o motivo do erro do código; Apagar dados limpa a Minha lista. Falta Actions e teste no TX9.

## Falta
- No TX9 e na TV de 50": ver banner, Ajustes 2x3, janelas, colunas, menu lateral, Minha lista e busca geral.
- Conferir o Actions depois de cada arquivo salvo ou apagado.
- No TX9: atualizar o app e ver se (a) não aparece a janela "Completar ação usando" e (b) a tela "Concluído / Abrir" fica na tela até escolher. Se o problema continuar, o dono conta o que viu (pode ser preciso abrir o app sozinho depois de atualizar).
- Diagnóstico temporário do player (MainActivity: showDiag, diagLabel): remover quando acabar a investigação da imagem pixelada de filmes e séries (hipótese: o arquivo é 720p).
- Opcional: trocar "PLAY" nas imagens do ícone e do banner.

## PROBLEMA ABERTO (o mais importante)
Depois da mudança de segurança no Supabase / ASC Admin (feita FORA deste repositório), a lista enviada à distância (DeviceLink, função fetch_playlist) não entra direto. O dono quer mandar a lista UMA vez à distância e ela ficar salva no aparelho. A tela de login mostra uma frase com o motivo (ex.: "Erro 401 em fetch_playlist"). Agora a tela mostra o motivo de verdade: esperando a foto dessa tela (depois da atualização).

## Perguntas abertas
- Qual é a frase na tela de login? O que mudou na segurança do Supabase (função, permissões, formato de senha)?
- "Jogos do dia": a faixa saiu da Home no redesign Glass e a CategoryOrder não é chamada por nenhuma tela. O dono quer essa faixa de volta (e onde)?
- Se o FPS mostrar 25 ou 30, o arrasto pode vir do próprio canal.

## Próximos passos
1. Pedir a foto da tela de login e descobrir o que o Supabase responde.
2. Perguntar ao dono sobre "Jogos do dia".
3. Dono testar no TX9 e na TV de 50" (lista de teste em "Falta").
