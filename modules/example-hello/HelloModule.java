package com.lumigram.module.hello;

import android.content.Context;

import app.lumigram.api.LumiModule;
import app.lumigram.api.ModuleHost;

/**
 * Приклад-скелет модуля. Компілюється проти :modules:api + Telegram UI,
 * пакується в hello-1.0.0.lumi (zip: module.json + classes.dex).
 */
public final class HelloModule implements LumiModule {

    @Override
    public String moduleId() {
        return "hello";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public int minCoreVersion() {
        return 1;
    }

    @Override
    public void onEnable(ModuleHost host, Context context) {
        host.registerScreen("hello_main", "Hello", () -> new HelloScreen());
    }

    @Override
    public void onDisable() {
        // Ядро саме зніме екрани через unregisterModule; тут — зупинити своє.
    }
}
