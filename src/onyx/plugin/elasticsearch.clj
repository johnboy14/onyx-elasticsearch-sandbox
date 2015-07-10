(ns onyx.plugin.elasticsearch
    (:require [onyx.peer.pipeline-extensions :as p-ext]
              [clojurewerkz.elastisch.rest :as esr]
              [clojurewerkz.elastisch.rest.document :as esd]))

(defmethod p-ext/write-batch :elasticsearch/write-messages
    [{:keys [onyx.core/results elasticsearch/server elasticsearch/index elasticsearch/type]}]
    (println server index type results)
    (let [connection (esr/connect server)
          messages (mapcat :leaves results)]
        (doseq [m (map :message messages)]
            (esd/create connection index type m)))
    {})

(defmethod p-ext/seal-resource :elasticsearch/write-messages
    [{:keys [elasticsearch/server]}]
    {})