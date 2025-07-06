package me.bristermitten.mittenlib.annotations.ast;

import me.bristermitten.mittenlib.config.Config;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Optional;

/**
 * Where a config came from (its declaring class/interface annotated with @{@link Config})
 */
public sealed interface ConfigTypeSource {
    TypeElement element();

    List<TypeMirror> parents();

    record ClassConfigTypeSource(TypeElement element, Optional<TypeMirror> parent) implements ConfigTypeSource {
        @Override
        public @NotNull List<TypeMirror> parents() {
            return parent.stream().toList();
        }
    }

    record InterfaceConfigTypeSource(TypeElement element, List<TypeMirror> parents) implements ConfigTypeSource {
    }
}