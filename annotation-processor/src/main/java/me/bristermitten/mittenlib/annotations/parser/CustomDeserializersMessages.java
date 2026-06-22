package me.bristermitten.mittenlib.annotations.parser;

import io.toolisticon.aptk.compilermessage.api.DeclareCompilerMessage;
import io.toolisticon.aptk.compilermessage.api.DeclareCompilerMessageCodePrefix;

@DeclareCompilerMessageCodePrefix("CUSTOM_DESERIALIZER")
@DeclareCompilerMessage(
        code = "001",
        enumValueName = "INVALID_STATIC_METHOD_SIGNATURE",
        message =
                "Custom deserializer method must be static and be of the signature Result<${0}> deserialize(DeserializationContext)")
@DeclareCompilerMessage(
        code = "002",
        enumValueName = "UNSUPPORTED_NON_STATIC",
        message = "Non static custom deserializers aren't supported yet")
public interface CustomDeserializersMessages {}
