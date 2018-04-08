all:
	rm -f l2lisp/*.class l2lisp/*/*.class
	javac -encoding utf-8 l2lisp/example/Extension.java
	jar cfm l2lisp.jar l2lisp/Manifest l2lisp/Copyright.txt l2lisp/Prelude.l l2lisp/*.class l2lisp/*/*.class

clean:
	rm -f l2lisp/*.class l2lisp/*/*.class
	rm -rf doc

doc:
	javadoc -encoding utf-8 -d doc l2lisp l2lisp.example
