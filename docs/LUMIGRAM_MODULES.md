# LumiGram — система модулів (проєкт + інтерфейс)

## Ідея

Без модулів ядро — майже чистий TG. Модуль — функція, якої нема,
доки не скачав: після установки зʼявляється екран/поведінка,
після видалення — зникає без слідів. Пише модулі лише автор,
лежать у хмарі, ставляться з екрану «Модулі». Це НЕ плагіни:
плагіни (`app/exteraless/plugins`, Python/Chaquopy) — накладка хуками
поверх коду і заморожені; модулі — нативний dex, що говорить з ядром
тільки через контракт `app.lumigram.api`.

## Шари

```
.hm. ядро (TMessagesProj)
  app.lumigram.api        контракт: LumiModule, ModuleHost, ModuleInfo
  app.lumigram.modules    менеджер: ModuleManager, ModuleRegistry,
                          ModulesCatalog, ModuleVerifier
  app.lumigram.modules.ui ModulesActivity (екран: встановлені/екрани/каталог)
.hm. хмара
  modules/modules.json    каталог: id/version/title/url/sha256/size/minCore/signature
  *.lumi                  zip: module.json + classes.dex (+res.apk у v2)
.hm. приклад
  modules/example-hello/  HelloModule + HelloScreen + module.json
```

## Життєвий цикл

Каталог → скачування (`.part`, атомарний rename, як в апдейтері) →
SHA-256 (+Ed25519 перед релізом, `ModuleVerifier.VERIFY_SIGNATURE`) →
install (перевірка `module.json`, `minCore <= CORE_VERSION`) →
`DexClassLoader` → `onEnable(host)` → `registerScreen` →
екран у `ModulesActivity` → вимкнення/видалення (`unregisterModule`).

## Інтерфейс (екран «Модулі»)

Три секції: **Встановлені** (чек = увімкнено, тап — перемкнути;
«Видалити» — діалог зі списком), **Екрани модулів** (відкрити),
**Каталог** («Перевірити» → список з хмари → тап качає з прогресом).
Без модулів перші дві секції порожні з поясненням — це і є «чистий TG».

Вхід: кореневий екран Lumi-налаштувань, рядок «Модулі» (`msg_download`).
Init: `ModuleManager.init()` в `ApplicationLoader.onCreate` поруч з іншими
`Config.init()`; після — автозавантаження ввімкнених.

## Безпека

- Підпис лише авторським ключем; чужі `.lumi` ядро відхиляє.
- `minCore` відсікає несумісні; версія ядра `ModuleManager.CORE_VERSION`.
- Пілюлі/провайдери — v2 поверх того ж хоста (патерн `PillRegistry`).

## Перед релізом

1. Вшити Ed25519-публічний ключ, `VERIFY_SIGNATURE = true`.
2. `ModulesCatalog.DEFAULT_URL` → реальний `modules.json`.
3. Перший модуль-кандидат на винос з ядра: `pill-weather` (мережа, ключі).
