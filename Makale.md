## Giriş
Günümüzde kod editörleri, sadece sözdizimi rengini (syntax highlighting) değil; aynı zamanda arka planda gramer tabanlı kontrol yaparak hatalı yazımları da anında işaretliyor. Bu proje, Java ve Swing kullanarak “Real-Time Grammar-Based Syntax Highlighter with GUI” geliştirmeyi amaçlıyor. Kullanıcı yazarken, girdi satırlarını önce lexical analiz (tokenlaştırma), sonra sözdizimi analiz (parser) ile denetliyor; en az beş token tipini renklerle vurguluyor ve hata varsa kırmızı altı çizili (underline) göstererek kullanıcıyı uyarıyor.

Bu makalede:

Dil ve gramer seçimimize neden bu kadar öncelik verdiğimizi,

Leksik analiz sürecini nasıl tasarladığımızı,

Parser (sözdizimi analizcisi) metodolojimizi,

Gerçek-zamanlı renklendirme şemamızı ve

GUI bileşenlerimizi

adım adım anlatacağım. Ayrıca, karşılaştığım zorluklar ve çözümleri de paylaşacağım.

## 1. Programlama Dili ve Gramer Seçimi
## 1.1 Neden Java ve Swing?
Swing Kütüphanesi: Java’nın standart GUI aracı Swing, JTextPane ve StyledDocument sayesinde her karakter aralığına istediğimiz stili (renk, yazı tipi, altı çizili vb.) kolaylıkla uygulamamıza imkân veriyor. Özellikle underline (altı çizili) vurgulama gibi özellikleri manuel tanımlamak Swing’de çok elverişli.

Platform Bağımsızlığı: Java ile oluşturulan JAR dosyası, Windows, macOS veya Linux fark etmeksizin çalışabiliyor.

Regex İmkanı: java.util.regex paketi, hem güçlü hem de kolay biçimde “master-regex” (tek bir düzenli ifade) kullanmamıza izin veriyor. Bu sayede token çakışmalarını daha minimal kodla çözebildik.

## 1.2 Desteklenen Gramer
Projenin hedefi, bir C/Java altkümesi (subset) sokmak ve en temel üç yapıyı gerçek zamanlı parse edip renklendirmek:

Değişken Tanımlama

java
Kopyala
Düzenle
int x = 5;
int sayi = 42;
Burada değişken adı (identifier) mutlaka harf veya alt çizgiyle (_) başlamalı, rakamla başlayamaz.

Atama (Assignment)

java
Kopyala
Düzenle
x = x + 6;
x = +6;
x = -3;
x = 6;
Bu yapıda üç alt senaryo var:

x = x + 6; (başka bir değişkenden aritmetik atama)

x = +6; veya x = -3; (tek bir sayı ataması, unary operatör)

x = 6; (işaretsiz sayı ataması)

If Bloğu (Boş İçerikli)

java
Kopyala
Düzenle
if(x > 3) { }
if(id < 10) { }
Burada yalnızca karşılaştırma operatörleri (>, <) destekleniyor. Blok içinin şu anda mutlak boş olması gerekiyor; ileride {} içine birden fazla statement desteği eklenebilir.

Bu gramer, örneğin while, else, for, return vb. yapıları içermiyor. İleride ihtiyaca göre eklenebilir, ama final projesi için yukarıdaki üç yapı yeterli sayıldı.

## 2. Leksik (Lexical) Analiz
## 2.1 Master-Regex Yaklaşımı
Amaç: Girdi metninden en küçük anlamsal birimleri (lexemleri) çıkarıp, her birini Token nesnesine dönüştürmek.

Java’da birden çok token tipini tek bir regex içinde “named-group” olarak topladık:

java
Kopyala
Düzenle
private static final Pattern MASTER_PATTERN = Pattern.compile(
      "(?<KEYWORD>\\b(?:int|if|else|while|return)\\b)"
    + "|(?<NUMBER>\\b\\d+\\b)"
    + "|(?<OPERATOR>[=+\\-*/<>!])"
    + "|(?<PAREN>[(){}])"
    + "|(?<SEPARATOR>;)"
    + "|(?<IDENTIFIER>\\b[a-zA-Z_][a-zA-Z0-9_]*\\b)"
);
Her bir named-group:

KEYWORD: \b(int|if|else|while|return)\b

NUMBER: \b\d+\b

OPERATOR: [=+\-*/<>!]

PAREN: [(){}]

SEPARATOR: ;

IDENTIFIER: \b[a-zA-Z_][a-zA-Z0-9_]*\b

Bu sayede “int” birden fazla gruba (KEYWORD, IDENTIFIER) uysa bile, regex’te ilk sırada olduğu için KEYWORD olarak yakalanır. Böylece token çakışması sorunu ortadan kalkar.

## 2.2 Token Sınıfı
java
Kopyala
Düzenle
public enum TokenType { 
    KEYWORD, NUMBER, OPERATOR, PAREN, SEPARATOR, IDENTIFIER 
}

public class Token {
    public TokenType type;
    public int start, end;

    public Token(TokenType type, int start, int end) {
        this.type = type;
        this.start = start;
        this.end = end;
    }
}
type → token’ın tipi (enum)

start → metin içindeki ilk karakterin indeksi

end → metin içindeki son karakterin indeksi (exclusive)

## 2.3 tokenize( ) Metodu
Her karakter dizisinde:

Matcher m = MASTER_PATTERN.matcher(text);

while (m.find()) { … }

