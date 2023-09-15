(ns devday-authorization.with-authorization
  (:require
   [schema.core :refer [defschema] :as schema]
   [devday-authorization.db :as db]
   [devday-authorization.authorization-dsl :refer [parse] :as dsl]
   [devday-authorization.authorization-data :as auth-data]
   [devday-authorization.handlers :as handlers]))

(defschema BridgeId
  (-> schema/Int
      (auth-data/attach
        {:BRIDGEID identity})))

(defschema Bridge
  (-> {:id   BridgeId
       :name schema/Str}
      (auth-data/attach
        {:BRIDGEID :id})))

(defschema DocumentId
  (-> schema/Int
      (auth-data/attach
        {:DOCUMENTID identity})))

(defschema Document
  (-> {:id        DocumentId
       :bridgeid BridgeId
       :contents  schema/Str}
      (auth-data/attach
        {:DOCUMENTID :id
         :BRIDGEID   :bridgeid})))

(auth-data/extract
  {:id 1 :name "Tower Bridge"}
  Bridge)

(auth-data/extract
  {:id 11 :bridgeid 1 :contents "Tower Bridge updated blueprints"}
  Document)

(def public-access-auth
  (parse true))

(def bridge-creation-auth
  (parse '(= :USERS-ROLE :builder)))

(def bridge-modification-auth
  (parse
    '(unique [:BRIDGEID [:BRIDGEID :DOCUMENTS-BRIDGEID]]
             (and (= :BRIDGES-OWNER :USERS-ORGANIZATION)
                  (= :USERS-ROLE :builder)))))
        
(def routes
  {"/bridges"
   {:GET  {:parameters {:id BridgeId}
           :authorization public-access-auth
           :handler    handlers/get-bridge}
    :PUT  {:parameters {:bridge Bridge}
           :authorization bridge-modification-auth
           :handler    handlers/modify-bridge}
    :POST {:parameters {:bridge Bridge}
           :authorization bridge-creation-auth
           :handler    handlers/create-bridge}}
   "/documents"
   {:GET  {:parameters {:id DocumentId}
           :authorization public-access-auth
           :handler    handlers/get-document}
    :PUT  {:parameters {:document Document}
           :authorization bridge-modification-auth
           :handler    handlers/modify-document}
    :POST {:parameters {:document Document}
           :authorization bridge-modification-auth
           :handler    handlers/create-document}}})

(defn authorizing-server [routes]
  (fn serve [address method userid parameters]
    (let [handler           (get-in routes [address method :handler])
          auth-condition    (get-in routes [address method :authorization])
          route-parameters  (get-in routes [address method :parameters])
          request-variables (->> route-parameters
                                 (map (fn [[param schema]]
                                        (auth-data/extract (param parameters) schema)))
                                 (apply merge))]
      (let [auth-result
            (dsl/eval-expr
              (assoc request-variables
                     :USERID (dsl/create-constant userid))
              auth-condition)]
        #_(do
            (println "### request variables:" request-variables)
            (println "### eval" (dsl/pretty auth-condition)
                     " => " (dsl/pretty auth-result)))
        (if (dsl/constant-true? auth-result)
          (handler parameters)
          {:error "Access denied"})))))


(def serve (authorizing-server routes))

;(db/init)
;(serve "/bridges" :GET "bob" {:id 1})
;(serve "/documents" :PUT "fred" {:document {:id 11 :bridgeid 1 :contents "Tower Bridge updated blueprints"}})
;(serve "/documents" :PUT "bob" {:document {:id 11 :bridgeid 1 :contents "Tower Bridge updated blueprints"}})
;(serve "/bridges" :POST "alice" {:bridge {:id 999 :name "SATAKUNNANSILTA" :owner "Spectre"}})
;(serve "/bridges" :GET "bob" {:id 999})
;(serve "/bridges" :PUT "bob" {:bridge {:id 999 :name "Satakunnansilta" :owner "Acme Inc."}})
;(serve "/bridges" :PUT "alice" {:bridge {:id 999 :name "Satakunnansilta" :owner "Spectre"}})
;(serve "/bridges" :GET "fred" {:id 999})
;(serve "/documents" :GET "bob" {:id 11})
;(serve "/documents" :POST "bob" {:document {:id 22 :bridgeid 999 :contents "Satakunnansilta blueprints"}})
;(serve "/documents" :POST "alice" {:document {:id 22 :bridgeid 999 :contents "Satakunnansilta blueprints"}})
;(serve "/documents" :GET "bob" {:id 22})
;(serve "/documents" :PUT "bob" {:document {:id 22 :bridgeid 999 :contents "Satakunnansilta updated blueprints"}})
;(serve "/documents" :PUT "alice" {:document {:id 22 :bridgeid 999 :contents "Satakunnansilta updated blueprints"}})
;(serve "/documents" :GET "fred" {:id 22})
