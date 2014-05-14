(defproject aji "0.1.0-SNAPSHOT"
  :description "Aji is a controller and services interface library for Om/React."
  :url "https://github.com/tony-landis/aji"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2197"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [om "0.6.2"]
                 [ankha "0.1.2"]
                 [sablono "0.2.16"]
                 [com.cemerick/clojurescript.test "0.3.0"] 
                 [midje "1.6.0"]
                 [org.clojure/test.check "0.5.8"]
                 [com.cemerick/piggieback "0.1.3"]
                 ]
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :plugins [[com.keminglabs/cljx "0.3.2"]]
  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :clj}
                  {:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :cljs}]}
  :profiles {:dev {:plugins [[lein-cljsbuild "1.0.3"]]
                   :cljsbuild {:builds [{:id "bootstrap"
                                         :source-paths ["src" "target/classes"]
                                         :compiler {:output-to "out/bootstrap.js"
                                                    :output-dir "out"
                                                    :optimizations :none ;:simple
                                                    :pretty-print true
                                                    :source-map true
                                                    }}]}}})
