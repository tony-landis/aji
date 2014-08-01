(ns bootstrap.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.browser.repl] ; needed for austin
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [cljs.core.async :refer [put! chan <! >! timeout]]
            [sablono.core :as html :refer-macros [html]]
            [ankha.core :as ankha]

            [aji.util :refer [path->uri owner-path ]]
            [aji.route :refer [key-controller
                               vi-focus?
                               vi-focus-in?
                               move-vi]]

            [bootstrap.app :refer [app-state]]

            ; for routing
            [goog.events :as events]
            [clojure.string :refer [split]])
  (:import goog.History
           goog.history.EventType
           goog.events.KeyHandler
           goog.events.KeyCodes))

;#_
(enable-console-print!)


(defn first-val [m ks]
  "get the value of the first matching key from seq ks in map m"
  (when (= om.core/MapCursor (type m))
    (try
      (val (first (select-keys m ks)))
      (catch js/Error e 
        (str "err " e)
        ))))


; Async helpers
;
(defn put-path! [ch owner]
  "put! path of the current cursor to chan ch"
  (let [path (owner-path owner)]
    (prn "put-path! " ch owner)
    (fn [] (put! ch path))))


; uri to key path 
(def route-ch  (chan))  ; location changes, for mocking/playback of location
(go (loop []
      (let [r (<! route-ch)
            path (mapv keyword (split r '/))]
        ;(prn "<! route-ch: " r path)
        ;(swap! app-state #(assoc % :focus-ns path))
        )
      (recur)))  


; token listener
(def history (History.))
(goog.events/listen history EventType.NAVIGATE #(go (>! route-ch (.-token %))))
(doto history (.setEnabled true))
;; http://docs.closure-library.googlecode.com/git/class_goog_History.html
;(doto history (.replaceToken token))


; key path to uri token
(def nav-ch (chan))  
(go (loop []
      (let [new-path (<! nav-ch)
            uri (path->uri @app-state new-path)]
        (move-vi app-state (:vi-path @app-state) new-path {})
        (doto history (.setToken uri))
        )
      (recur)))


; TODO  - redo
(def keyhandler (KeyHandler. js/document))
(goog.events/listen keyhandler KeyHandler.EventType.KEY 
  #(key-controller % app-state) )


; init a cursor pos
;
(put! nav-ch [:routes 1 :routes 0])





(defn quick-detail [app owner]
  "Need to add icons in the header to:
  - toggle open/close
  - un/freeze current focus (entity, help, etc)
  - show device state
  - change tab number if tab index != 1 "
  (reify 
    om/IRenderState 
    (render-state [_ state]
      (let [app-data  (get-in app (:vi-path app))   
           app-meta (or ;(meta (get-in app (butlast (:vi-path app))))
                        (meta (get-in app-data [:routes]))
                        nil
                        ) ]
        (html [:div 
               [:div {:id "quick-detail"
                      :class "open" } ; TODO
                [:a {:href "#TODO"}
                 [:span {:class "btn-sm glyphicon glyphicon-minus"}]]
                [:aside 
                 [:header [:h3 "Quick Detail"]]
                 [:section
                  [:p "Key Controls needed"]
                  [:ul
                   [:li "un/freeze current entity"]
                   [:li "toggle pane"]
                   ]]
                 [:section
                 
                  [:p {:style {:font-weight "bold"}} 
                   [:span ":vi-path "] 
                   (pr-str (:vi-path app)) ]
                  ]

                
                 (when app-meta
                   [:section
                    [:p (om/build ankha/inspector app-meta)]])

                 (when-not (or ;(:routes app-data) 
                               (= (:vi-path app) [:routes]) ; extremely slow, debugs entire app state!
                               ) 
                   [:section
                    [:p (om/build ankha/inspector (dissoc app-data :routes ))]])

                 ]] ])))))




(defn search-input [app owner]
  (reify 
    om/IInitState 
    (init-state [_] {:max-rs 10 })
    om/IRenderState 
    (render-state [this {:keys [max-rs to show-rs] :as state}]
      (let [ ]
        (html [:div {:id "search-container"
                     :style {} } ; TODO - toggle display
               [:div {:class "search-table"}
                [:div {:class "search-cell left"}
                 [:input {:type "text" 
                          :placeholder "search..." ; TODO - customize based on root selection
                          }]]
                [:div {:class "search-cell right"}
                 [:button {:type "reset" 
                           :on-click #(prn "TODO") }]]]])))))


(defn aside-menu-li [app owner]
  (reify 
    om/IRenderState 
    (render-state [_ _]
      (let [show-vi false ; TODO
            a-class (str (when (:vi-focus app)    "active ")
                         (when (:vi-focus-in app) "vi-parent ")
                         )
            i-class (str "glyphicons " (:r/glyph app)
                         (when (:vi-focus app) " white")) ] 
        (html [:li 
               [:a {:on-click #(put-path! nav-ch owner)
                    :class a-class }
                [:i {:class i-class} ]]
               (when show-vi
                 [:div {:class "nav-left-vi"} (str (:r/vi app))])
               ])))))




(defn menu-li [app owner]
  (reify
    om/IRender
    (render [_]
      (html [:li {:class (when (:vi-focus app) "active")
                  :on-click #(put-path! nav-ch owner)
                  :on-tap #(put-path! nav-ch owner) }
             [:a (first-val app [:text :name :web/uri-txt-en :id :db/ident])]
             (when (or (:routes app)
                       ;(:_links app) ; TODO - remove
                       )
               [:div {:style {:float "right"}}
                [:span {:class "glyphicon glyphicon-expand"
                        :style {:opacity 0.3}
                        }]
                ;[:span {:class "badge info"}  (count (:routes app))] 
                ])
             ]) )))


(defn menu-ul [app owner]
  (reify
    om/IInitState
    (init-state [_] {:max-rs 10})

    om/IRenderState
    (render-state  [this {:keys [max-rs to show-rs] :as state}] 
      (let [total-rs (count app)
            wide (some vi-focus? app)
            service (:service (meta app))  
            ]

        ;(prn "service: " service)

        (html
          [:div {:class "menu-ul" 
                 :style {:overflow "hidden"
                         :max-width (if wide "215px" "150px")} }
           [:div {:class "search-left"} ""]

           [:ul
            (om/build-all menu-li (take 20 app))]

           ; no results
           [:li {:class "active"
                 :style {:color "#888"
                         :display (when-not (>= 0 total-rs) "none")} } "No Results"]

           ; results exist
           [:div {:class "search-footer"
                  :style {:display (when (>= 0 total-rs) "none")} }
            (when (<= max-rs total-rs)
              [:span (str max-rs " of " )
               [:a {:href "#search-all"} total-rs]
               [:span " "]])
            ; send to list, group, ...
            (when to
              [:div {:class "btn-group"}
               (map #(html 
                       [:button {:type "button" 
                                 :class "btn btn-sm btn-default"
                                 :href "#/TODO" } 
                        [:span (name %)]]) to) ])]])))))


(defn aside-menu-ul [app owner]
  (reify
    om/IRender
    (render [_] 
      (html [:aside {:id "nav-left"}
             [:ul 
              (om/build-all aside-menu-li app)]]))))




(defn app-root [app owner]
  (reify 
    om/IRenderState 
    (render-state [_ state]
      (let [vi-path (:vi-path app)
            nav-path (if (> (count vi-path) 2)
                       (take (- (count vi-path) 2) vi-path)
                       vi-path) ]

        ;(prn vi-path nav-path)
        (html [:div
               [:div {:id "nav-left"}
                (om/build aside-menu-ul (:routes app))]
               [:section {:id "main"}
                (html [:header#main-search 
                       #_
                       (om/build search-input app)

                       (identity
                       (om/build menu-ul 
                                 (get-in app nav-path)
                                 {:fn :routes
                                  :state {:to #{:me :you :all} }})) ])
                
                #_
                (when pri-component
                  [:div {:style {:position :fixed :top 60 :left 60 :bottom 0 :overflow "scroll" }}
                   (om/build pri-component (pri-cursor app))])

                (om/build quick-detail app)]])))))

(om/root app-root app-state
         {:target (. js/document (getElementById "wrap"))})







; file://localhost/Users/tony/Downloads/glyphicons_pro/glyphicons/web/html_css/index.html
; https://news.ycombinator.com/item?id=7669836
; http://stackoverflow.com/questions/22883759/what-is-the-difference-between-application-state-and-component-local-state-in-cl
; https://www.firebase.com/blog/2014-05-01-using-firebase-with-react.html
; https://developer.chrome.com/extensions/messaging
;(prn (gdom/getElement "app"))
