(ns kafka-availability-check.consumer
  (:import java.util.Properties
           kafka.consumer.Consumer
           kafka.consumer.ConsumerConfig))

(defn check-linearability
  "Check if the message value on the topic increase"
  [zk topic group]
  (let [consumer (-> (doto (Properties.)
                       (.put "zookeeper.connect" zk)
                       (.put "group.id" group)
                       (.put "auto.offset.reset" "smallest")
                       (.put "zookeeper.session.timeout.ms" "400")
                       (.put "zookeeper.sync.time.ms" "200")
                       (.put "auto.commit.interval.ms" "1000"))
                     (ConsumerConfig.)
                     (Consumer/create))
        streams (.createMessageStreams consumer (scala.collection.JavaConversions/mapAsScalaMap {topic (int 1)}))
        stream-seq (-> streams
                       scala.collection.JavaConversions/mapAsJavaMap                       
                       (get topic)
                       scala.collection.JavaConversions/seqAsJavaList
                       first
                       (.iterator)
                       iterator-seq)
        last (atom nil)]

    (doseq [msg stream-seq]
      (let [n (Integer/parseInt (String. (.message msg)))]
        (println "consume message " n )
        (if (not (nil? @last))
          (assert (= 1 (- n @last))))
        (reset! last n)))))
