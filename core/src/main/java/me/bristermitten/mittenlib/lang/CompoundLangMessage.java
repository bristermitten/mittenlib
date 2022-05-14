package me.bristermitten.mittenlib.lang;

import com.google.common.collect.ImmutableList;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public class CompoundLangMessage extends LangMessage {
    private final List<LangMessage> components;


    public CompoundLangMessage(@NotNull List<LangMessage> components) {
        super(null, null, null, null, null);
        this.components = ImmutableList.copyOf(components);
    }

    public CompoundLangMessage(@NotNull LangMessage... components) {
        this(ImmutableList.copyOf(components));
    }

    public CompoundLangMessage(@NotNull List<LangMessage> components, @NotNull LangMessage... others) {
        //noinspection UnstableApiUsage
        this(ImmutableList.<LangMessage>builderWithExpectedSize(5)
                .addAll(components)
                .add(others)
                .build());
    }

    public @Unmodifiable List<LangMessage> getComponents() {
        return components;
    }
}
