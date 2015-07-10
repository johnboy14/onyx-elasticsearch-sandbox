## onyx-elasticsearch

Onyx plugin for elasticsearch.

#### Installation

In your project file:

```clojure
[onyx-elasticsearch "0.6.0"]
```

In your peer boot-up namespace:

```clojure
(:require [onyx.plugin.elasticsearch])
```

#### Functions

##### sample-entry

Catalog entry:

```clojure
{:onyx/name :entry-name
 :onyx/ident :elasticsearch/task
 :onyx/type :input
 :onyx/medium :elasticsearch
 :onyx/batch-size batch-size
 :onyx/doc "Reads segments from elasticsearch"}
```

Lifecycle entry:

```clojure
[{:lifecycle/task :your-task-name
  :lifecycle/calls :onyx.plugin.elasticsearch/lifecycle-calls}]
```

#### Attributes

|key                           | type      | description
|------------------------------|-----------|------------
|`:elasticsearch/attr`            | `string`  | Description here.

#### Contributing

Pull requests into the master branch are welcomed.

#### License

Copyright © 2015 FIX ME

Distributed under the Eclipse Public License, the same as Clojure.
