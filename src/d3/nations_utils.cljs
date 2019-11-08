(ns d3.nations-utils
(:require [cljsjs.d3]))
(enable-console-print!)
;; Various accessors that specify the four dimensions of data to visualize.
#_(defn x [d] (aget d "income"))
#_(defn y [d](aget d "lifeExpectancy"))
#_(defn radius [d] (aget d "population"))
#_(defn color [d] (aget d "region"))
#_(defn data-key [d] (aget d "name"))

(defn to-clj [d]
  (js->clj d :keywordize-keys true))
(defn x [d] (get  (to-clj d) :income))
(defn y [d](get (to-clj d) :lifeExpectancy))
(defn radius [d] (get (to-clj d) :population))
(defn color [d] (get (to-clj d) :region))
(defn data-key [d] (get (to-clj d) :name))

;;Chart dimensions.
(def  margin  {:top 19.5 :right 19.5 :bottom 19.5 :left 39.5})
(def width (- 960 (:right margin)))
(def height (- 500 (:top margin) (:bottom margin)))

;; Various scales. These domains make assumptions of data, naturally.

(def xScale (-> js/d3
(.scaleLog #js [300, 100000] #js[0, width])))
(def yScale (-> js/d3
(.scaleLinear #js [10 85] #js[height 0 ])))
(def radiusScale (-> js/d3
(.scaleSqrt #js [0 5e8] #js[0 40 ])) )
(def colorScale (-> js/d3
                    (.scaleOrdinal #js [0 10] (aget js/d3 "schemeCategory10" ))))


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
(-> dot
(.attr "cx" (fn [d] (let [income (x d)
                          result (xScale income )]
result)))
(.attr "cy" (fn [d]  (yScale (y d))))
(.attr "r" (fn [d]  (radiusScale (radius d))))))

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
  (let [new-income (interpolateValues income year)
        new-population (interpolateValues population year)
        new-lifeExpectancy (interpolateValues lifeExpectancy year)]
  (as-> {} x
(assoc x :name (data-key d))
(assoc x :region (color d))
(assoc x  :income new-income)
  (assoc x  :population  new-population )
  (assoc x  :lifeExpectancy new-lifeExpectancy )
(clj->js x)
(debug "interpolate-datum" x))))

  ;; Interpolates the dataset for the given (fractional) year.
(defn  interpolateData [nations year]
(as-> nations x
(js->clj x :keywordize-keys true)
  (map (partial interpolate-datum year) x)
(clj->js x)))

(defn displayYear [nations label box  dot year]

(-> dot
(.data (interpolateData nations year ) data-key)
(.call position)
(.sort order))
(.text label (.round js/Math year)))

(defn  enableInteraction 
  [svg nations  label box dot width overlay] 
  (let [yearScale (-> js/d3
(.scaleLinear #js [1800 2009] #js[(+ (x box) 10 ) (+ (x box) (width box) -10)])
(.clamp true))
        onmouseover (fn [] (.classed label "active" true))
        onmouseout (fn [] (.classed label "active" false))
        onmousemove (fn [] 
                    (this-as this
(displayYear nations label box dot (.invert yearScale (nth  (.mouse js/d3 this) 0)))))]
;; Cancel the current transition, if any.
(-> svg
(.transition)
(.duration 0))


(-> overlay
(.on "mouseover" onmouseover)
(.on "mouseout" onmouseout)
(.on "mousemove" onmousemove)
(.on "touchmouse" onmousemove))
))

(defn year [t] 
  (let [a 1800
        b 2009 
        diff (- b a)]
 (+ a (* t diff))))

  ;; Tweens the entire chart by first tweening the year, and then the data.
  ;; For the interpolated data, the dots and label are redrawn.
(defn  tweenYear [nations label box dot]
  (fn [t] (displayYear nations label box dot (year t))))


