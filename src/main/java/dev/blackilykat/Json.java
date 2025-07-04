/*
 * Copyright (C) 2025 Blackilykat
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.blackilykat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;

public class Json {
    // if i have to change its properties and stuff™ i can
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

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
