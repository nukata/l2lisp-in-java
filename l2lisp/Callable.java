// H22.7/22, H30.4/8 (鈴)
package l2lisp;

import java.util.*;

/** 組込み Lisp 関数の基底クラス
 */
public class Callable extends Function
{
    /** 特別な引数仕様を表す。*/
    public enum Option { 
        /** rest 引数をとる。*/ HAS_REST,
        /** 引数を自動的には force しない。*/ IS_LAZY 
    };

    private final String name;
    private final boolean isLazy;

    /** Lisp 関数としてのドキュメンテーション文字列 */
    protected String doc = null;

    private static final EnumSet<Option> NONE = EnumSet.noneOf(Option.class);

    /** オプションなしで構築する。
     * @param name 関数名
     * @param arity 引数の個数
     */
    public Callable(String name, int arity) {
        this (name, arity, NONE);
    }

    /** オプション付きで構築する。
     * @param name 関数名
     * @param arity 引数の個数 (rest 引数はまとめて１個と数える)
     * @param first 最初のオプション
     * @param options もしあれば追加のオプション
     */
    public Callable(String name, int arity, Option first, Option... options) {
        this (name, arity, EnumSet.of(first, options));
    }

    /** オプションを列挙集合で与えて構築する。
     * @param name 関数名
     * @param arity 引数の個数 (rest 引数はまとめて１個と数える)
     * @param options オプションの列挙集合
     */
    public Callable(String name, int arity, EnumSet<Option> options) {
        super (arity, options.contains(Option.HAS_REST));
        this.name = name;
        this.isLazy = options.contains(Option.IS_LAZY);
    }

    /** Lisp での関数名を返す。
     * @return 関数名
     */
    public final String getName() {
        return name;
    }

    /** 引数を自動的に force しないかどうかを返す。
     * @return 約束を約束のまま受け取る関数ならば true
     */
    public final boolean isLazy() {
        return isLazy;
    }

    /** Java による関数の本体を呼び出す。
     * 関数が rest 引数をとる (つまり hasRest() が true) ならば，
     * args の最後の要素として rest 引数が Lisp のリストで渡される。
     * つまり rest 引数の個数が 0 ならば null が渡され，
     * 1 以上ならば Cell インスタンスが渡される。
     * <p>
     * このメソッドは任意の例外を投げてよい。
     * ここでの実装は単に UnsupportedOperationException を投げる。
     * @param args 関数の実引数の並び
     * @return 関数の戻り値
     * @throws Exception 関数の呼び出しで発生した例外
     * @see l2lisp.Function#makeFrame(Cell)
     */
    public Object call(Object[] args) throws Exception {
        throw new UnsupportedOperationException ();
    }

    /** Java による関数の本体を呼び出す。
     * interp と env の引数を使って高階関数などを実現できる。
     * apply メソッドはこのメソッドを呼び出す。そして，ここでの実装は単に
     * call(args) を呼び出す。したがって，通常の関数を実現するためには，
     * このメソッドではなく１引数 call をオーバーライドすればよい。
     * @param args 関数の実引数の並び
     * @param interp 関数を呼び出した Lisp インタープリタ
     * @param env 関数を呼び出したときの Lisp 環境
     * @return 関数の戻り値
     * @throws Exception 関数の呼び出しで発生した例外
     */
    public Object call(Object[] args, IInterp interp, Cell env)
        throws Exception
    {
        return call(args);
    }

    /** call メソッドを呼び出す。例外が発生したらキャッチして
     * EvalException でラップする。
     */
    private Object callAndCatch(Object[] args, IInterp interp, Cell env)
        throws EvalException
    {
        try {
            return call(args, interp, env);
        } catch (EvalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new EvalException (ex + " -- " + this + " " + LL.str(args),
                                     ex);
        }
    }

    /** 
     * 実引数の並びから Object の配列を作り，各引数を評価する。
     * isLazy でないならば，さらに自動的に force する。
     * その結果に対して call メソッドを呼出し，例外をラップする。
     */
    final Object evalWith(Cell list, IInterp interp, Cell env)
        throws EvalException
    {
        Object[] frame = makeFrame(list);
        if (isLazy) {
            evalFrame(frame, interp, env);
        } else {
            evalAndForceFrame(frame, interp, env);
        }
        return callAndCatch(frame, interp, env);
    }

    /** apply の実装。
     * isLazy でないならば，各実引数を自動的に force する。
     * 実引数の並びから Object の配列を作る。
     * この配列を引数として３引数の call を呼び出す。
     * @param list Lisp のリスト (null ならば空リストとして扱う)
     * @param interp 実引数を評価するための Lisp インタープリタ
     * @param env    実引数を評価するための環境
     * @return call メソッドの戻り値
     * @throws EvalException 
     *   もしも call メソッドが EvalException 以外の例外を投げたときは，
     *   このメソッドがそれを EvalException でラップして投げ直す。
     */
    @Override public final Object apply(Cell list, IInterp interp, Cell env)
        throws EvalException
    {
        if ((! isLazy) && (list != null))
            list.forceEach();
        Object[] frame = makeFrame(list);
        return callAndCatch(frame, interp, env);
    }

    @Override public String toString() {
        String lOption = (isLazy) ? ":l" : "";
        return String.format("#<%s:%d%s>", name, carity(), lOption);
    }
} // Callable
