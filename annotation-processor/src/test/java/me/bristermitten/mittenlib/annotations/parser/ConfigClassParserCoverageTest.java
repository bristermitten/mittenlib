package me.bristermitten.mittenlib.annotations.parser;

import io.toolisticon.aptk.common.ToolingProvider;
import io.toolisticon.cute.Cute;
import io.toolisticon.cute.PassIn;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import me.bristermitten.mittenlib.annotations.compile.ConfigNameCache;
import me.bristermitten.mittenlib.annotations.compile.ConfigurationClassNameGenerator;
import me.bristermitten.mittenlib.annotations.compile.GeneratedTypeCache;
import me.bristermitten.mittenlib.annotations.util.ElementsFinder;
import me.bristermitten.mittenlib.annotations.util.TypesUtil;
import org.junit.jupiter.api.Test;

class ConfigClassParserCoverageTest {

    @Test
    void testParsingMissingConfigAnnotationThrowsException() {
        Cute.unitTest()
                .when()
                .passInElement()
                .<TypeElement>fromClass(MissingConfigDTO.class)
                .intoUnitTest((processingEnvironment, element) -> {
                    ToolingProvider.setTooling(processingEnvironment);
                    ConfigClassParser parser = createParser(processingEnvironment);
                    try {
                        parser.parseAbstract(element);
                    } catch (IllegalStateException e) {
                        // Expected
                    }
                })
                .thenExpectThat()
                .compilationFails()
                .andThat()
                .compilerMessage()
                .ofKindError()
                .contains("does not have a @Config annotation")
                .executeTest();
    }

    private ConfigClassParser createParser(ProcessingEnvironment processingEnvironment) {
        GeneratedTypeCache cache = new GeneratedTypeCache();
        TypesUtil typesUtil =
                new TypesUtil(processingEnvironment.getTypeUtils(), processingEnvironment.getElementUtils(), cache);
        ConfigNameCache nameCache = new ConfigNameCache();
        return new ConfigClassParser(
                typesUtil,
                new ElementsFinder(processingEnvironment.getElementUtils()),
                nameCache,
                cache,
                new ConfigurationClassNameGenerator(nameCache),
                processingEnvironment);
    }

    @PassIn
    static class MissingConfigDTO {
        public int x;
    }
}
