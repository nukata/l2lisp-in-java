// H22.8/17, H30.4/8, H30.4/30 (鈴)
package l2lisp;

import java.io.*;
import java.util.*;
import java.math.BigInteger;

/** 大域的な定数と関数等の置き場
 */
public class LL
{
    /** このクラスはインスタンスを作らない。*/
    private LL () {}

    /** L2 Lisp のバージョン */
    public static final double VERSION = 9.4;

    /** 初期化 Lisp スクリプト名 */
    public static String PRELUDE = "Prelude.l";

    /** 著作権表示テキスト名 */
    static String COPYRIGHT = "Copyright.txt";

    /** 再帰的に印字する深さ */
    static int MAX_EXPANSIONS = 10;

    /** 例外発生時の評価トレースの記録段数 */
    static int MAX_EXC_TRACES = 20;

    /** 静的にマクロ展開する深さ */
    static int MAX_MACRO_EXPS = 30;

    // シンボルの定数
    static final Symbol
        S_APPEND = Symbol.of("append"),
        S_CATCH = Symbol.Keyword.of("catch"),
        S_COND = Symbol.Keyword.of("cond"),
        S_CONS = Symbol.of("cons"),
        S_DELAY = Symbol.Keyword.of("delay"),
        S_ERROR = Symbol.of("*error*"),
        S_LAMBDA = Symbol.Keyword.of("lambda"),
        S_LIST = Symbol.of("list"),
        S_MACRO = Symbol.Keyword.of("macro"),
        S_PROGN = Symbol.Keyword.of("progn"),
        S_QUOTE = Symbol.Keyword.of("quote"),
        S_REST = Symbol.of("&rest"),
        S_SETQ = Symbol.Keyword.of("setq"),
        S_T = Symbol.of("t"),
        S_UNWIND_PROTECT = Symbol.Keyword.of("unwind-protect");

    /** Lisp の中で EOF を表す値 */
    public static final Object EOF = new Object () {
            public String toString() {
                return "#<eof>";
            }
        };


    // 文字列化関数

    /** Lisp としての値を文字列化する。文字列を引用符で囲む。
     * @param arg null を含む任意の値
     * @return 引数の Lisp 値としての文字列表現
     */
    public static String str(Object arg) {
        return str(arg, true);
    }

    /** Lisp としての値を文字列化する。
     * @param arg null を含む任意の値
     * @param printQuote 真ならば文字列を引用符で囲む。
     * @return 引数 arg の Lisp 値としての文字列表現
     */
    public static String str(Object arg, boolean printQuote) {
        return str(arg, printQuote, MAX_EXPANSIONS, new HashSet<Object> ());
    }

    /** 2 引数 str の下請け
     */
    static String str(Object x, boolean printQuote, int recLevel,
                      Set<Object> printed) {
        if (x == null) {
            return "nil";
        } else if (x instanceof Cell) {
            Cell xc = (Cell) x;
            if (xc.car == S_QUOTE && xc.cdr instanceof Cell) {
                Cell xcdr = (Cell) xc.cdr;
                if (xcdr.cdr == null)
                    return "'" + str(xcdr.car, printQuote, recLevel, printed);
            }
            return "(" + xc.repr(printQuote, recLevel, printed) + ")";
        } else if (x instanceof String) {
            String xs = (String) x;
            if (! printQuote)
                return xs;
            StringBuilder sb = new StringBuilder ();
            sb.append('"');
            for (int i = 0, n = xs.length(); i < n; i++) {
                char c = xs.charAt(i);
                if (c == '"')
                    sb.append("\\\"");
                else if (c == '\\')
                    sb.append("\\\\");
                else if (c == '\n')
                    sb.append("\\n");
                else if (c == '\r')
                    sb.append("\\r");
                else if (c == '\t')
                    sb.append("\\t");
                else if (Character.isISOControl(c))
                    sb.append(String.format("\\x%02x", (int) c));
                else
                    sb.append(c);
            }
            sb.append('"');
            return sb.toString();
        } else {
            if (x instanceof Object[]) // 参照型の配列ならば…
                x = Arrays.asList((Object[]) x);
            if (x instanceof Iterable) {
                Iterable xl = (Iterable) x;
                if (! printed.add(xl)) { // 重複していたならば…
                    recLevel--;
                    if (recLevel == 0)
                        return "[...]";
                }
                StringBuilder sb = new StringBuilder ();
                sb.append("[");
                boolean first = true;
                for (Object e: xl) {
                    if (first)
                        first = false;
                    else
                        sb.append(" ");
                    sb.append(str(e, printQuote, recLevel, printed));
                }
                sb.append("]");
                return sb.toString();
            } else {
                return
                    (x instanceof long[])   ? Arrays.toString((long[]) x) :
                    (x instanceof int[])    ? Arrays.toString((int[]) x) :
                    (x instanceof short[])  ? Arrays.toString((short[]) x) :
                    (x instanceof char[])   ? Arrays.toString((char[]) x) :
                    (x instanceof byte[])   ? Arrays.toString((byte[]) x) :
                    (x instanceof boolean[])? Arrays.toString((boolean[]) x) :
                    (x instanceof float[])  ? Arrays.toString((float[]) x) :
                    (x instanceof double[]) ? Arrays.toString((double[]) x) :
                    x.toString();
            }
        }
        // Java では小数点以下 0 の double 値の文字列表現は ".0" が付随する。
        // C# と異なり，そのような値に対し陽に + ".0" とする必要はない。
    }


