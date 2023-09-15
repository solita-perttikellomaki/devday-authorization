(ns devday-authorization.handlers
  (:require
   [devday-authorization.db :as db]))

(defn get-bridge [parameters]
  (db/get-bridge (:id parameters)))

(defn modify-bridge [parameters]
  (let [bridge (:bridge parameters)
        id     (:id bridge)]
    (if (db/get-bridge id)
      (db/modify-bridge bridge)
      {:error "Bridge with that id does not exist"})))

(defn create-bridge [parameters]
  (let [bridge (:bridge parameters)
        id     (:id bridge)]
    (if (db/get-bridge id)
      {:error "Bridge with that id already exists"}
      (db/create-bridge bridge))))

(defn get-document [parameters]
  (db/get-document (:id parameters)))

(defn modify-document [parameters]
  (let [document (:document parameters)
        id     (:id document)]
    (if (db/get-document id)
      (db/modify-document document)
      {:error "Document with that id does not exist"})))

(defn create-document [parameters]
  (let [document (:document parameters)
        id     (:id document)]
    (if (db/get-document id)
      {:error "Document with that id already exists"}
      (db/create-document document))))
