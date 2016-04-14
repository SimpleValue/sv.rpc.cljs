(ns sv.rpc.cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :as a :refer [<!]]))

(defn call-fn [config]
  (fn [fn & args]
    (go
      (let [params-key (keyword (str (name (:format config :edn)) "-params"))
            response (<! (http/post
                          (:path config)
                          {params-key {:fn fn
                                       :args args}}))
            body (:body response)]
        (if (contains? body :result)
          (:result body)
          (ex-info
           "RPC failed"
           {:error :rpc/failed}
           (:body response)))))))

(def default-config {:path "/rpc"
                     :format :edn})

;; TODO: handle exception in channel, possible solution: http://martintrojer.github.io/clojure/2014/03/09/working-with-coreasync-exceptions-in-go-blocks/
(def call (call-fn default-config))

(defn call-back-fn [config]
  (let [call (call-fn config)]
    (fn [callback fn & args]
      (go
        (let [result (<! (apply call fn args))]
          (if (instance? cljs.core.ExceptionInfo result)
            (throw result)
            (callback result)))))))

(def call-back (call-back-fn default-config))
