// H22.8/17, H30.4/8 (鈴)
package l2lisp;

import java.util.*;

/** Lisp のシンボル
 */
public class Symbol implements Comparable<Symbol>
{
    /** シンボルの印字名 */
    private final String name;

    /** シンボルの一意性を保つための表 */
    private static final Map<String, Symbol> dict
        = new HashMap<String, Symbol> ();

    /** 印字名からシンボルを構築する。
     */
    private Symbol (String name) {
        this.name = name;
    }

    /** 印字名を返す。
     * @return 印字名
     */
    public final String getName() {
        return name;
    }

    /** 同じ印字名に対し，同じシンボルを返す。
     * @param name 印字名
     * @return 印字名に対する Symbol の一意的なオブジェクト
     */
    public static Symbol of(String name) {
        synchronized (dict) {
            Symbol sym = dict.get(name);
            if (sym == null) {
                sym = new Symbol (name);
                dict.put(name, sym);
            }
            return sym;
        }
    }

    /** 印字名をそのまま返す。
     */
    @Override public String toString() {
        return name;
    }

    /** 印字名で比較する。
     */
    public int compareTo(Symbol s) {
        return name.compareTo(s.name);
    }


    /** キーワードを表すシンボル.
     * スペシャルフォームの構文キーワードと，コロンで始まるシンボルで
     * 表現されるキーワードの両方をこれで表す。
     */
    public static class Keyword extends Symbol
    {
        private Keyword (String name) {
            super (name);
        }

        /** 印字名に対するシンボルを Keyword インスタンスとして構築する。
         * それぞれの name に対して最初の呼出しでなければならない。
         * ２回目以降は Symbol.of(name) を使うこと。
         * @param name 印字名
         * @throws IllegalArgumentException 
         *   name に対して２度目の呼出しをした。
         * @return 印字名に対する Keyword の一意的なオブジェクト
         */
        public static Keyword of(String name) {
            synchronized (dict) {
                if (dict.containsKey(name)) {
                    throw new IllegalArgumentException (name);
                } else {
                    Keyword sym = new Keyword (name);
                    dict.put(name, sym);
                    return sym;
                }
            }
        }
    } // Keyword
} // Symbol
