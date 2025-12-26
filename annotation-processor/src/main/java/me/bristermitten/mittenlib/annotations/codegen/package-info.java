/**
 * Codegen DSL package for type-safe code generation.
 * 
 * This package provides a Domain-Specific Language (DSL) for generating code in a more
 * structured and type-safe manner. It eliminates the need for hardcoded strings and manual
 * tracking of variables, scopes, and control flow.
 * 
 * Key components:
 * <ul>
 *   <li>{@link me.bristermitten.mittenlib.annotations.codegen.Variable} - Represents typed variables</li>
 *   <li>{@link me.bristermitten.mittenlib.annotations.codegen.Scope} - Manages variable scoping</li>
 *   <li>{@link me.bristermitten.mittenlib.annotations.codegen.CodeGenBuilder} - Higher-level code building</li>
 *   <li>{@link me.bristermitten.mittenlib.annotations.codegen.FlatMapChainBuilder} - Builds flatMap chains automatically</li>
 *   <li>{@link me.bristermitten.mittenlib.annotations.codegen.CodeGenNames} - Centralized name constants</li>
 * </ul>
 */
package me.bristermitten.mittenlib.annotations.codegen;
