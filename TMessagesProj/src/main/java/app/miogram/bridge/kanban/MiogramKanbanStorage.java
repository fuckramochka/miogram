package app.miogram.bridge.kanban;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.telegram.messenger.ApplicationLoader;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MiogramKanbanStorage {

    private static final String PREFS_NAME = "miogram_kanban_prefs";
    private static final String KEY_ITEMS = "kanban_items_json";
    private static final Gson gson = new Gson();

    public static class KanbanItem {
        public String id;
        public String title;
        public String description;
        public int column; // 0 = Inbox, 1 = In Progress, 2 = Important, 3 = Done
        public long dialogId;
        public int messageId;
        public long createdAt;

        public KanbanItem(String id, String title, String description, int column, long dialogId, int messageId) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.column = column;
            this.dialogId = dialogId;
            this.messageId = messageId;
            this.createdAt = System.currentTimeMillis();
        }
    }

    public static List<KanbanItem> loadItems() {
        Context ctx = ApplicationLoader.applicationContext;
        if (ctx == null) return new ArrayList<>();
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ITEMS, "[]");
        try {
            Type listType = new TypeToken<ArrayList<KanbanItem>>(){}.getType();
            List<KanbanItem> items = gson.fromJson(json, listType);
            return items != null ? items : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void saveItems(List<KanbanItem> items) {
        Context ctx = ApplicationLoader.applicationContext;
        if (ctx == null) return;
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = gson.toJson(items);
        prefs.edit().putString(KEY_ITEMS, json).apply();
    }

    public static void addItem(String title, String desc, int column, long dialogId, int messageId) {
        List<KanbanItem> items = loadItems();
        String id = String.valueOf(System.currentTimeMillis());
        items.add(0, new KanbanItem(id, title, desc, column, dialogId, messageId));
        saveItems(items);
    }

    public static void moveItem(String itemId, int targetColumn) {
        List<KanbanItem> items = loadItems();
        for (KanbanItem item : items) {
            if (item.id.equals(itemId)) {
                item.column = targetColumn;
                break;
            }
        }
        saveItems(items);
    }

    public static void deleteItem(String itemId) {
        List<KanbanItem> items = loadItems();
        items.removeIf(it -> it.id.equals(itemId));
        saveItems(items);
    }
}
