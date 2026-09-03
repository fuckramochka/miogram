"""Quick Tools Plugin for Miogram.
Adds useful utility actions directly to the message context menu.
"""

from base_plugin import BasePlugin, MenuItemData, MenuItemType
from android_utils import show_toast

__id__ = "quick_tools"
__name__ = "Швидкі інструменти"
__description__ = "Додає корисні дії в меню повідомлень: копіювання ID, перегляд форматування."
__author__ = "@miogram"
__version__ = "1.0.0"

class QuickToolsPlugin(BasePlugin):

    def on_plugin_load(self):
        self.log("QuickToolsPlugin активовано")

        # Додаємо пункт меню для копіювання ID
        self.add_menu_item(MenuItemData(
            menu_type=MenuItemType.MESSAGE_CONTEXT_MENU,
            text="✦ Скопіювати ID повідомлення",
            icon="msg_info",
            priority=50,
            on_click=self.copy_msg_id
        ))

    def copy_msg_id(self, context):
        msg_id = context.get("message_id")
        dialog_id = context.get("dialog_id")
        if msg_id:
            show_toast(f"ID повідомлення: {msg_id} (Діалог: {dialog_id})")
