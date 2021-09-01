(defproject clj-testing "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]]
  :profiles
    {:dev
      {:resource-paths ["target"]
       :clean-targets ^{:protect false} ["target"]
       :dependencies [[org.clojure/clojurescript "1.10.773"]
                      [com.bhauman/figwheel-main "0.2.14"]
                      [org.slf4j/slf4j-simple "1.7.5"]
                      ;; [hiccup "1.0.5"] ;;might only be in clj, not cljs
                      [hiccups "0.3.0"]
                      ;; optional but recommended
                      [com.bhauman/rebel-readline-cljs "0.1.4"]]}}
  :aliases {"fig" ["trampoline" "run" "-m" "figwheel.main"]
            "fmt" ["cljfmt" "fix"]}
  :plugins [[lein-cljfmt "0.8.0"]])
