package com.constanttherapy.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GsonHelper {

    //public static final Type TYPE_FOR_MAP_OF_STRING_STRING = new TypeToken<Map<String, String>>() {}.getType();

    /**
     * Shared JSON parser for serializing/deserializing parameters from the database
     */
    private static Gson _gson;

    public static Gson getGson() {
        if (_gson == null) {
            _gson = getGsonBuilder().create();
        }
        return _gson;
    }

    private static Gson _gsonLite;

    public static Gson getGsonLite() {
        if (_gsonLite == null) {
            _gsonLite = getGsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
        }
        return _gsonLite;
    }

    /**
     * Utility method for converting JSON of parameters to a java Map<String,String>
     */
    public static <K, V> Map<K, V> mapFromJson(String json) {
        Type mapType = new TypeToken<Map<K, V>>() {
        }.getType();
        try {
            Map<K, V> map = getGson().fromJson(json, mapType);
            if (map != null)
                return map;
            else
                return new HashMap<K, V>();
        } catch (JsonSyntaxException e) {
            CTLogger.error(e);
            return new HashMap<K, V>();
        }
    }

    public static <T> T fromJson(String json, Type objectType) {
        // Type objectType = new TypeToken<T>(){}.getType();
        return getGson().fromJson(json, objectType);
    }

    public static <T> List<T> listFromJson(String json) {
        Type listType = new TypeToken<ArrayList<T>>() {
        }.getType();
        return listFromJson(json, listType);  //TODO: doesn't appear to work with custom types
    }

    public static <T> List<T> listFromJson(String json, Type listType) {
        try {
            List<T> list = getGson().fromJson(json, listType);
            if (list != null)
                return list;
            else
                return new ArrayList<T>();

        } catch (JsonSyntaxException e) {
            CTLogger.error(e);
            return new ArrayList<T>();
        }
    }


    private static GsonBuilder getGsonBuilder() {
        GsonBuilder builder = new GsonBuilder();
        /*
		 * We need to customize Gson to serialize/deserialize UNIX
		 * timestamps for all the Timestamp instance variables that show
		 * up throughout our classes
		 * UNIX timestamps are a standard method of representing dates, and
		 * they're much easier to generate on the iPad ;)
		 */
        builder.registerTypeAdapter(Timestamp.class, new JsonSerializer<Timestamp>() {
            @Override
            public JsonElement serialize(Timestamp timestamp, Type type, JsonSerializationContext context) {
                return new JsonPrimitive(timestamp.getTime() / 1000);
            }
        });
        builder.registerTypeAdapter(Timestamp.class, new JsonDeserializer<Timestamp>() {
            @Override
            public Timestamp deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context)
                    throws JsonParseException {
                Long timestamp = jsonElement.getAsLong();
                return new Timestamp(timestamp * 1000);
            }
        });
        builder.registerTypeAdapter(Date.class, new JsonSerializer<Date>() {
            @Override
            public JsonElement serialize(Date date, Type type, JsonSerializationContext context) {
                return new JsonPrimitive(date.getTime() / 1000);
            }
        });
        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context)
                    throws JsonParseException {
                Long timestamp = jsonElement.getAsLong();
                return new Date(timestamp * 1000);
            }
        });

        return builder;
    }

    public static String toJson(Object o) {
        return getGson().toJson(o);
    }

    public static String toJsonSerializeNulls(Object o) {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(o);
    }

    /**
     * Android client uses straight up JSON, not the altered version that the iPad uses, since the
     * Android client has native support for java.sql.Timestamp and the other Java types that we use
     * in our server objects
     */
    public static String toJson(Object o, Integer clientPlatform) {
        if (ClientCharacteristicsHelper.isAndroidClientQueryParam(clientPlatform))
            return (new Gson()).toJson(o);
        else
            return getGson().toJson(o);
    }

    public static String toJson(Object o, Type t) {
        return getGson().toJson(o, t);
    }

    /**
     * Excludes fields not marked with @Expose
     *
     * @param o Object to be serialized
     * @return JSON string
     */
    public static String toJsonLite(Object o) {
        return getGsonLite().toJson(o);
    }
}
