// H22.8/17, H30.4/8 (鈴)
package l2lisp;

import java.util.*;
import java.io.IOException;
import java.io.PrintWriter;

/** Lisp インタープリタ本体
 */
public class Interp implements IInterp
{
    private final Map<Symbol, Object> symbols
        = new NameMap<Symbol, Object> (); // シンボルから大域変数値への表
    private LispReader reader;  // Lisp から (read) するときに使う
    private PrintWriter writer; // Lisp から印字するときに使う

    /** 引数を null として構築する。
     */
    public Interp () {
        this (null, null);
    }

    /** 入力と出力を指定して構築する。
     * 引数が null ならばそれぞれ標準の入力と出力が使われる。
     * <p>
     * 通常，入力は (read) に，出力は (print x) に使われるが，
     * 具体的な使用法は load する関数に委ねられている。
     * 本クラスはそれらの関数に getReader() と getWriter() を
     * 通して入出力の手段を提供するだけである。
     * @param input null または Lisp の入力もと
     * @param output null または Lisp の出力さき
     */
    public Interp (IInput input, PrintWriter output) {
        if (input == null)
            input = new LinesFromConsole ("", "", null);
        if (output == null)
            output = new PrintWriter (System.out, true);

        reader = new LispReader (input);
        writer = output;
        symbols.put(LL.S_ERROR, LL.S_ERROR);
        symbols.put(LL.S_T, LL.S_T);
        symbols.put(Symbol.of("*version*"), LL.list(LL.VERSION, "Java"));
        symbols.put(Symbol.of("*eof*"), LL.EOF);
    }

    // IInterp のメソッドの実装
    public Map<Symbol, Object> getSymbolTable() {
        return symbols;
    }

    // IInterp のメソッドの実装
    public LispReader getReader() {
        return reader;
    }

    // IInterp のメソッドの実装
    public PrintWriter getWriter() {
        return writer;
    }

    // IInterp のメソッドの実装
    public void load(Callable[] functions) {
        for (Callable f: functions) {
            Symbol sym = Symbol.of(f.getName());
            symbols.put(sym, f);
        }
    }

    // IInterp のメソッドの実装
    public Object run(IInput script, IReceiver receiver)
        throws IOException
    {
        try {
            LispReader lr = new LispReader (script);
            Object result = null;
            for (;;) {
                try {
                    Object x = lr.read();
                    if (x == LL.EOF)
                        return result;
                    result = eval(x, null);
                    if (receiver != null)
                        receiver.receiveResult(result);
                } catch (EvalException ex) {
                    if (receiver != null)
                        receiver.receiveException(ex);
                    else
                        throw ex;
                }
            }
        } finally {
            script.close();
        }
    }

    /** 文字列に書かれたスクリプトを評価する便宜メソッド.
     * @param text ここに書かれた式を次々と評価する。
     * @return 最後に書かれた式の評価結果
     * @throws EvalException 式を評価した時に発生した例外
     */
    public Object run(String text) throws EvalException {
        IInput input = new LinesFromString (text);
        try {
            return run(input, null);
        } catch (IOException ex) { // この例外はありえない
            throw new RuntimeException ("Impossible!", ex);
        }
    }


