## R5 (08/11/2026)
- Decisão (do dono): cards maiores e nomes maiores para ler de longe. Canais: 3 por fileira; filmes e séries: 4 por fileira. Motivo: pôster é alto; com 3 caberia só uma fileira e meia. Trocar o número em setNumColumns de BrowseActivity, M3uBrowseActivity e SearchActivity.
- Decisão: arquivo novo item_card_live.xml para canais; item_card.xml ficou só para pôsteres. Motivo: o item_card.xml era dividido entre canais e pôsteres e precisavam de formatos diferentes.
- Decisão (do dono): capa de filme e série enche o card (centerCrop, 260dp, cantos arredondados), como na Home. Motivo: com fitCenter sobravam faixas dos lados.
- Decisão (do dono): canais ao vivo mostram "Agora: ..." e "A seguir: ..." (tv_now e tv_next), só em canais. Usa o epg.nextTitle que o EpgCache já traz; sem pedido novo ao servidor.
- Decisão (do dono): categorias de A a Z (Collator pt-BR, nível PRIMARY) e adultas no fim. Motivo: criança não deve ver conteúdo adulto no começo da lista. Só ordem; a senha continua dependendo do controle parental ligado.
- Decisão (do dono): faixa de canais da Home mostra a categoria "Jogos do dia" (até 20), achada pelo nome. Só no login por servidor; na lista M3U exigiria ler a lista inteira (1 GB de RAM).
- Decisão: LoginActivity.pollNow passa a testar a lista remota (DeviceLink.verify) antes de aplicar. Motivo: uma lista com defeito entrava sem teste e a Home ficava com "Não foi possível carregar". DeviceLink.lastDetail mostra o motivo na tela de login, sem senha nem endereço (só código HTTP e tamanhos).
- Aberto: o dono diz que, depois de mudanças de segurança no Supabase / ASC Admin (fora deste repositório), a lista remota não entra direto. A causa ainda não foi confirmada; aguarda a frase da tela de login.
