package dev.blackilykat;

import org.h2.mvstore.MVStore;

import java.io.File;
import java.util.Map;

public class Storage {
    public static final File LIBRARY = new File("library/");

    public static Map<String, Object> general;

    public static void init() {
        MVStore mvStore = MVStore.open("db");
        general = mvStore.openMap("general");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            mvStore.close();
        }));

        if(!LIBRARY.exists()) {
            LIBRARY.mkdir();
        }
    }

    public static int getCurrentActionID() {
        return (Integer) general.getOrDefault("currentActionID", -1);
    }

    public static void setCurrentActionID(int newValue) {
        general.put("currentActionID", newValue);
    }
}
