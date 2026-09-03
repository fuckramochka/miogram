"""Auto Reaction Plugin for Miogram.
Automatically reacts to messages from selected chats or contacts with favorite emojis.
"""

from base_plugin import BasePlugin
from android_utils import show_toast

__id__ = "auto_reaction"
__name__ = "Авто-реакції"
__description__ = "Автоматично ставить ваші улюблені емодзі-реакції на нові повідомлення у вибраних чатах."
__author__ = "@miogram"
__version__ = "1.0.0"

class AutoReactionPlugin(BasePlugin):

    def on_plugin_load(self):
        self.log("AutoReactionPlugin завантажено")
        self.add_hook("updateNewMessage", match_substring=True, priority=10)

    def on_update_hook(self, update):
        if not self.get_setting("enabled", True):
            return

        try:
            # Отримуємо об'єкт повідомлення з Update
            msg = getattr(update, "message", None)
            if msg is None or getattr(msg, "out", False):
                return  # Ігноруємо власні повідомлення

            dialog_id = getattr(msg, "dialog_id", 0)
            msg_id = getattr(msg, "id", 0)

            favorite_emoji = self.get_setting("emoji", "❤️")

            # Ставимо реакцію через AccountClient
            if dialog_id != 0 and msg_id != 0:
                self.client.send_reaction(dialog_id, msg_id, favorite_emoji)
                self.log(f"Поставлено реакцію {favorite_emoji} на повідомлення {msg_id} у діалозі {dialog_id}")
        except Exception as e:
            self.log(f"Помилка в AutoReaction: {e}")
