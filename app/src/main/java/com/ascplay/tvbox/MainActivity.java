     * Se algo vier errado, não usa a fila: o episódio escolhido toca normalmente
     * e só não passa para o próximo sozinho.
     */
    private void readEpisodeList() {
        ArrayList<String> ids = getIntent().getStringArrayListExtra(SeriesActivity.EXTRA_LIST_IDS);
        ArrayList<String> exts = getIntent().getStringArrayListExtra(SeriesActivity.EXTRA_LIST_EXTS);
        int index = getIntent().getIntExtra(SeriesActivity.EXTRA_LIST_INDEX, -1);
        if (ids == null || exts == null || ids.size() != exts.size()
                || ids.isEmpty() || ids.size() > MAX_LIST
                || index < 0 || index >= ids.size()) {
            return;
        }
        for (int i = 0; i < ids.size(); i++) {
            String eid = ids.get(i);
            String eext = exts.get(i);
            if (eid == null || !eid.matches("\\d{1,12}")
                    || eext == null || !eext.matches("[A-Za-z0-9]{0,5}")) {
                return;
            }
        }
        listIds = ids;
        listExts = exts;
        listIndex = index;
    }

    /**
     * Lê a fila de endereços dos episódios da série que a tela da série (lista M3U) mandou.
     * Cada endereço precisa ser http ou https. Se algo vier errado, não usa a fila: o episódio
     * escolhido toca normalmente e só não passa para o próximo sozinho.
     */
    private void readM3uEpisodeList() {
        ArrayList<String> urls = getIntent().getStringArrayListExtra(EXTRA_LIST_URLS);
        int index = getIntent().getIntExtra(SeriesActivity.EXTRA_LIST_INDEX, -1);
        if (urls == null || urls.isEmpty() || urls.size() > MAX_LIST
                || index < 0 || index >= urls.size()) {
            return;
        }
        for (int i = 0; i < urls.size(); i++) {
            String u = urls.get(i);
            if (!isHttp(u) || u.length() > MAX_URL) {
                return;
            }
        }
        listUrls = urls;
        listIndex = index;
    }

    /**
     * Lê os textos "Temporada N - Episódio N" (opcional). Só vale se a fila existe e se a lista
     * de textos tem o mesmo tamanho dela. Se algo vier errado, ignora e usa "Episódio N".
     */
    private void readEpisodeLabels() {
        int size = listUrls != null ? listUrls.size() : (listIds != null ? listIds.size() : 0);
        if (size <= 0) {
            return;
        }
        ArrayList<String> labels = getIntent().getStringArrayListExtra(EXTRA_LIST_LABELS);
        if (labels == null || labels.size() != size) {
            return;
        }
        for (int i = 0; i < labels.size(); i++) {
            String t = labels.get(i);
            if (t == null || t.length() > MAX_LABEL) {
                return;
            }
        }
        listLabels = labels;
    }

    @Override
    protected void onStart() {
        super.onStart();
        startPlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(hideNowbar);
        handler.removeCallbacks(hideLabel);
        handler.removeCallbacks(saveTick);
        handler.removeCallbacks(skipTick);
        skipButton.setVisibility(View.GONE);
        episodeLabel.setVisibility(View.GONE);
        nowbarToken++;
        // Anota o minuto e, se a pessoa voltar a esta tela, continua dali
        saveMark();
        rememberPositionForRestart();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(hideNowbar);
        handler.removeCallbacks(hideLabel);
        handler.removeCallbacks(saveTick);
        handler.removeCallbacks(skipTick);
        nowbarToken++;
        if (channelQueue != null) {
            channelQueue.close();
            channelQueue = null;
        }
    }

    /**
     * Teclas do player.
     * - Filme e episódio: OK pausa e continua (ou, com o botão de pular episódio na tela,
     *   OK pula para o próximo episódio).
     * - Filme e episódio: seta direita/esquerda, um toque, avança/volta 10 segundos; segurando,
     *   vai avançando/voltando enquanto segura, cada vez mais rápido.
     * - Canal ao vivo: seta para baixo = próximo canal, seta para cima = canal anterior.
     * - Canal ao vivo: ao apertar OK, mostra a barra Agora / A seguir.
     * - Canal ao vivo do servidor: a tecla das três listras (menu), ou segurar o OK,
     *   abre a janela com a programação do canal.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        boolean isOk = code == KeyEvent.KEYCODE_DPAD_CENTER
                || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER;

        // Tecla das três listras: programação do canal (só canal ao vivo do servidor)
        if (code == KeyEvent.KEYCODE_MENU && canShowSchedule()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                openSchedule();
            }
            return true;
        }

        // Canal ao vivo: cima e baixo trocam de canal
        if ((code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN)
                && canChangeChannel()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
                    channelQueue.next();
                } else {
                    channelQueue.previous();
                }
            }
            return true;
        }

        // Filme e episódio: setas esquerda e direita
        if ((code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT)
                && canSeekWithArrows()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                long direction = code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1L : -1L;
                long now = SystemClock.elapsedRealtime();
                if (event.getRepeatCount() == 0) {
                    // Um toque: sempre pula 10 segundos
                    holdStartAt = now;
                    lastSeekAt = now;
                    seekBy(direction * SEEK_MS);
                } else if (now - lastSeekAt >= HOLD_GAP_MS) {
                    // Segurando: passos maiores quanto mais tempo segura
                    long held = now - holdStartAt;
                    long step;
                    if (held < HOLD_PHASE1_MS) {
                        step = HOLD_STEP1_MS;
                    } else if (held < HOLD_PHASE2_MS) {
                        step = HOLD_STEP2_MS;
                    } else {
                        step = HOLD_STEP3_MS;
                    }
                    lastSeekAt = now;
                    seekBy(direction * step);
                }
            }
            return true;
        }

        // Filme e episódio: OK pausa e continua (ou pula o episódio, se o botão está na tela)
        if (isOk && canSeekWithArrows()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                if (skipButton.getVisibility() == View.VISIBLE) {
                    skipToNextEpisode();
                } else {
                    togglePause();
                }
            }
            return true;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN && isOk) {
            if (event.getRepeatCount() == 0) {
                showNowbar();
            } else if (event.getRepeatCount() == 1) {
                // OK segurado: segunda via para abrir a programação
                openSchedule();
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /** Só canal ao vivo, com a fila de canais pronta, troca de canal com as setas. */
    private boolean canChangeChannel() {
        return channelQueue != null && channelQueue.isReady() && item != null
                && item.kind == XtreamApi.Kind.LIVE;
    }

    /** Toca o canal novo escolhido pelas setas cima e baixo. */
    private void switchChannel(XtreamApi.Item next) {
        if (isFinishing() || isDestroyed() || next == null || creds == null || player == null) {
            return;
        }
        boolean m3u = CredentialStore.MODE_M3U.equals(creds.mode);
        String ext = next.ext;
        if (ext == null || !ext.matches("[A-Za-z0-9]{0,5}")) {
            ext = "";
        }
        String url = m3u && next.url != null ? next.url : "";
        item = new XtreamApi.Item(XtreamApi.Kind.LIVE, next.id, next.name == null ? "" : next.name,
                "", "", "", ext, url);
        streamUrl = XtreamApi.streamUrl(creds.server, creds.user, creds.pass, item);
        triedTs = false;
        playCurrent();
        if (m3u) {
            // Lista M3U não tem guia: mostra só o nome do canal
            Toast.makeText(this, item.name, Toast.LENGTH_SHORT).show();
        } else {
            showNowbar();
        }
    }

    /** A programação só existe para canal ao vivo do servidor (a lista M3U não tem guia). */
    private boolean canShowSchedule() {
        return item != null && creds != null && item.kind == XtreamApi.Kind.LIVE
                && CredentialStore.MODE_XTREAM.equals(creds.mode);
    }

    /** Abre a janela da programação do canal que está tocando. */
    private void openSchedule() {
        if (!canShowSchedule()) {
            return;
        }
        // Esconde a barra Agora / A seguir para não ficar uma por cima da outra
        handler.removeCallbacks(hideNowbar);
        nowbarToken++;
        groupNowbar.setVisibility(View.GONE);
        EpgDialog.show(this, creds.server, creds.user, creds.pass, item.id, item.name);
    }

    /** As setas e o OK só agem em filme e episódio que já estão carregando ou tocando. */
    private boolean canSeekWithArrows() {
        if (player == null || item == null) {
            return false;
        }
        if (item.kind != XtreamApi.Kind.MOVIE && item.kind != XtreamApi.Kind.EPISODE) {
            return false;
        }
        int state = player.getPlaybackState();
        return state != Player.STATE_IDLE && state != Player.STATE_ENDED;
    }

    /** Pula para frente (valor positivo) ou para trás (valor negativo) e mostra a barra. */
    private void seekBy(long deltaMs) {
        long target = player.getCurrentPosition() + deltaMs;
        if (target < 0) {
            target = 0;
        }
        long duration = player.getDuration();
        if (duration != C.TIME_UNSET && duration > 0 && target > duration) {
            target = duration;
        }
        player.seekTo(target);
        playerView.showController();
        showEpisodeLabel();
    }

    /** OK em filme e episódio: pausa se está tocando e continua se está pausado. */
    private void togglePause() {
        if (player.getPlayWhenReady()) {
            player.setPlayWhenReady(false);
        } else {
            player.setPlayWhenReady(true);
        }
        playerView.showController();
        showEpisodeLabel();
    }

    /** Existe um próximo episódio na fila? */
    private boolean hasNextEpisode() {
        int next = listIndex + 1;
        if (listUrls != null) {
            return next < listUrls.size();
        }
        return listIds != null && listExts != null && next < listIds.size();
    }

    /** Mostra o botão de pular quando faltam 2 minutos (ou menos) para acabar o episódio. */
    private void updateSkipButton() {
        boolean show = false;
        if (player != null && item != null && item.kind == XtreamApi.Kind.EPISODE
                && hasNextEpisode() && player.getPlaybackState() == Player.STATE_READY) {
            long duration = player.getDuration();
            if (duration != C.TIME_UNSET && duration > 0) {
                long left = duration - player.getCurrentPosition();
                show = left > 0 && left <= SKIP_BEFORE_END_MS;
            }
        }
        skipButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /** OK no botão: marca este episódio como visto e toca o próximo. */
    private void skipToNextEpisode() {
        skipButton.setVisibility(View.GONE);
        if (item == null || item.kind != XtreamApi.Kind.EPISODE || !hasNextEpisode()) {
            return;
        }
        saveFinished();
        playNextEpisode();
    }
