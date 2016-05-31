(ns kafka-availability-check.simulation
  (:require [kafka-availability-check.docker :as docker]
            [kafka-availability-check.either-monad :as either]))

(defn create-temporary-directory [suffix]
  (doto (java.io.File/createTempFile "vol" "_dir")
    (.delete)
    (.mkdir)
    (.deleteOnExit)))

(defn setup-servers
  "Setup the servers described in the argument."
  [& servers]
  (->> servers       
       (map (fn [{:keys [image name latency package-loss volumes volumes-mapping port-mapping ports link env] :or {port-mapping {}}}]
              (let [volumes' (->> volumes
                                  (map (fn [vol] [(.getPath (create-temporary-directory vol)) vol]))
                                  (into {}))
                    volumes'' (merge volumes' volumes-mapping)
                    maybe-ref (docker/run image :volume-mapping volumes'' :port-mapping port-mapping :publish-all-ports? true :link link :env env :name name)]
                (either/bind maybe-ref (fn [container-ref]
                (either/bind (docker/port container-ref) (fn [port-mapping]
                (either/bind (docker/inspect container-ref "{{ .NetworkSettings.IPAddress }}") (fn [ip-address]
                (either/bind (docker/exec container-ref "tc" "qdisc" "add" "dev" "eth0" "root" "netem" "delay" (str latency "ms") "loss" (str package-loss "%")) (fn [_]
                  (either/unit
                    {:container-ref container-ref
                     :port-mapping  port-mapping
                     :volumes volumes''
                     :ip-address ip-address
                     :link link
                     :env env
                     :image image
                     :name name}))))))))))))
       (apply either/sequence)))
