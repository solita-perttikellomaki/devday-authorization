(ns devday-authorization.authorization-dsl
  (:require
   [clojure.string :as string]
   [devday-authorization.db :as db]))

(declare eval-expr)

(defn create-constant [value]
  {:operator :constant :value value})

(defn constant? [expr]
  (= (:operator expr) :constant))

(def constant-true (create-constant true))
(def constant-false (create-constant false))

(defn constant-true? [expr]
  (= expr constant-true))

(defn constant-false? [expr]
  (= expr constant-false))

(defn create-variable [name]
  {:operator :variable :name name})

(defn variable? [expr]
  (= (:operator expr) :variable))

(defn- eval-variable [name env expr]
  (if ((set (keys env)) name)
           (get env name)
           expr))

(defn- variable [name compute-fn]
  [name (partial compute-fn name)])

(defn- compute-derived-variable
  [source-variable compute-fn name env expr]
  (or (get env name)
      (let [source-value (eval-expr env (create-variable source-variable))]
        (if (constant? source-value)
          (if-let [derived-value (compute-fn (:value source-value))]
            (create-constant derived-value)
            expr)
          expr))))

(defn- make-variable-semantics [[name spec]]
  (cond (= spec :request-variable)
        (variable name eval-variable)

        (and (vector? spec)
             (fn? (first spec)))
        (let [[compute-fn source-variable] spec]
          (variable name
                    (partial compute-derived-variable source-variable compute-fn)))))

(def variables
  {:BRIDGEID           :request-variable
   :DOCUMENTID         :request-variable
   :BRIDGES-OWNER      [db/get-bridges-owner :BRIDGEID]
   :DOCUMENTS-BRIDGEID [db/get-documents-bridgeid :DOCUMENTID]
   :USERID             :request-variable
   :USERS-ROLE         [db/get-users-role :USERID]
   :USERS-ORGANIZATION [db/get-users-organization :USERID]})

(def variable-semantics
  (->> variables
       (map make-variable-semantics)
       (into {})))

(defn dependencies [variable-name]
  (let [direct-dependencies
        (->> variables
             (filter (fn [[_ spec]]
                       (and (vector? spec) (= variable-name (second spec)))))
             (map first))]
    (->> (conj direct-dependencies variable-name)
         (conj (map dependencies direct-dependencies))
         (apply concat)
         set)))

(def variable-dependencies
  (->> (keys variables)
       (map (fn [v] [v (dependencies v)]))
       (into {})))

(defn- eval-= [evaluated-operands]
  (if (every? constant? evaluated-operands)
    (create-constant (apply = evaluated-operands))
    {:operator := :operands evaluated-operands}))

(defn create-and [operands]
  (cond (empty? operands)      constant-true
        (= (count operands) 1) (first operands)
        :else                  {:operator :and :operands operands}))

(defn drop-dependencies [env variable-name]
  (apply (partial dissoc env) (get variable-dependencies variable-name)))

(defn- eval-and [evaluated-operands]
  (if (some constant-false? evaluated-operands)
    (create-constant false)
    (create-and (remove constant-true? evaluated-operands))))

(defn eval-unique [env expr]
  (let [bound-variable (:bound-variable expr)
        condition (:condition expr)
        variable-values (map (fn [variable]
                               (eval-expr env variable))
                             (:variables expr))
        constants (filter constant? variable-values)
        stripped-env (drop-dependencies env bound-variable)]
    (cond (empty? constants)
          (if (constant? (eval-expr stripped-env condition))
            (eval-expr stripped-env condition)
            (assoc expr :condition (eval-expr stripped-env condition)))
          (not (apply = constants)) expr
          :else
          (eval-expr (assoc stripped-env
                            bound-variable
                            (first constants))
                         condition))))

(def operator-semantics
  {:=   eval-=
   :and eval-and})

(defn pretty [expr]
  (cond (constant? expr)
        (:value expr)

        (variable? expr)
        (:name expr)

        (= (:operator expr) :unique)
        `(~(symbol "unique") [~(:bound-variable expr) [~@(map pretty (:variables expr))]]
                 ~(pretty (:condition expr)))

        :else
        `(~(symbol (subs (str (:operator expr)) 1)) ~@(map pretty (:operands expr)))))

(defn eval-expr [env expr]
  ;; Remove #_ to follow expression evaluation
  #_{:post [(nil? (println "### eval" (pretty expr) "in" env "=>" (pretty %)))]}
  (cond (constant? expr)
        expr

        (variable? expr)
        ((get variable-semantics (:name expr)) env expr)

        (= (:operator expr) :unique)
        (eval-unique env expr)

        :else
        ((get operator-semantics (:operator expr))
         (map (fn [ex] (eval-expr env ex))
              (:operands expr)))))

 (defn parse [expr]
  (cond (or (true? expr) (false? expr) (#{:builder :ordinary-mortal} expr))
        (create-constant expr)

        (get variable-semantics expr)
        (create-variable expr)

        (= 'unique (first expr))
        (let [bound-variable (first (second expr))
              variables      (second (second expr))
              condition      (nth expr 2)]
          {:operator       :unique
           :bound-variable bound-variable
           :variables      (map parse variables)
           :condition      (parse condition)})

        :else
        (let [operator        (first expr)
              operands        (rest expr)
              operator-kw     (keyword operator)
              parsed-operands (map parse operands)]
          {:operator operator-kw
           :operands parsed-operands})))

(defn explain [expr]
  (cond (constant? expr)
        (str (:value expr))

        (variable? expr)
        (str (:name expr))

        (= (:operator expr) :unique)
        (str (explain (:condition expr))
             ", where "
             (str (:bound-variable expr))
             " is determined by ["
             (string/join ", " (map explain (:variables expr)))
             "]")

        :else
        (str
          (string/join (str " " (subs (str (:operator expr)) 1) " ")
                       (mapv (fn [ex]
                               (if (or (constant? ex) (variable? ex))
                                 (explain ex)
                                 (str "(" (explain ex) ")")))
                             (:operands expr))))))

(def auth-condition
  (parse
    '(unique [:BRIDGEID [:BRIDGEID :DOCUMENTS-BRIDGEID]]
             (and (= :BRIDGES-OWNER :USERS-ORGANIZATION)
                  (= :USERS-ROLE :builder)))))
