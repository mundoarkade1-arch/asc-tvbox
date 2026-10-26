# Status do projeto

Última atualização: 26/10/2026 (M10, M12 e M11 em código VERDE; M11 AINDA LENTO no teste do dono; M10, M12 e M9 sem teste)
Regras de trabalho: AGENTS.md. Mapa e limites: docs/ARQUIVOS.md. Montagem e versão: docs/ARCHITECTURE.md. Decisões: docs/DECISIONS.md.

## Fase atual
Tudo até a Rodada 5 está pronto e TESTADO pelo dono no TX9. Rodada 6 (menu estreito e degradê) VERDE, dono ainda não opinou (M9). Nesta sessão, três correções foram salvas e ficaram VERDES no Actions: M10, M12 e M11 (em 3 partes). Nada em andamento no código. O dono testou SÓ o M11: melhorou um pouco, mas voltou a ficar lento ("demorou bastante"). Precisa de OUTRA solução (próximo agente).

## Já feito (uma linha cada; detalhes em ARQUIVOS.md e DECISIONS.md)
- Base: login por servidor e lista M3U, atualização automática, controle parental, página inicial, "Ver mais", séries, busca, EPG por canal (só servidor).
- (a) a (f) nova aparência. R1 a R6, Rodadas 3 a 5, M6, R4-1, R4-2: testadas no TX9.
- Rodada 6 (verde): menu estreito e degradê. Falta o dono olhar (M9).
- M10 (verde, SEM teste): ContinueStore.save guarda UM episódio por série (o último, por seriesKey) e limpa repetições antigas.
- M12 (verde, SEM teste): teto das abas subiu de 400 para 5000 (BrowseActivity e M3uBrowseActivity). Já carregava de 40 em 40 ao chegar perto do fim da grade.
- M11 (verde, mas AINDA LENTO): (1) XtreamApi.normalizeText mais leve (sem Normalizer em texto ASCII); (2) M3uApi.search confere o nome antes de classificar; (3) searchStream nos dois (avisa cada resultado); (4) SearchActivity junta tudo e mostra de uma vez no fim (mostrar um a um fazia as capas piscarem).

## METAS (pedidos do dono ainda não feitos). Apague a meta daqui quando ficar pronta e verde.
- M11. PESQUISA AINDA LENTA (PRIORIDADE). Fato: o dono buscou "homem ar" em Filmes (servidor) e demorou bastante; ele diz que é lento em TODAS as abas e nos DOIS logins. Já se sabe que o custo de texto no aparelho NÃO era o principal: o tempo está em LER A LISTA INTEIRA pela internet (servidor: resposta get_vod_streams/get_live_streams/get_series de dezenas de MB; M3U: a lista toda, linha por linha) a cada busca. Ideias a avaliar, SEM decidir (conversar com o dono): (a) servidor: buscar categoria por categoria (category_id na URL, respostas menores) e juntar; (b) guardar a lista num ARQUIVO no armazenamento do app (não na RAM), com validade curta (ex.: 30 min), e buscar nele; para M3U, baixar uma vez para um arquivo e ler do disco; cuidado com espaço e com os 1 GB de RAM; (c) índice pequeno em disco só com nome, tipo e id; (d) mostrar "Buscando..." com contagem. MEDIR antes: pedir ao dono o tempo aproximado até o resultado em cada login e, se possível, o tamanho da lista (Ajustes > Sobre). Peça XtreamApi.java, M3uApi.java e SearchActivity.java (um por vez). SearchActivity hoje usa searchStream e mostra tudo no fim; a base já existe.
- TESTES PENDENTES do dono no TX9 (M10, M12, M9):
  - M10: abrir um episódio (~30 s), voltar; abrir OUTRO da mesma série (~30 s), voltar. A série deve aparecer UMA vez na faixa "Continue assistindo", com o último episódio.
  - M12: numa categoria com "mil e poucos" itens, segurar seta para baixo na grade passando dos 400; os itens devem continuar chegando até o fim. Na lista M3U, as páginas do fim podem demorar mais (cada página relê a lista).
  - M9: menu com 6 botões, foco desce até Atualizar, "Tela inicial" cabe, texto legível no vermelho.
- ADIADO: fundo com imagem e ícone novo (imagens grandes; reduzir no squoosh.app antes, nomes bg_app.jpg e ic_app_logo.png; o agente não envia imagens). ADIADO: otimização geral sem perder funções (medir antes; M11 é parte disso).
- Rodada 6: o dono pode lembrar de mais pedidos.

## Perguntas e ideias abertas
- Fim da lista na troca de canal: hoje dá a volta (escolha do agente).
- Ideia que precisa do OK do dono: o Actions ignorar mudanças só em .md.
- Ícone provisório; se o TX9 mostrar o app sem imagem, criar o banner.
- Tela de login ainda com o fundo antigo: perguntar se quer o degradê.
- Limpeza opcional: tirar lv_seasons e lv_episodes do activity_series.xml.

## Pontos a conferir no teste (riscos conhecidos)
- Menu: uns 440dp de altura; em tela baixa o último botão pode cortar.
- Degradê: texto cinza pode ficar menos legível sobre o vermelho do alto.
- Aba com 5000 itens: grade muito longa; se o TX9 ficar pesado, baixar MAX_ITEMS.

## Próximos passos
1. Conversar com o dono sobre o M11 (medir, escolher entre as ideias). Um arquivo por vez.
2. Pedir os testes de M10, M12 e M9 quando o dono quiser.
