(ns lab
  (:require [com.biffweb :as biff]
            [lab.app :as app]
            [lab.middleware :as mid]
            [lab.worker :as worker]
            [clojure.java.io :as io]
            [clojure.test :as test]
            [clojure.tools.logging :as log]
            [clojure.tools.namespace.repl :as tn-repl]
            [next.jdbc :as jdbc]

            ;; [steffan-westcott.clj-otel.api.trace.http :as trace-http]
            ;; [steffan-westcott.clj-otel.api.trace.span :as span]
            [nrepl.cmdline :as nrepl-cmd])
  (:gen-class))

(def modules
  [app/module
   worker/module])

(def routes [["" {:middleware [mid/wrap-site-defaults]}
              (keep :routes modules)]
             ["" {:middleware [mid/wrap-api-defaults]}
              (keep :api-routes modules)]])

(def handler (-> (biff/reitit-handler {:routes routes})
                 mid/wrap-base-defaults))

(def static-pages (apply biff/safe-merge (map :static modules)))

(defn generate-assets! [ctx]
  (biff/export-rum static-pages "target/resources/public")
  (biff/delete-old-files {:dir "target/resources/public"
                          :exts [".html"]}))

(defn on-save [ctx]
  (biff/add-libs)
  (biff/eval-files! ctx)
  (generate-assets! ctx)
  (biff/catchall (require 'lab.lab-test))
  (test/run-all-tests #"lab.*-test"))

(def initial-system
  {:biff/modules #'modules
   :biff/merge-context-fn identity
   :biff/handler #'handler
   :biff.beholder/on-save #'on-save })

(defonce system (atom {}))

(defn use-postgres [{:keys [biff/secret] :as ctx}]
  (let [ds (jdbc/get-datasource (secret :example/postgres-url))]
    (jdbc/execute! ds [(slurp (io/resource "migrations.sql"))])
    (assoc ctx :biff/ds ds)))

(def components
  [biff/use-aero-config
   use-postgres
   biff/use-queues
   biff/use-htmx-refresh
   biff/use-jetty
   biff/use-chime
   biff/use-beholder])

(defn start []
  (let [new-system (reduce (fn [system component]
                             (log/info "starting:" (str component))
                             (component system))
                           initial-system
                           components)]
    (reset! system new-system)
    (generate-assets! new-system)
    (log/debug "debug message")
    (log/info "info message")
    (log/warn "warn message")
    (log/error "error message")
    (log/info "System started.")
    (log/info "Go to" (:biff/base-url new-system))
    new-system))

(defn -main []
  (let [{:keys [biff.nrepl/args]} (start)]
    (apply nrepl-cmd/-main args)))

(defn refresh []
  (doseq [f (:biff/stop @system)]
    (log/info "stopping:" (str f))
    (f))
  (tn-repl/refresh :after `start)
  :done)
