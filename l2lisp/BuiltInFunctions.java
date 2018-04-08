// H22.8/18, H30.4/8 (鈴)
package l2lisp;

import java.io.*;
import java.util.*;
import java.math.BigInteger;
import java.lang.reflect.Field;

/** 組込み Lisp 関数の集合体.
 * 若干のユーティリティも含む。
 */
public class BuiltInFunctions
{
    /** このクラスはインスタンスを作らない。*/
    private BuiltInFunctions () {}

    /** 組込み Lisp 関数からなる配列 */
    public static final Callable[] FUNCTIONS = new Callable[] {
        new Callable ("car", 1) {
            { doc = "(car '(a b c)) => a; (car nil) => nil"; }
            public Object call(Object[] a) {
                return (a[0] == null) ? null : ((Cell) a[0]).car;
            }
        },

        new Callable ("cdr", 1) {
            { doc = "(cdr '(a b c)) => (b c); (cdr nil) => nil"; }
            public Object call(Object[] a) {
                return (a[0] == null) ? null : ((Cell) a[0]).cdr;
            }
        },

        new Callable ("cons", 2, Callable.Option.IS_LAZY) {
            { doc = "(cons 'a '(b c)) => (a b c)"; }
            public Object call(Object[] a) {
                return new Cell (a[0], a[1]);
            }
        },

        new Callable ("atom", 1) {
            { doc = "(atom x) => x が Cell のインスタンスならば偽"; }
            public Object call(Object[] a) {
                return (a[0] instanceof Cell) ? null : LL.S_T;
            }
        },

        new Callable ("eq", 2) {
            { doc = "(eq x y) => x と y が同じ参照か？"; }
            public Object call(Object[] a) {
                return (a[0] == a[1]) ? LL.S_T : null;
            }
        },

        new Callable ("stringp", 1) {
            { doc = "(stringp x) => x が文字列 (String) か？"; }
            public Object call(Object[] a) {
                return (a[0] instanceof String) ? LL.S_T : null;
            }
        },

        new Callable ("integerp", 1) {
            { doc = "(integerp x) => x が整数 (Integer か BigInteger) か？"; }
            public Object call(Object[] a) {
                Object x = a[0];
                return (x instanceof Integer ||
                        x instanceof BigInteger) ? LL.S_T : null;
            }
        },

        new Callable ("floatp", 1) {
            { doc = "(floatp x) => x が浮動小数点数 (Double) か？"; }
            public Object call(Object[] a) {
                return (a[0] instanceof Double) ? LL.S_T : null;
            }
        },

        new Callable ("symbolp", 1) {
            { doc = "(symbolp x) => x がシンボル (Symbol または null) か？"; }
            public Object call(Object[] a) {
                Object x = a[0];
                return (x == null || x instanceof Symbol) ? LL.S_T : null;
            }
        },

        new Callable ("keywordp", 1) {
            { doc = "(keywordp x) => x がキーワードか？"; }
            public Object call(Object[] a) {
                return (a[0] instanceof Symbol.Keyword) ? LL.S_T : null;
            }
        },

        new Callable ("vectorp", 1) {
            { doc = "(vectorp x) => x がベクトル (Object[]) か？"; }
            public Object call(Object[] a) {
                return (a[0] instanceof Object[]) ? LL.S_T : null;
            }
        },

        new Callable ("eql", 2) {
            { doc = "(eql x y) => x と y が同じ整数，実数，または参照か？"; }
            public Object call(Object[] a) {
                return eql(a[0], a[1]) ? LL.S_T : null;
            }
        },

        new Callable ("string=", 2) {
            { doc = "(string= s1 s2) => s1 と s2 が同じ文字列か？"; }
            public Object call(Object[] a) {
                String s1 = asString(a[0]);
                String s2 = asString(a[1]);
                return s1.equals(s2) ? LL.S_T : null;
            }
        },

        new Callable ("list", 1,
                      Callable.Option.HAS_REST,
                      Callable.Option.IS_LAZY) {
            { doc = "(list ...)"; }
             public Object call(Object[] a) {
                 return a[0];
             }
        },

        new Callable ("vector", 1,
                      Callable.Option.HAS_REST,
                      Callable.Option.IS_LAZY) {
            { doc = "(vector ...) => 引数を要素とするベクトル"; }
            public Object call(Object[] a) {
                ArrayList<Object> x = new ArrayList<Object> ();
                if (a[0] != null)
                    for (Object e: (Cell) a[0])
                        x.add(e);
                return x.toArray();
            }
        },

        new Callable ("make-vector", 2, Callable.Option.IS_LAZY) {
            { doc = "(make-vector L E) => 長さ L 各要素 E のベクトル"; }
            public Object call(Object[] a) {
                int length = (Integer) LL.force(a[0]);
                Object element = a[1];
                Object[] x = new Object[length];
                for (int i = 0; i < length; i++)
                    x[i] = element;
                return x;
            }
        },

        new Callable ("prin1", 1) {
            { doc = "(prin1 x): x を印字する (文字列は引用符付き)"; }
            public Object call(Object[] a, IInterp interp, Cell env) {
                PrintWriter pw = interp.getWriter();
                pw.print(LL.str(a[0], true));
                pw.flush();   // (terpri) に任せるべきかもしれないが…
                return a[0];
            }
        },

        new Callable ("princ", 1) {
            { doc = "(princ x): x を印字する (文字列は引用符なし)"; }
            public Object call(Object[] a, IInterp interp, Cell env) {
                PrintWriter pw = interp.getWriter();
                pw.print(LL.str(a[0], false));
                pw.flush();   // (terpri) に任せるべきかもしれないが…
                return a[0];
            }
        },

        new Callable ("terpri", 0) {
            { doc = "(terpri): 印字改行する"; }
            public Object call(Object[] a, IInterp interp, Cell env) {
                PrintWriter pw = interp.getWriter();
                pw.println();
                return true;
            }
        },

        new Callable ("read", 0) {
            { doc = "(read) => 入力から読み取った式"; }
            public Object call(Object[] a, IInterp interp, Cell env) 
                throws IOException
            {
                LispReader lr = interp.getReader();
                return lr.read();
            }
        },

        new Callable ("+", 1, Callable.Option.HAS_REST) {
            { doc = "算術加算 (+ ...); (+ 2 3) => 5; (+) => 0"; }
            public Object call(Object[] a) {
                Number x = (Integer) 0;
                for (Cell j = (Cell) a[0]; j != null; j = j.getCdrCell()) {
                    Number y = (Number) j.car;
                    if (x instanceof Integer && y instanceof Integer) {
                        x = reg(x.longValue() + y.longValue());
                    } else if (x instanceof Double || y instanceof Double) {
                        x = x.doubleValue() + y.doubleValue();
                    } else {
                        if (x instanceof Integer)
                            x = BigInteger.valueOf(x.longValue());
                        else if (y instanceof Integer)
                            y = BigInteger.valueOf(y.longValue());
                        x = reg(((BigInteger) x).add((BigInteger) y));
                    }
                }
                return x;
            }
        },

        new Callable ("*", 1, Callable.Option.HAS_REST) {
            { doc = "算術乗算 (* ...); (* 2 3) => 6; (*) => 1"; }
            public Object call(Object[] a) {
                Number x = (Integer) 1;
                for (Cell j = (Cell) a[0]; j != null; j = j.getCdrCell()) {
                    Number y = (Number) j.car;
                    if (x instanceof Integer && y instanceof Integer) {
                        x = reg(x.longValue() * y.longValue());
                    } else if (x instanceof Double || y instanceof Double) {
                        x = x.doubleValue() * y.doubleValue();
                    } else {
                        if (x instanceof Integer)
                            x = BigInteger.valueOf(x.longValue());
                        else if (y instanceof Integer)
                            y = BigInteger.valueOf(y.longValue());
                        x = reg(((BigInteger) x).multiply((BigInteger) y));
                    }
                }
                return x;
            }
        },

        new Callable ("/", 3, Callable.Option.HAS_REST) {
            { doc = "算術除算 (/ x y ...); (/ 6 5) => 1; (/ 6 5.0) => 1.2"; }
            public Object call(Object[] a) {
                Number x = (Number) a[0];
                Cell yy = new Cell (a[1], a[2]);
                for (Cell j = yy; j != null; j = j.getCdrCell()) {
                    Number y = (Number) j.car;
                    if (x instanceof Integer && y instanceof Integer) {
                        x = reg(x.longValue() / y.longValue());
                                // 注: 8bit演算で -128 / -1  は？
                    } else if (x instanceof Double || y instanceof Double) {
                        x = x.doubleValue() / y.doubleValue();
                    } else {
                        if (x instanceof Integer)
                            x = BigInteger.valueOf(x.longValue());
                        else if (y instanceof Integer)
                            y = BigInteger.valueOf(y.longValue());
                        x = reg(((BigInteger) x).divide((BigInteger) y));
                    }
                }
                return x;
            }
        },

        new Callable ("-", 2, Callable.Option.HAS_REST) {
            { doc = "算術減算 (- x ...); (- 10) => -10; (- 10 2) => 8"; }
            public Object call(Object[] a) {
                Number x = (Number) a[0];
                Cell yy = (Cell) a[1];
                if (yy == null) {
                    if (x instanceof Integer)
                        return reg(- x.longValue());
                    else if (x instanceof Double)
                        return - x.doubleValue();
                    else
                        return reg(((BigInteger) x).negate());
                } else {
                    for (Cell j = yy; j != null; j = j.getCdrCell()) {
                        Number y = (Number) j.car;
                        if (x instanceof Integer && y instanceof Integer) {
                            x = reg(x.longValue() - y.longValue());
                        } else if (x instanceof Double ||
                                   y instanceof Double) {
                            x = x.doubleValue() - y.doubleValue();
                        } else {
                            if (x instanceof Integer)
                                x = BigInteger.valueOf(x.longValue());
                            else if (y instanceof Integer)
                                y = BigInteger.valueOf(y.longValue());
                            x = reg(((BigInteger) x).subtract((BigInteger) y));
                        }
                    }
                    return x;
                }
            }
        },

        new Callable ("%", 2) {
            { doc = "算術剰余 (% a b); (% 5 7) => 5"; }
            public Object call(Object[] a) {
                Number x = (Number) a[0];
                Number y = (Number) a[1];
                if (x instanceof Integer && y instanceof Integer) {
                    x = x.intValue() % y.intValue();
                } else if (x instanceof Double || y instanceof Double) {
                    x = x.doubleValue() % y.doubleValue();
                } else {
                    if (x instanceof Integer)
                        x = BigInteger.valueOf(x.longValue());
                    else if (x instanceof Integer)
                        x = BigInteger.valueOf(y.longValue());
                    x = reg(((BigInteger) x).remainder((BigInteger) y));
                }
                return x;
            }
        },

        new Callable ("=", 2) {
            { doc = "算術比較 (= a b): a と b が数として等しいか？"; }
            public Object call(Object[] a) {
                Number x = (Number) a[0];
                Number y = (Number) a[1];
                if (x instanceof Integer && y instanceof Integer) {
                    return (x.intValue() == y.intValue()) ?
                        LL.S_T : null;
                } else if (x instanceof Double || y instanceof Double) {
                    return (x.doubleValue() == y.doubleValue()) ?
                        LL.S_T : null;
                } else {
                    if (x instanceof Integer)
                        x = BigInteger.valueOf(x.longValue());
                    else if (y instanceof Integer)
                        y = BigInteger.valueOf(y.longValue());
                    return (((BigInteger) x).compareTo((BigInteger) y) == 0) ?
                        LL.S_T : null;
                }
            }
        },

        new Callable ("<", 2) {
            { doc = "算術比較 (< a b): a < b か？"; }
            public Object call(Object[] a) {
                Number x = (Number) a[0];
                Number y = (Number) a[1];
                if (x instanceof Integer && y instanceof Integer) {
                    return (x.intValue() < y.intValue()) ?
                        LL.S_T : null;
                } else if (x instanceof Double || y instanceof Double) {
                    return (x.doubleValue() < y.doubleValue()) ?
                        LL.S_T : null;
                } else {
                    if (x instanceof Integer)
                        x = BigInteger.valueOf(x.longValue());
                    else if (y instanceof Integer)
                        y = BigInteger.valueOf(y.longValue());
                    return (((BigInteger) x).compareTo((BigInteger) y) < 0) ?
                        LL.S_T : null;
                }
            }
        },

        new Callable ("float", 1) {
            { doc = "(float 数) => 数を浮動小数点数にした値"; }
            public Object call(Object[] a) {
                Number x = (Number) a[0];
                return x.doubleValue();
            }
        },

        new Callable ("truncate", 1) {
            { doc = "(truncate 数) => 数をゼロの方向へ丸めた整数"; }
            public Object call(Object[] a) {
                Number x = (Number) a[0];
                if (x instanceof Integer || x instanceof BigInteger) {
                    return x;
                } else {
                    long i = x.longValue();
                    double f = x.doubleValue();
                    double delta = f - i;
                    if (-1.0 < delta && delta < 1.0) // 検算
                        return reg(i);
                    else
                        throw new ArithmeticException ("out of range: " + x);
                }
            }
        },

        new Callable ("load", 1) {
            { doc = "(load file-name): Lisp スクリプトファイルを実行する"; }
            public Object call(Object[] a, IInterp interp, Cell env) 
                throws IOException
            {
                String fileName = (String) a[0];
                InputStream stream = new FileInputStream (fileName);
                IInput script = new LinesFromInputStream (stream);
                return interp.run(script, null);
            }
        },

        new Callable ("eval", 1) {
            { doc = "(eval x) => x を大域的な環境で評価した値"; }
            public Object call(Object[] a, IInterp interp, Cell env) {
                return interp.eval(a[0], null);
            }
        },

        new Callable ("apply", 2) {
            { doc = "(apply fn (...)) => fn を引数のリストに適用した値"; }
            public Object call(Object[] a, IInterp interp, Cell env) {
                Function fn = (Function) a[0];
                Cell arg = (Cell) a[1];
                return fn.apply(arg, interp, env);
            }
        },

        new Callable ("force", 1) {
            { doc = "(force x) => x ただし x が約束ならばそれをかなえる"; }
            public Object call(Object[] a) {
                return a[0];    // 引数渡しの時にもうかなえられている
            }
        },

        new Callable ("rplaca", 2, Callable.Option.IS_LAZY) {
            { doc = "(rplaca x y): x の car を y で置き換える"; }
            public Object call(Object[] a) {
                Object x = LL.force(a[0]);
                ((Cell) x).car = a[1];
                return a[1];    // 結果は約束のままかもしれない
            }
        },

        new Callable ("rplacd", 2, Callable.Option.IS_LAZY) {
            { doc = "(rplacd x y): x の cdr を y で置き換える"; }
            public Object call(Object[] a) {
                Object x = LL.force(a[0]);
                ((Cell) x).cdr = a[1];
                return a[1];    // 結果は約束のままかもしれない
             }
         },

        new Callable ("throw", 2, Callable.Option.IS_LAZY) {
            { doc = "(throw tag value): tag を付けて value を送出する"; }
            public Object call(Object[] a) {
                Object x = LL.force(a[0]);
                throw new LispThrowException (x, a[1]);
            } // value は約束のままかもしれない
        },

        new Callable ("aref", 2) {
            { doc = "(aref array index)"; }
            public Object call(Object[] a) {
                Object x = a[0];
                int i = (Integer) a[1];
                if (x instanceof Object[])
                    return ((Object[]) x)[i];
                else
                    return (int) ((String) x).charAt(i);
            }
        },

        new Callable ("aset", 3, Callable.Option.IS_LAZY) {
            // Emacs Lisp と異なり，要素への代入はベクトルに限る。
            { doc = "(aset vector index value): vector[index] := value"; }
            public Object call(Object[] a) {
                Object x = LL.force(a[0]);
                int i = (Integer) LL.force(a[1]);
                Object value = a[2];
                ((Object[]) x)[i] = value;
                return value;   // value は約束のままかもしれない
            }
        },

        new Callable ("mapcar", 2) {
            { doc = "(mapcar fn (...)) => 各要素に fn を適用したリスト"; }
            public Object call(Object[] a,
                               final IInterp interp, final Cell env) {
                final Function f = (Function) a[0];
                Iterable list = toIterable(a[1]);
                LL.IUnary fn = new LL.IUnary () {
                    public Object apply(Object e) {
                        return f.apply(new Cell (e, null), interp, env);
                    }
                };
                return LL.mapcar(list, fn);
            }
        },

        new Callable ("mapc", 2) {
            { doc = "(mapc fn (...)): 各要素に fn を順に適用する"; }
            public Object call(Object[] a,
                               final IInterp interp, final Cell env) {
                Function f = (Function) a[0];
                Object x = a[1];
                if (x != null)
                    for (Object e: toIterable(x))
                        f.apply(new Cell (e, null), interp, env);
                return x;
            }
        },

        new Callable ("length", 1) {
            { doc = "(length x): 要素数"; }
            public Object call(Object[] a) {
                Object x = a[0];
                if (x == null) {
                    return 0;
                } else if (x instanceof Object[]) {
                    return ((Object[]) x).length;
                } else if (x instanceof String) {
                    return ((String) x).length();
                } else {
                    int i = 0;
                    for (Object e: toIterable(x))
                        i++;
                    return i;
                }
            }
        },

        new Callable ("string-to-char", 1) {
            { doc = "(string-to-char \"ABC\") => 65"; }
            public Object call(Object[] a) {
                String s = (String) a[0];
                if (s.length() == 0)
                    return 0;
                else
                    return (int) s.charAt(0);
            }
        },

        new Callable ("number-to-string", 1) {
            { doc = "(number-to-string -23) => \"-23\""; }
            public Object call(Object[] a) {
                Number x = (Number) a[0];
                return x.toString();
            }
        },

        new Callable ("string-to-number", 1) {
            // Emacs Lisp と異なり，Lisp 式の数の読み取りと一致させる。
            { doc = "(string-to-number \" 444.3622 \") => 444.3622"; }
            public Object call(Object[] a) throws IOException {
                IInput input = new LinesFromString ((String) a[0]);
                LispReader reader = new LispReader (input);
                try {
                    Object x = reader.read();
                    if (x instanceof Number)
                        return x;
                } catch (EvalException ex) {
                    ;           // 不正な式は 0 として扱う
                }
                // LinesFromString は close() をしなくてもよい
                return 0;
            }
        },

        new Callable ("_string+", 2) {
            { doc = "(_string+ str1 str2) => str1 + str2"; }
            public Object call(Object[] a) {
                return (String) a[0] + (String) a[1];
            }
        },

        new Callable ("_sequence-to-string", 1) {
            { doc = "(_sequence-to-string '(1 2 3)) => \"\\x01\\x02\\x03\""; }
            public Object call(Object[] a) {
                Object x = a[0];
                if (x == null) {
                    return "";
                } else if (x instanceof String) {
                    return x;
                } else {
                    StringBuilder sb = new StringBuilder ();
                    for (Object e: toIterable(x)) {
                        Number n = (Number) e;
                        char ch = (char) n.intValue();
                        sb.append(ch);
                    }
                    return sb.toString();
                }
            }
        },

        new Callable ("_vector+", 2) {
            { doc = "(_vector+ [1 2] [3 4]) => [1 2 3 4]"; }
            public Object call(Object[] a) {
                Object[] x = (Object[]) a[0];
                Object[] y = (Object[]) a[1];
                Object[] z = new Object[x.length + y.length];
                for (int i = 0; i < x.length; i++)
                    z[i] = x[i];
                for (int i = 0; i < y.length; i++)
                    z[i + x.length] = y[i];
                return z;
            }
        },
            
        new Callable ("_sequence-to-vector", 1) {
            { doc = "(_sequence-to-vector \"abc\") => [97 98 99]"; }
            public Object call(Object[] a) {
                ArrayList<Object> x = new ArrayList<Object> ();
                if (a[0] != null)
                    for (Object e: toIterable(a[0]))
                        x.add(e);
                return x.toArray();
            }
        },

        new Callable ("_list+", 2) {
            { doc = "(_list+ '(a b) '(c d)) => (a b c d)"; }
            public Object call(Object[] a) {
                Cell list = (Cell) a[0];
                Object tail = a[1];
                Object z = null;
                Cell y = null;
                if (list != null)
                    for (Object e: list) {
                        Cell x = new Cell (e, null);
                        if (z == null)
                            z = x;
                        else
                            y.cdr = x;
                        y = x;
                    }
                if (z == null)
                    z = tail;
                else
                    y.cdr = tail;
                return z;
            }
        },

        new Callable ("_sequence-to-list", 1) {
            { doc = "(_sequence-to-list \"abc\") => (97 98 99)"; }
            public Object call(Object[] a) {
                Object x = a[0];
                if (x == null || x instanceof Cell)
                    return x;
                else
                    return LL.mapcar(toIterable(x), null);
            }
        },

        new Callable ("dump", 0) {
            { doc = "(dump) => (大域的に定義されたシンボルの集合 環境)"; }
            public Object call(Object[] a, IInterp interp, Cell env) {
                Map<Symbol, Object> map = interp.getSymbolTable();
                Set<Symbol> keys = map.keySet();
                Object[] symbols = keys.toArray();
                Arrays.sort(symbols);
                return LL.list(symbols, env);
            }
        },

        new Callable ("java-load", 2) {
            { doc = "(java-load クラス名 静的公開フィールド名): " +
                    "フィールド値を Callable[] として関数を取り込む"; }
            public Object call(Object[] a, IInterp interp, Cell env)
                throws ClassNotFoundException, NoSuchFieldException,
                       IllegalAccessException
            {
                String className = (String) a[0];
                String fieldName = (String) a[1];
                Class klass = Class.forName(className);
                Field field = klass.getField(fieldName);
                Callable[] fs = (Callable[]) field.get(null);
                interp.load(fs);
                return fs;
            }
        },

        new Callable ("help", 1) {
            { doc = "(help callable): callable を説明する"; }
            public Object call(Object[] a, IInterp interp, Cell env) {
                if (a[0] instanceof Callable) {
                    Callable c = (Callable) a[0];
                    if (c.doc != null) {
                        PrintWriter pw = interp.getWriter();
                        pw.println(c.doc);
                    }
                }
                return a[0];
            }
        },
  
        new Callable ("prelude", 0) {
            { doc = "(prelude): 初期化スクリプトの内容を印字する"; }
            public Object call(Object[] a, IInterp interp, Cell env)
                throws IOException
            {
                return printResource(LL.class, LL.PRELUDE, interp);
            }
        },

        new Callable ("copyright", 0) {
            { doc = "(copyright): 著作権を表示する"; }
            public Object call(Object[] a, IInterp interp, Cell env)
                throws IOException
            {
                return printResource(LL.class, LL.COPYRIGHT, interp);
            }
        }
    };