    // IInterp のメソッドの実装
    public Object eval(Object x, Cell env) {
        try {
            for (;;) {
                if (x instanceof Symbol) {
                    if (symbols.containsKey(x))
                        return symbols.get(x);
                    else if (x instanceof Symbol.Keyword)
                        return x;
                    throw new EvalException ("void variable", x);
                } else if (x instanceof Arg) {
                    return ((Arg) x).getValue(env);
                } else if (x instanceof Cell) {
                    Cell xc = (Cell) x;
                    Object fn = xc.car;
                    Cell arg = xc.getCdrCell();
                    if (fn instanceof Symbol.Keyword) {
                        if (fn == LL.S_QUOTE) { // (quote value)
                            if (arg.cdr == null)
                                return arg.car;
                            throw new EvalException ("bad quote");
                        } else if (fn == LL.S_PROGN) {
                            x = evalProgN(arg, env);
                        } else if (fn == LL.S_COND) {
                            x = evalCond(arg, env);
                        } else if (fn == LL.S_SETQ) {
                            return evalSetQ(arg, env);
                        } else if (fn == LL.S_LAMBDA) {
                            return compile(Closure.FACTORY, arg, env);
                        } else if (fn == LL.S_MACRO) {
                            if (env != null)
                                throw new EvalException ("nested macro", x);
                            arg = replaceDummyVariables(arg);
                            return compile(Macro.FACTORY, arg, null);
                        } else if (fn == LL.S_CATCH) {
                            return evalCatch(arg, env);
                        } else if (fn == LL.S_UNWIND_PROTECT) {
                            return evalUnwindProtect(arg, env);
                        } else if (fn == LL.S_DELAY) {
                            if (arg.cdr == null)
                                return new Promise (arg.car, env, this);
                            throw new EvalException ("bad delay");
                        } else {
                            throw new EvalException ("bad keyword", fn);
                        }
                    } else {
                        fn = LL.force(eval(fn, env));
                        if (fn instanceof Closure) {
                            Closure cl = (Closure) fn;
                            env = cl.makeEnv(arg, this, env);
                            x = cl.evalBody(this, env);
                        } else if (fn instanceof Macro) {
                            x = ((Macro) fn).expandWith(arg, this);
                        } else if (fn instanceof Callable) {
                            return ((Callable) fn).evalWith(arg, this, env);
                        } else {
                            throw new EvalException ("not applicable", fn);
                        }
                    }
                } else if (x instanceof Lambda) {
                    return new Closure ((Lambda) x, env);
                } else {
                    return x;   // 数や文字列や null など
                }
            } // for (;;)
        } catch (EvalException ex) {
            List<Object> trace = ex.getTrace();
            if (trace.size() < LL.MAX_EXC_TRACES)
                trace.add(LL.str(x));
            throw ex;
        }
    }


    // (progn e1 e2 e3) ならば e1, e2 を評価して e3 をそのまま返す。
    // cf. Closure#evalBody
    private Object evalProgN(Cell arg, Cell env) {
        if (arg == null) {
            return null;
        } else {
            Cell j = arg;
            for (;;) {
                Object x = j.car;
                j = j.getCdrCell();
                if (j == null)
                    return x;   // 末尾の式は戻った先で評価する。
                eval(x, env);
            }
        }
    }

    // (cond (c1 e1...) (c2 e21 e22) ...) で c2 が最初に成立したならば
    // e21 を評価して e22 をそのまま返す。
    private Object evalCond(Cell arg, Cell env) {
        for (Cell j = arg; j != null; j = j.getCdrCell()) {
            Object clause = j.car;
            if (clause == null) {
                ;               // 空節ならば不成立扱いとする。
            } else if (! (clause instanceof Cell)) {
                throw new EvalException ("cond test expected", clause);
            } else {
                Cell c = (Cell) clause;
                Object result = LL.force(eval(c.car, env));
                if (result != null) { // テスト結果が真ならば…
                    Cell body = c.getCdrCell();
                    if (body == null) { // ⇒テスト結果をそのまま結果とする。
                        // 二重評価を防止するためクォートする。
                        return LL.list(LL.S_QUOTE, result);
                    } else {  // ⇒のこりを (progn ...) と同じに扱う。
                        return evalProgN(body, env);
                    }
                }
            }
        }
        return null;        // 成立する節がなかった。
    }

    // (setq v1 e1) ならば e1 を評価して v1 に代入し，e2 を返す。
    private Object evalSetQ(Cell arg, Cell env) {
        Object result = null;
        for (Cell j = arg; j != null; j = j.getCdrCell()) {
            Object lval = j.car;
            j = j.getCdrCell();
            if (j == null)
                throw new EvalException ("right value expected");
            result = eval(j.car, env);
            if (lval instanceof Symbol)
                symbols.put((Symbol) lval, result);
            else if (lval instanceof Arg)
                ((Arg) lval).setValue(result, env);
            else
                throw new VariableExpectedException (lval);
        }
        return result;
    }


    /** Lisp のリスト (macro ...) または (lambda ...) をコンパイルして 
     * Macro または Lambda または Closure のインスタンスを作る。
     * @param factory コンパイルした式を作るファクトリ
     * @param arg リストの cdr
     * @param env 現在の環境
     * @return コンパイルした式
     */
    private DefinedFunction compile(DefinedFunction.Factory factory,
                                    Cell arg, Cell env) {
        if (arg == null)
            throw new EvalException ("arglist and body expected");
        Map<Object, Arg> table = new NameMap<Object, Arg> ();
        boolean hasRest = makeArgTable(arg.car, table);
        int arity = table.size();
        Cell body = arg.getCdrCell();
        body = (Cell) scanForArgs(body, table);
        body = (Cell) expandMacros(body, LL.MAX_MACRO_EXPS);
        body = (Cell) compileInners(body);
        return factory.make(arity, hasRest, body, env);
    }

