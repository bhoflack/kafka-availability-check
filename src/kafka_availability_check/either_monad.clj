(ns kafka-availability-check.either-monad)

(defn unit [v] {:type :success :value v})

(defn fail! [e] {:type :failure :err e})

(defn bind
  [either & functions]
  (loop [e either
         fs functions]
    (cond
      (empty? fs) e
      (= (:type e) :failure) e
      :else (let [f (first fs)]
              (recur (f (:value e))
                     (rest fs))))))

(defn sequence
  [& ms]
  (let [head (first ms)]
    (if (empty? ms)
      (unit [])      
      (bind head
            (fn [v]
              (bind (apply sequence (rest ms))
                    (fn [tail]
                      (unit (cons v tail)))))))))

(comment
  "Work in progress"
 (defmacro perform
   [bindings &body]
   `(let [bindings# (partition 2 bindings)]
      (if-let [[v# m#] (first bindings#)]
        (bind
         m#
         (fn [~v#]               
           (reduce
            (fn [acc [k v]]
              (bind (v acc)
                    (fn [])))
            ~v#
            (rest bindings))))))))
