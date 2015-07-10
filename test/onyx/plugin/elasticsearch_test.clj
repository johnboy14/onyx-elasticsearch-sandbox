(ns onyx.plugin.elasticsearch-test
    (:require [onyx.peer.pipeline-extensions :as p-ext]
              [clojurewerkz.elastisch.rest :as esr]
              [clojurewerkz.elastisch.rest.index :as esi]
              [clojurewerkz.elastisch.rest.document :as esd]
              [clojurewerkz.elastisch.query :as q]
              [onyx.api]
              [clojure.core.async :as async])
    (:use [midje.sweet]))
;;Environment setup
(def id (java.util.UUID/randomUUID))

(def env-config
    {:zookeeper/address "127.0.0.1:2188"
     :zookeeper/server? true
     :zookeeper.server/port 2188
     :onyx/id id})

(def peer-config
    {:zookeeper/address "127.0.0.1:2188"
     :onyx.peer/job-scheduler :onyx.job-scheduler/greedy
     :onyx.messaging/impl :netty
     :onyx.messaging/peer-port-range [40200 40400]
     :onyx.messaging/peer-ports [40199]
     :onyx.messaging/bind-addr "localhost"
     :onyx/id id})

(def env (onyx.api/start-env env-config))

(def peer-group (onyx.api/start-peer-group peer-config))

(def v-peers (onyx.api/start-peers 4 peer-group))

;;ElasticSearch configuration
(def connection (esr/connect "http://127.0.0.1:9200"))
(esi/delete connection "index1")
(esi/create connection "index1")

;;workflow
(def workflow
    [[:in :identity]
     [:identity :write-messages]])

(def catalog
    [{:onyx/name :in
      :onyx/ident :core.async/read-from-chan
      :onyx/type :input
      :onyx/medium :core.async
      :onyx/max-peers 1
      :onyx/batch-size 100
      :onyx/doc "Reads segments from a core.async channel"}

     {:onyx/name :identity
      :onyx/fn :clojure.core/identity
      :onyx/type :function
      :onyx/batch-size 100}

     {:onyx/name :write-messages
      :onyx/ident :elasticsearch/write-messages
      :onyx/type :output
      :onyx/medium :elasticsearch
      :elasticsearch/server "127.0.0.1:9200"
      :elasticsearch/index "index1"
      :elasticsearch/type "person"
      :onyx/batch-size 5
      :onyx/doc "Writes messages to a ElasticSearch index of type person"}])


(def in-chan (async/chan 4))

(async/>!! in-chan {:name "John"})
(async/>!! in-chan {:name "Peter"})
(async/>!! in-chan {:name "Luke"})
(async/>!! in-chan :done)

(defn inject-in-ch [event lifecycle]
    {:core.async/chan in-chan})

(def in-calls
    {:lifecycle/before-task-start inject-in-ch})

(def lifecycles
    [{:lifecycle/task :in
      :lifecycle/calls :onyx.plugin.output-test/in-calls}
     {:lifecycle/task :in
      :lifecycle/calls :onyx.plugin.core-async/reader-calls}])

(println "Before Submit")

(onyx.api/submit-job
    peer-config
    {:catalog catalog :workflow workflow :lifecycles lifecycles
     :task-scheduler :onyx.task-scheduler/balanced})

(println "After Submit")
;;AFTER SUBMITTION JOB CHECK ELASTICSEARCH

(fact "Confirm the contents of the job in ElasticSearch"
      (let [person1 (first (:hits (:hits (esd/search connection "index1" "person" :query (q/term :name "John")))))
            person2 (first (:hits (:hits (esd/search connection "index1" "person" :query (q/term :name "Peter")))))
            person3 (first (:hits (:hits (esd/search connection "index1" "person" :query (q/term :name "Luke")))))]
          person1 => {:name "John"}
          person2 => {:name "Peter"}
          person3 => {:name "Luke"}))


(doseq [v-peer v-peers]
    (onyx.api/shutdown-peer v-peer))

(onyx.api/shutdown-peer-group peer-group)

(onyx.api/shutdown-env env)


