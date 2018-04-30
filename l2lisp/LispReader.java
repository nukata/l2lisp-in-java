// H22.8/17, H30.4/8, H30.4/30 (鈴)
package l2lisp;

import java.io.*;
import java.util.*;
import java.math.BigInteger;

/** Lisp 式の読み取り器
*/
public class LispReader implements Closeable
{
    private final Lexer lex;
    private boolean erred = false;

    /** 与えられた引数から次々と Lisp 式を読み取るように構築する。
     * @param lines Lisp 式の読み取り先となる入力ストリーム
     */
    public LispReader (IInput lines) {
        lex = new Lexer (lines);
    }

    /** Closeable のメソッドの実装。
     * コンストラクタの引数の close() メソッドを呼び出す。
     */
    public void close() throws IOException {
        lex.close();
    }

    /** １個の Lisp 式を読む。
     * 行が尽きたら LL.EOF をいつまでも返す。
     * @return 読み取った Lisp 式，または LL.EOF
     * @throws IOException 入力ストリームから伝播した例外
     * @see l2lisp.LL#EOF
     */
    public Object read() throws IOException, EvalException {
        if (erred) { // 前回が構文エラーだったならば次の行まで読み飛ばす。
            int n = lex.getLineNumber();
            do {
                lex.showPrompt();
                if (! lex.next())
                    return LL.EOF;
            } while (lex.getLineNumber() == n);
            erred = false;
        } else {
            lex.showPrompt();
            if (! lex.next())
                return LL.EOF;
        }
        try {
            return parseExpression();
        } catch (SyntaxException ex) {
            erred = true;
            throw new EvalException ("SyntaxError: " + ex.getMessage() +
                                     " -- " + lex.getLineNumber() +
                                     ": " + lex.getLine());
        }
    }

    private Object parseExpression() throws IOException, SyntaxException {
        Object token = lex.current();
        if (token == Token.DOT || token == Token.RPAREN ||
            token == Token.RBRACKET) {
            throw new SyntaxException ("unexpected " + token);
        } else if (token == Token.LPAREN) {
            lex.next();
            return parseListBody();
        } else if (token == Token.QUOTE) {
            lex.next();
            return LL.list(LL.S_QUOTE, parseExpression());
        } else if (token == Token.TILDE) {
            lex.next();
            return LL.list(LL.S_DELAY, parseExpression());
        } else if (token == Token.BQUOTE) {
            lex.next();
            return QQ.expand(parseExpression());
        } else if (token == Token.COMMA) {
            lex.next();
            return new QQ.Unquote (parseExpression());
        } else if (token == Token.COMMA_AT) {
            lex.next();
            return new QQ.UnquoteSplicing (parseExpression());
        } else if (token == Token.LBRACKET) {
            lex.next();
            return parseVectorBody();
        } else {
            return token;
        }
    }

    private Object parseListBody() throws IOException, SyntaxException {
        if (lex.current() == Token.RPAREN) {
            return null;
        } else {
            Object e1 = parseExpression();
            lex.next();
            Object e2;
            if (lex.current() == Token.DOT) {
                lex.next();
                e2 = parseExpression();
                lex.next();
                if (lex.current() != Token.RPAREN)
                    throw new SyntaxException ("\")\" expected: "
                                               + lex.current());

            } else {
                e2 = parseListBody();
            }
            return new Cell (e1, e2);
        }
    }

    private Object[] parseVectorBody() throws IOException, SyntaxException {
        ArrayList<Object> a = new ArrayList<Object> ();
        while (lex.current() != Token.RBRACKET) {
            Object element = parseExpression();
            a.add(element);
            lex.next();
        }
        return a.toArray();
    }


    /** 準引用 (Quasi-Quotation)
     */
    private static class QQ
    {
        static class Unquote {
            final Object x;

            Unquote (Object x) {
                this.x = x;
            }

            @Override public String toString() {
                return "," + LL.str(x);
            }
        } // Unquote

        static class UnquoteSplicing {
            final Object x;

            UnquoteSplicing (Object x) {
                this.x  = x;
            }

            @Override public String toString() {
                return ",@" + LL.str(x);
            }
        } // UnquoteSplicing

        /** 準引用式 `x の x を等価な S 式に展開する。
         */
        static Object expand(Object x) {
            if (x instanceof Cell) {
                Cell t = expand1(x);
                if ((t.car instanceof Cell) && (t.cdr == null)) {
                    Cell k = (Cell) t.car;
                    if (k.car == LL.S_LIST || k.car == LL.S_CONS)
                        return k;
                }
                return new Cell (LL.S_APPEND, t);
            } else if (x instanceof Unquote) {
                return ((Unquote) x).x;
            } else {
                return quote(x);
            }
        }

