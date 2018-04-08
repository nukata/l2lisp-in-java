// H22.7/22, H30.4/8 (鈴)
package l2lisp;

/** 文字列による IInput 実装クラス
 */
public class LinesFromString implements IInput
{
    private final String[] lines;
    private int index = 0;

    /** 引数を改行ごとに分割したコピーを内部に格納する。
     * @param s Lisp 式の読み取り先となる文字列 (複数行でもよい)
     */
    public LinesFromString (String s) {
        lines = s.split("\r?\n");
    }

    /** 行ごとに分割したコピーを一つずつ与える。このとき，
     * 資源回収に協力するため，そのコピーへの内部からの参照を消す。
     */
    public String readLine(boolean showPrompt) {
        if (index < lines.length) {
            String s = lines[index];
            lines[index] = null;
            index++;
            return s;
        } else {
            return null;
        }
    }

    /** 資源回収に協力するため，コピーへの内部からの参照を消す。 
     */
    public void close() {
        while (index < lines.length) {
            lines[index] = null;
            index++;
        }
    }
} // LinesFromString
