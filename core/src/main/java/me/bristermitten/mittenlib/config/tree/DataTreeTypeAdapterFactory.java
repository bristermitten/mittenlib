package me.bristermitten.mittenlib.config.tree;


import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

@SuppressWarnings("ALL")
public class DataTreeTypeAdapterFactory implements TypeAdapterFactory {


    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (DataTree.class.isAssignableFrom(type.getRawType())) {
            return (TypeAdapter<T>) new DataTreeTypeAdapter(() -> gson);
        }
        return null;
    }
}