Eğer m.group("KEYWORD") != null → tokens.add(new Token(TokenType.KEYWORD, m.start(), m.end()));

Eğer m.group("NUMBER") != null → tokens.add(new Token(TokenType.NUMBER, m.start(), m.end()));

… diğer gruplar benzer şekilde.

Elde edilen List<Token> döndürülür.

Bu sayede girdi tümüyle taranır ve token dizisi elde edilir. Ardından, parser bu token dizisi üzerinden sözdizimi denetimini yapar.

## 3. Parser (Sözdizimi Analizörü)
## 3.1. Genel Yapı
Elle yazılmış, top-down bir parser kullanıyoruz. “Recursive descent” değil, ama mantık olarak kural-kural ilerleyen bir yapı mevcut. Pseudocode:

csharp
Kopyala
Düzenle
int i = 0;
while (i < tokens.size()) {
    Token token = tokens.get(i);
    String txt = getTokenText(token, fullText);

    if (token.type == KEYWORD && txt.equals("int")) {
        // "int x = 5;" kuralı
    }
    else if (token.type == KEYWORD && txt.equals("if")) {
        // "if(x>n){ }" kuralı
    }
    else if (token.type == IDENTIFIER) {
        // Atama kuralı: 
        //   - x = x + 6;
        //   - x = +6;
        //   - x = 6;
    }
    else {
        // Bilinmeyen yapı → hata
    }
}
Her kural içinde:

i + k < tokens.size() ? kontrolü yapıp,

