// H22.7/21 (鈴)
package l2lisp.example;

import java.io.PrintWriter;
import java.util.ArrayList;
import l2lisp.*;

/** Java による Lisp 関数の記述例.
 * Lisp の中から
 * (java-load "l2lisp.example.Extension" "KANSŪ")
 * でロードされる。 Prelude.l 参照
 */
public class Extension
{
    public static final Callable[] KANSŪ = new Callable[] {
        new Callable ("exit", 1) {
            { doc = "(exit i): コード i でプロセスを終了する"; }
            public Object call(Object[] a) {
                int code = (Integer) a[0];
                System.exit(code);
                throw new RuntimeException ("ここには到達しない");
            }
        },

        new Callable ("printf", 2, Callable.Option.HAS_REST) {
            { doc = "(printf format ...): 書式付き印字"; }
            public Object call(Object[] a, IInterp interp, Cell env) {
                PrintWriter pw = interp.getWriter();
                String format = (String) a[0];
                ArrayList<Object> list = new ArrayList<Object> ();
                if (a[1] != null)
                    for (Object e: (Cell) a[1])
                        list.add(e);
                Object[] args = list.toArray();
                pw.format(format, args);
                return null;
            }
        }
    };
} // Extension
