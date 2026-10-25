## 2026-10-24: Nova interface, parte (b): abas com pesquisa própria

- Decisão: cada aba (Canais, Filmes, Séries) tem um botão "Pesquisar canais", "Pesquisar filmes" ou "Pesquisar séries" no alto da tela, ao lado do título. OK no botão abre o teclado de letras (SearchKeyboard) e, ao buscar, abre a tela de resultados (SearchActivity) SÓ com o tipo daquela aba.
- Motivo: o controle remoto não tem teclado, então a "barra de pesquisa" é um botão que abre o teclado na tela, igual ao que já existia. Reaproveita SearchKeyboard, SearchActivity, XtreamApi.search e M3uApi.search, sem código novo de busca. A busca por tipo também é mais rápida (lê só a lista do tipo).
- Decisão: a aba é a mesma tela do "Ver mais" (BrowseActivity no servidor e M3uBrowseActivity na lista M3U). Não foi criada tela nova.
- Motivo: essas telas já funcionam e já tratam categorias, páginas, controle parental e guia de programação. Menos código e menos risco no aparelho fraco (1 GB de RAM).
- Decisão: o menu à esquerda ficou guardado num arquivo só (layout_nav.xml), e uma classe pequena (NavBar.java) liga os botões. As telas de aba incluem o menu, escondido por padrão, e só mostram quando chamam o NavBar.
- Motivo: copiar o menu em cada tela deixaria o código repetido. Escondido por padrão porque a SearchActivity reaproveita o mesmo layout (activity_browse.xml) e não deve mudar.
- Decisão: a página inicial (activity_home.xml) NÃO usa o layout_nav.xml. Ela continua com o seu menu próprio, que já foi feito na parte (a).
- Motivo: não mexer numa tela que já funciona. O custo é um menu duplicado em dois arquivos (anotado em "Problemas em aberto" do STATUS.md).
- Decisão: trocar de aba pelo menu abre a aba nova e FECHA a de antes, sem animação. Se a pessoa já está na aba, nada acontece. Tela inicial volta à HomeActivity sem abrir outra por cima.
- Motivo: sem fechar a aba de antes, cada troca empilharia uma tela nova na memória e o botão Voltar andaria por todas elas.
- Decisão: o ícone Atualizar dentro de uma aba recarrega a categoria aberta e limpa o cache do guia (EpgCache).
- Decisão: a ordem de trabalho foi: ícones, depois layout, depois código. Arquivos novos (layout_nav.xml e NavBar.java) foram criados ANTES de serem usados, para o Actions não ficar vermelho.
- Falta fazer na parte (b): ligar o menu na M3uBrowseActivity, tirar o botão "Buscar" com menu de tipo da página inicial (código primeiro, layout depois) e decidir com o dono se a SearchActivity terá o menu.
- Dependências: nenhuma nova.

## 2026-10-24: Nova interface, parte (b) concluída: menu na lista M3U e busca só nas abas

- Decisão: a aba da lista M3U (M3uBrowseActivity) passou a ter o mesmo menu à esquerda da aba do servidor, ligado pelo NavBar. O ícone Atualizar recarrega a categoria aberta.
- Motivo: as duas abas precisam ter a mesma aparência e o mesmo comportamento. A lista M3U não tem guia de programação (decisão anterior do dono), então não há cache para limpar ao atualizar.
- Decisão: o botão "Buscar" e o menu de tipo (Canais, Filmes, Séries) saíram da página inicial. A busca agora só existe dentro de cada aba, pelo botão "Pesquisar ...".
- Motivo: cada aba pesquisa só o seu tipo, então o menu de tipo da página inicial ficou sem uso. Menos botões no alto e menos código na tela inicial.
- Decisão: a tela de resultados da busca (SearchActivity) fica SEM o menu à esquerda. Escolha do dono.
- Motivo: a busca sempre começa dentro de uma aba, e o Voltar leva de volta a ela. A tela de resultados é de passagem. Também evita mexer no layout compartilhado (activity_browse.xml).
- Decisão: a ordem de trabalho foi: primeiro o código Java (HomeActivity.java sem a busca), depois o layout (activity_home.xml sem o botão), para o Actions não ficar vermelho.
- Dependências: nenhuma nova.
- Ainda NÃO testado no TX9, por decisão do dono: os testes ficam para depois de toda a nova aparência.
