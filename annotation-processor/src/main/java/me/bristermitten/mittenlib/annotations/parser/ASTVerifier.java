package me.bristermitten.mittenlib.annotations.parser;

import me.bristermitten.mittenlib.annotations.ast.AbstractConfigStructure;

/**
 * Inspects the AST and sends errors/warnings for invalid setups
 */
public class ASTVerifier {

    public void verify(AbstractConfigStructure structure) {
        if(structure instanceof AbstractConfigStructure.Union union) {

        }
    }
}
