package tw.nekomimi.nekogram.helpers;

import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.ProfileActivity.sendLogs;

import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import app.exteraless.pillstack.PillStackSettingsActivity;
import app.exteraless.plugins.ui.PluginsActivity;
import app.exteraless.settings.OpenExteraAppearanceActivity;
import app.exteraless.settings.OpenExteraChatsActivity;
import app.exteraless.settings.OpenExteraGeneralActivity;
import app.exteraless.settings.OpenExteraOtherActivity;
import app.exteraless.settings.OpenExteraSettingsActivity;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.settings.BaseNekoXSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoAboutActivity;
import tw.nekomimi.nekogram.settings.NekoChatSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoEmojiSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoExperimentalSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoGeneralSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoPasscodeSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoTranslatorSettingsActivity;

public class SettingsHelper {

    private static final String HOST_NAGRAM = "nasettings";
    private static final String HOST_EXTERALESS = "exteraless";

    private static final Map<String, String> SEARCH_TITLE_ALIASES = new HashMap<>();

    static {
        SEARCH_TITLE_ALIASES.put("OEGeneral:translateChatButton", "OEGeneralTranslateWholeChat");
        SEARCH_TITLE_ALIASES.put("OEGeneral:translateToLang", "OEGeneralTranslationTarget");
        SEARCH_TITLE_ALIASES.put("OEGeneral:lastfm", "OEGeneralLastFm");
        SEARCH_TITLE_ALIASES.put("OEGeneral:ayuGhost", "GhostMode");
        SEARCH_TITLE_ALIASES.put("OEAppearance:appNavigation", "OEAppearanceNavigation");
        SEARCH_TITLE_ALIASES.put("OEChats:disableGreeting", "OEChatsDisableGreetingSticker");
        SEARCH_TITLE_ALIASES.put("OEChats:hideKeyboardOnScroll", "HideKeyboardOnChatScroll");
    }

    private static final Set<String> EXTERALESS_SCREENS = new HashSet<>(Arrays.asList(
            "settings", "general", "appearance", "chats", "plugins", "pillstack", "other"));

    public static boolean isDeepLink(String path) {
        if (path == null) {
            return false;
        }
        if (path.startsWith(HOST_NAGRAM + "/")) {
            return true;
        }
        if (!path.startsWith(HOST_EXTERALESS + "/")) {
            return false;
        }
        return EXTERALESS_SCREENS.contains(path.substring(HOST_EXTERALESS.length() + 1));
    }

    public static String linkPathFor(String key) {
        if (key == null) {
            return null;
        }
        switch (key) {
            case "exteraless":
                return HOST_EXTERALESS + "/settings";
            case "exteraless_general":
                return HOST_EXTERALESS + "/general";
            case "exteraless_appearance":
                return HOST_EXTERALESS + "/appearance";
            case "exteraless_chats":
                return HOST_EXTERALESS + "/chats";
            case "exteraless_other":
                return HOST_EXTERALESS + "/other";
            case "pillstack":
                return HOST_EXTERALESS + "/pillstack";
            default:
                return HOST_NAGRAM + "/" + key;
        }
    }

