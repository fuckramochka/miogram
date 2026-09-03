"""In-App Notifications Plugin for Miogram.
Displays subtle floating toasts when new messages arrive while reading other chats.
"""

from base_plugin import BasePlugin
from android_utils import show_toast

__id__ = "in_app_notifications"
__name__ = "Внутрішні сповіщення"
__description__ = "Показує акуратні спливаючі сповіщення про нові повідомлення всередині додатку."
__author__ = "@miogram"
__version__ = "1.0.0"

class InAppNotificationsPlugin(BasePlugin):

    def on_plugin_load(self):
        self.log("InAppNotificationsPlugin завантажено")
        self.add_hook("updateNewMessage", match_substring=True, priority=5)

    def on_update_hook(self, update):
        msg = getattr(update, "message", None)
        if msg is None or getattr(msg, "out", False):
            return

        text = getattr(msg, "message", "")
        if text:
            show_toast(f"✦ Нове повідомлення: {text[:40]}")