    /** 式の中のマクロを展開する。
     * @param j 対象となる式
     * @param count ここで展開できる入れ子深さの残り
     * @return 展開結果の式
     */
    private Object expandMacros(Object j, final int count) {
        if (count > 0 && j instanceof Cell) {
            Cell jc = (Cell) j;
            Object k = jc.car;
            if (k == LL.S_QUOTE || k == LL.S_LAMBDA || k == LL.S_MACRO) {
                return j;
            } else {
                if (k instanceof Symbol && symbols.containsKey(k))
                    k = symbols.get(k);
                if (k instanceof Macro) {
                    Cell jcdr = jc.getCdrCell();
                    Object z = ((Macro) k).expandWith(jcdr, this);
                    return expandMacros(z, count - 1);
                } else {
                    LL.IUnary fn = new LL.IUnary () {
                        public Object apply(Object x) {
                            return expandMacros(x, count);
                        }
                    };
                    return jc.mapcar(fn);
                }
            }
        } else {
            return j;
        }
    }

    /** 入れ子のラムダ式を Lambda インスタンスに置き換える。
     * @param j 元の式
     * @return 置き換えた式
     */
    private Object compileInners(Object j) {
        if (j instanceof Cell) {
            Cell jc = (Cell) j;
            Object k = jc.car;
            if (k == LL.S_QUOTE) {
                return j;
            } else if (k == LL.S_LAMBDA) {
                Cell jcdr = jc.getCdrCell();
                return compile(Lambda.FACTORY, jcdr, null);
            } else if (k == LL.S_MACRO) {
                throw new EvalException ("nested macro", j);
            } else {
                LL.IUnary fn = new LL.IUnary () {
                    public Object apply(Object x) {
                        return compileInners(x);
                    }
                };
                return jc.mapcar(fn);
            }
        } else {
            return j;
        }
    }

    /** 仮引数の表を作る。
     * 正確には，仮引数のシンボルまたはダミーシンボルをキーとし，
     * そのコンパイル結果の Arg インスタンスを値とする表を作る。
     * @param args  仮引数の並び
     * @param table この仮引数の表に内容が追加される。
     * @return rest 引数の有無
     */
    private static boolean makeArgTable(Object args, Map<Object, Arg> table)
    {
        if (args == null || args instanceof Cell) {
            int offset = 0; // 仮引数に割り当てるフレーム内オフセット値
            boolean hasRest = false;
            for (Cell i = (Cell) args; i != null; i = i.getCdrCell()) {
                Object j = i.car;
                if (hasRest)
                    throw new EvalException ("2nd rest", j);
                if (j == LL.S_REST) { // &rest rest_arg
                    i = i.getCdrCell();
                    if (i == null)
                        throw new VariableExpectedException (i);
                    j = i.car;
                    if (j == LL.S_REST)
                        throw new VariableExpectedException (j);
                    hasRest = true;
                }
                Symbol sym;
                if (j instanceof Symbol) {
                    sym = (Symbol) j;
                } else if (j instanceof Arg) {
                    sym = ((Arg) j).symbol;
                    j = sym;
                } else if (j instanceof Dummy) {
                    sym = ((Dummy) j).symbol;
                } else {
                    throw new VariableExpectedException (j);
                }
                table.put(j, new Arg (0, offset, sym));
                offset++;
            }
            return hasRest;
        } else {
            throw new EvalException ("arglist expected", args);
        }
    }

    /** 仮引数を求めて式をスキャンする。
     * 式の中のシンボルまたはダミーシンボルのうち仮引数表に該当があるものを，
     * 表の Arg 値に置き換える。
     * もともと Arg 値だったものについては，そのシンボルを表から探し，
     * あれば表の Arg 値に置き換える (入れ子の同名の変数だったら，最内
     * のものとみなす)。なければ，Arg 値のレベルを 1 だけ上げる (入れ子
     * の外の変数とみなす)。
     * @param j スキャン対象となる式
     * @param 仮引数表
     * @param スキャンして置換した結果の式
     */
    private static Object scanForArgs(Object j,
                                      final Map<Object, Arg> table)
    {
        if (j instanceof Symbol || j instanceof Dummy) {
            Arg k = table.get(j);
            return (k == null) ? j : k;
        } else if (j instanceof Arg) {
            Arg ja = (Arg) j;
            Arg k = table.get(ja.symbol);
            return (k == null) ?
                new Arg (ja.level + 1, ja.offset, ja.symbol) : k;
        } else if (j instanceof Cell) {
            Cell jc = (Cell) j;
            if (jc.car == LL.S_QUOTE) {
                return jc;
            } else {
                LL.IUnary fn = new LL.IUnary () {
                    public Object apply(Object x) {
                        return scanForArgs(x, table);
                    }
                };
                return jc.mapcar(fn);
            }
        } else {
            return j;
        }
    }

