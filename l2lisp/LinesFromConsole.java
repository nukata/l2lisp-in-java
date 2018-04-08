// H22.8/11 (鈴)
package l2lisp;

import java.io.*;

/** コンソールでの対話的入力のための IInput 実装クラス.
 * Java 1.5 に対応するため，java.io.Console は使わず，
 * System.in と System.out (プロンプト表示) で代用する。
 */
public class LinesFromConsole implements IInput
{
    private final BufferedReader br;
    private final String ps1;
    private final String ps2;
    private final String farewell;
    private boolean isOpen = true;

    /** System.in から構築する。
     * @param ps1 １次プロンプト
     * @param ps2 ２次プロンプト
     * @param farewell わかれのあいさつ または null
     */
    public LinesFromConsole (String ps1, String ps2, String farewell) {
        br = new BufferedReader (new InputStreamReader (System.in));
        this.ps1 = ps1;
        this.ps2 = ps2;
        this.farewell = farewell;
    }

    /** System.in から１行読む。 ただし showPrompt が true ならば，
     * その前に System.out に１次プロンプトを表示する。
     * そうでなければ２次プロンプトを表示する。
     */
    public String readLine(boolean showPrompt) throws IOException {
        System.out.print((showPrompt) ? ps1 : ps2);
        System.out.flush();
        return br.readLine();
    }

    /** もしあれば，System.out にわかれのあいさつを表示して改行する。
     * ただし，２回目以降呼び出されたときは何もしない。
     */
    public void close() throws IOException {
        if (isOpen) {
            isOpen = false;
            if (farewell != null)
                System.out.println(farewell);
            System.out.flush();
        }
    }
} // LinesFromConsole