Beklenen tip mi? Beklenen içerik (örn. “;”, “{” vb.) mi?

Eksik veya yanlışsa, Parser.errorStart = oToken.start; errorEnd = oToken.end; ataması yap, return false;

Eğer kural uyuyorsa i += kaç token deyip sonraki ifadeye geç.

## 3.2. Hata Tespiti: errorStart / errorEnd
errorStart ve errorEnd: Hatalı veya eksik token’ın start/end indekslerini saklar.

Örnek: “int 2 = 5;” yazıldığında:

Token listesi:

shell
Kopyala
Düzenle
#0 KEYWORD "int"
#1 NUMBER  "2"
#2 OPERATOR "="
#3 NUMBER  "5"
#4 SEPARATOR ";"
i=0’da “int” bulundu, değişken tanımlama kuralına girdi.

i+1 fazlası IDENTIFIER değil, NUMBER → hata

errorStart = tokens.get(1).start, errorEnd = tokens.get(1).end → “2” karakter aralığını işaretler.

return false; → Parser sonlanır.

Aynı yaklaşım, “kapanan ‘}’ eksik” gibi durumlarda önceki token ({) üzerine hata vurgusu yaparak kullanıcının nerede hata yaptığını kesin biçimde göstermemizi sağlıyor.

## 3.3. Detaylı Kod Örneği
java
Kopyala
Düzenle
public static boolean parse(List<Lexer.Token> tokens, String fullText) {
    errorStart = -1; errorEnd = -1;
    int i = 0;

    while (i < tokens.size()) {
        Lexer.Token token = tokens.get(i);
        String txt = getTokenText(token, fullText);

        // 1) "int x = 5;" kuralı
        if (token.type == Lexer.TokenType.KEYWORD && txt.equals("int")) {
            // i+1 → IDENTIFIER?
            if (i+1 >= tokens.size() || tokens.get(i+1).type != IDENTIFIER) {
                if (i+1 < tokens.size()) {
                    errorStart = tokens.get(i+1).start;
                    errorEnd   = tokens.get(i+1).end;
                } else {
                    errorStart = token.end;
                    errorEnd   = token.end;
                }
                return false;
            }
            // i+2 → OPERATOR "="?
            if (i+2 >= tokens.size() || 
                tokens.get(i+2).type != OPERATOR || 
                !getTokenText(tokens.get(i+2), fullText).equals("=")) {
                if (i+2 < tokens.size()) {
                    errorStart = tokens.get(i+2).start;
                    errorEnd   = tokens.get(i+2).end;
                } else {
                    errorStart = tokens.get(i+1).start;
                    errorEnd   = tokens.get(i+1).end;
                }
                return false;
            }
            // i+3 → NUMBER?
            if (i+3 >= tokens.size() || tokens.get(i+3).type != NUMBER) {
                if (i+3 < tokens.size()) {
                    errorStart = tokens.get(i+3).start;
                    errorEnd   = tokens.get(i+3).end;
                } else {
                    errorStart = tokens.get(i+2).start;
                    errorEnd   = tokens.get(i+2).end;
                }
                return false;
            }
            // i+4 → SEPARATOR ";"?
            if (i+4 >= tokens.size()) {
                // ";" eksik: "5" token üzerine vurgu
                errorStart = tokens.get(i+3).start;
                errorEnd   = tokens.get(i+3).end;
                return false;
            }
            if (tokens.get(i+4).type != SEPARATOR ||
                !getTokenText(tokens.get(i+4), fullText).equals(";")) {
                errorStart = tokens.get(i+4).start;
                errorEnd   = tokens.get(i+4).end;
                return false;
            }
            i += 5;
            continue;
        }

        // 2) "if(x > 3) { }" kuralı
        else if (token.type == KEYWORD && txt.equals("if")) {
            // i+1 "("?
            if (i+1 >= tokens.size() || 
                tokens.get(i+1).type != PAREN || 
                !getTokenText(tokens.get(i+1), fullText).equals("(")) {
                if (i+1 < tokens.size()) {
                    errorStart = tokens.get(i+1).start;
                    errorEnd   = tokens.get(i+1).end;
                } else {
                    errorStart = token.end;
                    errorEnd   = token.end;
                }
                return false;
            }
            // i+2 → IDENTIFIER?
            if (i+2 >= tokens.size() || tokens.get(i+2).type != IDENTIFIER) {
                if (i+2 < tokens.size()) {
                    errorStart = tokens.get(i+2).start;
                    errorEnd   = tokens.get(i+2).end;
                } else {
                    errorStart = tokens.get(i+1).start;
                    errorEnd   = tokens.get(i+1).end;
                }
                return false;
            }
            // i+3 → OPERATOR?
            if (i+3 >= tokens.size() || tokens.get(i+3).type != OPERATOR) {
                if (i+3 < tokens.size()) {
                    errorStart = tokens.get(i+3).start;
                    errorEnd   = tokens.get(i+3).end;
                } else {
                    errorStart = tokens.get(i+2).start;
                    errorEnd   = tokens.get(i+2).end;
                }
                return false;
            }
            // i+4 → NUMBER?
            if (i+4 >= tokens.size() || tokens.get(i+4).type != NUMBER) {
                if (i+4 < tokens.size()) {
                    errorStart = tokens.get(i+4).start;
                    errorEnd   = tokens.get(i+4).end;
                } else {
                    errorStart = tokens.get(i+3).start;
                    errorEnd   = tokens.get(i+3).end;
                }
                return false;
            }
            // i+5 → ")"?
            if (i+5 >= tokens.size() ||
                tokens.get(i+5).type != PAREN ||
                !getTokenText(tokens.get(i+5), fullText).equals(")")) {
                if (i+5 < tokens.size()) {
                    errorStart = tokens.get(i+5).start;
                    errorEnd   = tokens.get(i+5).end;
                } else {
                    errorStart = tokens.get(i+4).start;
                    errorEnd   = tokens.get(i+4).end;
                }
                return false;
            }
            // i+6 → "{"?
            if (i+6 >= tokens.size() ||
                tokens.get(i+6).type != PAREN ||
                !getTokenText(tokens.get(i+6), fullText).equals("{")) {
                if (i+6 < tokens.size()) {
                    errorStart = tokens.get(i+6).start;
                    errorEnd   = tokens.get(i+6).end;
                } else {
                    errorStart = tokens.get(i+5).start;
                    errorEnd   = tokens.get(i+5).end;
                }
                return false;
            }
            // i+7 → "}"?
            if (i+7 >= tokens.size() ||
                tokens.get(i+7).type != PAREN ||
                !getTokenText(tokens.get(i+7), fullText).equals("}")) {
                // "}" eksikse "{" üzerine vurgu
                errorStart = tokens.get(i+6).start;
                errorEnd   = tokens.get(i+6).end;
                return false;
            }
            i += 8;
            continue;
        }

        // 3) Atama kuralı (x = x + 6;  |  x = +6;  |  x = 6;)
        else if (token.type == IDENTIFIER) {
            // i+1 "="?
            if (i+1 >= tokens.size() || 
                tokens.get(i+1).type != OPERATOR ||
                !getTokenText(tokens.get(i+1), fullText).equals("=")) {
                if (i+1 < tokens.size()) {
                    errorStart = tokens.get(i+1).start;
                    errorEnd   = tokens.get(i+1).end;
                } else {
                    errorStart = token.start;
                    errorEnd   = token.end;
                }
                return false;
            }
            // 3A) x = x + 6;
            if (i+2 < tokens.size() && tokens.get(i+2).type == IDENTIFIER) {
                // i+3 → OPERATOR?
                if (i+3 >= tokens.size() || tokens.get(i+3).type != OPERATOR) {
                    if (i+3 < tokens.size()) {
                        errorStart = tokens.get(i+3).start;
                        errorEnd   = tokens.get(i+3).end;
                    } else {
                        errorStart = tokens.get(i+2).start;
                        errorEnd   = tokens.get(i+2).end;
                    }
                    return false;
                }
                // i+4 → NUMBER?
                if (i+4 >= tokens.size() || tokens.get(i+4).type != NUMBER) {
                    if (i+4 < tokens.size()) {
                        errorStart = tokens.get(i+4).start;
                        errorEnd   = tokens.get(i+4).end;
                    } else {
                        errorStart = tokens.get(i+3).start;
                        errorEnd   = tokens.get(i+3).end;
                    }
                    return false;
                }
                // i+5 → ";"?
                if (i+5 >= tokens.size() ||
                    tokens.get(i+5).type != SEPARATOR ||
                    !getTokenText(tokens.get(i+5), fullText).equals(";")) {
                    if (i+5 < tokens.size()) {
                        errorStart = tokens.get(i+5).start;
                        errorEnd   = tokens.get(i+5).end;
                    } else {
                        errorStart = tokens.get(i+4).start;
                        errorEnd   = tokens.get(i+4).end;
                    }
                    return false;
                }
                i += 6;
                continue;
            }
            // 3B) x = +6; veya x = -6;
            else if (i+2 < tokens.size() && 
                     tokens.get(i+2).type == OPERATOR && 
                     (getTokenText(tokens.get(i+2), fullText).equals("+") ||
                      getTokenText(tokens.get(i+2), fullText).equals("-"))) {
                // i+3 → NUMBER?
                if (i+3 >= tokens.size() || tokens.get(i+3).type != NUMBER) {
                    if (i+3 < tokens.size()) {
                        errorStart = tokens.get(i+3).start;
                        errorEnd   = tokens.get(i+3).end;
                    } else {
                        errorStart = tokens.get(i+2).start;
                        errorEnd   = tokens.get(i+2).end;
                    }
                    return false;
                }
                // i+4 → ";"?
                if (i+4 >= tokens.size() ||
                    tokens.get(i+4).type != SEPARATOR ||
                    !getTokenText(tokens.get(i+4), fullText).equals(";")) {
                    if (i+4 < tokens.size()) {
                        errorStart = tokens.get(i+4).start;
                        errorEnd   = tokens.get(i+4).end;
                    } else {
                        errorStart = tokens.get(i+3).start;
                        errorEnd   = tokens.get(i+3).end;
                    }
                    return false;
                }
                i += 5;
                continue;
            }
            // 3C) x = 6;
            else if (i+2 < tokens.size() && tokens.get(i+2).type == NUMBER) {
                // i+3 → ";"?
                if (i+3 >= tokens.size() ||
                    tokens.get(i+3).type != SEPARATOR ||
                    !getTokenText(tokens.get(i+3), fullText).equals(";")) {
                    if (i+3 < tokens.size()) {
                        errorStart = tokens.get(i+3).start;
                        errorEnd   = tokens.get(i+3).end;
                    } else {
                        errorStart = tokens.get(i+2).start;
                        errorEnd   = tokens.get(i+2).end;
                    }
                    return false;
                }
                i += 4;
                continue;
            }
            // 3D) Hiçbir kural uymadıysa
            else {
                if (i+2 < tokens.size()) {
                    errorStart = tokens.get(i+2).start;
                    errorEnd   = tokens.get(i+2).end;
                } else {
                    errorStart = tokens.get(i+1).start;
                    errorEnd   = tokens.get(i+1).end;
                }
                return false;
            }
        }
        // 4) Bilinmeyen yapı
        else {
            errorStart = token.start;
            errorEnd   = token.end;
            return false;
        }
    }

    errorStart = -1;
    errorEnd   = -1;
    return true;
}

private static String getTokenText(Lexer.Token token, String fullText) {
    return fullText.substring(token.start, token.end);
}
## 4. Gerçek-Zamanlı Renklendirme (Highlighting) Şeması
## 4.1. Stil (Color) Tanımları
SyntaxHighlighterGUI içinde, JTextPane’in StyledDocument nesnesine şu stilleri ekledik:

java
Kopyala
Düzenle
// DEFAULT (siyah)
Style defaultStyle = textPane.addStyle("DEFAULT", null);
StyleConstants.setForeground(defaultStyle, Color.BLACK);

// KEYWORD (mavi)
Style keyword = textPane.addStyle("KEYWORD", null);
StyleConstants.setForeground(keyword, Color.BLUE);

// NUMBER (kırmızı)
Style number = textPane.addStyle("NUMBER", null);
StyleConstants.setForeground(number, Color.RED);

// OPERATOR (turuncu)
Style operator = textPane.addStyle("OPERATOR", null);
StyleConstants.setForeground(operator, Color.ORANGE);

// PAREN (yeşil)
Style paren = textPane.addStyle("PAREN", null);
StyleConstants.setForeground(paren, new Color(0, 128, 0));

// SEPARATOR (noktalı virgül) – siyah
Style separator = textPane.addStyle("SEPARATOR", null);
StyleConstants.setForeground(separator, Color.BLACK);

// IDENTIFIER (siyah)
Style identifier = textPane.addStyle("IDENTIFIER", null);
StyleConstants.setForeground(identifier, Color.BLACK);

// ERROR (kırmızı altı çizili)
Style errorStyle = textPane.addStyle("ERROR", null);
StyleConstants.setForeground(errorStyle, Color.RED);
StyleConstants.setUnderline(errorStyle, true);
COLOR SEÇİMİ:

KEYWORD için canlı mavi,

NUMBER için belirgin kırmızı,

OPERATOR için turuncu (göz yakmadan öne çıkarma),

PAREN için koyu yeşil,

IDENTIFIER/SEPARATOR için siyah,

ERROR için kırmızı + altı çizili

## 4.2. Metni Renklendirme (highlight)
java
Kopyala
Düzenle
private void highlight() {
    String text = textPane.getText();
    if (text.isEmpty()) {
        setTitle("Real-Time Syntax Highlighter");
        return;
    }

    StyledDocument doc = textPane.getStyledDocument();

    // 1) Tüm metni DEFAULT (siyah) ile sıfırla
    Style defaultStyle = textPane.getStyle("DEFAULT");
    doc.setCharacterAttributes(0, text.length(), defaultStyle, true);

    // 2) Leksik analiz: token'ları al ve her birini renklendir
    List<Lexer.Token> tokens = Lexer.tokenize(text);
    for (Lexer.Token token : tokens) {
        Style style = textPane.getStyle(token.type.name());
        if (style != null) {
            doc.setCharacterAttributes(token.start, token.end - token.start, style, true);
        }
    }

    // 3) Parser çağrısı: hata var mı?
    boolean valid = Parser.parse(tokens, text);
    if (valid) {
        setTitle("Real-Time Syntax Highlighter - ✅ Kod Geçerli");
    } else {
        setTitle("Real-Time Syntax Highlighter - ❌ Hatalı Sözdizimi");
        int errStart = Parser.errorStart;
        int errEnd   = Parser.errorEnd;
        if (errStart >= 0 && errEnd > errStart) {
            Style errorStyle = textPane.getStyle("ERROR");
            doc.setCharacterAttributes(errStart, errEnd - errStart, errorStyle, true);
        }
    }
}
Önce tüm metin “DEFAULT” (siyah) ile boyanır.

Lexer.tokenize(text) çağrısıyla elde edilen token listesi üzerinde dönülür; her token tipi adına (token.type.name()) göre stil alınır ve o karakter aralığına uygulanır.

Parser.parse(...) çağrısı sonucu hata varsa, errorStart/errorEnd üzerinden “ERROR” stili (kırmızı altı çizili) uygulanır.

## 4.3. Performans Optimizasyonu: DocumentListener + Timer
java
Kopyala
Düzenle
highlightTimer = new Timer(100, e -> {
    highlight();
    highlightTimer.stop();
});
highlightTimer.setRepeats(false);

textPane.getDocument().addDocumentListener(new DocumentListener() {
    public void insertUpdate(DocumentEvent e)  { highlightTimer.restart(); }
    public void removeUpdate(DocumentEvent e)  { highlightTimer.restart(); }
    public void changedUpdate(DocumentEvent e) { highlightTimer.restart(); }
});
Her metin değiştiğinde (insert / remove / change), highlightTimer.restart() yapılarak 100 ms’lik bir geri sayım başlar.

100 ms içinde başka bir tuşa basılmazsa, Timer’ın actionPerformed metodu çalışır ve highlight() çağrılır.

Eğer kullanıcı çok hızlı yazıyorsa, Timer her tuştan sonra yeniden başlar; highlight() yalnızca yazma bittiğinde (son tuştan 100 ms sonra) tetiklenmiş olur.

Bu sayede GUI kitlenmesi engellenir ve CPU yükü azalır.

## 5. GUI (Grafiksel Kullanıcı Arayüzü) Uygulaması
Aşağıda, en baştan sona küçültülmüş ama işlevsel bir Swing tabanlı GUI akışını görebilirsiniz:

java
Kopyala
Düzenle
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class SyntaxHighlighterGUI extends JFrame {
    private JTextPane textPane;
    private Timer highlightTimer;

    public SyntaxHighlighterGUI() {
        setTitle("Real-Time Syntax Highlighter");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 1) Metin alanı
        textPane = new JTextPane();
        textPane.setFont(new Font("Consolas", Font.PLAIN, 16));
        JScrollPane scrollPane = new JScrollPane(textPane);
        add(scrollPane);

        // 2) Stil tanımları
        setupStyles();

        // 3) Gecikmeli highlight için Timer (100 ms)
        highlightTimer = new Timer(100, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                highlight();
                highlightTimer.stop();
            }
        });
        highlightTimer.setRepeats(false);

        // 4) DocumentListener: her yazı değişiminde timer yeniden başlatılır
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { highlightTimer.restart(); }
            public void removeUpdate(DocumentEvent e)  { highlightTimer.restart(); }
            public void changedUpdate(DocumentEvent e) { highlightTimer.restart(); }
        });

        setVisible(true);
    }

    private void setupStyles() {
        Style defaultStyle = textPane.addStyle("DEFAULT", null);
        StyleConstants.setForeground(defaultStyle, Color.BLACK);

        Style keyword = textPane.addStyle("KEYWORD", null);
        StyleConstants.setForeground(keyword, Color.BLUE);

        Style number = textPane.addStyle("NUMBER", null);
        StyleConstants.setForeground(number, Color.RED);

        Style operator = textPane.addStyle("OPERATOR", null);
        StyleConstants.setForeground(operator, Color.ORANGE);

        Style paren = textPane.addStyle("PAREN", null);
        StyleConstants.setForeground(paren, new Color(0, 128, 0));

        Style separator = textPane.addStyle("SEPARATOR", null);
        StyleConstants.setForeground(separator, Color.BLACK);

        Style identifier = textPane.addStyle("IDENTIFIER", null);
        StyleConstants.setForeground(identifier, Color.BLACK);

        Style errorStyle = textPane.addStyle("ERROR", null);
        StyleConstants.setForeground(errorStyle, Color.RED);
        StyleConstants.setUnderline(errorStyle, true);
    }

    private void highlight() {
        String text = textPane.getText();
        if (text.isEmpty()) {
            setTitle("Real-Time Syntax Highlighter");
            return;
        }

        StyledDocument doc = textPane.getStyledDocument();
        // Tüm metni sıfırla
        Style defaultStyle = textPane.getStyle("DEFAULT");
        doc.setCharacterAttributes(0, text.length(), defaultStyle, true);

        // 1) Leksik analiz: token'ları renklendir
        List<Lexer.Token> tokens = Lexer.tokenize(text);
        for (Lexer.Token token : tokens) {
            Style style = textPane.getStyle(token.type.name());
            if (style != null) {
                doc.setCharacterAttributes(token.start, token.end - token.start, style, true);
            }
        }

        // 2) Parser ile sözdizimi kontrolü ve hata vurgusu
        boolean valid = Parser.parse(tokens, text);
        if (valid) {
            setTitle("Real-Time Syntax Highlighter - ✅ Kod Geçerli");
        } else {
            setTitle("Real-Time Syntax Highlighter - ❌ Hatalı Sözdizimi");
            int errStart = Parser.errorStart;
            int errEnd   = Parser.errorEnd;
            if (errStart >= 0 && errEnd > errStart) {
                Style errorStyle = textPane.getStyle("ERROR");
                doc.setCharacterAttributes(errStart, errEnd - errStart, errorStyle, true);
            }
        }
    }
}
Ana pencere: JFrame

