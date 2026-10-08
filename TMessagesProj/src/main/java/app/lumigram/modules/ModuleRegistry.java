package app.lumigram.modules;

import android.content.Context;

import androidx.annotation.Keep;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.lumigram.api.ModuleHost;

/**
 * Реєстр розширень, які принесли модулі. v1: тільки екрани.
 * Пілюлі/провайдери підуть сюди ж у v2 (той самий патерн, що й
 * PillRegistry.register/unregister — транзакційно, без дублів).
 */
public final class ModuleRegistry {

    public static final class ScreenEntry {
        public final String screenId;
        public final String title;
        public final String moduleId;
        public final ModuleHost.ScreenFactory factory;

        ScreenEntry(String screenId, String title, String moduleId, ModuleHost.ScreenFactory factory) {
            this.screenId = screenId;
            this.title = title;
            this.moduleId = moduleId;
            this.factory = factory;
        }
    }

    private static final Map<String, ScreenEntry> screens = new LinkedHashMap<>();

    private ModuleRegistry() {
    }

    /** @Keep — кличеться з dex модулів через хост. */
    @Keep
    public static void registerScreen(String screenId, String title, String moduleId, ModuleHost.ScreenFactory factory) {
        if (screenId == null || factory == null) {
            return;
        }
        synchronized (screens) {
            screens.put(screenId, new ScreenEntry(screenId, title, moduleId, factory));
        }
    }

    /** @Keep — кличеться з dex модулів через хост. */
    @Keep
    public static void unregisterScreen(String screenId) {
        if (screenId == null) {
            return;
        }
        synchronized (screens) {
            screens.remove(screenId);
        }
    }

    /** Зняти всі екрани одного модуля (вимкнення/видалення). */
    public static void unregisterModule(String moduleId) {
        if (moduleId == null) {
            return;
        }
        synchronized (screens) {
            screens.values().removeIf(e -> moduleId.equals(e.moduleId));
        }
    }

    public static List<ScreenEntry> screensSnapshot() {
        synchronized (screens) {
            return new ArrayList<>(screens.values());
        }
    }

    /** Хост, який ModuleManager дає модулю. Контекст — аплікаційний. */
    static ModuleHost hostFor(final Context appContext, final String moduleId) {
        return new ModuleHost() {
            @Override
            public Context context() {
                return appContext;
            }

            @Override
            public void registerScreen(String screenId, String title, ScreenFactory factory) {
                ModuleRegistry.registerScreen(screenId, title, moduleId, factory);
            }

            @Override
            public void unregisterScreen(String screenId) {
                ModuleRegistry.unregisterScreen(screenId);
            }
        };
    }
}
