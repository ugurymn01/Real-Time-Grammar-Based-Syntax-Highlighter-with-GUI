import java.util.*;
import java.util.regex.*;

public class Lexer {
    public enum TokenType {
        KEYWORD,       // int, if, else, while, return
        NUMBER,        // 123, 45, vs.
        OPERATOR,      // = + - * / < > !
        PAREN,         // ( ) { }
        SEPARATOR,     // ;
        IDENTIFIER     // x, y, myVar, vs.
    }

    public static class Token {
        public TokenType type;
        public int start, end;

        public Token(TokenType type, int start, int end) {
            this.type = type;
            this.start = start;
            this.end = end;
        }
    }

    // Tek bir regex içinde named-group yöntemiyle bütün token türleri
    private static final Pattern MASTER_PATTERN = Pattern.compile(
            ""
                    + "(?<KEYWORD>\\b(?:int|if|else|while|return)\\b)"  // önce keyword
                    + "|(?<NUMBER>\\b\\d+\\b)"                          // sonra number
                    + "|(?<OPERATOR>[=+\\-*/<>!])"                      // operatörler
                    + "|(?<PAREN>[(){}])"                               // parantezler
                    + "|(?<SEPARATOR>;)"                                // noktalı virgül
                    + "|(?<IDENTIFIER>\\b[a-zA-Z_][a-zA-Z0-9_]*\\b)"    // en sonda identifier
            // NOT: Boşluk, tab, newline’ları ayrıca yakalamıyoruz; bunların arasındaki token’ları buluyoruz.
    );

    public static List<Token> tokenize(String text) {
        List<Token> tokens = new ArrayList<>();
        Matcher m = MASTER_PATTERN.matcher(text);

        while (m.find()) {
            TokenType type = null;

            // Hangi named group yakalandı, ona bak
            if (m.group("KEYWORD") != null) {
                type = TokenType.KEYWORD;
            } else if (m.group("NUMBER") != null) {
                type = TokenType.NUMBER;
            } else if (m.group("OPERATOR") != null) {
                type = TokenType.OPERATOR;
            } else if (m.group("PAREN") != null) {
                type = TokenType.PAREN;
            } else if (m.group("SEPARATOR") != null) {
                type = TokenType.SEPARATOR;
            } else if (m.group("IDENTIFIER") != null) {
                type = TokenType.IDENTIFIER;
            }

            if (type != null) {
                tokens.add(new Token(type, m.start(), m.end()));
            }
        }

        return tokens;
    }
}