Kod yazma alanı: JTextPane üzerinde, Consolas monospace font kullanıldı.

Stil tanımları: textPane.addStyle("KEYWORD", null) ile her token tipi için ayrı renkler tanımlandı.

Gecikmeli Renklendirme: Timer ve DocumentListener kombinasyonu.

Highlight Adımı:

Tüm metni siyaha boyar.

Lexer’dan dönen token listesine göre renk uygular.

Parser’dan dönen Boolean’a (hata/geçerli) göre altı çizili vurguyu ekler.

## 6. Karşılaşılan Zorluklar ve Çözümler
## 6.1. Token Çakışmaları
Sorun: “int” hem KEYWORD hem IDENTIFIER’a uyabilirdi. Eğer önce IDENTIFIER kontrol etseydik, “int” yanlışlıkla tanımlayıcı olarak algılanabilirdi.
Çözüm:

Named-group düzenli ifadesinde <KEYWORD>’ü en üst sıraya aldık.

Bu sayede “int” gördüğünde önce KEYWORD grubuna eşleşir; IDENTIFIER grubuna düşmez.

## 6.2. IndexOutOfBoundsException Hataları
Sorun: Parser’da tokens.get(i + k) ifadeleri, yazı kısmen girildiğinde sık sık Index out of bounds hatası veriyordu.
Çözüm:

