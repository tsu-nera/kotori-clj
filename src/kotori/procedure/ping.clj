(ns kotori.procedure.ping)

(defn ping-pong [{:keys [ping comment]}]
  (cond->
   {:pong "pong"}
    comment (assoc :comment comment)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (ping-pong {:ping "ping" :comment "hello"})
  ;; => [{:pong "pong", :comment "hello"} nil]
  (ping-pong {:ping "ping"})
  ;; => [{:pong "pong"} nil]
  )
