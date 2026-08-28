# Miogram Plugin Developer Guide & API Reference

Welcome to the **Miogram Plugin Engine** documentation. This guide details how to build, test, and distribute plugins for Miogram using Python (Chaquopy), Java/Kotlin, and WebAssembly bridges.

---

## 1. Plugin Architecture Overview

Miogram provides a sandboxed and extensible plugin runtime that allows third-party developers to:
- Intercept and modify incoming/outgoing messages.
- Inject custom UI elements (action mode buttons, drawer items, context menus, settings subpages).
- Automate actions (auto-reactions, translations, formatting, custom command triggers).
- Access local storage, network requests, and cryptographic utilities securely.

### Supported Runtimes:
1. **Python Plugins (Recommended)**: Powered by Chaquopy 3.11 with full access to Python standard libraries and Telegram bridges.
2. **Java / Kotlin Extensions**: High-performance direct hook integration via `ClassProxyFactory` and `MenuInjector`.
3. **WebAssembly (WASM)**: Portable Rust/C++ modules.

---

## 2. Plugin Structure & `plugin.json` Manifest

Every Miogram plugin is packaged as a `.zip` or `.mioplugin` archive containing:
```
my_plugin/
+-- plugin.json       # Required manifest
+-- main.py           # Python entrypoint
+-- icon.png          # Optional 128x128 icon
L-- assets/           # Any auxiliary resources
```

### `plugin.json` Schema:
```json
{
  "id": "app.miogram.sample_auto_reaction",
  "name": "Auto React & Quick Actions",
  "version": "1.0.0",
  "versionCode": 1,
  "author": "Miogram Developer",
  "description": "Automatically adds reactions and adds quick custom buttons.",
  "minAppVersion": "1.0.0",
  "entrypoint": "main.py",
  "permissions": [
    "MESSAGES_READ",
    "MESSAGES_SEND",
    "UI_INJECTION",
    "NETWORK_ACCESS",
    "STORAGE_READ_WRITE"
  ],
  "settings": [
    {
      "key": "auto_react_enabled",
      "type": "boolean",
      "title": "Enable Auto React",
      "defaultValue": true
    },
    {
      "key": "target_emoji",
      "type": "string",
      "title": "Default Emoji",
      "defaultValue": "??"
    }
  ]
}
```

---

## 3. Core Python Hook APIs

### Message Hooks:
```python
from miogram.plugins import Plugin, HookResult

class MyPlugin(Plugin):
    def on_load(self):
        self.logger.info("Plugin loaded successfully!")

    def on_message_send(self, chat_id: int, text: str, reply_to_msg_id: int = None) -> HookResult:
        """
        Called right before a message is transmitted.
        Return HookResult.modify(new_text) or HookResult.block() or HookResult.pass_through().
        """
        if text.startswith("/shrug"):
            return HookResult.modify(text.replace("/shrug", "?\_(?)_/?"))
        return HookResult.pass_through()

    def on_message_receive(self, chat_id: int, message_id: int, text: str, sender_id: int):
        """
        Called when a new message arrives in any dialog.
        """
        if self.get_setting("auto_react_enabled") and sender_id != self.get_my_user_id():
            emoji = self.get_setting("target_emoji", "??")
            self.send_reaction(chat_id, message_id, emoji)

    def on_unload(self):
        self.logger.info("Plugin unloaded.")
```

### UI Injection API:
```python
from miogram.plugins.ui import MenuItem, ActionTarget

class MyPlugin(Plugin):
    def on_load(self):
        # Inject button into Message Selection Bar
        self.register_action_mode_button(
            id="btn_quick_quote",
            icon="msg_quote",
            title="Quote to Notes",
            on_click=self.on_quote_clicked
        )

        # Inject item into Chat Long-press Context Menu
        self.register_chat_menu_item(
            MenuItem(
                title="Summarize Chat with AI",
                icon="msg_bot",
                target=ActionTarget.DIALOG,
                callback=self.on_summarize_chat
            )
        )

    def on_quote_clicked(self, selected_messages: list):
        for msg in selected_messages:
            self.save_to_cloud_notes(msg.text)
        self.show_toast("Saved to notes!")
```

---

## 4. Local Storage & Preferences API

Plugins have access to isolated, key-value and SQLite persistent storage:
```python
# Key-Value API
self.storage.set("last_sync", 1724851200)
timestamp = self.storage.get("last_sync", default=0)

# Settings binding
enabled = self.get_setting("auto_react_enabled", default=True)
```

---

## 5. Network Requests & Asynchronous Tasks

```python
import urllib.request
import json
from miogram.async_tasks import run_background

def fetch_weather(city: str):
    def _worker():
        url = f"https://wttr.in/{city}?format=j1"
        req = urllib.request.Request(url, headers={"User-Agent": "Miogram/1.0"})
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode())
            return data
    return run_background(_worker)
```

---

## 6. Testing & Sideloading Plugins

1. In Miogram, go to **Settings -> Плагіни Miogram (Plugins)**.
2. Tap **Install from File (.zip / .mioplugin)**.
3. Select your plugin archive.
4. Miogram will verify permissions and activate the plugin immediately in runtime without restarting the app.