Her tokens.get(i + k) çağrısının önüne if (i+k >= tokens.size()) { … hata… } kontrolü ekledik.

Eğer eksik token varsa (örneğin “int x = 5” yazılmış ama ; girilmemiş), hatayı bir önceki token (sayı “5”) üzerine atadık. Böylece GUI, “5” karakterinin altında kırmızı altı çizili gösterildi.

## 6.3. Gerçek-Zamanlı Renklendirme Performansı
Sorun: Kullanıcı hızlı yazarken her karakter girişinde highlight()’ı doğrudan çağırmak, GUI’yi kilitliyor (lag).
Çözüm:

javax.swing.Timer kullanarak 100 ms gecikmeyle renklendirme yapılmasını sağladık.

DocumentListener ile her insertUpdate/removeUpdate/changedUpdate’de timer.restart() çağrıldı. Eğer kullanıcı 100 ms içinde yeni bir tuşa basmazsa highlight() çalıştı.

Bu sayede CPU yükü azalıyor ve kullanıcı deneyimi akıcı oluyor.

## 6.4. Hatalı Token Konumlandırma
Sorun: “noktalı virgül eksik” veya “kapanan süslü parantez eksik” durumlarında, önce hatayı fullText.length() (metnin sonuna) atadığımızda ekranda hiçbir karakter altı çizili görünmüyor, çünkü “metnin sonu” boş karakter demek.
Çözüm:

