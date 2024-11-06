(ns lab.app
  (:require
   [lab.util.postgres :as pg]
   [medley.core :as medley]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [iapetos.collector.ring :as ring]
   [steffan-westcott.clj-otel.api.trace.http :as trace-http]
   [steffan-westcott.clj-otel.api.trace.span :as span]
   [lab.metrics :as metrics]))

(set! *warn-on-reflection* true)

(defn camelcase->snakecase [s]
  (-> (name s)
      (str/replace #"([a-z])([A-Z])" "$1_$2")
      str/lower-case))

(defn snakecase->camelcase [s]
  (let [words (clojure.string/split (name s) #"_")]
    (str (first words)
         (apply str (map clojure.string/capitalize (rest words))))))

(defn map-json-in [json]
  (medley/map-keys camelcase->snakecase json))

(defn map-json-out [json]
  (medley/map-keys snakecase->camelcase json))

(defn get-patients [{:keys [biff/ds]}]
  (span/with-span! "Fetch all patients"
    (metrics/inc-get-patient)
    (log/info "get patients info")
    (log/error "get patients error")
    (Thread/sleep (+ 1000 (rand-int 5000)))
    (let [patients
          (span/with-span! "Fetch patients from database"
            {:attributes {:db.operation "select"}}
            (pg/patients ds))
          result (span/with-span! "Transform Patients to json"
                   (mapv map-json-out patients))]
      {:status 200
       :body result})))

(defn add-patient [{:keys [biff/ds params]}]
  (span/with-span! "Adding new patient"
    (try
      (let [params (span/with-span! "Transforming input json" (map-json-in params))
            pat (span/with-span! "Insert patient to database"
                  {:attributes {:db.operation "insert"}}
                  (if (= 0 (rand-int 2))
                    (throw (Exception. "New patient 50% exception!"))
                    (pg/create-patient ds params)))]
        (cond
          pat (do
                (metrics/inc-created-patients)
                {:status 200
                 :body (map-json-out pat)})

          :else {:status 400}))

      (catch Exception e
        (span/add-exception! e {:escaping? false})))))

(defn get-patient [{:keys [biff/ds] :as ctx}]
  (let [id (-> ctx :path-params :id)
        pat (pg/patient-by-id ds id)]
    (cond
      pat
      (do
        (metrics/inc-get-patient)
        {:status 200
       :body (map-json-out pat)})
      :else
      {:status 404})))

(defn update-patient [{:keys [biff/ds params] :as ctx}]
  (let [id (-> ctx :path-params :id)
        params (map-json-in params)
        pat (pg/update-patient ds id params)]
    (cond
      pat
      {:status 200
       :body (map-json-out pat)}
      :else
      {:status 404})))

(defn delete-patient [{:keys [biff/ds] :as ctx}]
  (let [id (-> ctx :path-params :id)
        res (pg/delete-patient ds id)]
    (cond
      res
      {:status 204}
      :else
      {:status 404})))

(def module
  {:api-routes [["/metrics"
                 {:get metrics/metrics-handler}]

                ["/api/patient"
                 ["" {:post add-patient
                      :get get-patients}]

                 ["/:id"
                  {:get get-patient
                   :put update-patient
                   :delete delete-patient}]]]})
