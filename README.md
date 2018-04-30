# An experimental Lisp interpreter in Java

This is an experimental Lisp interpreter I wrote 8 years ago (2010) in Java.
It had been presented under the MIT License at 
<http://www.oki-osk.jp/esc/llsp/v9.html> until last spring (2017).
Now I have slightly modified it to resolve warnings issued by Java 8 and 10.

- It has almost the same features as
  [l2lisp-in-python](https://github.com/nukata/l2lisp-in-python) has.

- It can extend built-in functions at runtime.
  See [l2lisp/example/Extension.java](l2lisp/example/Extension.java)
  and [l2lisp/Prelude.l](l2lisp/Prelude.l#L279).


## How to use

It runs in Java 6 and later.

```
$ make
rm -f l2lisp/*.class l2lisp/*/*.class
javac -encoding utf-8 l2lisp/example/Extension.java
jar cfm l2lisp.jar l2lisp/Manifest l2lisp/Copyright.txt l2lisp/Prelude.l l2lisp/
*.class l2lisp/*/*.class
$ java -jar l2lisp.jar
> "hello, world"
=> "hello, world"
> (+ 5 6)
=> 11
> (help car)
(car '(a b c)) => a; (car nil) => nil
=> #<car:1>
> (exit 0)
$
```

You can give it a file name of your Lisp script.

```
$ cat -n fibs.l
     1  ;; from Haskell
     2  (defun zipWith (f x y)
     3    (if (or (null x)
     4            (null y)) nil
     5      (cons (f (car x) (car y))
     6            ~(zipWith f (cdr x) (cdr y)))))
     7  
     8  (setq fibs
     9        (cons 1 (cons 1 ~(zipWith + fibs (cdr fibs)))))
    10  
    11  (print (nth 33 fibs))                   ; => 5702887
$ java -jar l2lisp.jar fibs.l
5702887
$
```

**Note:** `~expression` is equivalent to `(delay expression)`.
This Lisp implements _"implicit forcing"_.
See [R5RS](http://www.schemers.org/Documents/Standards/R5RS/)
for `delay` and "implicit forcing".
They allow you lazy evaluation à la Haskell in Lisp.

If you put a "`-`" after the file name, it will 
begin an interactive session after running the file.

```
$ java -jar l2lisp.jar fibs.l -
5702887
> (take 20 fibs)
=> (1 1 2 3 5 8 13 21 34 55 89 144 233 377 610 987 1597 2584 4181 6765)
> (exit 0)
$ 
```

## License

It is under the MIT License.
See [l2lisp/Copyright.txt](l2lisp/Copyright.txt) or 
evaluate `(copyright)` in the Lisp.