Noktalı virgül eksikse, hatayı “bir önceki token” (sayı token’ı) üzerine attık. Böylece GUI’de “5” karakterinin altı çizili göründü.

Kapanan parantez eksikse, hatayı “açan {” karakteri üzerine attık. Böylece kullanıcı nerede süslü parantezi kapatması gerektiğini anında görebildi.

## 6.5. Atama Çeşitliliği: “x = +6;” ve “x = 6;”
Sorun: Başlangıçta parser yalnızca “x = x + 6;” biçimini kabul ediyordu. “x = +6;” veya “x = 6;” gibi tek token-atama ifadeleri hata veriyordu.
Çözüm:

Atama kuralını üç alt duruma böldük:

3A) x = x + 6;

3B) x = +6; veya x = -3;

3C) x = 6;

Kodda her bir alt kural için ayrı kontrol yaptık. Gerektiğinde i += 6, i += 5 veya i += 4 adımlarıyla döngüyü sürdürüyoruz. Böylece unary operatör içeren veya işaretsiz sayı ataması olan satırlar da “geçerli” kabul ediliyor.

## 7. Sonuç ve Gelecek Geliştirmeler
## 7.1. Proje Sonucu
Bu proje sayesinde:

Leksiksel analiz aşamasında 6 farklı token (KEYWORD, NUMBER, OPERATOR, PAREN, SEPARATOR, IDENTIFIER) başarıyla ayrıştırıldı.

Parser ile üç temel yapı (“int x = 5;”, “x = x + 6;” / “x = +6;” / “x = 6;” ve “if(x > 3) { }”) eksiksiz parse edildi.

errorStart / errorEnd mekanizmasıyla hatalı token konumu kesin biçimde bulundu ve GUI’de kırmızı altı çizili (underline) vurgusu yapıldı.

Gerçek-zamanlı renklendirme: Timer + DocumentListener yapısıyla performans sorunu çözülerek akıcı bir deneyim sunuldu.

GUI kısmında JTextPane + StyledDocument ile renklendirme, hatalı aralıkta altı çizili vurgulama ve başlık güncellemesi (✅ Kod Geçerli / ❌ Hatalı Sözdizimi) başarıyla uygulandı.

Bu adımlar, ödevin source code ve documentation gereksinimlerini eksiksiz karşıladı. Buna ek olarak, demo makale ve demo video ile proje halka açıklandı.

## 7.2. Gelecek Geliştirmeler ve İyileştirmeler
Yeni Token Tipleri ve Anahtar Kelimeler

else, while, for, return gibi ek keyword’ler eklenebilir.

MASTER_PATTERN içinde <KEYWORD> grubunu genişleterek bu kelimeleri yakalayabilir, parser’da ilgili kuralları tanımlayabiliriz.

