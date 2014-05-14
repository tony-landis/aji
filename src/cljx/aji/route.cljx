(ns aji.route
  (:require ;[cljs.core.async :refer [put! chan <! >!]]
            [aji.util :refer [walk-update-in]]
            [aji.service])
  (:import  goog.events.KeyCodes))


(defmulti next-pos
  "takes a path in a vector or map vm and index positon or key ik,
  and returns the next element in the vector or map."
  (fn [a _ ] (type a)))
(defmethod next-pos :default [a _ ]
  (throw (js/Error. "I dont know the type " (type a) )))
(defmethod next-pos PersistentArrayMap [a b ])
(defmethod next-pos PersistentVector [v i]
  "vect and number"
  (if (>= i (- (count v) 1))
    i ; end of list
    (+ 1 i)))

(defmulti prev-pos
  "takes a path in a vector or map vm and index positon or key ik,
  and returns the next element in the vector or map."
  (fn [a _ ] (type a)))
(defmethod prev-pos :default [a _ ]
  (throw (js/Error. "I dont know the type " (type a) )))
(defmethod prev-pos PersistentArrayMap [a b ])
(defmethod prev-pos PersistentVector [v i ]
  "vect and number"
  (if (== 0 i) 
    0 ; top of list 
    (- i 1)))

;(next-pos {:k "1" :x "2"} :k )
;(next-pos [1,2,3,4,5] 0)


(defn is-child [a b] 
  (and (not= a b)
       (= a (into [] (take (count a) b)))))

(defn is-parent [b a] 
  (and (not= a b)
       (= a (into [] (take (count a) b)))) )

(defn is-sibling [a b]
  (and (not= a b)
       (= (butlast a)
          (butlast b))))

;(is-child   [:a 1 :b 2] [:a 1])
;(is-parent  [:a 1 :b 2] [:a 1])
;(is-sibling [:a 1 ] [:a 1])


(def vi-focus?    #(= true (:vi-focus %)))
(def vi-focus-in? #(= true (:vi-focus-in %)))


(def ENTER #{KeyCodes.ENTER})
(def ESC   #{KeyCodes.ESC})
(def LEFT  #{KeyCodes.H KeyCodes.LEFT})
(def DOWN  #{KeyCodes.J KeyCodes.DOWN})
(def UP    #{KeyCodes.K KeyCodes.UP})
(def RIGHT #{KeyCodes.L KeyCodes.RIGHT})




(defn move-vi [a path-a path-b {:keys [vi-cursor] :as opts}]
  (when (not= path-a path-b) 
    (let [path-b (if (< (count path-b) 2)
                   [:routes]
                   path-b) ;  min depth safety
          a-is-child   (is-child path-a path-b)
          a-is-sibling (is-sibling path-a path-b)
          a-is-parent  (is-parent path-a path-b)

          meta-b (meta (get-in @a (butlast path-b)))
          svc-b  (:service meta-b)
          ]
      ;(prn a-is-child a-is-parent a-is-sibling) 
      ;(when meta-b)
      (swap! a 
             (fn [x] 
               (-> ; set the new path
                   (assoc-in x [:vi-path] path-b)
                   ; path-a lost focus
                   (update-in path-a #(if-not (map? %) %
                                        (merge % {:vi-focus false
                                                  :vi-focus-in a-is-child})))
                   ; path-b has focus
                   (update-in path-b #(if-not (map? %) %
                                        (merge % {:vi-focus true
                                                  :vi-focus-in false})))
                   ; when paths do not share common ancestors,
                   ; walk all non-common ancestors and remove focus/-in
                   ((fn [x]
                      (if (or a-is-child a-is-sibling a-is-parent)
                        x ; do nothing: shared ancestry for a & b
                        (walk-update-in 
                          x path-a
                          #(merge % {:vi-focus false :vi-focus-in false })
                          map? ; pass over non-map nodes
                          #(some #{:vi-focus-in :vi-focus} (keys %)) ; pass over
                          ))))
                   ; handle services logic
                   ((fn [x]
                      (if-not svc-b x ; no service here
                        ; a service exists, reload meta
                        (assoc-in x (butlast path-b)
                                  (aji.service/run meta-b path-b) ))))
                   ))))))



; keyboard
(defn key-controller [e app-state]
  "document level keyhandler"
  (let [;el (.-target e)
        kc  [(.-keyCode e)]
        app @app-state
        vi-path (:vi-path app)]

    (when (or (some LEFT kc)
              (some ESC kc))
      (let [new-path (take (- (count vi-path) 2) vi-path)]
        ;(prn "LEFT  old / new path : "  path  " / "new-path)
        (move-vi app-state vi-path new-path {})))
    
    (when (or (some RIGHT kc)
              (some ENTER kc))
      (let [new-path (into [] (flatten [vi-path :routes]))
            svc (:service (meta (get-in app new-path)))
            ] ; TODO - data level specification how to move right
        (if (or svc 
                (> (count (get-in app new-path)) 0)) ; check for children
          ;(prn "RIGHT old / new path : "  path  " / "new-path)
          (move-vi app-state vi-path (conj new-path 0) {}))))

    (when (some DOWN kc)
      (let [last-i (last vi-path) 
            last-el-path (or (butlast vi-path) vi-path)
            last-el (get-in app last-el-path)
            ni (if-not (number? last-i) 0 (next-pos last-el last-i))
            new-path (into [] (flatten [last-el-path ni])) ]
        ;(prn "DOWN >> next-i : " (last vi-path) " path: " new-path)
        (move-vi app-state vi-path new-path {})))

    (when (some UP kc)
      (let [last-i (last vi-path) 
            last-el-path (or (butlast vi-path) vi-path)
            last-el (get-in app last-el-path)
            ni (if-not (number? last-i) 0 (prev-pos last-el last-i))
            new-path (into [] (flatten [last-el-path ni])) ]
        ;(prn "UP >> next-i : " ni " path: " new-path)
        (move-vi app-state vi-path new-path {} )))
    
    

    ;(.preventDefault e)
    ))













; http://docs.closure-library.googlecode.com/git/class_goog_events_KeyHandler.html


; data cursor - a point in app data state
;
; component cursor - focal point in the view tree,
;                  contains behavior defiinitions for vim command mode
;
; command dom node/cell? - will recieve cursor focus in vim edit mode,
;                        will be highlighted in vim command or visual mode
;                       defines behavior for command mode, ie, ct_, yy, etc.
;                      textareas could support full vim commands.
;
; visual cursor(s) - selected dom nodes / data cells? in vim visual mode.
;                 control+click should allow multi-cell selection.
;                component cursor can define addional behaviors for multi-select,
;              for example a table header could support delete to hide a column,
;            multiple table cells (td) could support multi replace, update, delete,
;          or send to [collection, buffer, tab, device, party, googdocs, analytic, etc].
