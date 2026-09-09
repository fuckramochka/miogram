from pathlib import Path
import types

import corpus
from test_sdk_nx_port import sdk, load_module


def test_context_private_directory_belongs_only_to_its_plugin(sdk, monkeypatch, tmp_path):
    files = load_module(monkeypatch, 'file_utils')
    opened = []
    context = types.SimpleNamespace(getDir=lambda name, mode: (
        opened.append(name), types.SimpleNamespace(getAbsolutePath=lambda: str(tmp_path / ('app_' + name))))[1])
    monkeypatch.setattr(files, '_context', lambda: context)
    monkeypatch.setattr(files, 'get_plugins_dir', lambda: str(tmp_path / 'plugins'))
    monkeypatch.setattr(files, 'get_cache_dir', lambda: str(tmp_path / 'cache'))
    monkeypatch.setattr(files, '_own_files', lambda plugin: set())
    assert files._is_own_path('exitFy_v2', tmp_path / 'app_exitFy_v2/bridge/libexitfy_bridge.so')
    assert not files._is_own_path('other', tmp_path / 'app_exitFy_v2/bridge/libexitfy_bridge.so')
    assert not files._is_own_path('exitFy_v2', tmp_path / 'app_exitFy_v2_other/file')
    assert not files._is_own_path('exitFy_v2', tmp_path / 'app_exitFy_v2/../other/file')
    files._own_roots('../escape')
    assert '../escape' not in opened


def test_dex_pillstack_entry_points_exist_as_java_classes():
    root = Path(corpus.JAVA_ROOT, 'com/exteragram/messenger/pillstack')
    config = (root / 'core/PillStackConfig.java').read_text()
    assert 'getActivePills()' in config and 'getLastActivePillId()' in config
    registry = (root / 'core/PillRegistry.java').read_text()
    assert 'public static PillInfo getPillInfo(int id)' in registry
    assert 'public PillCreator creator()' in registry
    assert 'public BasePill create(Context context, Theme.ResourcesProvider resourcesProvider)' in registry
    view = (root / 'ui/PillStackView.java').read_text()
    assert 'public PillStackView(Context context)' in view
    assert 'public void addPill(BasePill pill)' in view
    assert 'com.exteragram.messenger.pillstack.ui.pills.BasePill' in view
    assert 'super.addPill(pill)' in view
    base = (root / 'ui/pills/BasePill.java').read_text()
    assert 'extends app.exteraless.pillstack.pills.BasePill' in base
    assert 'delegate.onStackVisibilityChanged(visible)' in registry
    assert 'delegate.onPillUnselected()' in registry
    assert 'delegate.onPillLongClicked()' in registry


def test_pill_layout_event_reaches_dex_observers():
    center = Path(corpus.JAVA_ROOT, 'org/telegram/messenger/NotificationCenter.java').read_text()
    events = Path(corpus.JAVA_ROOT, 'app/exteraless/pillstack/PillStackEvents.java').read_text()
    assert 'public static final int pillStackLayoutChanged = totalEvents++' in center
    assert 'NotificationCenter.pillStackLayoutChanged)' in events
    assert 'listener.onPillStackLayoutChanged()' in events


def test_badge_ownership_is_scoped_to_the_rendered_view_and_restored():
    hook = Path(corpus.JAVA_ROOT, 'app/exteraless/plugins/xposed/PyMethodHook.java').read_text()
    bar = Path(corpus.JAVA_ROOT, 'org/telegram/ui/ActionBar/ActionBar.java').read_text()
    assert 'param.thisObject instanceof ActionBar.UnreadImageView' in hook
    assert '"onDraw".equals(param.method.getName())' in hook
    assert 'target.toJava(Object.class) == view' in hook
    assert 'count.toInt() > 0' in hook
    assert 'finally {' in hook and '.endPluginUnreadBadge()' in hook
    assert 'pluginUnreadBadgeDepth > 0' in bar
    assert 'Math.max(0, pluginUnreadBadgeDepth - 1)' in bar
    assert 'NekoConfig.unreadBadgeOnBackButton.Bool()' in bar


def test_search_pills_can_be_removed_and_restored_through_the_legacy_hook():
    field = Path(corpus.JAVA_ROOT, 'org/telegram/ui/Components/FragmentSearchField.java').read_text()
    controller = Path(corpus.JAVA_ROOT, 'app/exteraless/pillstack/PillStackController.java').read_text()
    assert 'private app.exteraless.pillstack.PillStackView pillStackView' in field
    assert 'public void updatePillStack(boolean animated)' in field
    assert 'pillStackController.rebuildContents()' in field
    assert 'pillStackView = pillStackController.getStackView()' in field
    assert ').updatePillStack(false)' in controller
    assert 'stackView.getParent() != container' in controller
    assert 'public void rebuildContents()' in controller
