# modules/ — нативні модулі LumiGram

Ядро без них — майже чистий TG. Модуль пише лише автор,
лежить у хмарі (`modules.json` + `.lumi` на релізах), ставиться з екрану
«Модулі» і працює нативно (dex через `ModuleManager`), а не хуком поверх.

## Формат .lumi

Zip: `module.json` + `classes.dex` (+ опційно `res.apk` у v2).
`module.json`: id, version, minCore, api, entry (клас `LumiModule`), title.

## Збірка модуля

1. Скомпілювати `HelloModule.java` + `HelloScreen.java` проти
   `TMessagesProj` (api лежить в `app.lumigram.api`, окремих залежностей нема).
2. `d8` → `classes.dex`, запакувати з `module.json` в `hello-1.0.0.lumi`.
3. `sha256sum` → вписати в `modules.json`, підписати (перед релізом
   увімкнути `ModuleVerifier.VERIFY_SIGNATURE` і вшити ключ).
4. Викласти обидва файли на реліз; URL каталогу — `ModulesCatalog.DEFAULT_URL`.

## Контракт

`app.lumigram.api`: `LumiModule` (id/version/minCore/onEnable/onDisable),
`ModuleHost` (registerScreen/unregisterScreen), `ModuleInfo` (module.json).
Ядро: `ModuleManager` (install/enable/download), `ModuleRegistry` (екрани),
`ModulesCatalog` (fetch), екран `modules.ui.ModulesActivity`.
Пілюлі/провайдери — v2 поверх того ж хоста.
