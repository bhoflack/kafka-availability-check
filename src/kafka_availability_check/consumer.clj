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
                       (.put "auto.offset.reset" "largest")
                       (.put "zookeeper.session.timeout.ms" "400")
                       (.put "zookeeper.sync.time.ms" "200")
                       (.put "auto.commit.interval.ms" "1000"))
                     (ConsumerConfig.)
                     (Consumer/create))
        streams (.createMessageStreams consumer {topic 1})
        stream-seq (-> streams
                       first
                       (.iterator)
                       iterator-seq)
        last (atom nil)]

    (for [msg stream-seq]
      (if (not (nil? @last))
        (assert (= 1 (- msg last))))
      (reset! last msg))))