        // 引数をクォートする。ただし数や文字列はわざわざクォートしない。
        private static Object quote(Object x) {
            if (x instanceof Symbol || x instanceof Cell)
                return LL.list(LL.S_QUOTE, x);
            else
                return x;
        }

        // `x の x を append の引数として使えるように展開する。
        // 例 1: (,a b) => h=(list a) t=((list 'b)) => ((list a 'b))
        // 例 2: (,a ,@(cons 2 3)) => h=(list a) t=((cons 2 3)) 
        //                         => ((cons a (cons 2 3)))
        private static Cell expand1(Object x) {
            if (x instanceof Cell) {
                Cell xc = (Cell) x;
                Object h = expand2(xc.car);
                Object t = expand1(xc.cdr);
                if (t instanceof Cell) {
                    Cell tc = (Cell) t;
                    if (tc.car == null && tc.cdr == null) {
                        return LL.list(h);
                    } else if (h instanceof Cell) {
                        Cell hc = (Cell) h;
                        if (hc.car == LL.S_LIST) {
                            if (tc.car instanceof Cell) {
                                Cell tcar = (Cell) tc.car;
                                if (tcar.car == LL.S_LIST) {
                                    Object hh = concat(hc, tcar.cdr);
                                    return new Cell (hh, tc.cdr);
                                }
                            }
                            if (hc.cdr instanceof Cell) {
                                Object hh = consCons((Cell) hc.cdr, tc.car);
                                return new Cell (hh, tc.cdr);
                            }
                        }
                    }
                }
                return new Cell (h, t);
            } else if (x instanceof Unquote) {
                return LL.list(((Unquote) x).x);
            } else {
                return LL.list(quote(x));
            }
        }

        // concat(LL.list(1, 2), LL.list(3, 4)) => (1 2 3 4)
        private static Object concat(Cell x, Object y) {
            if (x == null)
                return y;
            else
                return new Cell (x.car, concat((Cell) x.cdr, y));
        }

        // consCons(LL.list(1, 2, 3), "a") => (cons 1 (cons 2 (cons 3 "a")))
        private static Object consCons(Cell x, Object y) {
            if (x == null)
                return y;
            else
                return LL.list(LL.S_CONS, x.car, consCons((Cell) x.cdr, y));
        }

        // `x の x.car を append の１引数として使えるように展開する。
        // 例 ,a => (list a);  ,@(foo 1 2) => (foo 1 2); b => (list 'b)
        private static Object expand2(Object x) {
            if (x instanceof Unquote)
                return LL.list(LL.S_LIST, ((Unquote) x).x);
            else if (x instanceof UnquoteSplicing)
                return ((UnquoteSplicing) x).x;
            else
                return LL.list(LL.S_LIST, expand(x));
        }
    } // QQ


    /** 字句解析器 */
    private static final class Lexer
    {
        private final CharIterator ci;
        private Object token = "Ling!"; // 先読みしたトークン
        // token の初期値は EndOfFileException でなければ何でもよい。

        /** 文字イテレータを構築する。*/
        Lexer (IInput lines) {
            ci = new CharIterator (lines);
        }

        /** 先読みしておいたトークンを返す。
         * @throws SyntaxException 先読みしたトークンに対して例外があった。
         *   とりわけ EOF を先読みしていたときは EndOfFileException。
         */
        Object current() throws IOException, SyntaxException
        {
            if (token instanceof SyntaxException) {
                throw (SyntaxException) token;
            } else {
                return token;
            }
        }

        /** 次のトークンを先読みする。読んだトークンは current() で得られる。
         * 今までのトークンは捨てられる。
         * @return EOF を先読みしたら false
         */
        boolean next() throws IOException {
            if (token instanceof EndOfFileException) {
                return false;
            } else {
                try {
                    token = nextToken();
                    return true;
                } catch (EndOfFileException ex) {
                    token = ex;
                    return false;
                }
            }
        }
        
