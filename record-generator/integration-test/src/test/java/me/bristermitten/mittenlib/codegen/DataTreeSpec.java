package me.bristermitten.mittenlib.codegen;

import java.util.Map;

/**
 * Copy of {@link me.bristermitten.mittenlib.config.tree.DataTree}
 */
@UnionSpec
public interface DataTreeSpec {
    DataTreeSpec Null();

    DataTreeSpec String(String value);

    DataTreeSpec Integer(long value);

    DataTreeSpec Float(double value);

    DataTreeSpec Boolean(boolean value);

    DataTreeSpec Map(Map<DataTreeSpec, DataTreeSpec> map);

    DataTreeSpec Array(DataTreeSpec[] values);


}
