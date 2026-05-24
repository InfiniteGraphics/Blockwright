package top.huliawsl.blockwright.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class JsonHelper {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonHelper() {
    }
}
