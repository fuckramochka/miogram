package app.miogram.bridge.badge;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.miogram.bridge.MiogramLocale;

/**
 * 10 Canonical Miogram Badges from the official design system:
 * 01 - ORIGINAL (Classic winged heart with antenna)
 * 02 - PINK (Neon pink style)
 * 03 - CYAN (Cyber sky blue style)
 * 04 - DARK (Obsidian with velvet purple edge glow)
 * 05 - ANGEL (Floating halo with lavender heart)
 * 06 - DEVIL (Devil horns & bat wings)
 * 07 - RAINBOW (Prismatic spectrum wings)
 * 08 - OUTLINE (Crisp 1px wireframe pixel contour)
 * 09 - GLITCH (Split RGB displacement glitch)
 * 10 - PREMIUM (Golden royal crown & golden wings)
 */
public enum MiogramBadgeType {

    ORIGINAL("original", "01 — ORIGINAL", "Класичний варіант", "Классический вариант", "Classic style"),
    PINK("pink", "02 — PINK", "Рожевий стиль", "Розовый стиль", "Pink style"),
    CYAN("cyan", "03 — CYAN", "Блакитний стиль", "Голубой стиль", "Cyan style"),
    DARK("dark", "04 — DARK", "Темний варіант", "Темный вариант", "Dark style"),
    ANGEL("angel", "05 — ANGEL", "З німбом", "С нимбом", "Angel with halo"),
    DEVIL("devil", "06 — DEVIL", "З ріжками", "С рожками", "Devil with horns"),
    RAINBOW("rainbow", "07 — RAINBOW", "Веселковий", "Радужный", "Rainbow style"),
    OUTLINE("outline", "08 — OUTLINE", "Контурний", "Контурный", "Outline style"),
    GLITCH("glitch", "09 — GLITCH", "Глітч-стиль", "Глитч-стиль", "Glitch style"),
    PREMIUM("premium", "10 — PREMIUM", "Преміум варіант", "Премиум вариант", "Premium style");

    private final String id;
    private final String code;
    private final String titleUk;
    private final String titleRu;
    private final String titleEn;

    MiogramBadgeType(String id, String code, String titleUk, String titleRu, String titleEn) {
        this.id = id;
        this.code = code;
        this.titleUk = titleUk;
        this.titleRu = titleRu;
        this.titleEn = titleEn;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return MiogramLocale.get(titleUk, titleRu, titleEn);
    }

    @NonNull
    public static MiogramBadgeType fromId(@Nullable String id) {
        if (id != null) {
            String lower = id.trim().toLowerCase();
            for (MiogramBadgeType type : values()) {
                if (type.id.equals(lower)) {
                    return type;
                }
            }
        }
        return ORIGINAL;
    }
}
