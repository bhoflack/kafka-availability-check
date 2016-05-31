(ns kafka-availability-check.producer
  (:import java.util.Properties
           org.apache.kafka.clients.producer.KafkaProducer
           org.apache.kafka.clients.producer.ProducerRecord))

(defn increase [broker topic]
  (let [producer (-> (doto (Properties.)
                       (.put "key.serializer" "org.apache.kafka.common.serialization.StringSerializer")
                       (.put "value.serializer" "org.apache.kafka.common.serialization.StringSerializer")
                       (.put "producer.type" "sync")
                       (.put "bootstrap.servers" broker))
                     (KafkaProducer.))]
    (try
      
      (doseq [n (range 100000000)]
        (.get (.send producer (ProducerRecord. topic "test" (str n))))
        (println "produce message " n))
      (catch Exception e
        (println "exception: " e))
      (finally
        (.close producer)))))