    /** $ ではじまるシンボルをダミーシンボルに置き換える 
     */
    private static Cell replaceDummyVariables(Cell arg) {
        Map<Symbol, Dummy> names = new NameMap<Symbol, Dummy> ();
        return (Cell) scanForDummies(arg, names);
    }

    private static Object scanForDummies(Object j,
                                         final Map<Symbol, Dummy> names)
    {
        if (j instanceof Symbol) {
            Symbol js = (Symbol) j;
            if (js.getName().startsWith("$")) {
                Dummy k = names.get(js);
                if (k == null) {
                    k = new Dummy (js);
                    names.put(js, k);
                }
                return k;
            }
        } else if (j instanceof Cell) {
            LL.IUnary fn = new LL.IUnary () {
                public Object apply(Object x) {
                    return scanForDummies(x, names);
                }
            };
            return ((Cell) j).mapcar(fn);
        }
        return j;
    }

    
    // (catch tag body...)
    private Object evalCatch(Cell arg, Cell env) {
        if (arg == null)
            throw new EvalException ("tag and body expected");
        Object tag = LL.force(eval(arg.car, env));
        try {
            Cell body = arg.getCdrCell();
            Object result = null;
            for (Cell j = body; j != null; j = j.getCdrCell())
                result = eval(j.car, env);
            return result;
        } catch (LispThrowException th) {
            if (tag == th.getTag()) // タグを eq で比較する
                return th.getValue();
            else
                throw th;
        } catch (EvalException ex) { // 一般の評価時例外の捕捉
            if (tag == LL.S_ERROR)
                return ex;
            else
                throw ex;
        }
    }

    // (unwind-protect body cleanup...)
    private Object evalUnwindProtect(Cell arg, Cell env) {
        if (arg == null)
            throw new EvalException ("body and cleanup expected");
        try {
            return eval(arg.car, env);
        } finally {
            Cell cleanup = arg.getCdrCell();
            for (Cell j = cleanup; j != null; j = j.getCdrCell())
                eval(j.car, env);
        }
    }


    /** 名前表，ただしキーワードは除外する
     */
    private static class NameMap<S, T> extends HashMap<S, T>
    {
        public T put(S k, T v) {
            if (k instanceof Symbol.Keyword)
                throw new EvalException ("keyword not expected", k);
            return super.put(k, v);
        }
    }

    /** 変数があるべき場所に，変数がなかったことを知らせる例外
     */
    private static class VariableExpectedException extends EvalException
    {
        VariableExpectedException (Object exp) {
            super ("variable expected", exp);
        }
    } // VariableExpectedException

    // コンパイル後の Lisp 式を表すクラス

    /** コンパイル後の束縛変数。
     * ラムダ式やマクロ式の本体に含まれる。
     * 静的リンクの深さ (level) とフレーム内のオフセット (offset) を持つ。
     * 静的リンクは Cell によるリストで，フレームは Object[] でそれぞれ
     * 表現されるものとする。
     */
    private static final class Arg
    {
        final int level;
        final int offset;
        final Symbol symbol;

        Arg (int level, int offset, Symbol symbol) {
            this.level = level;
            this.offset = offset;
            this.symbol = symbol;
        }

        @Override public String toString() {
            return "#" + level + ":" + offset + ":" + symbol;
        }

        void setValue(Object x, Cell env) {
            for (int i = 0; i < level; i++)
                env = (Cell) env.cdr;
            ((Object[]) env.car)[offset] = x;
        }

        Object getValue(Cell env) {
            for (int i = 0; i < level; i++)
                env = (Cell) env.cdr;
            return ((Object[]) env.car)[offset];
        }
    } // Arg


    /** コンパイル後のマクロ式の dummy symbol */
    private static final class Dummy
    {
        final Symbol symbol;

        Dummy (Symbol symbol) {
            this.symbol = symbol;
        }

        @Override public String toString() {
            return String.format(":%s:%x", symbol, hashCode());
        }
    } // Dummy


