import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.Timer;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SyntaxHighlighterGUI();

        });
    }
}

 class SyntaxHighlighterGUI extends JFrame {
    private JTextPane textPane;
    private Timer highlightTimer; // ✅ performans için gecikmeli highlight


    public SyntaxHighlighterGUI() {
        setTitle("Real-Time Syntax Highlighter");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);


        // Yazı alanı
        textPane = new JTextPane();
        textPane.setFont(new Font("Consolas", Font.PLAIN, 16));
        JScrollPane scrollPane = new JScrollPane(textPane);
        add(scrollPane);

        // Stil ayarlarını yap
        setupStyles();

        // ✅ Highlight işlemini gecikmeli yapacak timer
        highlightTimer = new Timer(100, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                highlight();
                highlightTimer.stop(); // işlemden sonra timer dursun
            }
        });
        highlightTimer.setRepeats(false); // her tetiklemede yalnızca 1 kez çalışsın

        // ✅ DocumentListener → her yazı değişiminde highlightTimer yeniden başlar
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                highlightTimer.restart();
            }

            public void removeUpdate(DocumentEvent e) {
                highlightTimer.restart();
            }

            public void changedUpdate(DocumentEvent e) {
                highlightTimer.restart();
            }
        });

        setVisible(true);
    }

    // 🔹 Tüm token türleri için stil (renk) tanımları
    private void setupStyles() {
        StyledDocument doc = textPane.getStyledDocument();

        // Varsayılan metin rengi
        Style defaultStyle = textPane.addStyle("DEFAULT", null);
        StyleConstants.setForeground(defaultStyle, Color.BLACK);

        // Keyword (mavi)
        Style keyword = textPane.addStyle("KEYWORD", null);
        StyleConstants.setForeground(keyword, Color.BLUE);

        // Number (kırmızı)
        Style number = textPane.addStyle("NUMBER", null);
        StyleConstants.setForeground(number, Color.RED);

        // Operator (turuncu)
        Style operator = textPane.addStyle("OPERATOR", null);
        StyleConstants.setForeground(operator, Color.ORANGE);

        // Paren (yeşil)
        Style paren = textPane.addStyle("PAREN", null);
        StyleConstants.setForeground(paren, new Color(0, 128, 0));

        // Identifier (siyah)
        Style identifier = textPane.addStyle("IDENTIFIER", null);
        StyleConstants.setForeground(identifier, Color.BLACK);

        // **Error stili: kırmızı altı çizili**
        Style errorStyle = textPane.addStyle("ERROR", null);
        StyleConstants.setForeground(errorStyle, Color.RED);
        StyleConstants.setUnderline(errorStyle, true);
    }



     // 🔹 Metne göre renklendirme işlemi
     private void highlight() {
         String text = textPane.getText();
         if (text.isEmpty()) {
             setTitle("Real-Time Syntax Highlighter");
             return;
         }

         StyledDocument doc = textPane.getStyledDocument();

         // 1) Önce tüm metni DEFAULT (siyah) yap
         Style defaultStyle = textPane.getStyle("DEFAULT");
         doc.setCharacterAttributes(0, text.length(), defaultStyle, true);

         // 2) Token'ları al ve renklendir
         List<Lexer.Token> tokens = Lexer.tokenize(text);
         for (Lexer.Token token : tokens) {
             Style style = textPane.getStyle(token.type.name());
             if (style != null) {
                 doc.setCharacterAttributes(token.start, token.end - token.start, style, true);
             }
         }

         // 3) Parser ile kontrol, hata varsa errorStart/errorEnd kullan
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