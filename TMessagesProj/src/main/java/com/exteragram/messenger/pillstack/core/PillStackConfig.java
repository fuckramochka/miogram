package com.exteragram.messenger.pillstack.core;

import java.util.ArrayList;

public final class PillStackConfig {
    private PillStackConfig() {
    }

    public static ArrayList<Integer> getActivePills() {
        app.exteraless.pillstack.PillStackConfig.loadConfig(false);
        return app.exteraless.pillstack.PillStackConfig.getActivePills();
    }

    public static ArrayList<Integer> getHiddenPills() {
        app.exteraless.pillstack.PillStackConfig.loadConfig(false);
        return app.exteraless.pillstack.PillStackConfig.getHiddenPills();
    }

    public static int getLastActivePillId() {
        app.exteraless.pillstack.PillStackConfig.loadConfig(false);
        return app.exteraless.pillstack.PillStackConfig.lastActivePillId.Int();
    }
}
