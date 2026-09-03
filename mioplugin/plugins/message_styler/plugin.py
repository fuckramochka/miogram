"""Message Styler Plugin for Miogram.
Transforms outgoing message text with PC-98 cyber aesthetics, small-caps, and Ame-chan sparkles.
"""

from base_plugin import BasePlugin, HookResult, HookStrategy

__id__ = "message_styler"
__name__ = "Стилізатор тексту"
__description__ = "Трансформує вихідні повідомлення за допомогою команд: .pixel, .sparkle, .needy."
__author__ = "@miogram"
__version__ = "1.0.0"

_SMALL_CAPS = {
    'a': 'ᴀ', 'b': 'ʙ', 'c': 'ᴄ', 'd': 'ᴅ', 'e': 'ᴇ', 'f': 'ꜰ',
    'g': 'ɢ', 'h': 'ʜ', 'i': 'ɪ', 'j': 'ᴊ', 'k': 'ᴋ', 'l': 'ʟ',
    'm': 'ᴍ', 'n': 'ɴ', 'o': 'ᴏ', 'p': 'ᴘ', 'q': 'ǫ', 'r': 'ʀ',
    's': 's', 't': 'ᴛ', 'u': 'ᴜ', 'v': 'ᴠ', 'w': 'ᴡ', 'x': 'x',
    'y': 'ʏ', 'z': 'ᴢ'
}

class MessageStylerPlugin(BasePlugin):

    def on_plugin_load(self):
        self.log("MessageStylerPlugin активовано")
        self.add_on_send_message_hook(priority=20)

    def on_send_message_hook(self, peer, message, params):
        text = str(message)

        if text.startswith(".sparkle "):
            content = text[9:]
            styled = f"✦ {content} ✦"
            return HookResult(HookStrategy.MODIFY, message=styled)

        if text.startswith(".needy "):
            content = text[7:]
            styled = f"† {content} † (Ame-chan bless)"
            return HookResult(HookStrategy.MODIFY, message=styled)

        if text.startswith(".small "):
            content = text[7:]
            styled = "".join(_SMALL_CAPS.get(c.lower(), c) for c in content)
            return HookResult(HookStrategy.MODIFY, message=styled)

        return HookResult(HookStrategy.DEFAULT)
