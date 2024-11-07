# Lab5

## Up
```
docker compose -d --build
```
## App 

### Traces Library
https://github.com/steffan-westcott/clj-otel

Adding dependency in deps.edn:
```
com.github.steffan-westcott/clj-otel-api {:mvn/version "0.2.7"}
```

### Traces Docker setup
```Dockerfile
COPY opentelemetry-javaagent.jar opentelemetry-javaagent.jar
...
CMD ["/usr/bin/java", <...>, "-javaagent:opentelemetry-javaagent.jar", "-Dotel.resource.attributes=service.name=counter-service", "-Dotel.metrics.exporter=none", "-Dotel.logs.exporter=none", "-jar", "app.jar"]
```
### Traces middleware init

Using [wrap-server-span](https://cljdoc.org/d/com.github.steffan-westcott/clj-otel-api/0.2.7/api/steffan-westcott.clj-otel.api.trace.http?q=wrap-server-span#wrap-server-span) middleware: 

```clojure
(require '[steffan-westcott.clj-otel.api.trace.http :as trace-http])

(def handler (-> (biff/reitit-handler {:routes routes})
                 mid/wrap-base-defaults
                 trace-http/wrap-server-span)) ;; here
```

Using tempo exporter endpoint in `docker-compose.yaml`:
```
OTEL_EXPORTER_OTLP_ENDPOINT: "http://tempo:4318"
```

### Traces example

GET /api/patient - задержка от 1 до 6 сек
```clojure
(require '[steffan-westcott.clj-otel.api.trace.span :as span])

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

