(ns clj-cdk-wrapper.core
  (:import
   [org.openscience.cdk.smiles SmilesParser SmilesGenerator]
   [org.openscience.cdk.inchi InChIGeneratorFactory]
   [org.openscience.cdk.silent SilentChemObjectBuilder]
   [org.openscience.cdk.io MDLV2000Reader MDLV2000Writer SDFWriter]
   [org.openscience.cdk.io.iterator IteratingSDFReader]
   [org.openscience.cdk.tools.manipulator
    AtomContainerManipulator
    MolecularFormulaManipulator]
   ;; [org.openscience.cdk.interfaces IMolecularFormula]
   [java.io StringReader StringWriter FileReader File])
  (:require [clj-result.core :as r]))

(defprotocol IMolecule
  "Protocol for manipulation molecule objects"
  (add-metadata [this new-data]
    "Add metadata 'name, id, etc.' to Molecule object
     mew data shall be provided as map object
     returns new molecule object")

  (remove-metadata [this k]
    "Removes a key (k) and associated value from Molecule object.
     Returns a new Molecule object")

  ;; property calculators
  (calculate-mass [this flavor]
    "returns average or monoizotopic mass of an molecule
     -flavor: keyword :average or :monoizotopic
     -- monoizotopic takes most abundant izotope into calculations")

  (molecular-formula [this]
    "returns string of molecular formula"
    ))

(defrecord Molecule [structure metadata]
  IMolecule
  ;; (-molecule? [_] true)
  (add-metadata [this new-data]
    (assoc this :metadata (merge metadata new-data)))

  (remove-metadata [this k]
    (assoc this :metadata (dissoc metadata k)))

  (calculate-mass
    [this {:keys [flavor]}]
     (let [mass-type (case flavor
                       :average 1
                       :monoisotopic 4)]
       (AtomContainerManipulator/getMass (:structure this) mass-type)))


  (molecular-formula [this]
    (let [formula (MolecularFormulaManipulator/getMolecularFormula (:structure this))]
      (MolecularFormulaManipulator/getString formula)))
  )

(defn molecule?
  [obj]
  (instance? Molecule obj))

;; molecule object constructors

(defn make-molecule
  ([structure] (->Molecule structure {}))
  ([structure metadata] (->Molecule structure metadata)))


(defn smiles->molecule
  "converts smiles string into Molecule record
  returns success of Molecule record / failure of error
  Arguments:
  - smiles: string
  "
  [smiles]
  (try
    (let [
        builder (SilentChemObjectBuilder/getInstance)
        parser (SmilesParser. builder)]
      (r/success
       (make-molecule
        (.parseSmiles parser smiles))))
    (catch Exception e
      (r/failure (.getMessage e) :cdk-error {:details e}))))

(defn inchi-string->molecule
  "converts InChI string to Molecule record
  returns:
  success of Molecule record / failure of error

  Arguments:
  - inchi-str: string
  "
  [inchi-str]

  (try
    (let [factory (InChIGeneratorFactory/getInstance)
          builder (SilentChemObjectBuilder/getInstance)
          loader (.getInChIToStructure factory inchi-str builder)
          status (.getReturnStatus loader)]

      (if (= (.toString  status) "OKAY")
        (r/success
         (make-molecule
          (.getAtomContainer loader)))
        (r/failure "Bad InChI format" :inchi-error {})))
    (catch Exception e
      (r/failure (.getMessage e) :java-error {:exception e}))))


(defn molv2000->molecule
  "converts MOL V2000 string into Molecule record
  returns:
  success of Molecule record / failure of error

  Arguments:
  - mol-string: string
  "
  [mol-string]
  (try
    (with-open [mol-reader (MDLV2000Reader. (StringReader. mol-string))]
      (let [builder  (SilentChemObjectBuilder/getInstance)
            molecule (.read mol-reader (.newAtomContainer builder))]
        (r/success
         (make-molecule molecule))))
    (catch Exception e
      (r/failure (.getMessage e) :molfile-error {:exception e}))))


(defn molecule->inchi-key
    "calculates calculates an InChI-key for a nolecule object 'IAtomContainer'

  Arguments:
  - molecule: Molecule record

  "
    [molecule]
    (try
      (let [factory (InChIGeneratorFactory/getInstance)
            generator (.getInChIGenerator factory (:structure molecule))
            status (.getReturnStatus generator)]
        (if (= (.toString status) "OKAY")
          (r/success (.getInchiKey generator))
          (r/failure "InChI generator error" :inchi-error {})))
      (catch Exception e
        (r/failure (.getMessage e) :cdk-error {:details e}))))

(defn molecule->smiles
    "Converts a molecule to SMILES.
  Arguments:
  - molecule: Molecule record
   Options:
   - :flavor (keyword) e.g., :generic, :isomeric, :absolute, :canonical"
    ([molecule]
     (molecule->smiles molecule {:flavor :generic})) ;; Default arity

    ([molecule {:keys [flavor]}]
     (try
       (let [generator (case flavor
                         :generic   (SmilesGenerator/generic)
                         :isomeric  (SmilesGenerator/isomeric)
                         :absolute  (SmilesGenerator/absolute)
                         :canonical (SmilesGenerator/unique)
                         (SmilesGenerator/generic))
             smiles (.createSMILES generator (:structure molecule))]
         (r/success smiles))
       (catch Exception e
         (r/failure "SMILES generation failed" :conversion-error {:exception e})))))



(defn molecule->inchi-string
  "calculates InChI-string for given molecule object  'IAtomContainer'

  Arguments:
  - molecule: Molecule record
  "
  [molecule]
  (let [factory (InChIGeneratorFactory/getInstance)
        generator (.getInChIGenerator factory (:structure molecule))
        status (.getReturnStatus generator)]
    (if (= (.toString status) "OKAY")
      (r/success (.getInchi generator))
      (r/failure "InChI generation error" :inchi-error {})
      )
   ))



(defn molecule->molv2000
  "generates mol V2000 string for molecule

  Argunemts
  - molecule: Molecule
  "

  [molecule]
  (try
    (let [str-writer (StringWriter.)
          ;; structure (:structure molecule)
          ]
      (with-open [mol-writer (MDLV2000Writer. str-writer)]
        (.write mol-writer (:structure molecule)))
      (r/success (.toString str-writer)))
    (catch Exception e
      (r/failure "Failed to generate molfile" :io-error {:exception e}))))


;; sdf capabilities

(defn- extract-metadata
  "Extracts metadata from IAtomContainer"
  [mol]
  (let [properties (.getProperties mol)]
    (into {} (for [[k v] properties]
               [(keyword (str k)) v ]))))

(defn- clean-medatata!
  "cleans .propeties attribute on IAtomContainer
  returns IAtomContainer with nulified properties"
  [mol]

  (.setProperties mol {})
  mol)

(defn- sync-metadata!
  [molecule]

  (let [mol (:structure molecule)
        props (:metadata molecule)]
    (doseq [[k v] props]
            (.setProperty mol (name k) (str v)))
    mol))

(defprotocol SDFSource
  (get-reader [source]))


(extend-protocol SDFSource
  java.io.File
  (get-reader [source]
    (java.io.FileReader source))

  java.net.URL
  (get-reader [source]
    (clojure.java.io/reader source))

  java.lang.String
  (get-reader [source]
    (if (clojure.string/ends-with? (clojure.string/lower-case source) ".sdf")
      (FileReader. (File. source))
      (StringReader. source))))

(defn sdf->molecules
  "Takes in file stream (object or filename)
  or directly SDF formated string and parses that to vector of molecules"

  [stream]
  (try
    (with-open [is (get-reader stream)
                reader (IteratingSDFReader. is (SilentChemObjectBuilder/getInstance))]

      (let [mols
            (doall
             (for [mol (iterator-seq reader)]
               (let [metadata (extract-metadata mol)]
                 (make-molecule
                  (clean-medatata! mol)
                  metadata
                  ))))]
        (r/success mols)))
    (catch Exception e
      (r/failure "Error reading SDF file" :io-error {:exception e}))))

(defprotocol SDFSink
  (get-writer [this]))


(extend-protocol SDFSink
  java.io.File
  (get-writer [f] (clojure.java.io/writer f))

  java.lang.String
  (get-writer [s] (clojure.java.io/writer s))

  java.io.Writer
  (get-writer [w] w))

(defn molecules->sdf!
  [sink molecules]
  (try
  (with-open [out-writer (get-writer sink)
              sdf-writer (SDFWriter. out-writer)]
    (let [counter (atom 0)]
      (doseq [m molecules]
        (.write sdf-writer (sync-metadata! m))
        (swap! counter inc))
      (r/success @counter)
      ))
  (catch Exception e
    (r/failure "Streaming SDF write failed" :io-error {:exception e})))
  )


;; (->
;;  (inchi-string->molecule "InChI=1S/C6H6/c1-2-4-6-5-3-1/h1-6H")
;;  (r/tap-any println println)
;;  (r/unwrap)
;;  (calculate-mass {:flavor :monoisotopic})
;;  (println)
;;  )



