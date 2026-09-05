package app.exteraless.drawer;

/**
 * Реестр пунктов главного меню: один и тот же список используется меню «⋮»
 * в {@code DialogsActivity} и боковой шторкой {@link DrawerContainer}.
 *
 * Значения id хранятся в настройках, поэтому менять их нельзя.
 *
 * {@code PLUGINS(102)} пока нет в списке, хотя движок плагинов уже есть:
 * пункт в боковом меню не заведён, вход только через настройки.
 */
public enum MainMenuItem {

    /** Разделитель между группами пунктов, а не сам пункт. */
    DIVIDER(-1),
    PROFILE(18),
    ARCHIVE(14),
    /** Разворачивается в список attach-menu-ботов, у которых {@code show_in_side_menu}. */
    BOTS(105),
    NEW_GROUP(2),
    CONTACTS(6),
    NEW_CHANNEL(3),
    CALLS(10),
    SAVED(11),
    SETTINGS(8),
    BROWSER(101),
    QR(17),
    FEED(106),
    /** Пункт наш, а не из exteraGram, поэтому id взят выше их диапазона. */
    GHOST_MODE(107),
    SMART_FEED(108),
    KANBAN(109),
    SPLIT_CHAT(110),
    BADGE_STUDIO(111);

    private final int id;

    MainMenuItem(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /** Линейный поиск по значениям. */
    public static MainMenuItem getById(int id) {
        for (MainMenuItem item : values()) {
            if (item.id == id) {
                return item;
            }
        }
        return null;
    }
}
