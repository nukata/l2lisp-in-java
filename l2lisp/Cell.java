// H22.7/26, H30.4/8 (鈴)
package l2lisp;

import java.util.*;

/** cons セル */
public final class Cell implements Iterable
{
    Object car;
    Object cdr;

    /** Lisp の (cos car cdr) に相当
     * @param car car 値となる任意の Lisp 値
     * @param cdr cdr 値となる任意の Lisp 値
     */
    public Cell(Object car, Object cdr) {
        this.car = car;
        this.cdr = cdr;
    }

    /** 第１要素の getter
     * @return 素の car 値 (約束は約束のまま)
     */
    public Object getCar() {
        return car;
    }
    /** 第１要素の setter
     * @param value 新しく car 値となる任意の Lisp 値
     */
    public void setCar(Object value) {
        car = value;
    }

    /** 第２要素の getter
     * @return 素の cdr 値 (約束は約束のまま)
     */
    public Object getCdr() {
        return cdr;
    }
    /** 第２要素の setter
     * @param value 新しく cdr 値となる任意の Lisp 値
     */
    public void setCdr(Object value) {
        cdr = value;
    }

    /** 第２要素の getter。ただし Cell または null として。
     * このとき必要ならば第２要素を force する。
     * @return force した cdr 値 (ただし cons セルまたは nil)
     * @throws ProperListExpectedException Cell または null ではなかった。
     */
    public Cell getCdrCell() throws ProperListExpectedException {
        if (cdr instanceof Cell) {
            return (Cell) cdr;
        } else if (cdr == null) {
            return null;
        } else if (cdr instanceof Promise) {
            cdr = ((Promise) cdr).deliver();
            return getCdrCell();
        } else {
            throw new ProperListExpectedException (this);
        }
    }

    /** Lisp のリストとして各要素を与えるイテレータを作る。
     * このとき必要ならば各セルの第２要素を force する。
     * proper list でなければ最後に ProperListExpectedException 例外を
     * 発生させる。
     */
    public Iterator iterator() {
        return new Iterator () {
            private Object j = Cell.this;

            public boolean hasNext() {
                if (j instanceof Cell) {
                    return true;
                } else if (j == null) {
                    return false;
                } else if (j instanceof Promise) {
                    j = ((Promise) j).deliver();
                    return hasNext();
                } else {
                    throw new ProperListExpectedException (Cell.this);
                }
            }

            /** リストの次の要素を返す。内部のポインタを次のセルへと進める。
             */
            public Object next() {
                if (j == null) {
                    throw new NoSuchElementException ();
                } else {
                    Cell c = (Cell) j;
                    j = c.cdr;
                    return c.car;
                }
            }

            public void remove() {
                throw new UnsupportedOperationException ();
            }
        };
    }

    /** Lisp のリストとして各要素を force する。
     * improper list であってもよい。
     */
    public void forceEach() {
        Cell j = this;
        for (;;) {
            if (j.car instanceof Promise)
                j.car = ((Promise) j.car).deliver();
            if (j.cdr instanceof Promise)
                j.cdr = ((Promise) j.cdr).deliver();
            if (j.cdr instanceof Cell)
                j = (Cell) j.cdr;
            else                // null または非 Cell に到達したら終了
                break;
        }
    }

    /** Lisp のリストとしての各要素に関数を適用した新しいリストを作る。
     * 汎用の LL.mapcar よりも効率がよい。
     * @param fn 各要素に適用する関数，ただし null ならば恒等関数とみなす。
     * @return 各要素に関数を適用した結果からなるリスト
     * @see LL#mapcar
     */
    public Cell mapcar(LL.IUnary fn) {
        Cell z = null;
        Cell y = null;
        for (Cell j = this; j != null; j = j.getCdrCell()) {
            Object e = j.car;
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

    /** Lisp のリストとしての文字列表現を返す。
     */
    @Override public String toString() {
        return LL.str(this);
    }

    /** LL.str の補助関数
     */
    String repr(boolean printQuote, int recLevel, Set<Object> printed) 
    {
        if (! printed.add(this)) { // 重複していたならば…
            recLevel--;
            if (recLevel == 0)
                return "...";
        }
        Object kdr = cdr;
        if (kdr instanceof Promise) // ここでは，かなえない
            kdr = ((Promise) kdr).value();
        if (kdr == null) {
            return LL.str(car, printQuote, recLevel, printed);
        } else {
            String s = LL.str(car, printQuote, recLevel, printed);
            if (kdr instanceof Cell) {
                String t = ((Cell) kdr).repr(printQuote, recLevel, printed);
                return s + " " + t;
            } else {
                String t = LL.str(kdr, printQuote, recLevel, printed);
                return s + " . " + t;
            }
        }
    }


    /** proper list ではなかったことを知らせる例外
     */
    public static class ProperListExpectedException extends EvalException
    {
        public ProperListExpectedException (Object exp) {
            super ("proper list expected", exp);
        }
    } // ProperListExpectedException
} // Cell
