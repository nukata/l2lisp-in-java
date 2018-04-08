;; from Haskell
(defun zipWith (f x y)
  (if (or (null x)
          (null y)) nil
    (cons (f (car x) (car y))
          ~(zipWith f (cdr x) (cdr y)))))

(setq fibs
      (cons 1 (cons 1 ~(zipWith + fibs (cdr fibs)))))

(print (nth 33 fibs))                   ; => 5702887
