(ns clj-testing.dev)

;; IDK why this needs to be done at the toplevel and throws a syntax error if its in a function
;; (defn setup-vim-cljs-repl []
;; (do (require 'figwheel.main.api)
;;     (figwheel.main.api/start {:mode :serve} "dev")
;;     (figwheel.main.api/cljs-repl "dev"))

;; it actually needs to be done in a macro so that vim's Piggieback can access it. IDK why either
(defmacro set-up-figwheel []
  `(do (require 'figwheel.main.api)
      (figwheel.main.api/start {:mode :serve} "dev")
      (figwheel.main.api/cljs-repl "dev")))

(set-up-figwheel)
