package me.bristermitten.mittenlib.config.tree;

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataTreeTransformsTest {

    private static Arbitrary<DataTree> atomicDataTreeArbitrary() {
        return Arbitraries.oneOf(
                Arbitraries.integers().map(DataTree.DataTreeLiteral.DataTreeLiteralInt::new),
                Arbitraries.strings().map(DataTree.DataTreeLiteral.DataTreeLiteralString::new),
                Arbitraries.doubles().map(DataTree.DataTreeLiteral.DataTreeLiteralFloat::new),
                Arbitraries.of(true, false).map(DataTree.DataTreeLiteral.DataTreeLiteralBoolean::new),
                Arbitraries.of(DataTree.DataTreeNull.INSTANCE)
        );
    }

    @Provide
    Arbitrary<DataTree> dataTreeArbitrary() {
        return Arbitraries.recursive(
                DataTreeTransformsTest::atomicDataTreeArbitrary,
                arb -> Arbitraries.oneOf(
                        arb.list().ofMaxSize(5).map(
                                list -> new DataTree.DataTreeArray(list.toArray(new DataTree[0]))
                        ),
                        Arbitraries.maps(arb, arb)
                                .ofMaxSize(5)
                                .map(DataTree.DataTreeMap::new)
                ), 4
        );
    }

    @Property
    void dataTreeTransformsInverse(@ForAll("dataTreeArbitrary") DataTree tree) {
        Object pojo = DataTreeTransforms.toPOJO(tree);

        DataTree loaded = DataTreeTransforms.loadFrom(pojo);

        assertEquals(tree, loaded);
    }
}