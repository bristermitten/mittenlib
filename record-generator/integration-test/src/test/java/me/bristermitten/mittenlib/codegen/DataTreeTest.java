package me.bristermitten.mittenlib.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

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
