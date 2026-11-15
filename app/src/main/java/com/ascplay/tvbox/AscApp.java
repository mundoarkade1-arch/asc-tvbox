package com.ascplay.tvbox;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;

/** Registra o fundo global em todas as telas e confere se o tamanho da tela mudou. */
public class AscApp extends Application {

    /** Tela aberta agora (para conferir a escala se a resolução do aparelho mudar). */
    private WeakReference<Activity> current = new WeakReference<>(null);

    @Override
    public void onCreate() {
        super.onCreate();
        final Handler main = new Handler(Looper.getMainLooper());
        registerComponentCallbacks(new ComponentCallbacks() {
            @Override
            public void onConfigurationChanged(Configuration newConfig) {
                main.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ScreenScale.recheck(current.get());
                    }
                }, 600);
            }

            @Override
            public void onLowMemory() {
            }
        });
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
                Glass.attach(activity);
            }

            @Override
            public void onActivityResumed(Activity activity) {
                current = new WeakReference<>(activity);
                ScreenScale.recheck(activity);
                Glass.attach(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }
}
