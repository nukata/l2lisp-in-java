;;; Prelude.l  H22.8/18 (鈴)  -*- coding: utf-8 -*-

(setq defmacro
      (macro (name args &rest body)
             `(progn (setq ,name (macro ,args ,@body))
                     ',name)))

(defmacro defun (name args &rest body)
  `(progn (setq ,name (lambda ,args ,@body))
          ',name))

(defun caar (x) (car (car x)))
(defun cadr (x) (car (cdr x)))
(defun cdar (x) (cdr (car x)))
(defun cddr (x) (cdr (cdr x)))
(defun caaar (x) (car (car (car x))))
(defun caadr (x) (car (car (cdr x))))
(defun cadar (x) (car (cdr (car x))))
(defun caddr (x) (car (cdr (cdr x))))
(defun cdaar (x) (cdr (car (car x))))
(defun cdadr (x) (cdr (car (cdr x))))
(defun cddar (x) (cdr (cdr (car x))))
(defun cdddr (x) (cdr (cdr (cdr x))))
(defun not (x) (eq x nil))
(defun consp (x) (not (atom x)))
(defun print (x) (prin1 x) (terpri) x)
(defun identity (x) x)

(setq
 null not
 setcar rplaca
 setcdr rplacd)

(defun > (x y) (< y x))
(defun >= (x y) (not (< x y)))
(defun <= (x y) (not (< y x)))
(defun /= (x y) (not (= x y)))
(defun abs (number)
  (cond ((< number 0) (- number))
        (t number)))

(defun equal (x y)
  (cond ((stringp x) (cond ((stringp y)
                            (string= x y))))
        ((vectorp x) (cond ((vectorp y)
                            (cond ((= (length x) (length y))
                                   (_equal-vec x y
                                               (- (length x) 1)))))))
        ((atom x) (eql x y))
        ((atom y) nil)
        ((equal (car x) (car y))
         (equal (cdr x) (cdr y)))))

(defun _equal-vec (x y index)
  (cond ((< index 0) t)
        ((equal (aref x index) (aref y index))
         (_equal-vec x y
                     (- index 1)))))

(defun string (&rest characters)
  (_sequence-to-string characters))

(defun concat (&rest x)
  (cond ((null x) "")
        ((null (cdr x)) (_sequence-to-string (car x)))
        (t (_string+ (_sequence-to-string (car x))
                     (apply concat (cdr x))))))

(defun vconcat (&rest x)
  (cond ((null x) [])
        ((null (cdr x)) (_sequence-to-vector (car x)))
        (t (_vector+ (_sequence-to-vector (car x))
                     (apply vconcat (cdr x))))))

(defun append (&rest x)
  (cond ((null x) nil)
        ((null (cdr x)) (car x))
        (t (_list+ (_sequence-to-list (car x))
                   (apply append (cdr x))))))

(defmacro if (test then &rest else)
  `(cond (,test ,then)
         ,@(cond (else `((t ,@else))))))

(defmacro when (test &rest body)
  `(cond (,test ,@body)))

(defmacro let (args &rest body)
  ((lambda (vars vals)
     (defun vars (x)
       (cond (x (cons (if (atom (car x))
                          (car x)
                        (caar x))
                      (vars (cdr x))))))
     (defun vals (x)
       (cond (x (cons (if (atom (car x))
                          nil
                        (cadar x))
                      (vals (cdr x))))))
     `((lambda ,(vars args) ,@body) ,@(vals args)))
   nil nil))

(defmacro and (x &rest y)
  (if (null y)
      x
    `(cond (,x (and ,@y)))))

(defmacro or (x &rest y)
  (if (null y)
      x
    `(cond (,x)
           ((or ,@y)))))

(defun listp (x)
  (or (null x) (consp x)))    ; NB (listp (lambda (x) (+ x 1))) => nil

(defun numberp (x)
  (or (integerp x) (floatp x)))

(defun arrayp (x)
  (or (vectorp x) (stringp x)))

(defun sequencep (x)
  (or (listp x) (arrayp x)))

(defun memq (key x)
  (cond ((null x) nil)
        ((eq key (car x)) x)
        (t (memq key (cdr x)))))

(defun member (key x)
  (cond ((null x) nil)
        ((equal key (car x)) x)
        (t (member key (cdr x)))))