Blok İçeriği Desteği

Şu anda if(x > 3) { } sadece boş blok kabul ediyor.

Gelecekte “if(x > 3) { int y = 2; y = y + 1; }” gibi birden fazla statement’ı blok içinde parse etmek mümkün olabilir. Bunun için parser içinde { sonrasında “}” görülene kadar tekrar oku/kontrol et döngüsü ekleyebiliriz.

Else-If Zinciri

“if(...) { ... } else if (...) { ... } else { ... }” yapısını, altlı üstlü kural zinciri (chain) olarak eklemek.

Örneğin “if (COND) { … } (else if (COND) { … })* (else { … })?” şeklinde bir genişletme yapılabilir.

Döngüler (while, for)

“while(x < 10) { x = x + 1; }” ve “for(i = 0; i < 10; i = i + 1) { … }” kural setleri parser’a eklenebilir.

Özellikle for yapısı üç bölüm (“başlatma; koşul; döngü içi artırım”) içerdiği için parser’da ek sayfa boyutu kuralı belirlemek gerekecek.

Yorum Satırları (Comments) ve String Literal Desteği

// … ve /* … */ yorumlarını tokenizasyon aşamasında gözardı etmek, böylece parser’a dahil olmamalarını sağlamak.

Çift tırnak içindeki string literal’ler (örneğin "Merhaba Dünya") tek bir token olarak yakalamak. Bunun için MASTER_PATTERN’e (“(?:\\.|[^\\"])*”) gibi bir grup eklenebilir.

Sembol Tablosu ve Semantic Analiz

Şu anda değişkenlerin tanımlanıp tanımlanmadığını kontrol etmiyoruz; sadece sözdizimi denetimi yapılıyor.

İleride her int x = 5; tanımında symbolTable.put("x", "int") gibi bir tabloya ekleme yapabilir, x = y; gibi atamalarda y’nin önceden tanımlanıp tanımlanmadığını denetleyebiliriz.

Kod Tamamlama (Auto-Completion) ve Satır Numaraları

Daha ileri bir IDE deneyimi için, yazılan anahtar kelimenin ilk birkaç harfine göre öneriler sunan bir AutoComplete mekanizması eklenebilir.

Pencerenin yan tarafına satır numaraları ekleyerek, hata ayıklamayı kolaylaştırmak mümkün olabilir.

Tema Desteği

Koyu tema / açık tema seçeneği, renk paletini dinamik olarak değiştirme olanağı.

Örneğin “ayarlar” menüsü ekleyip, kullanıcıya yazı tipi, renk şeması ve altı çizili stilini seçme imkânı sunabiliriz.

Bu eklemeler, temel projenin sunduğu altyapıyı daha güçlü ve “gerçek bir IDE” hissi veren bir ortama dönüştürecektir.

## 8. Kısa Kod Parçacığı Örnekleri
Aşağıda, projenin en kritik kısımlarından bazı küçük kod parçacıkları yer alıyor. Bunları makalenin ilerleyen noktalarında örnek olarak gösterebilirsiniz.

## 8.1. Master-Regex Tanımı
java
Kopyala
Düzenle
private static final Pattern MASTER_PATTERN = Pattern.compile(
      "(?<KEYWORD>\\b(?:int|if|else|while|return)\\b)"
    + "|(?<NUMBER>\\b\\d+\\b)"
    + "|(?<OPERATOR>[=+\\-*/<>!])"
    + "|(?<PAREN>[(){}])"
    + "|(?<SEPARATOR>;)"
    + "|(?<IDENTIFIER>\\b[a-zA-Z_][a-zA-Z0-9_]*\\b)"
);
İlk grup KEYWORD, ikinci grup NUMBER, vs.

“int” → KEYWORD, “123” → NUMBER, “(” → PAREN, “;” → SEPARATOR, vb.

## 8.2. Token Sınıfı
java
Kopyala
Düzenle
public enum TokenType { KEYWORD, NUMBER, OPERATOR, PAREN, SEPARATOR, IDENTIFIER }

public static class Token {
    public TokenType type;
    public int start, end;
    public Token(TokenType type, int start, int end) {
        this.type = type; this.start = start; this.end = end;
    }
}
## 8.3. Leksik Analizci (tokenize)
java
Kopyala
Düzenle
public static List<Token> tokenize(String text) {
    List<Token> tokens = new ArrayList<>();
    Matcher m = MASTER_PATTERN.matcher(text);
    while (m.find()) {
        if (m.group("KEYWORD") != null) {
            tokens.add(new Token(TokenType.KEYWORD, m.start(), m.end()));
        } else if (m.group("NUMBER") != null) {
            tokens.add(new Token(TokenType.NUMBER, m.start(), m.end()));
        } else if (m.group("OPERATOR") != null) {
            tokens.add(new Token(TokenType.OPERATOR, m.start(), m.end()));
        } else if (m.group("PAREN") != null) {
            tokens.add(new Token(TokenType.PAREN, m.start(), m.end()));
        } else if (m.group("SEPARATOR") != null) {
            tokens.add(new Token(TokenType.SEPARATOR, m.start(), m.end()));
        } else if (m.group("IDENTIFIER") != null) {
            tokens.add(new Token(TokenType.IDENTIFIER, m.start(), m.end()));
        }
    }
    return tokens;
}
## 8.4. Parser’ın Kısmî Atama Kuralı (3B: unary atama)
java
Kopyala
Düzenle
// 3B) "x = +6;" veya "x = -6;"
else if (i+2 < tokens.size()
      && tokens.get(i+2).type == OPERATOR
      && (getTokenText(tokens.get(i+2), fullText).equals("+")
          || getTokenText(tokens.get(i+2), fullText).equals("-"))) {
    // i+3 → NUMBER
    if (i+3 >= tokens.size() || tokens.get(i+3).type != NUMBER) {
        if (i+3 < tokens.size()) {
            errorStart = tokens.get(i+3).start;
            errorEnd   = tokens.get(i+3).end;
        } else {
            errorStart = tokens.get(i+2).start;
            errorEnd   = tokens.get(i+2).end;
        }
        return false;
    }
    // i+4 → ";"
    if (i+4 >= tokens.size() ||
        tokens.get(i+4).type != SEPARATOR ||
        !getTokenText(tokens.get(i+4), fullText).equals(";")) {
        if (i+4 < tokens.size()) {
            errorStart = tokens.get(i+4).start;
            errorEnd   = tokens.get(i+4).end;
        } else {
            errorStart = tokens.get(i+3).start;
            errorEnd   = tokens.get(i+3).end;
        }
        return false;
    }
    i += 5; // Bu atama ifadesi toplam 5 token uzunluğunda
    continue;
}
## 8.5. Gerçek-Zamanlı Highlight Metodu
java
Kopyala
Düzenle
private void highlight() {
    String text = textPane.getText();
    if (text.isEmpty()) {
        setTitle("Real-Time Syntax Highlighter");
        return;
    }

    StyledDocument doc = textPane.getStyledDocument();
    Style defaultStyle = textPane.getStyle("DEFAULT");
    doc.setCharacterAttributes(0, text.length(), defaultStyle, true);

    List<Lexer.Token> tokens = Lexer.tokenize(text);
    for (Lexer.Token token : tokens) {
        Style style = textPane.getStyle(token.type.name());
        if (style != null) {
            doc.setCharacterAttributes(token.start, token.end - token.start, style, true);
        }
    }

    boolean valid = Parser.parse(tokens, text);
    if (valid) {
        setTitle("Real-Time Syntax Highlighter - ✅ Kod Geçerli");
    } else {
        setTitle("Real-Time Syntax Highlighter - ❌ Hatalı Sözdizimi");
        int errStart = Parser.errorStart;
        int errEnd   = Parser.errorEnd;
        if (errStart >= 0 && errEnd > errStart) {
            Style errorStyle = textPane.getStyle("ERROR");
            doc.setCharacterAttributes(errStart, errEnd - errStart, errorStyle, true);
        }
    }
}
## 9. Demo ve Beni Bekleyen Adımlar
Bu makale, Medium, GitHub Pages veya kişisel blog gibi herkese açık bir platforma yüklenecek.

Video ile, aşağıdaki örnekleri ekran paylaşımı üzerinden göstereceğim:

“int x = 5;” → renkli renklendirme, başlık: ✅ Kod Geçerli

“x = x + 6;” → renkli renklendirme, başlık: ✅ Kod Geçerli

“x = +6;” → renkli renklendirme, başlık: ✅ Kod Geçerli

“int 2 = 5;” → “2” altı kırmızı çizili, başlık: ❌ Hatalı Sözdizimi

“int x = 5” (noktalı virgül eksik) → “5” altı kırmızı çizili, başlık: ❌ Hatalı Sözdizimi

“if(x>10){” → “{” altı kırmızı çizili, başlık: ❌ Hatalı Sözdizimi

“x = *6;” → “*” altı kırmızı çizili, başlık: ❌ Hatalı Sözdizimi

Bu demo video, YouTube’a yüklenerek linki ilgili öğretim üyesine iletilecek.

## 10. Sonuç
“Real-Time Grammar-Based Syntax Highlighter with GUI” projesi, adres bazlı altı çizili (underline) hata vurgusu ve gerçek zamanlı renkli sözdizimi sunan bir temel kod editö (mini-IDE) işlevi görüyor. Java’nın Swing ve Regex imkânları sayesinde:

Leksiksel analiz hızlı ve doğru biçimde yapıldı.

Sözdizimi analizi basit bir top-down işlemle, hatalı token konumlarını kesin göstererek kullanıcıya anlık geri bildirim verdi.

Gerçek-zamanlı renklendirme Timer + DocumentListener kombinasyonu sayesinde performanslı ve akıcı bir deneyim sundu.

GUI kısmı, Minimum düzeyde ama gerekli tüm işlevleri karşılayacak biçimde tasarlandı: Renkli vurgulama, hatalı token-altı çizme, “✅ Kod Geçerli” / “❌ Hatalı Sözdizimi” başlıkları.

Bu makaledeki kod parçacıkları ve açıklamalar, projenin nasıl çalıştığını, hangi tasarım kararlarının alındığını ve karşılaşılan zorlukları özetliyor. Gelecekte bu temele else-if, döngü, yorum satırları, string literal, sembol tabanı ve tip denetimi gibi gelişmiş özellikler eklenebilir. Böylece, bu mini-IDE benzeri uygulama hem eğitim hem de pratik kodlama desteği veren bir araç haline dönüşebilir.

Kaynakça & Ekler

Java Platform SE 11 – Swing Documentation (Oracle)

Java Regular Expressions Tutorial (Oracle)

“Lexer and Parser Implementation in Java” – Makale

“Programlama Dilleri Projesi” PDF (Ders Koordinatörü)

IntelliJ IDEA Community Edition Documentation
