(ns onyx.plugin.elasticsearch
    (:require [onyx.peer.pipeline-extensions :as p-ext]
              [clojurewerkz.elastisch.rest :as esr]
              [clojurewerkz.elastisch.rest.document :as esd]
              [clojurewerkz.elastisch.rest.bulk :as esrb]))

(defn inject-write-messages
  [{:keys [onyx.core/task-map] :as pipeline} lifecycle]
  (let [connection (esr/connect (:elasticsearch/server task-map))]
    {:elasticsearch/connection connection
     :elasticsearch/index (:elasticsearch/index task-map)
     :elasticsearch/type (:elasticsearch/type task-map)}))

(def write-messages-calls
  {:lifecycle/before-task-start inject-write-messages})

(defmethod p-ext/write-batch :elasticsearch/write-messages
    [{:keys [onyx.core/results elasticsearch/connection elasticsearch/index elasticsearch/type]}]
  (let [messages (map :message (mapcat :leaves results))]
    (doseq [m messages]
    (esd/create connection index type m))
    {}))

(defmethod p-ext/seal-resource :elasticsearch/write-messages
    [{:keys [elasticsearch/server]}]
    {})