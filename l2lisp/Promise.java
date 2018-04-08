// H22.7/14 (鈴)
package l2lisp;

/** 約束，つまり Lisp 式 (delay exp) の評価結果
 */
public final class Promise
{
    private Object exp;
    private Cell env;
    private final IInterp interp;

    /** ほかのどこにもない環境。
     * 約束が果たされたかどうかを，env がこの環境かどうかで判断する。
     */
    private static final Cell NONE = new Cell (null, null);

    /** 
     * @param exp いつか評価されると約束される Lisp 式
     * @param env そのとき使うべき環境
     * @param interp 約束を果たすべき Lisp インタープリタ
     */
    public Promise (Object exp, Cell env, IInterp interp) {
        this.exp = exp;
        this.env = env;
        this.interp = interp;
    }

    /** 約束の文字列表現。
     * @return 約束を果たす前ならば，約束であることを表す文字列。
     * 約束を果たした後ならば，かなえた Lisp 値の文字列表現。
     */
    @Override public String toString() {
        return (env == NONE) ?  // 約束を果たした？
            LL.str(exp) :
            String.format("#<promise:%x>", hashCode());
    }

    /** 約束が表す値。
     * @return 約束を果たす前ならば，約束それ自身。
     * 約束を果たした後ならば，かなえた Lisp 値。
     */
    public Object value() {
        return (env == NONE) ?  // 約束を果たした？
            exp :
            this;
    }

    /** もしもまだならば，約束を果たす。
     * @return かなえた Lisp 値
     * @throws EvalException かなえる時に発生した例外
     */
    public Object deliver() throws EvalException {
        if (env != NONE) {
            Object x;
            x = interp.eval(exp, env);
            if (x instanceof Promise)
                x = ((Promise) x).deliver();
            if (env != NONE) {
                exp = x;
                env = NONE;
            }
        }
        return exp;
    }
} // Promise
