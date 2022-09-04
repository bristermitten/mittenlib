package me.bristermitten.mittenlib.lang;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * A message that is composed of multiple {@link LangMessage}s
 * This allows {@link LangMessage}s to be easily "concatenated", allowing for more complex messages to be created
 */
public class CompoundLangMessage extends LangMessage {
    private final List<LangMessage> components;


    /**
     * Create a new CompoundLangMessage
     *
     * @param components the components of this message
     */
    public CompoundLangMessage(@NotNull List<LangMessage> components) {
        super(null, null, null, null, null);
        this.components = ImmutableList.copyOf(components);
    }

    /**
     * Create a new CompoundLangMessage
     *
     * @param components the components of this message
     */
    public CompoundLangMessage(@NotNull LangMessage... components) {
        this(ImmutableList.copyOf(components));
    }

    /**
     * Create a new CompoundLangMessage, concatenating the 2 inputs.
     * This is used to provide slightly more efficient concatenation in ({@link LangMessage#add(LangMessage)})
     *
     * @param components the components of this message
     * @param others     extra components to add
     */
    public CompoundLangMessage(@NotNull List<LangMessage> components, @NotNull LangMessage... others) {
        //noinspection UnstableApiUsage
        this(ImmutableList.<LangMessage>builderWithExpectedSize(components.size() + others.length)
                .addAll(components)
                .add(others)
                .build());
    }

    /**
     * @return the components of this message
     */
    public @Unmodifiable List<LangMessage> getComponents() {
        return components;
    }
}
