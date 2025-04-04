package Highlighters;

import java.awt.Color;
import java.util.regex.Pattern;

public class KotlinHighlighter extends ScriptHighlighter {

    private static final String[] KEYWORDS = {"if", "else", "when", "while", "for", "do", "in", "is", "as", "by"};
    private static final String[] TYPES = {"String", "Int", "Double", "Float", "Boolean", "List", "Map", "Set", "Any", "Unit", "Nothing"};
    private static final String[] CONSTANTS = {"true", "false", "null"};
    private static final String[] DECLARATIONS = {"val", "var", "fun", "class", "interface", "object", "typealias", "this", "super", "constructor"};


    public KotlinHighlighter() {
        super(
                new Color(0,85,250),
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
    protected Pattern getStringPattern() {
        return Pattern.compile("\"([^\"]|\\\\\")*\"|\"\"\".*?\"\"\"|'[^']*'", Pattern.DOTALL);
    }
}