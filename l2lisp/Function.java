// H22.7/27, H30.4/8 (鈴)
package l2lisp;

/** Lisp から使うことができる関数の共通基底クラス */
public abstract class Function
{
    private final int arity;
    private final boolean hasRest;

    /** 引数の個数と rest の有無を指定する。
     * @param arity 引数の個数 (rest 引数は１個と数える)
     * @param hasRest rest 引数がある。
     */
    public Function (int arity, boolean hasRest) {
        this.arity = arity;
        this.hasRest = hasRest;
    }

    /** 引数の個数を返す。
     * @return その関数の引数の個数 (rest 引数は１個と数える)
     */
    public final int getArity() {
        return arity;
    }

    /** rest 引数の有無を返す。
     * @return その関数に rest 引数があれば true
     */
    public final boolean hasRest() {
        return hasRest;
    }

    /** 実引数の並びからローカル変数のフレームを作る。
     * @param list Lisp のリストによる実引数の並び
     * @return 新しいローカル変数のフレーム。
     *  フレームの形式は Callable#call(Object[]) の引数と同じ。
     * @throws EvalException
     *  リストの後続要素が約束のとき，固定引数の取り出し時に発生し得る。
     * @see l2lisp.Callable#call(Object[])
     */
    public final Object[] makeFrame(Cell list)
    {
        Object[] frame = new Object[arity];
        int n = (hasRest) ? arity - 1 : arity; // 固定引数の個数
        Object d = list;
        int i;
        for (i = 0; i < n && d instanceof Cell; i++) { // 固定引数並びの設定
            Cell c = (Cell) d;
            frame[i] = c.car;
            d = LL.force(c.cdr);
        }
        if (! (i == n && (d == null || (hasRest && d instanceof Cell))))
            throw new EvalException ("arity not matched", carity());
        if (hasRest)
            frame[n] = d;
        return frame;          // これがローカル変数のフレームになる。
    }

    /** フレームの各式を評価する。
     * @throws EvalException 各式の評価時に発生した例外
     */
    final void evalFrame(Object[] frame, final IInterp interp, final Cell env)
        throws EvalException
    {
        int n = (hasRest) ? arity - 1 : arity;
        for (int i = 0; i < n; i++) {
            frame[i] = interp.eval(frame[i], env);
        }
        if (hasRest && frame[n] != null) {
            LL.IUnary fn = new LL.IUnary () {
                public Object apply(Object x) {
                    return interp.eval(x, env);
                }                
            };
            frame[n] = ((Cell) frame[n]).mapcar(fn);
        }
    }

    /** フレームの各式を評価し，さらに自動的に force する。
     * @throws EvalException 各式の評価時に発生した例外
     */
    final void evalAndForceFrame(Object[] frame, IInterp interp, Cell env)
        throws EvalException
    {
        int n = (hasRest) ? arity - 1 : arity;
        for (int i = 0; i < n; i++) {
            frame[i] = LL.force(interp.eval(frame[i], env));
        }
        if (hasRest) {
            // XXX 頻繁に呼ばれる箇所だから，Cell#mapcar を使わず，
            // ここでループを展開して処理を高速化した。Java が高階関数を
            // 効率よく実行できるようになったら見直すこと。
            Cell z = null; 
            Cell y = null;
            for (Cell j = (Cell) frame[n]; j != null; j = j.getCdrCell()) {
                Object e = LL.force(interp.eval(j.car, env));
                Cell x = new Cell (e, null);
                if (z == null)
                    z = x;
                else
                    y.cdr = x;
                y = x;
            }
            frame[n] = z;
        }
    }

    /** 評価済みの Lisp の実引数の並びに関数を適用する。
     * ここでの実装は単に UnsupportedOperationException を投げる。
     * @param list Lisp のリスト (null ならば空リストとして扱う)
     * @param interp Lisp インタープリタ
     * @param env 関数を呼び出したときの環境
     * @return call メソッドの戻り値
     * @throws EvalException なんらかの評価時に発生した例外
     */
    public Object apply(Cell list, IInterp interp, Cell env)
        throws EvalException 
    {
        throw new UnsupportedOperationException();
    }

    /** 引数の個数を (rest 引数があるときは符号を反転して) 返す。
     */
    final int carity() {   // combined arity: 名前に深い意味はない
        return (hasRest) ? -arity : arity;
    }
} // Function
