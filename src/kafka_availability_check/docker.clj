(ns kafka-availability-check.docker
  (:require [clojure.java.shell :refer [sh]])
  (:require [kafka-availability-check.either-monad :as either]))

(defn sh!
  [& cmds]
  (let [r (apply sh cmds)]
    (if [(= (:exit r) 0)]
      (either/unit r)
      (either/fail! r))))

(defn run
  "Run a docker image"
  [image & {:keys [env volume-mapping port-mapping publish-all-ports?]
            :or {env {}
                 volume-mapping {}
                 port-mapping {}
                 publish-all-ports? true}}]
  (let [command ["sudo" "docker" "run" "-d" "--privileged=true"]
        volume-args (mapcat (fn [[host container]] ["-v" (str host ":" container)]) volume-mapping)
        env-args (mapcat (fn [[k v]] ["-e" (str k "=" v)]) env)
        port-args (mapcat (fn [[host container]] ["-p" (str host ":" container)]) port-mapping)
        cmd (if publish-all-ports?
              (concat command volume-args env-args port-args ["-P"] [image])
              (concat command volume-args env-args port-args [image]))]
    (either/bind (apply sh! cmd)
                 (fn [{:keys [out]}] (either/unit (.trim out))))))

(defn stop
  "Stop a docker image"
  [docker-ref]
  (sh! "sudo" "docker" "stop" docker-ref))

(defn port
  "Find the host port that is mapped to a specific container port"
  ([docker-ref port]
   (either/bind (sh! "sudo" "docker" "port" docker-ref port)
                (fn [v] (either/unit (:out v)))
                (fn [s]
                  (if-let [m (re-seq #".*:(\d+)" s)]
                    (-> m
                        first
                        second
                        either/unit)
                    (either/fail! "Could not find the host port.")))))
  ([docker-ref]
   (either/bind (sh! "sudo" "docker" "port" docker-ref)
                (fn [v] (either/unit (:out v)))
                (fn [v] (either/unit (if (nil? v)
                                       []
                                       (clojure.string/split v #"\n"))))

                ; extract the container port and the host port
                (fn [v] (either/unit (map (partial re-seq #"(\d+/\w+) -> \d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}:(\d+)") v)))
                (fn [v] (either/unit (map (fn [m] (-> m first rest)) v)))

                (fn [v] (either/unit (map (fn [[container-port host-port]] [container-port host-port]) v)))
                (fn [v] (->> v (into {}) either/unit)))))

(defn exec
  "Execute a command in the context of a container"
  [docker-ref cmd & args]
  (->> (concat ["sudo" "docker" "exec"] [docker-ref cmd] args)
       (apply sh!)))

(defn inspect
  "Inspect a container"
  [docker-ref f]
  (let [command (concat ["sudo" "docker" "inspect" "-f" f docker-ref])]
    (either/bind (apply sh! command)
                 (fn [resp] (either/unit (.trim (:out resp)))))))
