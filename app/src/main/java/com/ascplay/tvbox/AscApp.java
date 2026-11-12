package com.ascplay.tvbox;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/** Registra o fundo global em todas as telas (visual apenas). */
public class AscApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
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