    /** Lisp によって定義される関数の共通基底クラス  */
    private static abstract class DefinedFunction extends Function
    {
        final Cell body;

        DefinedFunction (int arity, boolean hasRest, Cell body) {
            super (arity, hasRest);
            this.body = body;
        }

        /** Lisp によって定義される関数をつくるファクトリ */
        static interface Factory {
            DefinedFunction make(int arity, boolean hasRest,
                                 Cell body, Cell env);
        } // Factory
    } // DefinedFunction


    /** コンパイル後の (lambda ...) ただし環境なし。
     * 入れ子のラムダ式をコンパイルした結果としてだけ使う。
     */
    private static final class Lambda extends DefinedFunction
    {
        Lambda (int arity, boolean hasRest, Cell body) {
            super (arity, hasRest, body);
        }

        @Override public String toString() {
            return LL.str(new Cell (Symbol.of("#<lambda>"),
                                    new Cell (carity(), body)));
        }

        static final Factory FACTORY = new Factory () {
                public DefinedFunction make(int arity, boolean hasRest,
                                            Cell body, Cell env) {
                    return new Lambda (arity, hasRest, body);
                }
            };
    } // Lambda


    /** コンパイル後の (macro ...) つまりマクロ式 */
    private static final class Macro extends DefinedFunction
    {
        Macro (int arity, boolean hasRest, Cell body) {
            super (arity, hasRest, body);
        }

        /** マクロを実引数の並びで展開する。
         */
        Object expandWith(Cell list, IInterp interp) {
            Object[] frame = makeFrame(list);
            Cell env = new Cell (frame, null);
            Object x = null;
            for (Cell j = body; j != null; j = j.getCdrCell())
                x = interp.eval(j.car, env);
            return x;
        }

        /** apply の実装。マクロを展開する。
         */
        @Override public Object apply(Cell list, IInterp interp, Cell env) {
            return expandWith(list, interp); // XXX 陽に禁止すべきか？
        }

        @Override public String toString() {
            return LL.str(new Cell (Symbol.of("#<macro>"),
                                    new Cell (carity(), body)));
        }

        static final Factory FACTORY = new Factory () {
                public DefinedFunction make(int arity, boolean hasRest,
                                            Cell body, Cell env) {
                    return new Macro (arity, hasRest, body);
                }
            };
    } // Macro


    /** コンパイル後の (lambda ...) ただし環境つき，つまりクロージャ */
    private static final class Closure extends DefinedFunction
    {
        private final Cell env;

        Closure (int arity, boolean hasRest, Cell body, Cell env) {
            super (arity, hasRest, body);
            this.env = env;
        }

        Closure (Lambda x, Cell env) {
            this (x.getArity(), x.hasRest(), x.body, env);
        }

        /** 実引数の並びから，クロージャを評価するための環境を作る。
         * 実引数はそれぞれ評価するが，暗黙の force はしない。
         */
        Cell makeEnv(Cell list, Interp interp, Cell interpEnv) {
            Object[] frame = makeFrame(list);
            evalFrame(frame, interp, interpEnv);
            return new Cell (frame, env);
        }

        /** 与えられた環境で，クロージャを次々と評価する。
         * ただし末尾の式は評価せずにそのまま返す。(末尾呼出しの最適化のため)
         */
        Object evalBody(IInterp interp, Cell interpEnv) {
            if (body == null) {
                return null;
            } else {
                Cell j = body;
                for (;;) {
                    Object x = j.car;
                    j = j.getCdrCell();
                    if (j == null)
                        return x; // 末尾の式は戻った先で評価する。
                    interp.eval(x, interpEnv);
                }
            }
        }

        /** apply の実装。
         * list の各式を評価せずに新しい環境を作り，evalBody を呼ぶ。
         * 戻り値である末尾の式を同じく新しい環境で評価して結果の値とする。
         */
        @Override public Object apply(Cell list,
                                      IInterp interp, Cell interpEnv) {
            Object[] frame = makeFrame(list);
            Cell newEnv = new Cell (frame, env);
            Object x = evalBody(interp, newEnv);
            return interp.eval(x, newEnv);
        }

        @Override public String toString() {
            return LL.str(new Cell (Symbol.of("#<closure>"),
                                    new Cell (new Cell (carity(), env),
                                              body)));
        }

        static final Factory FACTORY = new Factory () {
                public DefinedFunction make(int arity, boolean hasRest,
                                            Cell body, Cell env) {
                    return new Closure (arity, hasRest, body, env);
                }
            };
    } // Closure
} // Interp