(defun assq (key alist)
  (cond (alist (let ((e (car alist)))
                 (if (and (consp e) (eq key (car e)))
                     e
                   (assq key (cdr alist)))))))

(defun assoc (key alist)
  (cond (alist (let ((e (car alist)))
                 (if (and (consp e) (equal key (car e)))
                     e
                   (assoc key (cdr alist)))))))

(defun _nreverse (x prev)
  (let ((next (cdr x)))
    (setcdr x prev)
    (if (null next)
        x
      (_nreverse next x))))
(defun nreverse (list)            ; (nreverse '(a b c d)) => (d c b a)
  (cond (list (_nreverse list nil))))

(defun last (list)
  (if (atom (cdr list))
      list
    (last (cdr list))))

(defun nconc (&rest lists)
  (if (null (cdr lists))
      (car lists)
    (setcdr (last (car lists))
            (apply nconc (cdr lists)))
    (car lists)))

(defmacro push (newelt listname)
  `(setq ,listname (cons ,newelt ,listname)))

(defmacro pop (listname)
  `(let (($a (car ,listname)))
     (setq ,listname (cdr ,listname))
     $a))

(defmacro while (test &rest body)
  `(let ($loop)
     (setq $loop (lambda () (cond (,test ,@body ($loop)))))
     ($loop)))

(defun nth (n list)
  (while (< 0 n)
    (setq list (cdr list)
          n (- n 1)))
  (car list))

(defun elt (sequence index)
  (if (listp sequence)
      (nth index sequence)
    (aref sequence index)))

(defmacro dolist (spec &rest body) ; (dolist (name list [result]) body...)
  (let ((name (car spec)))
    `(let (,name
           ($list ,(cadr spec)))
       (while $list
         (setq ,name (car $list))
         ,@body
         (setq $list (cdr $list)))
       ,@(if (cddr spec)
             `((setq ,name nil)
               ,(caddr spec))))))

(defmacro dotimes (spec &rest body) ; (dotimes (name count [result]) body...)
  (let ((name (car spec)))
    `(let ((,name 0)
           ($count ,(cadr spec)))
       (while (< ,name $count)
         ,@body
         (setq ,name (+ ,name 1)))
       ,@(if (cddr spec)
             `(,(caddr spec))))))

(defun reduce (f x)
  (if (null x)
      (f)
    (let ((r (car x)))
      (setq x (cdr x))
      (while x
        (setq r (f r (car x))
              x (cdr x)))
      r)))

(defun max (number &rest numbers)       ; NB (max 1 3 2.5) => 3
  (reduce (lambda (x y)
            (if (< x y) y x))
          (cons number numbers)))

(defun min (number &rest numbers)
  (reduce (lambda (x y)
            (if (< y x) y x))
          (cons number numbers)))

(defun some (f x)
  (cond ((null x) nil)
        ((f (car x)))
        (t (some f (cdr x)))))

(defun take (n x)                       ; Haskell
  (if (or (= 0 n) (null x))
      nil
    (cons (car x) (take (- n 1) (cdr x)))))

(defun drop (n x)                       ; Haskell
  (if (or (= 0 n) (null x))
      x
    (drop (- n 1) (cdr x))))

(defun _zip (x)
  (if (some null x)
      nil
    (let ((cars (mapcar car x))
          (cdrs (mapcar cdr x)))
      (cons cars ~(_zip cdrs)))))
(defun zip (&rest x) (_zip x))          ; Python 3 & Haskell

(defun range (m n)                      ; Python 3
  (cond ((< m n) (cons m ~(range (+ m 1) n)))))

(defun map (f x)                        ; Haskell
  (cond (x (cons ~(f (car x)) ~(map f (cdr x))))))

(defun mapf (f x)                       ; map force
  (cond (x (cons (f (car x)) ~(map f (cdr x))))))

(defun scanl (f q x)                    ; Haskell
  (cons q ~(cond (x (scanl f (f q (car x)) (cdr x))))))

(defun filter (f x)                     ; Haskell & Python 3
  (cond ((null x) nil)
        ((f (car x)) (cons (car x) ~(filter f (cdr x))))
        (t (filter f (cdr x)))))

;;
(setq _error
      (catch *error*
        (when (equal (cadr *version*) "Java")
          (java-load "l2lisp.example.Extension" "KANSŪ"))
        nil))
(when _error
  (print _error))
