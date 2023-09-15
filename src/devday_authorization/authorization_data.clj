(ns devday-authorization.authorization-data
  (:require [devday-authorization.authorization-dsl :as dsl]))

(defn attach [schema template]
  (vary-meta schema
             #(assoc %
                     :get-auth-data
                     (fn [value]
                       (->> template
                            (map (fn [[key f]]
                                   [key (dsl/create-constant (f value))]))
                            (into {}))))))

(defn extract [value schema]
  (let [extractor (:get-auth-data (meta schema))]
    (extractor value)))