    /** 二つの引数が同じ整数，実数，または参照か？
     * @param a 任意の Lisp 値
     * @param b 任意の Lisp 値
     * @return Lisp の eql 関数としての比較結果
     */
    public static boolean eql(Object a, Object b) {
        if (a == b) {           // 同じ参照または nil か？
            return true;
        } else if (a instanceof Number && b instanceof Number) { // 数か？
            Number x = (Number) a;
            Number y = (Number) b;
            if (x instanceof Double) {
                return (y instanceof Double &&
                        x.doubleValue() == y.doubleValue());
            } else if (y instanceof Double) {
                return false;
            } else {          // x と y は整数 (Integer か BigInteger)
                if (x instanceof Integer)
                    if (y instanceof Integer)
                        return x.intValue() == y.intValue();
                    else
                        x = BigInteger.valueOf(x.longValue());
                else if (y instanceof Integer)
                    y = BigInteger.valueOf(y.longValue());
                return ((BigInteger) x).compareTo((BigInteger) y) == 0;
            }
        } else {
            return false;
        }
    }

    /** null, シンボル，文字列に対する文字列値
     * @param x Lisp 値，ただし nil かシンボルか文字列
     * @return 引数の Lisp 値としての文字列表現
     */
    public static String asString(Object x) {
        if (x == null || x instanceof Symbol || x instanceof String)
            return LL.str(x, false);
        else
            throw new EvalException ("string or symbol expected", x);
    }

