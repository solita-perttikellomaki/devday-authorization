(ns devday-authorization.db
  (:require [devday-authorization.initial-db :as initial-db]))

(def db (atom initial-db/contents))

(defn init []
  (reset! db initial-db/contents))

(defn query [path]
  (get-in @db path))

(defn modify [path value]
  (swap! db
         (fn [contents]
           (assoc-in contents path value)))
  value)

(defn get-bridge [id]
  (query [:bridges id]))

(defn modify-bridge [bridge]
  (let [id (:id bridge)]
    (if (get-bridge id)
      (modify [:bridges id] bridge)
      {:error "Bridge with that id does not exist"})))

(defn create-bridge [bridge]
  (let [id (:id bridge)]
    (if (get-bridge id)
      {:error "Bridge with that id already exists"}
      (modify [:bridges id] bridge))))

(defn get-bridges-owner [id]
  (query [:bridges id :owner]))

(defn get-document [id]
  (query [:documents id]))

(defn modify-document [document]
  (let [id (:id document)]
    (if (get-document id)
      (modify [:documents id] document)
      {:error "Document with that id does not exist"})))

(defn create-document [document]
  (let [id (:id document)]
    (if (get-document id)
      {:error "Document with that id already exists"}
      (modify [:documents id] document))))

(defn get-documents-bridgeid [documentid]
  (query [:documents documentid :bridgeid]))

(defn get-users-role [userid]
  (query [:users userid :role]))

(defn get-users-organization [userid]
  (query [:users userid :organization]))
