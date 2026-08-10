<h3> Object info </h3>

* Monitor
    * id
    * URL
    * name
    * check interval
    * next scheduled check
    * current status
        * PENDING
        * UP
        * DEGRADED
        * DOWN
    * consecutive failure count

* Check Result
    * id
    * monitor_id
    * checked_at
    * HTTP status code
    * latency
    * error
        * TIMEOUT
        * CONNECTION_REFUSED

* Incident
    * id
    * monitor_id
    * incident_id
    * started
    * ended

* Alert
    * id
    * incident_id
    * delivery_status
    * created_at
    * Type
        * OUTAGE
        * RECOVERY
    * sent_at

