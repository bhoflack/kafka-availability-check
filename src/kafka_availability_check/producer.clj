(ns kafka-availability-check.producer
  (:import java.util.Properties
           kafka.producer.Producer
           kafka.producer.ProducerConfig
           kafka.producer.KeyedMessage))

(defn increase [broker topic]
  (let [producer (-> (doto (Properties.)
                       (.put "metadata.broker.list" broker)
                       (.put "serializer.class" "kafka.serializer.StringEncoder")
                       (.put "producer.type" "sync"))
                     (ProducerConfig.)
                     (Producer.))]
    (try
      (for [n (range 100000000)]
        (->> (KeyedMessage. topic (str n))
             (.send producer)))
      (finally
        (.close producer)))))
