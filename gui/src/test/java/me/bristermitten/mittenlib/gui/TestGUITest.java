package me.bristermitten.mittenlib.gui;

import org.junit.jupiter.api.Test;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withTextFromSystemIn;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGUITest {


    @Test
    public void test() throws Exception {
        withTextFromSystemIn("3\n")
                .execute(() -> {
                    Counter finalModel = new GUIExecutor<>(new TestGUI())
                            .execute();

                    assertEquals(42, finalModel.value(), "Counter should be 42 after execution");
                });

    }
}
