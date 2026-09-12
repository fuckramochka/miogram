"""Heroku & Hikka Userbot Loader compatibility layer for Miogram."""

import inspect
import logging
from typing import Any, Callable, Dict, List, Optional

logger = logging.getLogger(__name__)


def tds(cls):
    """Decorator to mark Heroku module class."""
    cls.__heroku_module__ = True
    return cls


def command(name: Optional[str] = None):
    """Decorator for module commands."""
    def decorator(func: Callable):
        func.__heroku_cmd__ = True
        func.__heroku_cmd_name__ = name or func.__name__
        return func
    return decorator


class ConfigValue:
    def __init__(self, key: str, default: Any, doc: str = "", validator: Any = None):
        self.key = key
        self.default = default
        self.doc = doc
        self.validator = validator
        self.value = default


class ModuleConfig:
    def __init__(self, *configs: ConfigValue):
        self._configs: Dict[str, ConfigValue] = {c.key: c for c in configs}

    def __getitem__(self, item: str) -> Any:
        if item in self._configs:
            return self._configs[item].value
        return None

    def __setitem__(self, key: str, value: Any):
        if key in self._configs:
            self._configs[key].value = value

    def get(self, key: str, default: Any = None) -> Any:
        if key in self._configs:
            return self._configs[key].value
        return default


class Validators:
    class Boolean:
        def validate(self, v): return bool(v)

    class Choice:
        def __init__(self, choices): self.choices = choices
        def validate(self, v): return v in self.choices

    class String:
        def validate(self, v): return str(v)

    class Integer:
        def validate(self, v): return int(v)


validators = Validators()


class Module:
    """Base class for all Heroku/Hikka userbot modules."""
    strings = {"name": "Unnamed Module"}

    def __init__(self):
        self.config = ModuleConfig()
        self._client = None
        self.inline = None

    def strings(self, key: str, default: str = "") -> str:
        if isinstance(self.strings, dict):
            return self.strings.get(key, default or key)
        return str(key)
