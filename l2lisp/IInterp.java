// H22.7/22, H30.4/8 (鈴)
package l2lisp;

import java.util.Map;
import java.io.IOException;
import java.io.PrintWriter;

/** Lisp インタープリタ本体のインタフェース
 */
public interface IInterp
{
    /** シンボルから大域変数値への表を得る。
     * @return Lisp の大域変数の表 (変更した場合の効果は定めない)
     */
    Map<Symbol, Object> getSymbolTable();

    /** Lisp から (read) するときに使う読み取り器を得る。
     * @return Lisp の read 関数などの入力もと
     */
    LispReader getReader();

    /** Lisp から印字するときに使う書き込み器を得る。
     * @return Lisp の print 関数などの出力さき
     */
     PrintWriter getWriter();

    /** Java で書かれた関数をロードする。
     * @param functions Java で書かれた関数の集合体。それぞれの要素 f を
     *        f.getName() という名前の大域変数として表に登録する。
     * @see #getSymbolTable()
     */
    void load(Callable[] functions);

    /** Lisp 式を評価する。
     * @param exp 評価される Lisp 式
     * @param env 評価するときに使う環境
     * @return 評価結果
     * @throws EvalException 評価中に例外が発生した。
     */
    Object eval(Object exp, Cell env) throws EvalException;

    /** スクリプトを評価する。
     * @param script ここに書かれている式を次々と評価する。
     *   正常，異常にかかわらずメソッドが終わるとき close する。
     * @param receiver 
     *   もしも null でなければ，スクリプトのトップレベルの式の評価を終える
     *   たびに評価結果を引数として receiver.receiveResult を呼び出す。
     *   EvalException の発生時にはその例外を引数として
     *   receiver.receiveException を呼び出す。
     *   しかるにもしも recdeiver が null ならば，発生した EvalException
     *   によって run メソッドが終了する。
     * @return トップレベルの最後の式の評価結果
     * @throws IOException スクリプトからの読み込み時に例外が発生した。
     * @throws EvalException 評価時例外が発生したが，receiver が null だった
     *   ため受け止められることなく外に伝播した。あるいは receiver がわざと
     *   再送出した。
     */
    Object run(IInput script, IReceiver receiver)
        throws IOException, EvalException;


    /** スクリプト評価時の結果や例外の受信器
     * @see #run
     */
    interface IReceiver
    {
        /** 結果を受信する。
         * スクリプト評価時，トップレベルの各式の評価結果を受け止める。
         * @param result Lisp 式の評価結果である Lisp 値
         */
        void receiveResult(Object result);

        /** 例外を受信する。
         * スクリプト評価時，トップレベルの各式の評価で発生した例外を
         * 受け止める。
         * @param ex Lisp 式の評価で発生した例外
         */
        void receiveException(EvalException ex);
    } // IReceiver
} // IInterp
