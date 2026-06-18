package me.bristermitten.mittenlib.codegen;

import io.toolisticon.aptk.compilermessage.api.DeclareCompilerMessage;

@DeclareCompilerMessage(
        code = "001",
        enumValueName = "METHOD_BAD_RETURN",
        message = "Method must return the record type ${0}!")
@DeclareCompilerMessage(
        code = "002",
        enumValueName = "DUPLICATE_CONSTRUCTOR",
        message = "Constructors must have distinct names, overloading is not allowed")
@DeclareCompilerMessage(code = "003", enumValueName = "INVALID_RECORD", message = "Could not parse record ${0}.")
public interface MittenLibCodegenProcessorMessages {}
