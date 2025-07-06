package me.bristermitten.mittenlib.annotations.config;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import io.toolisticon.aptk.tools.TypeMirrorWrapper;
import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;

import javax.inject.Singleton;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class ConfigNameCache {
    private final Map<ClassName, AbstractConfigStructure> astCache = new HashMap<>();

    public Optional<AbstractConfigStructure> lookupAST(ClassName name) {
        return Optional.ofNullable(astCache.get(name));
    }

    public Optional<AbstractConfigStructure> lookupAST(TypeName name) {
        if (name instanceof ClassName cn) {
            return Optional.ofNullable(astCache.get(cn));
        }
        return Optional.empty();
    }

    public Optional<AbstractConfigStructure> lookupAST(TypeMirror mirror) {
        if (mirror.getKind() != TypeKind.DECLARED) {
            return Optional.empty();
        }
        return lookupAST(ClassName.bestGuess(TypeMirrorWrapper.wrap(mirror)
                .getQualifiedName()));
    }

    public void put(AbstractConfigStructure ast) {
        astCache.put(ast.name(), ast);
    }
}
