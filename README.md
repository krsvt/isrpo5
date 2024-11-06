# Lab3

## Up
```
docker compose -d --build
```
## App Traces example
```clojure
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
```

## Jaeger
![image](https://github.com/user-attachments/assets/23f47f3a-646c-4fef-a907-e2a9110b797e)

![image](https://github.com/user-attachments/assets/2d10db01-5fc4-467d-85f2-4f314bc4c58e)


## Grafana
TraceQL
![image](https://github.com/user-attachments/assets/997f1a8b-42a2-4cc6-80b7-942d8ee5a951)
![image](https://github.com/user-attachments/assets/b76afafb-e1da-4caa-86ec-048dd5d48335)

