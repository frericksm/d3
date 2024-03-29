(ns d3.nations-utils
  (:require [cljsjs.d3]))
(enable-console-print!)
;; Various accessors that specify the four dimensions of data to visualize.
(defn to-clj [o]
  
  (if (object? o)
    (js->clj o :keywordize-keys true)
    o)
  )
(defn x [d] (get (to-clj d) :income))
(defn y [d](get (to-clj d) :lifeExpectancy))
(defn radius [d] (get (to-clj d) :population))
(defn color [d] (get (to-clj d) :region))
(defn data-key [d] (get (to-clj d) :name))

(def start-year 1850)
(def end-year 1870);; TODO 2009

;;Chart dimensions.
(def  margin  {:top 19.5 :right 19.5 :bottom 19.5 :left 39.5})
(def width (- 960 (:right margin)))
(def height (- 500 (:top margin) (:bottom margin)))

;; Various scales. These domains make assumptions of data, naturally.

(def xScale (-> js/d3
                
                (.scaleLog)
                (.domain #js [300, 1e5])
                (.range #js [0 width] )))
(def yScale (-> js/d3
                (.scaleLinear)
                (.domain #js [10 85])
                (.range  #js [height 0])
                )) 
(def radiusScale (-> js/d3
                     (.scaleSqrt)
                     (.domain #js [0 5e8])
                     (.range #js [0 40])))
(def colorScale (-> js/d3
                    (.scaleOrdinal)
                    (.range (aget js/d3 "schemeCategory10" ))))

(defn yearScale [box] 
  (-> js/d3
      (.scaleLinear)
      (.domain #js[start-year end-year])
      (.range #js[(+ (.-x box) 10 ) (+ (.-x box) (.-width box) -10)])

      (.clamp true)))

;; A bisector since many nation's data is sparsely-defined.
(def  bisect  (-> js/d3
                  (.bisector (fn [d] (nth d 0)))
                  ))

;; Defines a sort order so that the smallest dots are drawn on top.
(defn order [a b]
  (-  (radius b) (radius a)))

(defn debug [msg obj]
  (println msg obj)
  obj)
;; Positions the dots based on data.
(defn position [dot]

  (as-> dot v  
    #_(debug (str "position:x:" (.attr v "cx")) v)
    (.attr v "cx" (fn [d] (xScale (x d))))
    (.attr v "cy" (fn [d]  (yScale (y d))))
    (.attr v "r" (fn [d]  (radiusScale (radius d))))))


;; Finds (and possibly interpolates) the value for the specified year.
(defn  interpolateValues [values year]
  (let [i ((aget bisect "left")  values year 0 (dec (count values)))
        a (nth values i)
        b (if (> i  0 )( nth values (dec i)))
        t (if (not (nil? b)) (/ (- year (nth a 0)) (- (nth b 0) (nth a 0 ))))
        result (if (not (nil? t))(+ (* (nth a 1) (-1 t)) (* (nth b 1) t))
                   (nth a 1))]
    result))


;;

(defn interpolate-datum [year {:keys [ income population lifeExpectancy] :as d}]
  (debug "interpolate-datum:year:" year)
  (let [new-income (interpolateValues income year)
        new-population (interpolateValues population year)
        new-lifeExpectancy (interpolateValues lifeExpectancy year)]
    (as-> {} x
      (assoc x :name (data-key d))
      (assoc x :region (color d))
      (assoc x  :income new-income)
      (assoc x  :population  new-population )
      (assoc x  :lifeExpectancy new-lifeExpectancy )
      (debug "interpolate-datum" x)
      (clj->js x)
      )))

;; Interpolates the dataset for the given (fractional) year.
(defn  interpolateData [nations year]
  (as-> nations x
    (js->clj x :keywordize-keys true)
    (map (partial interpolate-datum year) x)
    (clj->js x)))

(defn display-year [nations label box  dot year]
  #_(println year)
  (-> dot
      (.data (interpolateData nations year) data-key)
      (.call position)
      (.sort order))
  (.text label (.round js/Math year)))

(defn onmouseover-factory [label] (.classed label "active" true))
(defn onmouseout-factory [label] (.classed label "active" false))
(defn onmousemove-factory [nations label box dot]
  (fn []
    (this-as this
      (let [m (.mouse js/d3 this)
            ys (yearScale box)]
        (display-year nations label box dot (.invert ys (nth  m  0)))))))
(defn  enableInteraction 
  [svg nations  label box dot width overlay] 
  ;; Cancel the current transition, if any.
  (-> svg
      (.transition)
      (.duration 0))


  (let [onmousemove (onmousemove-factory nations label box dot)
        onmouseover (onmouseover-factory label)
        onmouseout (onmouseout-factory label)]
    (-> overlay
        (.on "mouseover" onmouseover)
        (.on "mouseout" onmouseout)
        (.on "mousemove" onmousemove)
        (.on "touchmouse" onmousemove))
    ))

(defn year [t] 
  (let [a start-year
        b end-year
        diff (- b a)]
    (+ a (* t diff))))

;; Tweens the entire chart by first tweening the year, and then the data.
;; For the interpolated data, the dots and label are redrawn.
(defn  tween-year [nations label box dot]
  (this-as node
    (fn [t] 
      (println "tweenYear:t:" t)
      (display-year nations label box dot (year t)))))
