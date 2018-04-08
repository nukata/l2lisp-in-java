// H22.7/7, H30.4/8 (鈴)
package l2lisp;

/** Lisp の throw 関数が送出する例外
 */
public class LispThrowException extends EvalException
{
    private final Object tag;
    private final Object value;

    /** Lisp の (throw tag value) に相当する。
     * @param tag 任意の Lisp 値
     * @param value 任意の Lisp 値
     */
    public LispThrowException (Object tag, Object value) {
        super ("no catcher for (" + LL.str(tag) + " " + LL.str(value) + ")");
        this.tag = tag;
        this.value = value;
    }

    /** Lisp の throw 関数の第１引数
     * @return (throw tag value) の tag 値
     */
    public Object getTag() {
        return tag;
    }

    /** Lisp の throw 関数の第２引数
     * @return (throw tag value) の value 値
     */
    public Object getValue() {
        return value;
    }
} // LispThrowException
