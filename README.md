# Lab3

## Up
```
docker compose -d --build
```
## App

### Logging
```clojure
(defn get-patients [{:keys [biff/ds]}]
  (metrics/inc-get-patient)
  (log/info "get patients info")
  (log/error "get patients error")
  {:status 200
   :body (mapv map-json-out (pg/patients ds))})
```
### Logback
Push на `http://loki:3100/loki/api/v1/push`
```
<configuration>
    <appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
        <http>
            <url>http://loki:3100/loki/api/v1/push</url>
        </http>
        <format>
            <label>
                <pattern>app=my-app,host=${HOSTNAME}</pattern>
            </label>
            <message>
                <pattern>%-5level [%.5(${HOSTNAME})] %.10thread %logger{20} | %msg %ex</pattern>
            </message>
        </format>
    </appender>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="info">
        <appender-ref ref="LOKI"/>
        <appender-ref ref="STDOUT"/>
    </root>

    <statusListener class="ch.qos.logback.core.status.OnConsoleStatusListener"/>
```

## Grafana
LogQL:
```
{app="my-app"}
```
```
{app="my-app"} |= "error"
```
```
sum(rate({app="my-app"}[1m])) by (level)
```
![image](https://github.com/user-attachments/assets/c7d90d0f-8df3-4b0e-abf2-288a959dda6f)
