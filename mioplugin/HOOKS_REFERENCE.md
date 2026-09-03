# ✦ Повний довідник хуків та API Mioplugin ✦

Цей документ містить вичерпний опис архітектури, життєвого циклу, перехоплювачів (хуків), клієнтського API та UI-інструментів для розробки плагінів у Miogram.

---

## Зміст

1. [Маніфест та метадані плагіна](#1-маніфест-та-метадані-плагіна)
2. [Життєвий цикл плагіна (Lifecycle)](#2-життєвий-цикл-плагіна-lifecycle)
3. [Перехоплення повідомлень (Message Hooks)](#3-перехоплення-повідомлень-message-hooks)
4. [Перехоплення MTProto/TL-запитів (Network Hooks)](#4-перехоплення-mtprototl-запитів-network-hooks)
5. [Інтеграція в меню інтерфейсу (Menu Items)](#5-інтеграція-в-меню-інтерфейсу-menu-items)
6. [Низькорівневе перехоплення Java-методів (Xposed Hooking API)](#6-низькорівневе-перехоплення-java-методів-xposed-hooking-api)
7. [Декларативні фільтри хуків (Hook Filters)](#7-декларативні-фільтри-хуків-hook-filters)
8. [Клієнтський API Telegram (AccountClient)](#8-клієнтський-api-telegram-accountclient)
9. [Android та UI утиліти](#9-android-та-ui-утиліти)
10. [Збереження та експорт налаштувань](#10-збереження-та-експорт-налаштувань)
11. [WebAssembly / Rust ABI (.mioplugin)](#11-webassembly--rust-abi-mioplugin)

---

## 1. Маніфест та метадані плагіна

Кожен плагін визначає глобальні змінні на рівні модуля:

```python
__id__ = "unique_plugin_id"          # Унікальний ідентифікатор плагіна (рядок, латиниця)
__name__ = "Назва плагіна"          # Людиночитана назва
__description__ = "Опис функціоналу" # Детальний опис для списку плагінів
__author__ = "@developer"            # Автор плагіна
__version__ = "1.2.0"                # Версія плагіна
__icon__ = "icon_name"               # Іконка (опціонально)
__app_version__ = ">=12.10.0"        # Мінімальна сумісна версія Miogram
```

---

## 2. Життєвий цикл плагіна (Lifecycle)

Клас плагіна успадковує `base_plugin.BasePlugin`:

```python
from base_plugin import BasePlugin, AppEvent

class MyPlugin(BasePlugin):

    def on_plugin_load(self):
        """
        Викликається автоматично при завантаженні/увімкненні плагіна.
        Тут слід реєструвати хуки, пункти меню, ініціалізувати стан.
        """
        self.log("Плагін активовано")

    def on_plugin_unload(self):
        """
        Викликається при вимкненні або оновленні плагіна.
        Усі зареєстровані хуки через self.hook_method() та меню
        знімаються клієнтом автоматично, але тут слід зупинити власні таймери.
        """
        self.log("Плагін зупинено")

    def on_app_event(self, event: str):
        """
        Отримує системні події життєвого циклу програми:
        - AppEvent.START  : Додаток запущено в пам'яті
        - AppEvent.RESUME : Додаток розгорнуто на передній план
        - AppEvent.PAUSE  : Додаток згорнуто у фоновий режим
        - AppEvent.STOP   : Додаток завершує роботу
        """
        if event == AppEvent.RESUME:
            self.log("Користувач повернувся в додаток")
```

---

## 3. Перехоплення повідомлень (Message Hooks)

Дозволяє інспектувати, змінювати текст/параметри або повністю блокувати вихідні повідомлення.

```python
from base_plugin import BasePlugin, HookResult, HookStrategy

class MessageModifierPlugin(BasePlugin):

    def on_plugin_load(self):
        # Реєструємо хук на вихідні повідомлення з пріоритетом 0
        self.add_on_send_message_hook(priority=0)

    def on_send_message_hook(self, peer, message, params):
        """
        peer: TLRPC.InputPeer призначення
        message: рядок з текстом повідомлення
        params: java.util.Map параметрів (entities, reply_to, silent, schedule_date тощо)

        Повертає:
        - HookResult(HookStrategy.DEFAULT) - пропустити без змін
        - HookResult(HookStrategy.MODIFY, message="новий текст") - змінити текст
        - HookResult(HookStrategy.CANCEL) - заблокувати відправку
        """
        text = str(message)
        if text.startswith(".shrug"):
            new_text = text.replace(".shrug", "¯\_(ツ)_/¯")
            return HookResult(HookStrategy.MODIFY, message=new_text)

        if text == ".blockme":
            return HookResult(HookStrategy.CANCEL)

        return HookResult(HookStrategy.DEFAULT)
```

---

## 4. Перехоплення MTProto/TL-запитів (Network Hooks)

Дозволяє перехоплювати низькорівневі виклики Telegram API до та після їх відправки на сервер.

```python
from base_plugin import BasePlugin, HookResult, HookStrategy

class NetworkSpyPlugin(BasePlugin):

    def on_plugin_load(self):
        # Додаємо хук на конкретний TL-запит
        self.add_hook("messages.sendReaction", match_substring=False, priority=10)

    def pre_request_hook(self, request):
        """
        Викликається ДО відправки MTProto запиту на сервер.
        request: екземпляр TLRPC.TL_...
        """
        self.log(f"Відправляється запит: {request}")
        # Можна відхилити запит:
        # return HookResult(HookStrategy.CANCEL)
        return HookResult(HookStrategy.DEFAULT)

    def post_request_hook(self, request, response):
        """
        Викликається ПІСЛЯ отримання успішної відповіді від сервера.
        response: отриманий об'єкт TL_...
        """
        self.log(f"Отримано відповідь на {request}: {response}")
        return HookResult(HookStrategy.DEFAULT)

    def on_update_hook(self, update):
        """
        Викликається для всіх вхідних подій/оновлень Telegram (Updates).
        """
        self.log(f"Вхідне оновлення: {update}")
        return HookResult(HookStrategy.DEFAULT)
```

---

## 5. Інтеграція в меню інтерфейсу (Menu Items)

Плагіни можуть вбудовувати власні пункти в будь-яке меню Miogram.

### Доступні типи меню (`MenuItemType`):
- `MenuItemType.MESSAGE_CONTEXT_MENU` — меню повідомлення в чаті
- `MenuItemType.DRAWER_MENU` — бокова шторка навігації
- `MenuItemType.MAIN_MENU` — головне меню клієнта
- `MenuItemType.CHAT_ACTION_MENU` — верхнє 3-крапкове меню чату
- `MenuItemType.PROFILE_ACTION_MENU` — меню дій у профілі

### Приклад реєстрації:

```python
from base_plugin import BasePlugin, MenuItemData, MenuItemType
from android_utils import show_toast

class QuickMenuPlugin(BasePlugin):

    def on_plugin_load(self):
        self.add_menu_item(MenuItemData(
            menu_type=MenuItemType.MESSAGE_CONTEXT_MENU,
            text="✦ Скопіювати ID повідомлення",
            icon="msg_info",
            priority=100,
            on_click=self.on_copy_message_id
        ))

    def on_copy_message_id(self, context):
        # context містить dialog_id, message_id, message_object
        msg_id = context.get("message_id")
        show_toast(f"ID повідомлення: {msg_id}")
```

---

## 6. Низькорівневе перехоплення Java-методів (Xposed Hooking API)

Miogram містить вбудоване ядро динамічного перехоплення Java-методів для глибокої кастомізації логіки клієнта:

```python
from base_plugin import BasePlugin, MethodHook
from hook_utils import find_class

class JavaInterceptorPlugin(BasePlugin):

    def on_plugin_load(self):
        # Знаходимо потрібний Java-клас
        ChatMessageCell = find_class("org.telegram.ui.Cells.ChatMessageCell")

        # Перехоплюємо виклик методу setMessageObject
        self.hook_method(
            (ChatMessageCell, "setMessageObject"),
            handler=self.MessageCellHook(),
            priority=50
        )

    class MessageCellHook(MethodHook):
        def before_hooked_method(self, param):
            # param.thisObject : екземпляр ChatMessageCell
            # param.args       : масив аргументів [MessageObject, ...]
            msg = param.args[0]
            # Можна змінити аргумент або скасувати виконання:
            # param.setResult(None)

        def after_hooked_method(self, param):
            # param.result : результат виконання методу
            pass
```

---

## 7. Декларативні фільтри хуків (Hook Filters)

Щоб не прокидати тисячі непотрібних викликів через Python-міст і зберігати 120 FPS, використовуються фільтри нативної сторони:

- `HookFilter.ArgumentIsNull(index)` — запускати хук лише якщо аргумент `index` є `null`
- `HookFilter.ArgumentEquals(index, value)` — фільтр за точним значенням
- `HookFilter.ArgumentInstanceOf(index, "full.class.Name")` — фільтр за типом
- `HookFilter.ThisInstanceOf("full.class.Name")` — фільтр за типом `this`
- `HookFilter.ResultIsNull()` — після методу: результат `null`
- `HookFilter.ResultEquals(value)` — результат дорівнює значенню

```python
from base_plugin import HookFilter

self.hook_method(
    (target_class, "onProcess"),
    before=self.on_process,
    filters=[HookFilter.ArgumentIsNull(0)]
)
```

---

## 8. Клієнтський API Telegram (AccountClient)

Об'єкт `self.client` дозволяє надсилати повідомлення, реакції та керувати даними облікового запису:

```python
# Надіслати повідомлення
self.client.send_message(peer=dialog_id, text="Привіт з плагіна!")

# Поставити реакцію
self.client.send_reaction(peer=dialog_id, msg_id=12345, reaction="💖")

# Видалити повідомлення
self.client.delete_messages(peer=dialog_id, ids=[12345], revoke=True)

# Переслати повідомлення
self.client.forward_messages(from_peer=src_id, to_peer=dst_id, ids=[12345])

# Надіслати локальний файл
self.client.send_file(peer=dialog_id, file_path="/sdcard/photo.jpg", caption="Опис")
```

---

## 9. Android та UI утиліти

- `from android_utils import run_on_ui_thread, post_delayed, show_toast, show_alert`
  - `show_toast("Повідомлення", long=True)` — спливаюча підказка
  - `show_alert("Заголовок", "Текст повідомлення")` — діалогове вікно
  - `run_on_ui_thread(lambda: ...)` — виконання в головному потоці Android

- `from ui.settings import Header, Switch, Selector, Text, Divider`
  - Створення нативних налаштувань плагіна:

```python
def create_settings():
    return [
        Header("ОСНОВНІ НАЛАШТУВАННЯ"),
        Switch("Увімкнути функцію", key="enabled", default=True),
        Selector("Швидкість анімації", key="speed", options=["Повільно", "Швидко"], default=1),
        Divider(),
        Text("Про плагін", subtext="Версія 1.0.0")
    ]
```

---

## 10. Збереження та експорт налаштувань

- `self.get_setting(key, default=None)` — зчитати збережене значення
- `self.set_setting(key, value, reload_settings=False)` — зберегти значення
- `self.export_settings()` — експортувати всі налаштування в словник Python
- `self.import_settings(dict)` — імпортувати налаштування

---

## 11. WebAssembly / Rust ABI (.mioplugin)

Для надшвидких плагінів Miogram підтримує запуск бінарних модулів WebAssembly:

```rust
// Приклад Rust ABI для .mioplugin:
#[no_mangle]
pub extern "C" fn mio_init() -> i32 {
    0 // OK
}

#[no_mangle]
pub extern "C" fn mio_hook_on_send_message(ptr: *mut u8, len: usize) -> i32 {
    // Швидка модифікація вихідного буфера в пам'яті WASM
    0
}
```
Модуль компілюється в `plugin.wasm` і пакується в zip-архів із розширенням `.mioplugin`.
