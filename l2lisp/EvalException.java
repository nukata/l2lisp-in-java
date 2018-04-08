// H22.7/7, H30.4/8 (鈴)
package l2lisp;

import java.util.*;

/** 評価時例外
 */
public class EvalException extends RuntimeException
{
    private final List<Object> trace = new ArrayList<Object> ();
    
    /**
     * @param message 例外の説明
     * @param exp 例外を引き起こした Lisp 式
     */
    public EvalException (String message, Object exp) {
        super (message + ": " + LL.str(exp));
    }

    /**
     * @param message 例外の説明
     * @param inner 入れ子の例外
     */
    public EvalException (String message, Exception inner) {
        super (message, inner);
    }

    /**
     * @param message 例外の説明
     */
    public EvalException (String message) {
        super (message);
    }

    /** Lisp の評価トレースを返す。
     * @return 評価トレース。ここに追記して例外に各段階のトレースを残す。
     */
    public List<Object> getTrace() {
        return trace;
    }

    /** メッセージに評価トレースを加えた文字列を返す。
     */
    @Override public String toString() {
        StringBuilder sb = new StringBuilder ();
        sb.append("*** " + getMessage());
        int i = 0;
        for (Object t: trace) {
            sb.append(String.format("\n%3d: %s", i, t));
            i++;
        }
        return sb.toString();
    }
} // EvalException