        private Object nextToken() throws IOException, EndOfFileException {
            char ch;
            for (;;) {          // 空白とコメントを読み飛ばす
                while (Character.isWhitespace(ci.current()))
                    ci.next();
                ch = ci.current();
                if (ch == ';') { // ; コメント
                    ci.next();
                    while (ci.current() != '\n')
                        ci.next();
                } else {
                    break;      // 空白でもコメントでもないから break
                }
            }
            if (ch == ',') {
                ci.next();
                if (ci.current() == '@') {
                    ci.next(); return Token.COMMA_AT;
                } else {
                    return Token.COMMA;
                }
            } else if (ch == '(') {
                ci.next(); return Token.LPAREN;
            } else if (ch == ')') {
                ci.next(); return Token.RPAREN;
            } else if (ch == '.') {
                ci.next(); return Token.DOT;
            } else if (ch == '\'') {
                ci.next(); return Token.QUOTE;
            } else if (ch == '~') {
                ci.next(); return Token.TILDE;
            } else if (ch == '`') {
                ci.next(); return Token.BQUOTE;
            } else if (ch == '[') {
                ci.next(); return Token.LBRACKET;
            } else if (ch == ']') {
                ci.next(); return Token.RBRACKET;
            } else if (ch == '"') {
                Object s = getString(ci);
                ci.next(); return s;
            } else {
                StringBuilder sb = new StringBuilder ();
                for (;;) {
                    sb.append(ch);
                    if (! ci.next())
                        break;
                    ch = ci.current();
                    if (ch == '(' || ch == ')' || ch == '\'' ||
                        ch == '[' || ch == ']' || ch == '"' ||
                        ch == '~' || Character.isWhitespace(ch))
                        break;
                }
                String tk = sb.toString();
                if ("nil".equals(tk)) {
                    return null;
                } else {
                    Number num = tryToParseAsNumber(tk);
                    if (num != null) {
                        return num;
                    } else if (tk.startsWith(":")) { // :ではじまるkeyword？
                        String tail = tk.substring(1);
                        if (checkSymbol(tail))
                            try {
                                return Symbol.Keyword.of(tk);
                            } catch (IllegalArgumentException ex) {
                                return Symbol.of(tk);
                            }
                    } else if (checkSymbol(tk)) { // シンボル？
                        return Symbol.of(tk);
                    }
                    return new SyntaxException ("bad token: " + LL.str(tk));
                }
            }
        }

        /** 可能ならば１次プロンプトを表示する。*/
        void showPrompt() {
            ci.showPrompt();
        }

        /** IInput インスタンスを close する。*/
        void close() throws IOException {
            ci.lines.close();
        }

        /** 現在の行番号 */
        int getLineNumber() { return ci.lineNumber; }

        /** 現在の行 */
        String getLine() { return ci.line; }
    } // Lexer


    /** 文字イテレータ */
    private static final class CharIterator
    {
        final IInput lines;
        String line = null;     // 現在の行
        int lineNumber = 0;
        private char ch = ' ';  // 先読みした１文字 (初期値は空白)
        private boolean hasSeenEOF = false; //  EOF を先読みしたか？
        private int i = -1;     // 現在の行の中の文字の位置
        private boolean showPrompt = false;

        CharIterator (IInput lines) {
            this.lines = lines;
        }

        /** 次の next() で行を読み取るならば，１次プロンプトを表示させる。
         */
        void showPrompt() {
            showPrompt = (i < 0);
        }

        /** 次の文字を先読みする。
         * @return EOF を先読みしたら false
         */
        boolean next() throws IOException {
            if (i < 0) {
                line = lines.readLine(showPrompt);
                showPrompt = false;
                lineNumber++;
                if (line == null) {
                    hasSeenEOF = true;
                    return false;
                }
                i = 0;
            }
            if (i < line.length()) {
                ch = line.charAt(i);
                i++;
            } else {
                i = -1;
                ch = '\n';  // 行末文字
            }
            return true;
        }

        /** 先読みしておいた文字を返す。
         * @throws EndOfFileException EOF を先読みしていた。
         */
        char current() throws EndOfFileException {
            if (hasSeenEOF)
                throw new EndOfFileException ();
            else
                return ch;
        }
    } // CharIterator


    // 字句解析の下請け関数

    /** 文字列を読み取る。
     * String または SyntaxException を返す。
     * ci.current() が '"' を指している状態で呼び出される。
     * ci.current() が次の '"' を指している状態で終わる。
     */
    private static Object getString(CharIterator ci)
        throws IOException, EndOfFileException
    {
        StringBuilder sb = new StringBuilder ();
        ci.next();              // '"' の次へ進む
        for (;;) {
            char ch = ci.current();
            if (ch == '"') {
                return sb.toString();
            } else if (ch == '\n') {
                return new SyntaxException
                    ("string not terminated: " + LL.str(sb.toString()));
            } else if (ch == '\\') {
                ci.next();
                switch (ci.current()) {
                case '0': case '1': case '2': case '3':
                case '4': case '5': case '6': case '7':
                    sb.append(getOctChar(ci));
                    continue;   // 八進文字の次を先読み済みだから
                case 'x':
                    sb.append(getHexChar(ci));
                    continue;   // 十六進文字の次を先読み済みだから
                case '\"':
                    ch = '\"'; break;
                case '\\':
                    ch = '\\'; break;
                case 'a':
                    ch = '\007'; break; // Java には \a がないから
                case 'b':
                    ch = '\b'; break;
                case 'f':
                    ch = '\f'; break;
                case 'n':
                    ch = '\n'; break;
                case 'r':
                    ch = '\r'; break;
                case 't':
                    ch = '\t'; break;
                case 'v':
                    ch = '\013'; break; // Java には \v がないから
                default:
                    return new SyntaxException
                        ("bad escape: " + ci.current());
                }
            }
            sb.append(ch);
            ci.next();
        }

    }

