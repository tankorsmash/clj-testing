(defn josh-test []
  (prn "this is josh-test doing its thing"))

(defproject clj-testing "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.slf4j/slf4j-simple "1.7.5"]
                 [cljs-http "0.1.46"]
                 [hiccups "0.3.0"]
                 [reagent "1.1.0"]
                 [cljsjs/react "17.0.2-0"]
                 [cljsjs/react-dom "17.0.2-0"]]
  :compiler {:external-config {:devtools/config
                               {
                                :print-config-overrides true
                                :instance-custom-printing-background "rgba(255,255,255)" ;; (d/get-custom-printing-background-markup)
                                :type-header-background                             "rgba(255,255,255)" ;;(d/get-instance-type-header-background-markup)
                                :native-reference-background                        "rgba(255,255,255)" ;;(d/get-native-reference-background-markup)
                                :protocol-background                                "rgba(255,255,255)" ;;(d/get-protocol-background-markup)
                                }
                               }}
  :profiles
    {:dev
      {:resource-paths ["target"]
       :clean-targets ^{:protect false} ["target"]
       :dependencies [[org.clojure/clojurescript "1.10.773"]
                      [com.bhauman/figwheel-main "0.2.14"]
                      [cider/piggieback "0.4.2"]
                      [clj-commons/pomegranate "1.2.1"]
                      [org.clojure/test.check "0.10.0"]
                      ;; optional but recommended
                      [com.bhauman/rebel-readline-cljs "0.1.4"]]}}
       ;; :repl-options  {
       ;;                 :init (do (println "in init but not josh-test yet")
       ;;                           (load-file "setup_repl_josh.clj")
       ;;                           (set_up_vim_outer_repl))}}}
       ;; :compiler { :preloads [devtools.preload]}}}
  :aliases {"fig" ["trampoline" "run" "-m" "figwheel.main"]
            "fmt" ["cljfmt" "fix"]}
  :plugins [[lein-cljfmt "0.8.0"] [cider/cider-nrepl "0.26.0"]]
  :nrepl-port 5284
  :cljfmt { :remove-multiple-non-indenting-spaces true})
