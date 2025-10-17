package me.bristermitten.mittenlib.codegen;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DataTreeTest {
    @Test
    void simpleTest() {
        DataTree tree = DataTree.Integer(1);

        assertThat(tree)
                .asInstanceOf(InstanceOfAssertFactories.type(DataTree.Integer.class))
                .extracting(DataTree.Integer::value)
                .isEqualTo(1L);
    }
}
