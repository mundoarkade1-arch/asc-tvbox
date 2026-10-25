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

- ## 2026-10-24: Nova interface, parte (c): aba Ajustes

- Decisão: criar uma tela nova para os Ajustes (SettingsActivity), com menu à esquerda, lista de 5 opções no meio e uma área de texto à direita. As opções são: Controle parental, Trocar de lista, Sobre o dispositivo, Sobre o aplicativo e Verificar atualização.
- Motivo: o dono pediu uma aba Ajustes para tirar os botões do alto da página inicial. Uma tela própria não mexe nas telas que já funcionam. A lista usa o mesmo estilo das listas de categorias (ListView com item_category.xml), então o foco com setas e OK já é conhecido.
- Decisão: o controle parental e a troca de lista passam a viver só nos Ajustes. Os botões "Controle parental" e "Trocar login" saíram do alto da página inicial.
- Motivo: evitar o mesmo comando em dois lugares. O código do menu do controle parental foi movido da HomeActivity para a SettingsActivity, sem mudar as regras (senha de 4 números, trava depois de 5 erros).
- Decisão: ao voltar de Ajustes, a página inicial confere se o controle parental mudou (onResume) e, se mudou, recarrega as faixas sozinha.
- Motivo: antes, a página inicial recarregava as faixas na hora em que a pessoa mexia no controle parental. Agora a mudança acontece em outra tela, então a página inicial precisa perceber ao voltar. Só recarrega quando mudou, para não gastar internet à toa.
- Decisão: "Trocar de lista" abre o login e fecha todas as telas de antes (flags NEW_TASK e CLEAR_TASK).
- Motivo: sem fechar as telas de antes, o botão Voltar do login levaria de volta a telas da lista antiga.
- Decisão: "Sobre o dispositivo" mostra modelo, versão do Android, memória (total e livre), armazenamento (livre e total) e tamanho da tela.
- Motivo: ajuda a conferir o aparelho e responde uma dúvida antiga do projeto (espaço livre do TX9). Só lê informações do sistema, sem permissão nova.
- Decisão: "Verificar atualização" usa as mesmas funções do Updater que a página inicial já usa. Se achar versão nova, a linha vira "Instalar atualização".
- Motivo: reaproveitar o que já funciona, sem código novo de atualização. O aviso automático da página inicial continua como era.
- Decisão: dentro de Ajustes, o ícone Atualizar do menu não faz nada (não há lista para recarregar). Fica anotado em "Problemas em aberto" do STATUS.md para ver no teste.
- Decisão: o ícone Ajustes do NavBar abre a SettingsActivity e fecha a tela de antes, como na troca de abas.
- Decisão: a ordem de trabalho foi: layout novo, depois a classe nova, depois o registro no AndroidManifest.xml, depois o NavBar, depois a HomeActivity (código) e por último o layout da página inicial (sem os dois botões). Assim o Actions ficou verde em todos os passos.
- Dependências: nenhuma nova.
- Ainda NÃO testado no TX9, por decisão do dono: os testes ficam para depois de toda a nova aparência.

- ## 2026-10-24: Nova interface, parte (d): página do filme e da série (início)

- Decisão: a página do filme e da série é a MESMA nos dois logins (servidor e lista M3U). Escolha do dono ("opção A"). Na lista M3U, que quase nunca traz sinopse, a página mostra a imagem do item, o nome, os botões e o texto "Sem sinopse nesta lista".
- Motivo: o visual e o "Continuar de onde parei" ficam iguais nos dois logins. A alternativa (filme da lista M3U tocando direto, sem página) deixaria esses filmes sem o botão "Continuar".
- Decisão: a parte (d) é feita em DOIS TEMPOS. Agora: páginas com capa, sinopse e botão Tocar (filme) e com capa, sinopse, temporadas e episódios (série). Depois, na parte (e): ligar o "Continuar de onde parei" e as cores de episódio visto.
- Motivo: o "Continuar" e as cores precisam do minuto exato, e o arquivo de minutos só nasce na parte (e). Assim as páginas ficam prontas sem esperar o player.
- Decisão: capa e sinopse vêm do servidor com um pedido por item (get_vod_info para filme e get_series_info para série), pela função XtreamApi.details. Lê só o bloco "info" e para.
- Motivo: aparelho com 1 GB de RAM. Nunca baixar a lista inteira só para mostrar uma sinopse.
- Decisão: details() devolve um resultado vazio (sem erro) quando o servidor não informa nada, e a tela mostra a página sem sinopse. Erros de rede viram IOException e a tela os trata.
- Motivo: os servidores Xtream usam nomes de campo um pouco diferentes. A página precisa abrir mesmo sem capa ou sem sinopse.
- Decisão: ordem de trabalho da parte (d): primeiro a função details (nada usa ainda), depois o layout novo, depois a tela nova, depois o registro no AndroidManifest.xml, e só no fim mudar os lugares que tocam o filme direto. A página da série vem depois da do filme.
- Motivo: o Actions não fica vermelho em nenhum passo.
- Dependências: nenhuma nova.
- Ainda NÃO testado no TX9, por decisão do dono: os testes ficam para depois de toda a nova aparência.
- Dependências: nenhuma nova.
- Ainda NÃO testado no TX9, por decisão do dono: os testes ficam para depois de toda a nova aparência.
