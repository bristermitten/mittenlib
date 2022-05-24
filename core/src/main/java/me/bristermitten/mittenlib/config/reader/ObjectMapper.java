package me.bristermitten.mittenlib.config.reader;

import com.google.gson.reflect.TypeToken;
import me.bristermitten.mittenlib.util.Result;

import java.util.Map;

/**
 * Interface for mapping objects
 * It should hold that <code>map(map(t), T.class).getOrThrow().equals(t)</code>, i.e both map functions are inverses
 * <p>
 * There are no guarantees made about the Objects taken and returned. An ObjectMapper may use any type it wishes,
 * as long as the inverses hold true.
 * 1 ObjectMapper is not required to produce an output compatible with other mappers.
 */
public interface ObjectMapper {
    /**
     * Map a given object and type to an object of the Type
     *
     * @param data The data to map. Usually, this will be a {@link Map}}, but not always
     * @param type Class of the type to map to
     * @param <T>  The type to map to
     * @return A result containing either a mapped object, or an error
     */
    <T> Result<T> map(Object data, TypeToken<T> type);

    /**
     * Turn a given object into a mapped object
     *
     * @param value The type being mapped
     * @return A mapped representation of the object
     */
    Object map(Object value);

}
