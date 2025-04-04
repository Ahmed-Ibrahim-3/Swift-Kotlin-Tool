package Highlighters;

import javax.swing.text.*;
import java.awt.Color;
import java.util.regex.*;

public abstract class ScriptHighlighter {
    protected final StyleContext styleContext;
    protected final Style defaultStyle;
    protected final Style keywordStyle;
    protected final Style typeStyle;
    protected final Style constantStyle;
    protected final Style declarationStyle;
    protected final Style stringStyle;
    protected final Style commentStyle;

    public ScriptHighlighter(
            Color keywordColor,
            Color typeColor,
            Color constantColor,
            Color declarationColor,
            Color stringColor,
            Color commentColor) {

        styleContext = StyleContext.getDefaultStyleContext();
        defaultStyle = styleContext.getStyle(StyleContext.DEFAULT_STYLE);

        keywordStyle = styleContext.addStyle("KeywordStyle", defaultStyle);
        StyleConstants.setForeground(keywordStyle, keywordColor);
        StyleConstants.setBold(keywordStyle, true);

        typeStyle = styleContext.addStyle("TypeStyle", defaultStyle);
        StyleConstants.setForeground(typeStyle, typeColor);

        constantStyle = styleContext.addStyle("ConstantStyle", defaultStyle);
        StyleConstants.setForeground(constantStyle, constantColor);
        StyleConstants.setBold(constantStyle, true);

        declarationStyle = styleContext.addStyle("DeclarationStyle", defaultStyle);
        StyleConstants.setForeground(declarationStyle, declarationColor);
        StyleConstants.setBold(declarationStyle, true);

        stringStyle = styleContext.addStyle("StringStyle", defaultStyle);
        StyleConstants.setForeground(stringStyle, stringColor);

        commentStyle = styleContext.addStyle("CommentStyle", defaultStyle);
        StyleConstants.setForeground(commentStyle, commentColor);
        StyleConstants.setItalic(commentStyle, true);
    }

    protected abstract String[] getKeywords();
    protected abstract String[] getTypes();
    protected abstract String[] getConstants();
    protected abstract String[] getDeclarations();


    protected Pattern getStringPattern() {
        return Pattern.compile("\"([^\"]|\\\\\")*\"", Pattern.DOTALL);
    }


    protected Pattern getSingleLineCommentPattern() {
        return Pattern.compile("//.*$", Pattern.MULTILINE);
    }

    protected Pattern getMultiLineCommentPattern() {
        return Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    }

    public void highlight(StyledDocument doc) {
        try {
            String text = doc.getText(0, doc.getLength());
            doc.setCharacterAttributes(0, text.length(), defaultStyle, true);

            highlightStrings(doc, text);
            highlightComments(doc, text);

            highlightKeywords(doc, text, getKeywords(), keywordStyle);
            highlightKeywords(doc, text, getTypes(), typeStyle);
            highlightKeywords(doc, text, getConstants(), constantStyle);
            highlightKeywords(doc, text, getDeclarations(), declarationStyle);

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    protected void highlightKeywords(StyledDocument doc, String text, String[] keywords, Style style) {
        for (String keyword : keywords) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b");
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                if (!isDefaultStyleAt(doc, matcher.start())) {
                    continue;
                }

                doc.setCharacterAttributes(
                        matcher.start(),
                        matcher.end() - matcher.start(),
                        style,
                        false
                );
            }
        }
    }

    protected void highlightStrings(StyledDocument doc, String text) {
        Pattern pattern = getStringPattern();
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            doc.setCharacterAttributes(
                    matcher.start(),
                    matcher.end() - matcher.start(),
                    stringStyle,
                    false
            );
        }
    }

    protected void highlightComments(StyledDocument doc, String text) {
        Pattern singleLinePattern = getSingleLineCommentPattern();
        Matcher singleLineMatcher = singleLinePattern.matcher(text);

        while (singleLineMatcher.find()) {
            doc.setCharacterAttributes(
                    singleLineMatcher.start(),
                    singleLineMatcher.end() - singleLineMatcher.start(),
                    commentStyle,
                    false
            );
        }

        Pattern multiLinePattern = getMultiLineCommentPattern();
        Matcher multiLineMatcher = multiLinePattern.matcher(text);

        while (multiLineMatcher.find()) {
            doc.setCharacterAttributes(
                    multiLineMatcher.start(),
                    multiLineMatcher.end() - multiLineMatcher.start(),
                    commentStyle,
                    false
            );
        }
    }

    protected boolean isDefaultStyleAt(StyledDocument doc, int pos) {
        Element element = doc.getCharacterElement(pos);
        AttributeSet attrs = element.getAttributes();

        if (attrs.isEqual(defaultStyle)) {
            return true;
        }

        Color fg = StyleConstants.getForeground(attrs);
        Color defaultFg = StyleConstants.getForeground(defaultStyle);

        return fg == null || fg.equals(defaultFg);
    }
}