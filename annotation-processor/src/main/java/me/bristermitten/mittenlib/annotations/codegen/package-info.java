/**
 * Pure, declarative codegen DSL package for type-safe code generation.
 * 
 * This package provides a Domain-Specific Language (DSL) for generating code in a
 * structured, type-safe, and purely functional manner. It separates the specification
 * of code generation operations from their execution, providing a single impure boundary.
 * 
 * <h2>Architecture:</h2>
 * <ul>
 *   <li><strong>Pure specification</strong> - Code generation is represented as immutable data structures</li>
 *   <li><strong>Functional composition</strong> - Operations compose without side effects</li>
 *   <li><strong>Single impure boundary</strong> - Only {@code apply()} methods mutate builders</li>
 * </ul>
 * 
 * <h2>Key Components:</h2>
 * <ul>
 *   <li>{@link me.bristermitten.mittenlib.annotations.codegen.CodeGenDSL} - Pure data structures for code operations</li>
 *   <li>{@link me.bristermitten.mittenlib.annotations.codegen.CodeGenMonad} - Functional monad for composable generation</li>
 *   <li>{@link me.bristermitten.mittenlib.annotations.codegen.FlatMapChainBuilder} - Builds flatMap chains automatically</li>
 *   <li>{@link me.bristermitten.mittenlib.annotations.codegen.Variable} - Typed variable representation</li>
 *   <li>{@link me.bristermitten.mittenlib.annotations.codegen.Scope} - Automatic variable scoping</li>
 *   <li>{@link me.bristermitten.mittenlib.annotations.codegen.CodeGenBuilder} - Higher-level code building</li>
 *   <li>{@link me.bristermitten.mittenlib.annotations.codegen.CodeGenNames} - Centralized name constants</li>
 * </ul>
 * 
 * <h2>Example Usage:</h2>
 * <pre>
 * // Pure specification of code generation
 * CodeGenResult result = CodeGenDSL.statement("int x = $L", 10)
 *     .combine(CodeGenDSL.returnValue("x * 2"));
 * 
 * // Functional monad for trying multiple strategies
 * CodeGenMonad.pure(methodBuilder)
 *     .tryCase(() -> CodeGenDSL.returnValue("$T.success($L)", Result.class, "value"))
 *     .orElse(() -> CodeGenDSL.returnValue("$T.error($S)", Result.class, "Failed"))
 *     .apply();  // Single point of mutation
 * </pre>
 */
package me.bristermitten.mittenlib.annotations.codegen;
