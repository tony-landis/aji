(ns aji.util
  (:require [clojure.string :refer [split]]))


(defn positions
  "Returns a lazy sequence containing the positions at which pred
  is true for items in coll.
  http://richhickey.github.io/clojure-contrib/seq-utils-api.html"
  [pred coll]
  (for [[idx elt] (map-indexed vector coll) :when (pred elt)] idx))


(defn path->keys [m v]
  "takes a path vector, looks up
  TODO - figure out how to deal with route-ids not :id? part of metadata?
  "
  (first 
    (reduce (fn [[acc m] pair]
              (let [m (get-in m pair)]
                [(conj acc (:id m)) 
                 m]))
            [[] m ] 
            (partition 2 v))))


(defn path->uri [m v]
  (apply str (interpose "/" 
                        (map name (path->keys m v)))))



(defn keys->path [m p]
  "walks the nodes of a map to find the full path to the given
  keys, traversing into sequential nodes when needed.  "
  (loop [remain p
         path []
         m m]
    (if (empty? remain)
      (into [] (flatten path)) ; return the path
      (let [find-key (first remain)
            is-key (get m find-key)
            pos (when-not is-key 
                  [(first (positions #(= find-key (:id %)) m)) find-key] )]
        (recur (rest remain)
               (conj path (or pos find-key))
               (if is-key
                 (get m find-key)
                 (nth m (first pos)) ))))))


(defn uri->path [m u]
  "takes a uri string, splits it into keys, 
  and converts the keys into a "
  (let [uv (into [] (map keyword (clojure.string/split u #"/")  ))]
    (keys->path m uv)))




(defn get-parent [m path]
  "get node above the closes vector "
  (let [root-path (take 1 path)] ; dont return nil 
    (loop [p (butlast path)]
      (if (empty? p)
        root-path 
        (let [v (get-in m p)]
          (if (vector? v)
            (or (butlast p) ; return path with key to current vector removed 
                root-path)
            (recur (butlast p))))))))




(defn walk-update-in [m pvs f & pred]
  "walks along path vector or seq pvs in obj m and calls update-in on every
  position along the path whose value passes optional predicate(s) pred."
  ;(prn m)
  (let [max-recur (count pvs)
        pred (if (empty? pred) '(identity true) pred)
        ]
    (loop [n 1
           m m]
      (let [p (take n pvs)]
        (do 
          ;(prn n p (get-in m p))
          (if (or (empty? p) (> n max-recur)) ; limit recursion
            m                                 ; return the map
            (recur (+ 1 n)                    ; inc pos
                   (if ((apply every-pred pred) (get-in m p))    ; pos passes predicate?
                     (do 
                       (prn :pred-match p)
                       (update-in m p f))     ; update! at this pos
                     m                        ; no change at this pos  
                     ))))))))



(defn find-pred-path [a pred base-path]
  "gets index, path, and value of the position
  matching pred at base-path in vector a"
  (let [i (first (positions pred (get-in a base-path)))
        path (conj base-path (or i 0)) 
        path-el (get-in a (butlast path)) ]
    (prn "find-pred-path:" i path)
    {:i i
     :path path
     :path-el path-el}))




; Om helpers, dom needed
;

(defn owner-path [owner]
  "get the path of the cursor "
  (let [cursor (.-__om_cursor (.-props owner))
        path (if cursor
              (.-path cursor)
              (do (prn "ERROR: aji.util/owner-path: no cursor for owner: " owner)
                  []))
        k (or (.-key (.-props owner)) [])]
    ;(prn  (into [] (flatten (conj path k))))
    (into [] (flatten (conj path k)))))