    // その他の関数

    /** Lisp の list 関数に相当する。
     * @param args null を含む任意の値の並び
     * @return 引数と同じ要素を同じ順に並べた Lisp のリスト
     */
    public static Cell list(Object... args) {
        return mapcar(Arrays.asList(args), null);
    }

    /** list 引数の各要素に fn 引数を適用した Lisp のリストを作る。
     * @param list null を含む任意の値の並びまたは null
     * @param fn 各要素に適用する関数，ただし null ならば恒等関数とみなす。
     * @return 各要素に関数を適用した結果からなるリスト
     * @see Cell#mapcar
     */
    public static Cell mapcar(Iterable list, IUnary fn) {
        if (list == null)
            return null;
        Cell z = null;
        Cell y = null;
        for (Object e: list) {
            if (fn != null)
                e = fn.apply(e);
            Cell x = new Cell (e, null);
            if (z == null)
                z = x;
            else
                y.cdr = x;
            y = x;
        }
        return z;
    }

    /** Java による１引数関数のためのインタフェース
     * @see #mapcar
     */
    public static interface IUnary
    {
        Object apply(Object x);
    }

    /** 引数が約束ならばそれをかなえた値を返す。そうでなければ引数を返す。
     * @param x 任意の Lisp 値
     * @return Lisp の force 関数を適用した結果と同じ値
     */
    public static Object force(Object x) {
        if (x instanceof Promise)
            x = ((Promise) x).deliver();
        return x;
    }


    /** 単独の Lisp インタープリタとしての主プログラムのサンプル実装.
     * IInterp インタフェース実装クラス Intep の外部では，本メソッドだけが
     * Interp に依存する。その処理は次のとおりである。
     * <ol>
     * <li> Interp インスタンス interp を構築する。
     * <li> BuiltInFunctions.FUNCTIONS を interp.load する。 
     *      car, cdr, cons 等が定義される。
     * <li> このクラスと同じ場所にある PRELUDE ファイルを UTF-8 で読んで
     *      interp.run する。defun, let, equal 等が定義される。
     * <li> コマンド行引数をファイル名としてそれぞれ UTF-8 で読んで
     *     interp.run する。ただし，コマンド行引数がないか "_" ならば，
     *     対話セッションとして interp.run する。
     * </ol>
     * @param args Lisp プログラムのファイル名または "-" からなる並び
     * @throws Exception 対話セッション中の EvalException 以外の未捕獲例外
     */
    public static void main(String[] args) throws Exception {
        IInterp interp = new Interp ();
        interp.load(BuiltInFunctions.FUNCTIONS);

        IInput prelude = new LinesFromInputStream
            (LL.class.getResourceAsStream(PRELUDE));
        interp.run(prelude, null);

        for (String fname: ((args.length == 0) ? new String[] {"-"} : args)) {
            IInput input;
            IInterp.IReceiver receiver;
            if (fname.equals("-")) { // 対話セッション？
                input = new LinesFromConsole ("> ", "| ", "Goodbye");
                receiver = new IInterp.IReceiver () {
                        public void receiveResult(Object result) {
                            System.out.println("=> " + LL.str(result));
                        }
                        public void receiveException(EvalException ex) {
                            System.out.println(ex);
                        }
                    };
            } else {
                input = new LinesFromInputStream (new FileInputStream (fname));
                receiver = null;
            }
            interp.run(input, receiver);
        }
    }
} // LL
