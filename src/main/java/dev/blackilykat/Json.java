package dev.blackilykat;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;

public class Json {
    // if i have to change its properties and stuff™ i can
    public static final Gson GSON = new Gson();

    /**
     * Converts an object to a json string
     * @see Gson#toJson(Object)
     */
    public static String toJson(Object object) {
        return GSON.toJson(object);
    }

    /**
     * Converts the json string to a JsonObject
     * @see Gson#fromJson(String, Type)
     */
    public static JsonObject fromJsonObject(String json) {
        return GSON.fromJson(json, JsonObject.class);
    }

    /**
     * Converts the json string to a JsonElement
     * @see Gson#fromJson(String, Type)
     */
    public static JsonElement fromJsonElement(String json) {
        return GSON.fromJson(json, JsonElement.class);
    }
}