    /** 文字列の中で \ に続く八進数で表現された文字を読み取る。
     * ci は \ の次の数字を指しているとする。
     * 数の次の文字を先読みした状態で終わる。
     */
    private static char getOctChar(CharIterator ci)
        throws IOException, EndOfFileException
    {
        int result = ci.current() - '0';
        for (int i = 0; ci.next() && i < 2; i++) {
            char ch = ci.current();
            if ('0' <= ch && ch <= '9')
                result = (result << 3) + ch - '0';
            else
                break;
        }
        return (char) result;
    }

    /** 文字列の中で \x に続く十六進数で表現された文字を読み取る。
     * ci は \ の次の x を指しているとする。
     * 数の次の文字を先読みした状態で終わる。
     */
    private static char getHexChar(CharIterator ci)
        throws IOException, EndOfFileException
    {
        int result = 0;
        for (int i = 0; ci.next() && i < 2; i++) {
            char ch = ci.current();
            if ('0' <= ch && ch <= '9')
                result = (result << 4) + ch - '0';
            else if ('A' <= ch && ch <= 'F')
                result = (result << 4) + ch - 'A' + 10;
            else if ('a' <= ch && ch <= 'f')
                result = (result << 4) + ch - 'a' + 10;
            else
                break;
        }
        return (char) result;
    }

    /** 文字列に対して数としての解釈を試みる。
     * 失敗時は null を返す。
     */
    private static Number tryToParseAsNumber(String s) {
        boolean isNegative = false;
        if (s.startsWith("-")) {
            isNegative = true;
            s = s.substring(1);
        }
        int radix = 10;
        if (s.startsWith("#") && s.length() >= 3) {
            switch (s.charAt(1)) {
            case 'b': case 'B':
                radix = 2; break;
            case 'o': case 'O':
                radix = 8; break;
            case 'x': case 'X':
                radix = 16; break;
            default:
                return null;
            }
            s = s.substring(2);
        }
        if (isNegative)
            s = "-" + s;
        try {
            return Integer.valueOf(s, radix);
        } catch (NumberFormatException ex) {}
        try {
            return new BigInteger (s, radix);
        } catch (NumberFormatException ex) {}
        if (radix == 10) {
            try {
                return Double.valueOf(s); // new Double(s) は deprecated
            } catch (NumberFormatException ex) {}
        }
        return null;
    }

    /** 引数がシンボルとして適切かどうか判定する。
     */
    private static boolean checkSymbol(String s) {
        for (int i = 0, n = s.length(); i < n; i++) {
            char c = s.charAt(i);
            if (! (('a' <= c && c <= 'z') ||
                   ('A' <= c && c <= 'Z') ||
                   ('0' <= c && c <= '9') ||
                   c == '_' || c == '&' || c == '$' ||
                   c == '*' || c == '/' || c == '%' ||
                   c == '+' || c == '-' || c == '<' ||
                   c == '>' || c == '=' || c == '!' ||
                   c == '?'))
                return false;
        }
        return true;
    }


    /* 構文の組立てに使われるトークン
       これらが結果の Lisp 式に出現することはない。
       ここではデバッグの便宜のため金物表現と一致するシンボルを使うが，
       一意に識別できるオブジェクトであれば何でもよい。
    */
    private static class Token {
        static final Symbol BQUOTE = Symbol.of("`");
        static final Symbol COMMA = Symbol.of(",");
        static final Symbol COMMA_AT = Symbol.of(",@");
        static final Symbol DOT = Symbol.of(".");
        static final Symbol LBRACKET = Symbol.of("[");
        static final Symbol LPAREN = Symbol.of("(");
        static final Symbol RBRACKET = Symbol.of("]");
        static final Symbol RPAREN = Symbol.of(")");
        static final Symbol QUOTE = Symbol.of("'");
        static final Symbol TILDE = Symbol.of("~");
    } // Token

    /** 構文上の誤りを表す例外 */
    private static class SyntaxException extends Exception
    {
        SyntaxException (String message) {
            super (message);
        }
    } // SyntaxException

    /** ファイルの終端に達したことを表す例外 */
    private static class EndOfFileException extends SyntaxException
    {
        EndOfFileException () {
            super ("unexpected EOF");
        }
    } // EndOfFileException
} // LispReader
