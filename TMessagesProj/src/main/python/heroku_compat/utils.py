"""Heroku & Hikka utils compatibility layer for Miogram."""

import random
import string
from typing import Any, Optional


def get_args_raw(text: str) -> str:
    """Extract raw arguments after command name."""
    if not text:
        return ""
    parts = text.split(maxsplit=1)
    return parts[1] if len(parts) > 1 else ""


def rand(length: int = 6) -> str:
    """Generate random string of characters."""
    return "".join(random.choices(string.ascii_lowercase + string.digits, k=length))


def get_version_raw() -> str:
    return "2.0.0"


async def answer(message: Any, text: str, **kwargs):
    """Answers a message: edits if outgoing or responds if incoming."""
    if hasattr(message, "edit"):
        try:
            return await message.edit(text, **kwargs)
        except Exception:
            pass
    if hasattr(message, "respond"):
        return await message.respond(text, **kwargs)
    return None
