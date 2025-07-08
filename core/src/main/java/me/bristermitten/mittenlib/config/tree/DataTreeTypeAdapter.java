package me.bristermitten.mittenlib.config.tree;


import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.bristermitten.mittenlib.files.json.ExtraTypeAdapter;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DataTreeTypeAdapter extends ExtraTypeAdapter<DataTree> {
    private final Provider<Gson> gson;

    @Inject
    DataTreeTypeAdapter(Provider<Gson> gson) {
        this.gson = gson;
    }

    @Override
    public Type type() {
        return DataTree.class;
    }

    @Override
    public void write(JsonWriter out, DataTree value) {
        gson.get().toJson(gson.get().toJsonTree(DataTreeTransforms.toPOJO(value)), out);
    }

    @Override
    public DataTree read(JsonReader in) throws IOException {
        switch (in.peek()) {
            case NULL:
                in.nextNull();
                return DataTree.DataTreeNull.INSTANCE;
            case BEGIN_OBJECT:
                in.beginObject();
                Map<DataTree, DataTree> map = new LinkedHashMap<>();
                while (in.hasNext()) {
                    DataTree key = read(in);
                    DataTree value = read(in);
                    map.put(key, value);
                }
                in.endObject();
                return new DataTree.DataTreeMap(map);
            case BEGIN_ARRAY:
                in.beginArray();
                List<DataTree> list = new LinkedList<>();
                while (in.hasNext()) {
                    list.add(read(in));
                }
                in.endArray();
                return new DataTree.DataTreeArray(list.toArray(new DataTree[0]));

            case STRING:
                String s = in.nextString();
                return new DataTree.DataTreeLiteral.DataTreeLiteralString(s);

            case NAME:
                String name = in.nextName();
                return new DataTree.DataTreeLiteral.DataTreeLiteralString(name);

            case NUMBER:
                try {
                    int i = in.nextInt();
                    return new DataTree.DataTreeLiteral.DataTreeLiteralInt(i);
                } catch (NumberFormatException e) {
                    return new DataTree.DataTreeLiteral.DataTreeLiteralFloat(in.nextDouble());
                }
            case BOOLEAN:
                return new DataTree.DataTreeLiteral.DataTreeLiteralBoolean(in.nextBoolean());
        }
        throw new IllegalStateException("Invalid DataTree type: " + in.peek());
    }
}
