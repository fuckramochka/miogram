package app.lumigram.api;

import android.content.Context;

/**
 * LumiGram native module contract, v1.
 *
 * <p>Модуль — це dex в підписаному .lumi-архіві, який вантажиться через
 * DexClassLoader і говорить з ядром ТІЛЬКИ через {@link ModuleHost}.
 * Ніяких прямих імпортів ядра всередині модуля (крім api + Telegram UI
 * для екранів). Нема модуля — нема функції, а не «приховано».
 *
 * <p>Модулі підписує лише автор LumiGram, чужі ключі ядро відхиляє.
 * Движок Python-плагінів лишається окремо і замороженим.
 */
public interface LumiModule {

    /** Стабільний id, напр. "pill-weather". Має збігатися з module.json. */
    String moduleId();

    /** Версія модуля, напр. "1.2.0". */
    String version();

    /** Мінімальна версія ядра ({@code ModuleManager.CORE_VERSION}). */
    int minCoreVersion();

    /**
     * Увімкнення: модуль реєструє свої розширення в хості
     * (екрани, пілюлі v2, провайдери). Важку роботу робити ліниво.
     */
    void onEnable(ModuleHost host, Context context);

    /** Вимкнення: модуль мусить зняти все, що реєстрував. */
    void onDisable();
}
