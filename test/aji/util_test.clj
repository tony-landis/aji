(ns aji.util-test
  (:use midje.sweet)
  (:require [clojure.test :refer :all]
            [aji.util :as util]))

;(contains :tag/tag) 
;col =not=> (contains :tag/fakie) 

(def app {:routes [{:id :test-a
                    :routes [{:id :test-a1}
                             {:id :test-a2}]}
                   {:id :test-b
                    :routes [{:id :test-b1}
                             {:id :test-b2}]} ]})

(facts "about path->keys"
       (let []
         (util/path->keys app [:routes 0 :routes 0]) => [:test-a :test-a1] 
         (util/path->keys app [:routes 1 :routes 1]) => [:test-b :test-b2] 
         ))

(facts "about path->uri"
       (let []
         (util/path->uri app [:routes 0 :routes 0]) => "test-a/test-a1" 
         (util/path->uri app [:routes 1 :routes 1]) => "test-b/test-b2" 
         ))

(facts "about get-parent"
       (let []
         (util/get-parent app [:routes 0 :routes 0]) => '(:routes 0)
         (util/get-parent app [:routes 1 :routes 1]) => '(:routes 1)
         ))

(facts "about walk-update-in"
       (let []
         (util/walk-update-in app 
                              [:routes 0 :routes 1]
                              #(merge % {:test true})
           map?) => (-> app
                        (update-in [:routes 0] #(merge % {:test true}))
                        (update-in [:routes 0 :routes 1] #(merge % {:test true}))
                        )))

(facts "about find-pred-path"
       ; is this actually being used?
       )

(facts "about owner-path"
       ; this is clojurescript & needs a react component instance
       )
