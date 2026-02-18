# clj-cdk-wrapper

Convenient wrapper around Java CDK library.
Utilizes other library "clj-result" for convenient wrepping or errors.
Currently work in progress. subject to refactoring

## Usage
### Objects
``` clojure
(Molecule [structure metadata])

```
#### Creating a molecule instance
several record constructors are provided

```clojure
(:require
    [clj-result.core :as r]
    [clj-cdk-wrapper.core as :ckd])

(cdk/smiles->molecule [^String smiles]) ;;
(cdk/inchi-string->molecule [^String inchi-string])
(cdk/molV2000->molecule [^String molv2000])
```

pipeline computation example

```clojure
(->
    (cdk/inchi-string->molecule "InChI=1S/C4H10/c1-3-4-2/h3-4H2,1-2H3")
    ;; returns (success :value Molecule) object  or (failure :message String :error Keyword :metadata Map) if fails operation
    ;; value may be unwraped
    (r/unwrap)
    (pprint)
)

;;output....
{:structure
 #object[org.openscience.cdk.silent.AtomContainer...
 :metadata {}}

```

## License

Copyright © 2026 QuadroLT

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
https://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
