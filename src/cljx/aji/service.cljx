(ns aji.service)

(defn run [metadata path]
  "takes a the metadata on a cursor, the path of the cursor in the app.
  returns new app state for the path.
  "
  (let [provider-fn (get-in metadata [:service :provider])
        provider-rs (provider-fn (:service metadata)) 
        rs (mapv (fn [[i x]]
                   (merge x (when (= i (last path))
                              {:vi-focus true} )))
                 (map-indexed vector provider-rs))]
    ;(prn provider-fn)
  (with-meta rs metadata)))

; TESTS
;
;(def mock-meta {:service {:provider #(vec [{:fake :data}]) }})
;(str mock-meta)
;(run mock-meta [:this 1])
;(meta (run mock-meta [:this 1]))



(def *provider* [:mock])

(def mock-data {#{:pt/usr :list} [{:id :test-a :text "Mocking a Service"}
                                  {:id :test-b :text "Mock B Service"}]
                #{:pt/country} [{:id :costa-rica :text "Costa Rica" }
                                {:id :panama :text "Panama" }
                                {:id :peru :text "Peru"}
                                {:id :guatemala :text "Guatemala"}] })

(def providers
  "each provider returns a fn which takes the service and returns the data"
  {:mock (fn [service] 
           ;(prn "In :mock provider " (get service :args))
           ;(prn (get mock-data (get service :args)))
           (get mock-data (get service :args)))
   :ws   (fn [] )
   :http (fn [] ) })

(def base-sv {:stale-timeout 360 ; seconds
              :focus-get-timeout 0.8
              ;:provider :mock
              ;:stale-inst #inst "2014-12-12"
              ;:load-state nil ; :wait :timeout :success :error
              ;:load-error nil
              ;:dirty [] ; each dirty attr and it's dirty value
              ;:fresh-as-of nil ; data is fresh/clean as of #inst
              })

(defn gen [opts & init-vec]
  "generate a mock service"
  (with-meta (vec init-vec)
             {:service (-> (merge base-sv {:provider (get-in providers *provider*)})
                           (merge opts))} ))
