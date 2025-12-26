package me.bristermitten.mittenlib.annotations.codegen;

import com.squareup.javapoet.TypeName;

import java.util.*;

/**
 * Manages variable scope and naming during code generation.
 * This class tracks variables in scope and generates unique variable names to avoid conflicts.
 */
public class Scope {
    private final Map<String, Variable> variables = new LinkedHashMap<>();
    private int anonymousVarCounter = 0;

    /**
     * Declares a variable with a specific name and type.
     *
     * @param name The variable name
     * @param type The variable type
     * @return The created Variable
     * @throws IllegalStateException if a variable with that name already exists in scope
     */
    public Variable declare(String name, TypeName type) {
        if (variables.containsKey(name)) {
            throw new IllegalStateException("Variable " + name + " already exists in scope");
        }
        Variable variable = new Variable(name, type);
        variables.put(name, variable);
        return variable;
    }

    /**
     * Declares an anonymous variable with an auto-generated name.
     * The name will be in the format "var0", "var1", etc.
     *
     * @param type The variable type
     * @return The created Variable
     */
    public Variable declareAnonymous(TypeName type) {
        String name = "var" + anonymousVarCounter++;
        return declare(name, type);
    }

    /**
     * Gets a variable by name if it exists in scope.
     *
     * @param name The variable name
     * @return Optional containing the variable if found
     */
    public Optional<Variable> get(String name) {
        return Optional.ofNullable(variables.get(name));
    }

    /**
     * Checks if a variable with the given name exists in scope.
     *
     * @param name The variable name
     * @return true if the variable exists
     */
    public boolean contains(String name) {
        return variables.containsKey(name);
    }

    /**
     * Gets all variables currently in scope.
     *
     * @return A list of all variables in declaration order
     */
    public List<Variable> allVariables() {
        return new ArrayList<>(variables.values());
    }

    /**
     * Creates a child scope that inherits all variables from this scope.
     * Variables declared in the child scope will not affect the parent.
     *
     * @return A new child Scope
     */
    public Scope createChild() {
        Scope child = new Scope();
        // Copy parent variables to child
        child.variables.putAll(this.variables);
        // Child inherits the parent's counter to continue the sequence
        child.anonymousVarCounter = this.anonymousVarCounter;
        return child;
    }
}
