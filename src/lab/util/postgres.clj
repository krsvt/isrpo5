(ns lab.util.postgres
  (:require [next.jdbc :as jdbc]
            [honey.sql :as sql]))

(defn execute-all! [ds statements]
  (when (not-empty statements)
    (jdbc/with-transaction [tx ds]
      (doseq [statement statements]
        (jdbc/execute! tx statement)))))

(defn patients [ds]
  (-> (jdbc/execute! ds ["SELECT * FROM patient"])))

(defn patient-by-id [ds id]
  (-> (jdbc/execute! ds
                     (sql/format {:select :*
                                  :from [:patient]
                                  :where [:= :id [:cast id :uuid]]}
                                 {:pretty true}))
      first))

(defn create-patient [ds patient]
  (-> (jdbc/execute! ds
                     (sql/format {:insert-into [:patient]
                                  :values [(assoc patient :id (random-uuid))]
                                  :returning :*}
                                 {:pretty true}))
      first))

(defn update-patient [ds id patient]
  (-> (jdbc/execute! ds
                     (sql/format {:update [:patient]
                                  :set (dissoc patient :id)
                                  :where [:= :id [:cast id :uuid]]
                                  :returning :*}
                                 {:pretty true}))
      first))
(defn delete-patient [ds id]
  (-> (jdbc/execute! ds
                     (sql/format {:delete-from [:patient]
                                  :where [:= :id [:cast id :uuid]]}
                                 {:pretty true}))
      first))