    public static void processDeepLink(Activity activity, Uri uri, Callback callback, Runnable unknown) {
        if (uri == null) {
            unknown.run();
            return;
        }
        var segments = uri.getPathSegments();
        if (segments.isEmpty() || segments.size() > 2) {
            unknown.run();
            return;
        }
        final boolean exteraless = HOST_EXTERALESS.equals(segments.get(0));
        if (!exteraless && !HOST_NAGRAM.equals(segments.get(0))) {
            unknown.run();
            return;
        }
        if (exteraless && segments.size() < 2) {
            unknown.run();
            return;
        }
        BaseFragment fragment;
        BaseNekoSettingsActivity neko_fragment = null;
        BaseNekoXSettingsActivity nekox_fragment = null;
        if (exteraless) {
            switch (segments.get(1)) {
                case "settings":
                    fragment = neko_fragment = new OpenExteraSettingsActivity();
                    break;
                case "general":
                    fragment = neko_fragment = new OpenExteraGeneralActivity();
                    break;
                case "appearance":
                    fragment = neko_fragment = new OpenExteraAppearanceActivity();
                    break;
                case "chats":
                    fragment = neko_fragment = new OpenExteraChatsActivity();
                    break;
                case "other":
                    fragment = neko_fragment = new OpenExteraOtherActivity();
                    break;
                case "pillstack":
                    fragment = neko_fragment = new PillStackSettingsActivity();
                    break;
                case "plugins":
                    fragment = new PluginsActivity();
                    break;
                default:
                    unknown.run();
                    return;
            }
        } else if (segments.size() == 1) {
            fragment = new NekoSettingsActivity();
        } else if (PasscodeHelper.getSettingsKey().equals(segments.get(1))) {
            fragment = neko_fragment = new NekoPasscodeSettingsActivity();
        } else {
            switch (segments.get(1)) {
                case "about":
                    fragment = new NekoAboutActivity();
                    break;
                case "chat":
                case "chats":
                case "c":
                    fragment = nekox_fragment = new NekoChatSettingsActivity();
                    break;
                case "experimental":
                case "e":
                    fragment = nekox_fragment = new NekoExperimentalSettingsActivity();
                    break;
                case "emoji":
                    fragment = neko_fragment = new NekoEmojiSettingsActivity();
                    break;
                case "general":
                case "g":
                    fragment = nekox_fragment = new NekoGeneralSettingsActivity();
                    break;
                case "translator":
                case "translate":
                case "t":
                    fragment = nekox_fragment = new NekoTranslatorSettingsActivity();
                    break;
                case "exteraless":
                    fragment = neko_fragment = new OpenExteraSettingsActivity();
                    break;
                case "exteraless_general":
                    fragment = neko_fragment = new OpenExteraGeneralActivity();
                    break;
                case "exteraless_appearance":
                    fragment = neko_fragment = new OpenExteraAppearanceActivity();
                    break;
                case "exteraless_chats":
                    fragment = neko_fragment = new OpenExteraChatsActivity();
                    break;
                case "exteraless_other":
                    fragment = neko_fragment = new OpenExteraOtherActivity();
                    break;
                case "pillstack":
                    fragment = neko_fragment = new PillStackSettingsActivity();
                    break;
                case "send_logs":
                    sendLogs(activity, false);
                    return;
                default:
                    unknown.run();
                    return;
            }
        }
        callback.presentFragment(fragment);
        var row = uri.getQueryParameter("r");
        if (TextUtils.isEmpty(row)) {
            row = uri.getQueryParameter("row");
        }
        var value = uri.getQueryParameter("v");
        if (TextUtils.isEmpty(value)) {
            value = uri.getQueryParameter("value");
        }
        if (!TextUtils.isEmpty(row)) {
            var rowFinal = row;
            if (neko_fragment != null) {
                BaseNekoSettingsActivity finalNeko_fragment = neko_fragment;
                AndroidUtilities.runOnUIThread(() -> finalNeko_fragment.scrollToRow(rowFinal, unknown));
            } else if (nekox_fragment != null) {
                BaseNekoXSettingsActivity finalNekoX_fragment = nekox_fragment;
                if (!TextUtils.isEmpty(value)) {
                    String finalValue = value;
                    AndroidUtilities.runOnUIThread(() -> finalNekoX_fragment.importToRow(rowFinal, finalValue, unknown));
                } else {
                    AndroidUtilities.runOnUIThread(() -> finalNekoX_fragment.scrollToRow(rowFinal, unknown));
                }
            }
        }
    }

