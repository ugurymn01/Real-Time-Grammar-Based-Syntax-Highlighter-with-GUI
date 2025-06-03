import java.util.List;

public class Parser {
    // Hatalı token'ın başladığı ve bittiği indeksleri burada tutulacak
    public static int errorStart = -1;
    public static int errorEnd   = -1;

    /**
     * parse(...) metodu:
     *  • tokens: Lexer.tokenize(text) ile elde edilen token listesi
     *  • fullText: metnin tamamı (String)
     *
     * Döndürür:
     *  • true  ⇒ sözdizimi geçerli
     *  • false ⇒ sözdizimi hatalı (errorStart/errorEnd dolu)
     */
    public static boolean parse(List<Lexer.Token> tokens, String fullText) {
        // Her çağrıldığında hata konumlarını sıfırla
        errorStart = -1;
        errorEnd   = -1;
        int i = 0;

        while (i < tokens.size()) {
            Lexer.Token token = tokens.get(i);
            String tokenText = getTokenText(token, fullText);

            // --- 1) "int x = 5;" yapısını kontrol et ---
            if (token.type == Lexer.TokenType.KEYWORD && tokenText.equals("int")) {
                // 1.1) i+1 ⇒ IDENTIFIER olmalı
                if (i + 1 >= tokens.size() || tokens.get(i + 1).type != Lexer.TokenType.IDENTIFIER) {
                    if (i + 1 < tokens.size()) {
                        Lexer.Token t = tokens.get(i + 1);
                        errorStart = t.start;
                        errorEnd   = t.end;
                    } else {
                        //  "int " yazılıp identifier eksikse "int" token'ının bitişine vurgu
                        errorStart = token.end;
                        errorEnd   = token.end;
                    }
                    return false;
                }

                // 1.2) i+2 ⇒ OPERATOR "=" olmalı
                if (i + 2 >= tokens.size()) {
                    // "=" eksikse identifier üzerine vurgu
                    Lexer.Token t = tokens.get(i + 1);
                    errorStart = t.start;
                    errorEnd   = t.end;
                    return false;
                }
                Lexer.Token tokOp = tokens.get(i + 2);
                String opText = getTokenText(tokOp, fullText);
                if (tokOp.type != Lexer.TokenType.OPERATOR || !opText.equals("=")) {
                    errorStart = tokOp.start;
                    errorEnd   = tokOp.end;
                    return false;
                }

                // 1.3) i+3 ⇒ NUMBER olmalı
                if (i + 3 >= tokens.size() || tokens.get(i + 3).type != Lexer.TokenType.NUMBER) {
                    if (i + 3 < tokens.size()) {
                        Lexer.Token t = tokens.get(i + 3);
                        errorStart = t.start;
                        errorEnd   = t.end;
                    } else {
                        // "=" yazıldı ama sayı eksikse "=" üzerine vurgu
                        errorStart = tokOp.start;
                        errorEnd   = tokOp.end;
                    }
                    return false;
                }

                // 1.4) i+4 ⇒ SEPARATOR ";" olmalı
                if (i + 4 >= tokens.size()) {
                    // noktalı virgül eksikse "5" token'ı üzerine vurgu
                    Lexer.Token tokNumber = tokens.get(i + 3);
                    errorStart = tokNumber.start;
                    errorEnd   = tokNumber.end;
                    return false;
                }
                Lexer.Token tokSep = tokens.get(i + 4);
                String sepText = getTokenText(tokSep, fullText);
                if (tokSep.type != Lexer.TokenType.SEPARATOR || !sepText.equals(";")) {
                    errorStart = tokSep.start;
                    errorEnd   = tokSep.end;
                    return false;
                }

                // "int x = 5;" doğru ise bir sonraki token'a geç
                i += 5;
                continue;
            }

            // --- 2) "if(x > 3) { }" yapısını kontrol et ---
            else if (token.type == Lexer.TokenType.KEYWORD && tokenText.equals("if")) {
                // 2.1) i+1 ⇒ "(" olmalı
                if (i + 1 >= tokens.size()) {
                    // if sonrası "(" eksikse if token'unun bitişine vurgu
                    errorStart = token.end;
                    errorEnd   = token.end;
                    return false;
                }
                Lexer.Token tokLp = tokens.get(i + 1);
                String lpText = getTokenText(tokLp, fullText);
                if (tokLp.type != Lexer.TokenType.PAREN || !lpText.equals("(")) {
                    errorStart = tokLp.start;
                    errorEnd   = tokLp.end;
                    return false;
                }

                // 2.2) i+2 ⇒ IDENTIFIER olmalı
                if (i + 2 >= tokens.size() || tokens.get(i + 2).type != Lexer.TokenType.IDENTIFIER) {
                    if (i + 2 < tokens.size()) {
                        Lexer.Token t = tokens.get(i + 2);
                        errorStart = t.start;
                        errorEnd   = t.end;
                    } else {
                        // "(" yazıldı ama identifier eksikse "(" üzerine vurgu
                        errorStart = tokLp.start;
                        errorEnd   = tokLp.end;
                    }
                    return false;
                }

                // 2.3) i+3 ⇒ OPERATOR (örneğin ">") olmalı
                if (i + 3 >= tokens.size() || tokens.get(i + 3).type != Lexer.TokenType.OPERATOR) {
                    if (i + 3 < tokens.size()) {
                        Lexer.Token t = tokens.get(i + 3);
                        errorStart = t.start;
                        errorEnd   = t.end;
                    } else {
                        // identifier yazıldı ama operatör eksikse identifier üzerine vurgu
                        Lexer.Token t = tokens.get(i + 2);
                        errorStart = t.start;
                        errorEnd   = t.end;
                    }
                    return false;
                }

                // 2.4) i+4 ⇒ NUMBER olmalı
                if (i + 4 >= tokens.size() || tokens.get(i + 4).type != Lexer.TokenType.NUMBER) {
                    if (i + 4 < tokens.size()) {
                        Lexer.Token t = tokens.get(i + 4);
                        errorStart = t.start;
                        errorEnd   = t.end;
                    } else {
                        // operatörden sonra sayı eksikse operatör üzerine vurgu
                        Lexer.Token t = tokens.get(i + 3);
                        errorStart = t.start;
                        errorEnd   = t.end;
                    }
                    return false;
                }

                // 2.5) i+5 ⇒ ")" olmalı
                if (i + 5 >= tokens.size()) {
                    // sayı yazıldı ama ")" eksikse sayı üzerine vurgu
                    Lexer.Token t = tokens.get(i + 4);
                    errorStart = t.start;
                    errorEnd   = t.end;
                    return false;
                }
                Lexer.Token tokRp = tokens.get(i + 5);
                String rpText = getTokenText(tokRp, fullText);
                if (tokRp.type != Lexer.TokenType.PAREN || !rpText.equals(")")) {
                    errorStart = tokRp.start;
                    errorEnd   = tokRp.end;
                    return false;
                }

                // 2.6) i+6 ⇒ "{" olmalı
                if (i + 6 >= tokens.size()) {
                    // ")" yazıldı ama "{" eksikse ")" üzerine vurgu
                    errorStart = tokRp.start;
                    errorEnd   = tokRp.end;
                    return false;
                }
                Lexer.Token tokLb = tokens.get(i + 6);
                String lbText = getTokenText(tokLb, fullText);
                if (tokLb.type != Lexer.TokenType.PAREN || !lbText.equals("{")) {
                    errorStart = tokLb.start;
                    errorEnd   = tokLb.end;
                    return false;
                }

                // 2.7) i+7 ⇒ "}" olmalı
                if (i + 7 >= tokens.size()) {
                    // "{" yazıldı ama "}" eksikse "{" üzerine vurgu
                    errorStart = tokLb.start;
                    errorEnd   = tokLb.end;
                    return false;
                }
                Lexer.Token tokRb = tokens.get(i + 7);
                String rbText = getTokenText(tokRb, fullText);
                if (tokRb.type != Lexer.TokenType.PAREN || !rbText.equals("}")) {
                    errorStart = tokRb.start;
                    errorEnd   = tokRb.end;
                    return false;
                }

                // "if(x>3){ }" bloğu doğruysa bir sonraki token'a geç
                i += 8;
                continue;
            }

            // --- 3) Atama satırlarını kontrol et:
            //      - "x = x + 6;"
            //      - "x = +6;"
            //      - "x = 6;"  gibi durumlar ---
            else if (token.type == Lexer.TokenType.IDENTIFIER) {
                // 3.1) i+1 ⇒ "=" operatörü olmalı
                if (i + 1 >= tokens.size()) {
                    // "=" eksikse identifier üzerine vurgu
                    errorStart = token.start;
                    errorEnd   = token.end;
                    return false;
                }
                Lexer.Token tokEq = tokens.get(i + 1);
                String eqText = getTokenText(tokEq, fullText);
                if (tokEq.type != Lexer.TokenType.OPERATOR || !eqText.equals("=")) {
                    errorStart = tokEq.start;
                    errorEnd   = tokEq.end;
                    return false;
                }

                // 3A) "x = x + 6;" kuralı
                if (i + 2 < tokens.size() && tokens.get(i + 2).type == Lexer.TokenType.IDENTIFIER) {
                    //   i+2 ⇒ IDENTIFIER
                    //   i+3 ⇒ OPERATOR (örneğin "+")
                    //   i+4 ⇒ NUMBER
                    //   i+5 ⇒ SEPARATOR ";"
                    // 3A.1) i+3 ⇒ OPERATOR olmalı
                    if (i + 3 >= tokens.size() || tokens.get(i + 3).type != Lexer.TokenType.OPERATOR) {
                        if (i + 3 < tokens.size()) {
                            Lexer.Token t = tokens.get(i + 3);
                            errorStart = t.start;
                            errorEnd   = t.end;
                        } else {
                            // operatör eksikse identifier üzerine vurgu
                            Lexer.Token t = tokens.get(i + 2);
                            errorStart = t.start;
                            errorEnd   = t.end;
                        }
                        return false;
                    }
                    // 3A.2) i+4 ⇒ NUMBER olmalı
                    if (i + 4 >= tokens.size() || tokens.get(i + 4).type != Lexer.TokenType.NUMBER) {
                        if (i + 4 < tokens.size()) {
                            Lexer.Token t = tokens.get(i + 4);
                            errorStart = t.start;
                            errorEnd   = t.end;
                        } else {
                            // "+" yazıldı ama sayı eksikse "+" üzerine vurgu
                            Lexer.Token t = tokens.get(i + 3);
                            errorStart = t.start;
                            errorEnd   = t.end;
                        }
                        return false;
                    }
                    // 3A.3) i+5 ⇒ SEPARATOR ";" olmalı
                    if (i + 5 >= tokens.size()) {
                        // noktalı virgül eksikse sayı üzerine vurgu
                        Lexer.Token t = tokens.get(i + 4);
                        errorStart = t.start;
                        errorEnd   = t.end;
                        return false;
                    }
                    Lexer.Token tokSc = tokens.get(i + 5);
                    String scText = getTokenText(tokSc, fullText);
                    if (tokSc.type != Lexer.TokenType.SEPARATOR || !scText.equals(";")) {
                        errorStart = tokSc.start;
                        errorEnd   = tokSc.end;
                        return false;
                    }
                    // "x = x + 6;" doğru → i'yi 6 artır
                    i += 6;
                    continue;
                }

                // 3B) "x = +6;" veya "x = -6;" kuralı
                else if (i + 2 < tokens.size()
                        && tokens.get(i + 2).type == Lexer.TokenType.OPERATOR
                        && (getTokenText(tokens.get(i + 2), fullText).equals("+")
                        || getTokenText(tokens.get(i + 2), fullText).equals("-"))) {
                    //   i+2 ⇒ OPERATOR ("+" veya "-")
                    //   i+3 ⇒ NUMBER
                    //   i+4 ⇒ SEPARATOR ";"
                    Lexer.Token tokSign = tokens.get(i + 2);
                    // 3B.1) i+3 ⇒ NUMBER olmalı
                    if (i + 3 >= tokens.size() || tokens.get(i + 3).type != Lexer.TokenType.NUMBER) {
                        if (i + 3 < tokens.size()) {
                            Lexer.Token t = tokens.get(i + 3);
                            errorStart = t.start;
                            errorEnd   = t.end;
                        } else {
                            // "+" veya "-" yazıldı ama sayı eksikse sign üzerine vurgu
                            errorStart = tokSign.start;
                            errorEnd   = tokSign.end;
                        }
                        return false;
                    }
                    // 3B.2) i+4 ⇒ SEPARATOR ";" olmalı
                    if (i + 4 >= tokens.size()) {
                        // noktalı virgül eksikse sayı üzerine vurgu
                        Lexer.Token t = tokens.get(i + 3);
                        errorStart = t.start;
                        errorEnd   = t.end;
                        return false;
                    }
                    Lexer.Token tokSc2 = tokens.get(i + 4);
                    String sc2Text = getTokenText(tokSc2, fullText);
                    if (tokSc2.type != Lexer.TokenType.SEPARATOR || !sc2Text.equals(";")) {
                        errorStart = tokSc2.start;
                        errorEnd   = tokSc2.end;
                        return false;
                    }
                    // "x = +6;" veya "x = -6;" doğru → i'yi 5 artır
                    i += 5;
                    continue;
                }

                // 3C) "x = 6;" kuralı
                else if (i + 2 < tokens.size() && tokens.get(i + 2).type == Lexer.TokenType.NUMBER) {
                    //   i+2 ⇒ NUMBER
                    //   i+3 ⇒ SEPARATOR ";"
                    // 3C.1) i+3 ⇒ SEPARATOR ";" olmalı
                    if (i + 3 >= tokens.size()) {
                        // noktalı virgül eksikse sayı üzerine vurgu
                        Lexer.Token t = tokens.get(i + 2);
                        errorStart = t.start;
                        errorEnd   = t.end;
                        return false;
                    }
                    Lexer.Token tokSc3 = tokens.get(i + 3);
                    String sc3Text = getTokenText(tokSc3, fullText);
                    if (tokSc3.type != Lexer.TokenType.SEPARATOR || !sc3Text.equals(";")) {
                        errorStart = tokSc3.start;
                        errorEnd   = tokSc3.end;
                        return false;
                    }
                    // "x = 6;" doğru → i'yi 4 artır
                    i += 4;
                    continue;
                }

                // 3D) Yukarıdaki hiçbir kural uymadıysa burada hata
                else {
                    if (i + 2 < tokens.size()) {
                        Lexer.Token t = tokens.get(i + 2);
                        errorStart = t.start;
                        errorEnd   = t.end;
                    } else {
                        // i+2 bile yoksa "=" üzerine vurgu
                        errorStart = tokEq.start;
                        errorEnd   = tokEq.end;
                    }
                    return false;
                }
            }

            // --- 4) Yukarıdakilerin hiçbiri uymadıysa → bilinmeyen yapı, hata ---
            else {
                errorStart = token.start;
                errorEnd   = token.end;
                return false;
            }
        }

        // Eğer döngü bittiyse (tüm token'ları yedik), hata yok demektir
        errorStart = -1;
        errorEnd   = -1;
        return true;
    }

    /**
     * getTokenText(...) metodu:
     *  • Hangi Token’ın, fullText içindeki hangi altstring’e karşılık geldiğini döner.
     *  • token.start ile token.end arasındaki substring’i return eder.
     */
    private static String getTokenText(Lexer.Token token, String fullText) {
        return fullText.substring(token.start, token.end);
    }
}
