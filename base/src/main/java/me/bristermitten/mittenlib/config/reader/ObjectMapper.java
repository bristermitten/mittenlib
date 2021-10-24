package me.bristermitten.mittenlib.config.reader;

import me.bristermitten.mittenlib.util.Result;

import java.util.Map;

public interface ObjectMapper {
    <T> Result<T> map(Map<Object, Object> map, Class<T> type);

    <T> Map<Object, Object> map(T t);

}