    /** map 系ユーティリティ.
     * 引数が null, Iterable ならばそのまま返す。
     * Object[] ならば List にラップして返す。
     * String ならば，対応する文字コードの Integer 値の並びを
     * 返す Iterable を作成して，それを返す。
     * @param x null, Iterable, Object[] または String
     * @return 引数の各要素を順に与える Iterable 値
     */
    public static Iterable toIterable(Object x) {
        if (x == null || x instanceof Iterable) {
            return (Iterable) x;
        } else if (x instanceof Object[]) {
            return Arrays.asList((Object[]) x);
        } else {
            final String s = (String) x;
            return new Iterable () {
                public Iterator iterator() {
                    return new Iterator () {
                        int n = s.length();
                        int i = 0;

                        public boolean hasNext() {
                            return i < n;
                        }

                        public Integer next() {
                            if (i < n) {
                                int ch = (int) s.charAt(i);
                                i++;
                                return ch;
                            } else {
                                throw new NoSuchElementException ();
                            }
                        }

                        public void remove() {
                            throw new UnsupportedOperationException ();
                        }
                    };
                }
            };
        }
    }

    /** 算術ユーティリティ.
     * long 値をできれば int 値に，できなければ BigInteger 値にする。
     * @param x 正規化したい数値
     * @return 引数に等しい Integer または BigInteger のオブジェクト
     */
    public static Number reg(long x) {
        int i = (int) x;
        return (i == x) ? i : BigInteger.valueOf(x);
    }

    /** 算術ユーティリティ.
     * BigInteger 値をできれば int 値に，できなければそのままにする。
     * @param x 正規化したい数値
     * @return 引数に等しい Integer または BigInteger のオブジェクト
     */
    public static Number reg(BigInteger x) {
        return (x.bitLength() < 32) ? x.intValue() : x;
    }

    /** クラスのリソースをテキストとして印字し，その行数を返す。
     * @param klass 対象とするクラス
     * @param name 対象とするリソース名
     * @param interp ここから印字に用いる PrintWriter を得る。
     * @return 印字行数
     * @throws IOException リソースの入出力に関して発生した例外
     */
    public static int printResource(Class klass, String name, IInterp interp)
        throws IOException
    {
        PrintWriter pw = interp.getWriter();
        IInput input = new LinesFromInputStream
            (klass.getResourceAsStream(name));
        try {
            int count = 0;
            for (;;) {
                String line = input.readLine(false);
                if (line == null)
                    break;
                pw.println(line);
                count++;
            }
            return count;
        } finally {
            input.close();
        }
    }
} // BuiltInFunctions
