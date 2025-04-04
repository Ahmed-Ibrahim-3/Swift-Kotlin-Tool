package Highlighters;

import java.awt.Color;
import java.util.regex.*;

public class SwiftHighlighter extends ScriptHighlighter {

    private static final String[] KEYWORDS = {"if", "else", "while", "for", "switch", "case", "default", "guard", "defer", "where", "in"};
    private static final String[] TYPES = {"String", "Int", "Double", "Float", "Bool", "Array", "Dictionary", "Set"};
    private static final String[] CONSTANTS = {"true", "false", "nil"};
    private static final String[] DECLARATIONS = {"var", "let", "func", "class", "struct", "enum", "protocol", "extension"};


    public SwiftHighlighter() {
        super(
                new Color(0,0,255),
                new Color(139,0,0),
                new Color(128,0,128),
                new Color(255,140,0),
                new Color(0,100,0),
                new Color(128,128,128)
        );
    }

    @Override
    protected String[] getKeywords() {
        return KEYWORDS;
    }

    @Override
    protected String[] getTypes() {
        return TYPES;
    }

    @Override
    protected String[] getConstants() {
        return CONSTANTS;
    }

    @Override
    protected String[] getDeclarations() {
        return DECLARATIONS;
    }

    @Override
    protected Pattern getStringPattern(){
        return Pattern.compile("\"([^\"]|\\\\\")*\"|\"\"\".+?\"\"\"", Pattern.DOTALL);
    }
}