(ns clj-cdk-wrapper.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clj-result.core :as r]
            [clj-cdk-wrapper.core :refer :all]
            ))

(defn approx=
  ([a b] (approx= a b 0.00001))
  ([a b epsilon]
   (<= (Math/abs (- a b)) epsilon)
   ))

(deftest molecule-interchange
  (let [test-molecule1
        {:inchi-string "InChI=1S/C9H11NO2/c10-8(9(11)12)6-7-4-2-1-3-5-7/h1-5,8H,6,10H2,(H,11,12)/t8-/m1/s1"
         :inchi-key "COLNVLDHVKWLRT-MRVPVSSYSA-N"
         :smiles "C1=CC=C(C=C1)C[C@H](C(=O)O)N"}]
    (testing "testing molecule structure representation conversion"
      (let [mol-rez (smiles->molecule (:smiles test-molecule1))
            mol-obj (r/unwrap mol-rez)]
        (do
          (is (r/success? mol-rez))
          (is (molecule? (r/unwrap mol-rez)))
          (is (= (r/unwrap (molecule->smiles mol-obj {:flavor :isomeric}))
                 (:smiles test-molecule1)))
          (is (= (r/unwrap (molecule->inchi-key mol-obj))
                 (:inchi-key test-molecule1)))
          (is (= (r/unwrap (molecule->inchi-string mol-obj))
                 (:inchi-string test-molecule1))))))
    (testing "reading SDF files"
      (let [source (io/resource "testfile1.sdf")
            source1 (io/resource "testfile.sdf")
            molecules-res (sdf->molecules source)
            molecules-list (r/unwrap molecules-res)
            molecules-list-long (r/unwrap (sdf->molecules source1))
            mol (first molecules-list)
            ]
        (do
          (is (r/success? molecules-res))
          (is (molecule? (first molecules-list)))
          (is (= (r/unwrap (molecule->inchi-key mol)) "BSYNRYMUTXBXSQ-UHFFFAOYSA-N"))
          (is (not (empty? (:metadata mol))))
          (is (contains? (:metadata mol) :PUBCHEM_SMILES))
          (is (= (count molecules-list-long) 2))
          )))
    (testing "molecule properties calculations"
      (let [test-molecule2 "CCCC"
            mol (r/unwrap (smiles->molecule test-molecule2))
            mass-average1 58.12
            mass-monoizotopic1 58.078250319
            test-molecule3 "C(Cl)Br"
            mol2 (r/unwrap (smiles->molecule test-molecule3))
            mass-average2 129.38
            mass-monoizotopic2 127.90284]
        (do
          (is (= (molecular-formula mol) "C4H10"))
          (is (approx= (calculate-mass mol {:flavor :average}) mass-average1 0.01))
          (is (approx= (calculate-mass mol {:flavor :monoisotopic}) mass-monoizotopic1))
          (is (approx= (calculate-mass mol2 {:flavor :average}) mass-average2 0.01))
          (is (approx= (calculate-mass mol2 {:flavor :monoisotopic}) mass-monoizotopic2)))))))
