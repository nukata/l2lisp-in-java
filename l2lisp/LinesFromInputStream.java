// H22.7/26, H30.4/8 (鈴)
package l2lisp;

import java.io.*;

/** java.io.InputStream から構築する IInput 実装クラス.
 * 文字エンコーディングには UTF-8 を使う。
 */
public class LinesFromInputStream implements IInput
{
    private final BufferedReader br;

    /** 引数から BufferedReader インスタンスを作成して構築する。 
     * @param stream Lisp 式の読み取り先となる入力ストリーム
     */
    public LinesFromInputStream (InputStream stream) {
        try {
            Reader reader = new InputStreamReader (stream, "UTF-8");
            br = new BufferedReader (reader);
        } catch (UnsupportedEncodingException ex) {
            // UTF-8 がサポートされていないことはあり得ない
            throw new RuntimeException ("Impossible!", ex);
        }
    }

    /** BufferedReader インスタンスの readLine() を呼び出す。 
     */
    public String readLine(boolean showPrompt) throws IOException {
        return br.readLine();
    }

    /** BufferedReader インスタンスの close() を呼び出す。
     */
    public void close() throws IOException {
        br.close();
    }
} // LinesFromInputStream
