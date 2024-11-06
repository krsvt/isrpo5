(ns lab.metrics
  (:require [iapetos.core :as prometheus]
            [iapetos.export :as export]))

(def registry
  (-> (prometheus/collector-registry)
      (prometheus/register
        (prometheus/counter :patients/get {:description "Number of GET requests to /patient endpoint"})
        (prometheus/counter :patients/created {:description "Number of created patients"}))))

(defn inc-created-patients []
  (prometheus/inc registry :patients/created))

(defn inc-get-patient []
  (prometheus/inc registry :patients/get))

(defn metrics-handler [_request]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (export/text-format registry)})
