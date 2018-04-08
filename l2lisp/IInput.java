// H22.7/22, H30.4/8 (鈴)
package l2lisp;

import java.io.*;

/** Lisp 式を読み込むために使われるストリーム.
 * @see java.io.BufferedReader
 */
public interface IInput extends Closeable
{
    /** 入力行を１行読み込む。
     * @param showPrompt Lisp 式の読込み開始のプロンプト
     *   (いわゆる１次プロンプト) を表示すべきならば true。
     *   対話入力でだけ意味がある。
     * @return 行の内容を含む文字列。ただし末尾の改行文字は含めない。
     *   入力ストリームの終わりに達している場合は null を返す。
     * @throws IOException 読み込みに関して発生した例外
     */
    String readLine(boolean showPrompt) throws IOException;
}
