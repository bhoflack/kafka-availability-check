(ns kafka-availability-check.core
  (:require [kafka-availability-check.producer :as producer]
            [kafka-availability-check.consumer :as consumer]))

(def brokers
  "A list of the containers running the kafka brokers."
  [{:image "kafka:0.10.0.0"
    :name "kafka-1"
    :package-loss 10.0
    :latency 100
    :ports [9092]
    }
   {:image "kafka:0.10.0.0"
    :name "kafka-2"
    :package-loss 10.0
    :latency 100
    :ports [9092]
    }
   {:image "kafka:0.10.0.0"
    :name "kafka-3"
    :package-loss 10.0
    :latency 100
    :ports [9092]
    }
   {:image "kafka:0.10.0.0"
    :name "kafka-4"
    :package-loss 10.0
    :latency 100
    :ports [9092]
    }
   {:image "kafka:0.10.0.0"
    :name "kafka-5"
    :package-loss 10.0
    :latency 100
    :ports [9092]
    }])

(defn run
  [zk]
  (either/bind
   (simulation/setup-servers brokers)
   (fn [servers]
     (let [seed (first servers)
           seed-port (-> seed
                         :port-mapping
                         snd)
           seed-ip (:ip-address seed)
           topic "test"]
       (Thread/new (producer/increase (str seed-ip ":" seed-port) topic))
       (consumer/check-linearability zk topic "consumer1")
       ))))

(defn -main [zk]
  (run zk))
