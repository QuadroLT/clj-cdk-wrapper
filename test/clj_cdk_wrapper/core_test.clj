(ns clj-cdk-wrapper.core-test
  (:require [clojure.test :refer :all]
            [clj-result.core :as r]
            [clj-cdk-wrapper.core :as cdk]
            ))



(deftest molecule-interchange
  (testing "Context of the test assertions"
    (let [test-molecule {:inchi-string "InChI=1S/C9H11NO2/c10-8(9(11)12)6-7-4-2-1-3-5-7/h1-5,8H,6,10H2,(H,11,12)/t8-/m1/s1"
                         :inchi-key "COLNVLDHVKWLRT-MRVPVSSYSA-N"
                         :smiles "C1=CC=C(C=C1)C[C@H](C(=O)O)N"}
          mol-rez (cdk/smiles->molecule (:smiles test-molecule))
          mol-obj (r/unwrap mol-rez)]
      (do
        (is (r/success? mol-rez))
        (is (cdk/molecule? (r/unwrap mol-rez)))
        (is (= (r/unwrap (cdk/molecule->smiles mol-obj {:flavor :isomeric}))
               (:smiles test-molecule)))
        (is (= (r/unwrap (cdk/molecule->inchi-key mol-obj))
               (:inchi-key test-molecule)))
        (is (= (r/unwrap (cdk/molecule->inchi-string mol-obj))
               (:inchi-string test-molecule)))
        ))))
