(ns devday-authorization.without-authorization
  (:require
   [schema.core :refer [defschema] :as schema]
   [devday-authorization.db :as db]
   [devday-authorization.handlers :as handlers]))

(defschema BridgeId schema/Int)

(defschema Bridge
  {:id   BridgeId
   :name schema/Str})

(defschema DocumentId schema/Int)

(defschema Document
  {:id        DocumentId
   :bridgeid BridgeId
   :contents  schema/Str})

(def routes
  {"/bridges"
   {:GET  {:parameters {:id BridgeId}
           :handler    handlers/get-bridge}
    :PUT  {:parameters {:bridge Bridge}
           :handler    handlers/modify-bridge}
    :POST {:parameters {:bridge Bridge}
           :handler    handlers/create-bridge}}
   "/documents"
   {:GET  {:parameters {:id DocumentId}
           :handler    handlers/get-document}
    :PUT  {:parameters {:document Document}
           :handler    handlers/modify-document}
    :POST {:parameters {:document Document}
           :handler    handlers/create-document}}})

(defn simple-server [routes]
  (fn serve [address method parameters]
    (let [handler (get-in routes [address method :handler])]
      (handler parameters))))

(def serve (simple-server routes))

;(db/init)
;(serve "/bridges" :GET {:id 1})
;(serve "/bridges" :POST {:bridge {:id 999 :name "SATAKUNNANSILTA" :owner "Spectre"}})
;(serve "/bridges" :GET {:id 999})
;(serve "/bridges" :PUT {:bridge {:id 999 :name "Satakunnansilta" :owner "Spectre"}})
;(serve "/bridges" :GET {:id 999})
;(serve "/documents" :GET {:id 11})
;(serve "/documents" :POST {:document {:id 22 :bridgeid 999 :contents "Satakunnansilta blueprints"}})
;(serve "/documents" :GET {:id 22})
;(serve "/documents" :PUT {:document {:id 22 :bridgeid 999 :contents "Satakunnansilta updated blueprints"}})
;(serve "/documents" :GET {:id 22})
