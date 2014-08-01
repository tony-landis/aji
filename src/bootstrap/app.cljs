(ns bootstrap.app
  (:require [aji.service :refer [gen providers!]]
            [cljs.reader :as reader]
            [goog.events :as events])
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

;#_
(enable-console-print!)


(def app-state (atom {}))



(defn gen-service-cls [cls & args]
  (gen 
    (merge {:provider [:edn]
            :host "http://dev.api1.anywhere.com:8080"
            :path (str "/c/cls/" cls)
            } (first args))))



; services
(def hotels     (gen-service-cls "hotel"))
(def tours      (gen-service-cls "tour"))
(def transports (gen-service-cls "transport"))
(def flights    (gen-service-cls "flight"))
(def rentals    (gen-service-cls "rental"))
(def service-routes
  {:id :usr    
   :r/glyph "tag"
   :routes [{:id :cls/hotel :text "Hotel" :routes hotels}
            {:id :cls/tour :text "Tour" :routes tours} 
            {:id :cls/transport :text "Transport" :routes transports} 
            {:id :cls/flight :text "Flight" :routes flights}
            {:id :cls/rental :text "Rental"  :routes rentals} ]})


; org 
(def orgs   (gen-service-cls "org"))
(def users  (gen-service-cls "usr"))
(def staffs (gen-service-cls "staff"))
(def org-routes
  {:id :usr    
   :r/glyph "group"
   :routes [{:id :cls/org :text "Orgs" :routes orgs}
            {:id :cls/staff :text "Staff" :routes staffs} 
            {:id :cls/usr :text "Users" :routes users} 
            {:id :lists :text "Lists"}
            {:id :recent :text "Recent" }
            {:id :buffers :text "Buffers" }
            {:id :dashboard :text "Dashboard" } ]})


; cms / locations
(def countries    (gen-service-cls "country"
                                   { :this :that
                                    :them :us }))

(def regions      (gen-service-cls "region"))
(def destinations (gen-service-cls "dest"))
(def attractions  (gen-service-cls "attr"))
(def routes       (gen-service-cls "route"))
(def animals      (gen-service-cls "animal"))
(def webpages     (gen-service-cls "web"))
;(def reviews      (gen-service-cls "review"))
(def airports     (gen-service-cls "airport"))
(def cms-routes
  {:id :cms 
   :r/glyph "globe" 
   :routes [{:id :cls/country :text "Countries" :routes countries }
            {:id :cls/region  :text "Regions" :routes regions}
            {:id :cls/attr    :text "Attractions" :routes attractions}
            {:id :cls/dest    :text "Destinations" :routes destinations}
            {:id :cls/route   :text "Routes" :routes routes}
            {:id :cls/animal  :text "Animals" :routes animals}
            {:id :cls/airport :text "Airports" :routes airports} 
            ;{:id :cls/review  :text "Review" :routes reviews} 
            {:id :cls/web     :text "Pages" :routes webpages} 
            ]})

(def tags
  (gen-service-cls "tag"))

; init app state
(reset! app-state 
        {:vi-path [:routes]
         :routes [{:id :search :r/glyph "search" :vi/key 's } 
                  cms-routes
                  service-routes
                  org-routes
                  {:id :pref :r/glyph "cogwheel" :vi/key 'p
                   :routes [{:id :cls/tag 
                             :text "Tags" 
                             :routes tags}
                            {:id :inspect :vi 'i :text "Inspect App State" }
                            {:id :tests   :vi 'u :text "Unit Tests" }
                            {:id :history :vi 't :text "Time Machine" }
                            {:id :schemas :vi 's :text "Schemas" } ]} ]})




;:msg    {:r/glyph "user" :r/vi 'm :r/tabindex 4 }
;:msg    {:r/glyph "conversation" :r/vi 'm :r/tabindex 4 }
;:team   {:r/glyph "group"        :r/vi 't :r/tabindex 5 }
;:org    {:r/glyph "share_alt"    :r/vi 'm :r/tabindex 6 }
;:loc    {:r/glyph "globe"        :r/vi 'p :r/tabindex 7 }
;:col    {:r/glyph "list"         :r/vi 'l :r/tabindex 9 }
;:tag    {:r/glyph "tag"          :r/vi 'r :r/tabindex 10 }
;:acct   {:r/glyph "money"        :r/vi 'a :r/tabindex 11 }
;:stat   {:r/glyph "charts"       :r/vi 'z :r/tabindex 12 }


; TODO - post processing to apply services to loaded data
; this should be possible service side as well.
; (mapv #(merge % {:routes country-routes}) countries)

#_
(def country-routes
  ;^{:service mock-svc}
  [{:id :regions :text "Dashboard"} 
   {:id :regions :text "Regions"}
   {:id :destinations :text "Destinations"}
   {:id :attractions :text "Attractions"} ])
