(defproject clj-testing "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]]
  :profiles
    {:dev
      {:resource-paths ["target"]
       :clean-targets ^{:protect false} ["target"]
       :dependencies [[org.clojure/clojurescript "1.10.773"]
                      [com.bhauman/figwheel-main "0.2.14"]
                      [org.slf4j/slf4j-simple "1.7.5"]
                      [cider/piggieback "0.4.2"]
                      [org.clojure/test.check "0.10.0"]
                      [cljs-http "0.1.46"]
                      [hiccups "0.3.0"]
                      ;; optional but recommended
                      [com.bhauman/rebel-readline-cljs "0.1.4"]]}}
  :aliases {"fig" ["trampoline" "run" "-m" "figwheel.main"]
            "fmt" ["cljfmt" "fix"]}
  :plugins [[lein-cljfmt "0.8.0"] [cider/cider-nrepl "0.26.0"]]
  :nrepl-port 5284
  :cljfmt { :remove-multiple-non-indenting-spaces true})
