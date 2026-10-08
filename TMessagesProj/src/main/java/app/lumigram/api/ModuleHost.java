package app.lumigram.api;

import android.content.Context;

import org.telegram.ui.ActionBar.BaseFragment;

/**
 * Хост, який ядро дає модулю в {@link LumiModule#onEnable}.
 * Єдина легальна дорога модуля в клієнт. v1: тільки екрани;
 * пілюлі/провайдери — v2 поверх цього ж хоста.
 */
public interface ModuleHost {

    Context context();

    /** Зареєструвати екран налаштувань/фічі модуля. */
    void registerScreen(String screenId, String title, ScreenFactory factory);

    /** Зняти екран (модуль кличе в onDisable; ядро — при видаленні). */
    void unregisterScreen(String screenId);

    /** Фабрика екрану. create() викликається на UI-потоці. */
    interface ScreenFactory {
        BaseFragment create();
    }
}
