(ns aji.service
  (:require [cljs.reader :as reader]
            [goog.events :as events])
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

(def providers (atom {}))

(defn providers! [k fn]
  "adds a provider to the providers atom"
  (swap! providers assoc k fn))

; setup default edn provider
(providers!
  :edn 
  (fn [service on-complete]
    (let [xhr (XhrIo.)
          url (str (:host service) (:path service))
          args (:args service)
          on-error (fn [res] (prn "on-error:: " rest) res) ]
      ;(println ":edn provider called with service: " service)
      (events/listen xhr goog.net.EventType.SUCCESS
                     (fn [e]
                       ;(prn "events/listen on-complete")
                       (on-complete (reader/read-string (.getResponseText xhr))
                                    {:load-state :success
                                     :load-error :nil})))
      (events/listen xhr goog.net.EventType.ERROR
                     (fn [e]
                       (on-error {:error (.getResponseText xhr)})))
      (. xhr
         (send url "GET" (when args (pr-str args))
               #js {"Content-Type" "application/edn" "Accept" "application/edn"}     
               )))))


(def base-sv {:stale-timeout 360 ; seconds
              :focus-get-timeout 0.8
              :provider [:edn]
              ;:load-state nil ; :wait :timeout :success :error
              ;:load-error nil
              })

(defn gen [opts & init-vec]
  "generate a service"
  (with-meta (vec init-vec)
             {:service (-> base-sv 
                           (merge opts))} ))

(defn autogen-routes [v]
  "takes a vector of route maps which may contain a :service key.
  If present, the :service value is used to generate a service.
  returns the route-v with the service meta."
  (mapv (fn [m]
          (let [service-m (:service m)]
            (if-not service-m
              m ; returns orig input
              (-> m
                  (dissoc :service)
                  (assoc :routes (gen service-m)))))) 
        v))


(defn run [app-state path path-meta ii]
  "takes a the metadata on a cursor, the path of the cursor in the app.
  returns new app state for the path. "
  ;(prn "service/run on path " path " with metadata " metadata )
  ;(prn "current providers atom: " @providers)
  (let [service (:service path-meta)
        ;provider-path (get-in path-meta [:service :provider])
        provider-fn (get-in @providers (:provider service))]
    ;(prn "load-state: " (:load-state path-meta))
    ;(prn service)
    ;(prn "aji.service/run " path (:service path-meta))
    ;(prn provider-fn)
    (provider-fn (:service path-meta)
                 (fn [res new-meta]
                   ;(prn "new-meta" new-meta)
                   (->> (mapv (fn [[i m]]
                                (let [m (if (= i ii) ; maintain current focal point  
                                          (assoc m :vi-focus true) m)
                                      ; merge in auto-route metadata
                                      routes (autogen-routes (:_routes m))
                                      m (dissoc m :_routes)
                                      m (if-not (empty? routes)
                                          (assoc m :routes routes) m)
                                      ]
                                  m))
                              (map-indexed vector res))
                        (#(with-meta % (merge path-meta new-meta)) )
                        (swap! app-state assoc-in path) )))))


