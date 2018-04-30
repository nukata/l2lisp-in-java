Little Lazy Lisp 9.4 in Java                             H30.4/30 (鈴)

これは Java による小さな Lisp インタープリタです。

$ java -jar l2lisp.jar

として起動すると Lisp の対話セッションに入ります。

$ java -jar l2lisp.jar
> (+ 2 3)
=> 5
> (help car)
(car '(a b c)) => a; (car nil) => nil
=> #<car:1>
> (exit 0)
$ 

(dump) を実行すると現在の大域シンボルと環境が得られます。
(copyright) を実行すると著作権表示を読むことができます。

引数を与えて起動すると Lisp スクリプトのファイル名として実行します。

$ java -jar l2lisp.jar fibs.l
5702887
$ 

引数は複数与えることができます。引数として - を与えるとスクリプトを
実行するかわりに対話セッションに入ります。

$ java -jar l2lisp.jar fibs.l -
5702887
> (take 20 fibs)
=> (1 1 2 3 5 8 13 21 34 55 89 144 233 377 610 987 1597 2584 4181 6765)
> (exit 0)
$

ソースは l2lisp にあります。コンパイルするには

$ make

をするか，あるいは (make コマンドが利用できない場合は)

$ javac -encoding utf-8 l2lisp/example/Extension.java
$ jar cfm l2lisp.jar l2lisp/Manifest l2lisp/Copyright.txt l2lisp/Prelude.l l2lisp/*.class l2lisp/*/*.class

をしてください。Java 本体以外に必要なライブラリ等はありません。Mac 上の
Java 6u65, 8u152, 10.0.1 で試していますが，他の Java も使えるはずです。

上記 jar コマンド行から分かるように，Lisp 初期化スクリプトを含め必要な
ファイル一式を l2lisp.jar に含めています。実行に必要なのは l2lisp.jar
だけです。

＃ この Lisp は H29 春まで下記に置かせてもらっていました。

http://www.oki-osk.jp/esc/llsp/v9.html
