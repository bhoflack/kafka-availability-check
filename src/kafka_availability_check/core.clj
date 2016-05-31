(ns kafka-availability-check.core
  (:require [kafka-availability-check.producer :as producer]
            [kafka-availability-check.consumer :as consumer]
            [kafka-availability-check.either-monad :as either]
            [kafka-availability-check.simulation :as simulation]
            [kafka-availability-check.docker :as docker]))

(def brokers
  "A list of the containers running the kafka brokers."
  [{:name "zookeeper"
    :image "wurstmeister/zookeeper"
    :package-loss 0.0
    :latency 0
    :port-mapping {2181 2181}
    }
   {:image "kafka:0.10.0.0"
    :package-loss 10.0
    :latency 0
    :ports [9092]
    :volumes-mapping {"/var/run/docker.sock" "/var/run/docker.sock"}
    :link "zookeeper:zk"
    :env {"KAFKA_ADVERTISED_HOST_NAME" "192.168.124.1"
          "KAFKA_LOG_LEVEL" "DEBUG"}
    }
   {:image "kafka:0.10.0.0"
    :package-loss 10.0
    :latency 0
    :ports [9092]
    :volumes-mapping {"/var/run/docker.sock" "/var/run/docker.sock"}
    :env {"KAFKA_ADVERTISED_HOST_NAME" "192.168.124.1"
          "KAFKA_LOG_LEVEL" "DEBUG"}
    :link "zookeeper:zk"
   }
   {:image "kafka:0.10.0.0"
    :package-loss 10.0
    :latency 0
    :ports [9092]
    :volumes-mapping {"/var/run/docker.sock" "/var/run/docker.sock"}
    :env {"KAFKA_ADVERTISED_HOST_NAME" "192.168.124.1"
          "KAFKA_LOG_LEVEL" "DEBUG"}
    :link "zookeeper:zk"
    }
   {:image "kafka:0.10.0.0"
    :package-loss 10.0
    :latency 0
    :ports [9092]
    :volumes-mapping {"/var/run/docker.sock" "/var/run/docker.sock"}
    :env {"KAFKA_ADVERTISED_HOST_NAME" "192.168.124.1"
          "KAFKA_LOG_LEVEL" "DEBUG"}
    :link "zookeeper:zk"
    }
   {:image "kafka:0.10.0.0"
    :package-loss 10.0
    :latency 0
    :ports [9092]
    :volumes-mapping {"/var/run/docker.sock" "/var/run/docker.sock"}
    :env {"KAFKA_ADVERTISED_HOST_NAME" "192.168.124.1"
          "KAFKA_LOG_LEVEL" "DEBUG"}
    :link "zookeeper:zk"
    }])

(defn run
  [zk]
  (either/bind
   (apply simulation/setup-servers brokers)
   (fn [servers]
     (let [seed (->> servers (filter #(.contains (:image %) "kafka")) first)
           seed-port (-> seed
                         :port-mapping
                         first
                         second)
           seed-ip (:ip-address seed)
           topic "test"]
       (print "Seed" seed)
       (Thread/sleep 30000)
       (either/bind
        (docker/exec (:container-ref seed) "/bin/bash" "-c" (str "$KAFKA_HOME/bin/kafka-topics.sh --zookeeper zk:2181 --create --topic " topic " --partitions 2 --replication-factor 2"))
        (fn [output]
          (println output)
          (.start (Thread. (fn [] (producer/increase (str "localhost:" seed-port) topic))))
          (consumer/check-linearability zk topic "consumer1")
      ))
       ))))

(defn -main [zk]
  (run zk))
