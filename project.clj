(defproject clj-cdk-wrapper "0.1.0-SNAPSHOT"
  :description "lispify chemistry development kit (CDK) for convenience"
  :url ""
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :plugins [[lein-git-deps "0.0.2"]]
  :dependencies [[org.clojure/clojure "1.12.2"]
                 [org.openscience.cdk/cdk-bundle "2.11"]
                 ;; [clj-result "0.1.1-SNAPSHOT"]
                 ]
  :git-dependencies [["https://github.com/QuadroLT/clj-result.git" "main"]]
  :source-paths ["src" ".lein-git-deps/clj-result/src"]
  :repl-options {:init-ns clj-cdk-wrapper.core})
