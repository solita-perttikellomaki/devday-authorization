(ns devday-authorization.initial-db)

(def contents
  {:bridges   {1 {:id    1
                  :name  "Tower Bridge"
                  :owner "Acme Inc."}
               2 {:id    2
                  :name  "Three Gorges"
                  :owner "Spectre"}}
   :documents {11 {:id       11
                   :bridgeid 1
                   :contents "Tower Bridge blueprints"}}
   :users     {"bob"   {:role         :builder
                        :organization "Acme Inc."}
               "fred"  {:role         :mear-mortal
                        :organization "Acme Inc."}
               "alice" {:role         :builder
                        :organization "Spectre"}}})

                
