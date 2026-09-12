"""On-device executor for Hikka/Heroku .py userbot modules (Miogram).

Loads a module file with the local device-safe ``heroku_compat`` shims
(no Telethon), finds the ``loader.Module`` subclass, and runs a command
handler or ``filter_outgoing`` with a fake message object. All replies are
captured and returned as JSON so Java can send them into chats.

Runs on a background thread; every await is guarded by a timeout so a bad
module can never hang the client.
"""

import asyncio
import importlib.util
import inspect
import json
import sys
import traceback


class _FakeMessage:
    """Minimal stand-in for a Telethon message (what compat utils need)."""

    def __init__(self, text, args):
        self.text = text or ""
        self.raw_text = text or ""
        self.args = args or ""
        self.replies = []

    async def edit(self, text, **kwargs):
        self.replies.append(["edit", str(text)])
        return text

    async def respond(self, text, **kwargs):
        self.replies.append(["respond", str(text)])
        return text


def _run_coro(coro, timeout):
    try:
        loop = asyncio.new_event_loop()
        try:
            return (True, loop.run_until_complete(asyncio.wait_for(coro, timeout)))
        finally:
            try:
                loop.close()
            except Exception:
                pass
    except Exception as e:
        return (False, repr(e))


def _load_module(path):
    import heroku_compat  # noqa: F401  (ensures shims are importable)
    from heroku_compat import loader as compat_loader
    mod_name = "miogram_user_mod_%d" % (abs(hash(path)) % 1000000)
    spec = importlib.util.spec_from_file_location(mod_name, path)
    if spec is None or spec.loader is None:
        return None, None, "cannot load module file"
    mod = importlib.util.module_from_spec(spec)
    sys.modules[mod_name] = mod
    try:
        spec.loader.exec_module(mod)
    except Exception as e:
        return None, None, "import error: %s" % (repr(e),)
    return mod, compat_loader, None


def _iter_module_classes(mod, compat_loader):
    found = []
    for attr in dir(mod):
        if attr.startswith("__"):
            continue
        try:
            obj = getattr(mod, attr)
        except Exception:
            continue
        try:
            if inspect.isclass(obj) and issubclass(obj, compat_loader.Module) and obj is not compat_loader.Module:
                found.append(obj)
        except Exception:
            continue
    return found


def _match_cmd(fn_name, marked_name, cmd):
    cmd = (cmd or "").lower()
    if marked_name and str(marked_name).lower() == cmd:
        return True
    base = fn_name[:-3] if fn_name.endswith("cmd") else fn_name
    return base.lower() == cmd


def _find_handler(mod, compat_loader, cmd):
    """Returns (instance, bound_fn) or (None, None)."""
    for cls in _iter_module_classes(mod, compat_loader):
        try:
            inst = cls()
        except Exception:
            continue
        for mname in dir(inst):
            if mname.startswith("_"):
                continue
            try:
                fn = getattr(inst, mname)
            except Exception:
                continue
            if not callable(fn):
                continue
            raw = getattr(fn, "__func__", fn)
            marked = getattr(fn, "__heroku_cmd__", False) or getattr(raw, "__heroku_cmd__", False)
            marked_name = getattr(fn, "__heroku_cmd_name__", None) or getattr(raw, "__heroku_cmd_name__", None)
            fname = getattr(fn, "__name__", None) or getattr(raw, "__name__", mname)
            if (marked and _match_cmd(fname, marked_name, cmd)) or _match_cmd(fname, None, cmd):
                return inst, fn
    # Fallback: plain module-level function named <cmd> (FTG-lite style).
    for fname in (cmd, cmd + "cmd"):
        try:
            fn = getattr(mod, fname, None)
        except Exception:
            fn = None
        if callable(fn):
            return None, fn
    return None, None


def run_command(path, cmd, args, full_text, timeout=25):
    """Runs one module command. Returns JSON: {"replies": [[kind, text]], "error": str|None}."""
    out = {"replies": [], "error": None}
    try:
        mod, compat_loader, err = _load_module(path)
        if err is not None:
            out["error"] = err
            return json.dumps(out)
        inst, fn = _find_handler(mod, compat_loader, cmd or "")
        if fn is None:
            out["error"] = "command not found in module"
            return json.dumps(out)
        msg = _FakeMessage(full_text, args)

        async def _call():
            res = fn(msg) if inst is None else fn(msg)
            if inspect.isawaitable(res):
                await res
            return True

        ok, res = _run_coro(_call(), timeout)
        if not ok:
            out["error"] = str(res)
        out["replies"] = list(msg.replies)
        return json.dumps(out)
    except Exception:
        out["error"] = traceback.format_exc(limit=3)
        return json.dumps(out)


def has_filter(path):
    """True if the module defines filter_outgoing (checked without running)."""
    try:
        mod, compat_loader, err = _load_module(path)
        if err is not None:
            return False
        for cls in _iter_module_classes(mod, compat_loader):
            if hasattr(cls, "filter_outgoing"):
                return True
        return hasattr(mod, "filter_outgoing")
    except Exception:
        return False


def run_filter(path, text, timeout=8):
    """Runs filter_outgoing(self, text). Returns JSON: {"text": str, "error": str|None}."""
    out = {"text": text, "error": None}
    try:
        mod, compat_loader, err = _load_module(path)
        if err is not None:
            out["error"] = err
            return json.dumps(out)
        target = None
        inst = None
        for cls in _iter_module_classes(mod, compat_loader):
            if hasattr(cls, "filter_outgoing"):
                try:
                    inst = cls()
                    target = getattr(inst, "filter_outgoing")
                    break
                except Exception:
                    continue
        if target is None and hasattr(mod, "filter_outgoing"):
            try:
                target = getattr(mod, "filter_outgoing")
            except Exception:
                target = None
        if target is None:
            out["error"] = "no filter_outgoing"
            return json.dumps(out)

        async def _call():
            res = target(text) if inst is None else target(text)
            if inspect.isawaitable(res):
                res = await res
            return res

        ok, res = _run_coro(_call(), timeout)
        if not ok:
            out["error"] = str(res)
        elif isinstance(res, str):
            out["text"] = res
        return json.dumps(out)
    except Exception:
        out["error"] = traceback.format_exc(limit=2)
        return json.dumps(out)