    private static String rowTitle(BaseNekoSettingsActivity fragment, String key) {
        String title = getString(key);
        if (title != null && !title.isEmpty() && !title.equals(key)) {
            return title;
        }
        if (key.isEmpty()) {
            return null;
        }
        String capitalized = Character.toUpperCase(key.charAt(0)) + key.substring(1);
        String prefix = fragment.getSearchPrefix();
        if (prefix != null) {
            String alias = SEARCH_TITLE_ALIASES.get(prefix + ":" + key);
            if (alias != null) {
                title = resolved(alias);
                if (title != null) {
                    return title;
                }
            }
            title = resolved(prefix + capitalized);
            if (title != null) {
                return title;
            }
        }
        return resolved(capitalized);
    }

    private static String resolved(String name) {
        if (LocaleController.getStringResId(name) == 0) {
            return null;
        }
        String value = getString(name);
        return value == null || value.isEmpty() ? null : value;
    }

    public interface Callback {
        void presentFragment(BaseFragment fragment);
    }

    public static ArrayList<SettingsSearchResult> onCreateSearchArray(Callback callback) {
        ArrayList<SettingsSearchResult> items = new ArrayList<>();
        ArrayList<BaseNekoXSettingsActivity> fragments = new ArrayList<>();
        fragments.add(new NekoGeneralSettingsActivity());
        fragments.add(new NekoChatSettingsActivity());
        fragments.add(new NekoExperimentalSettingsActivity());
        fragments.add(new NekoTranslatorSettingsActivity());

        ArrayList<BaseNekoSettingsActivity> exteralessFragments = new ArrayList<>();
        exteralessFragments.add(new OpenExteraSettingsActivity());
        exteralessFragments.add(new OpenExteraGeneralActivity());
        exteralessFragments.add(new OpenExteraAppearanceActivity());
        exteralessFragments.add(new OpenExteraChatsActivity());
        exteralessFragments.add(new OpenExteraOtherActivity());
        exteralessFragments.add(new PillStackSettingsActivity());

        String e_title = getString(R.string.OpenExtera);
        for (BaseNekoSettingsActivity fragment : exteralessFragments) {
            try {
                fragment.buildRowsForSearch();
            } catch (Exception e) {
                continue;
            }
            int uid = fragment.getSearchGuid();
            int drawable = fragment.getSearchIcon();
            String f_title = fragment.getSearchTitle();
            for (Map.Entry<Integer, String> entry : fragment.getRowMapReverse().entrySet()) {
                String key = entry.getValue();
                if (key == null || key.endsWith("Header") || key.equals(String.valueOf(entry.getKey()))) {
                    continue;
                }
                String title = rowTitle(fragment, key);
                if (title == null || title.isEmpty()) {
                    continue;
                }
                Runnable open = () -> {
                    callback.presentFragment(fragment);
                    AndroidUtilities.runOnUIThread(() -> fragment.scrollToRow(key, null));
                };
                items.add(new SettingsSearchResult(
                        uid + entry.getKey(), title, e_title, f_title, drawable, open));
            }
        }

        String n_title = getString(R.string.NekoSettings);
        for (BaseNekoXSettingsActivity fragment: fragments) {
            int uid = fragment.getBaseGuid();
            int drawable = fragment.getDrawable();
            String f_title = fragment.getTitle();
            for (Map.Entry<Integer, String> entry : fragment.getRowMapReverse().entrySet()) {
                Integer i = entry.getKey();
                String key = entry.getValue();
                if (key.equals(String.valueOf(i))) {
                    continue;
                }
                int guid = uid + i;
                String title = getString(key);
                if (title == null || title.isEmpty()) {
                    continue;
                }
                Runnable open = () -> {
                    callback.presentFragment(fragment);
                    AndroidUtilities.runOnUIThread(() -> fragment.scrollToRow(key, null));
                };
                SettingsSearchResult result = new SettingsSearchResult(
                        guid, title, n_title, f_title, drawable, open
                );
                items.add(result);
            }
        }
        return items;
    }
}
