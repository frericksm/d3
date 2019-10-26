(ns d3.nations-utils
(:require [cljsjs.d3]))

;; Various accessors that specify the four dimensions of data to visualize.
(defn x [d] (aget d "income"))
(defn y [d](aget d "lifeExpectancy"))
(defn radius [d] (aget d "population"))
(defn color [d] (aget d "region"))
(defn data-key [d] (aget d "name"))

;; A bisector since many nation's data is sparsely-defined.
(def  bisect  (-> js/d3
(.bisector (fn [d] (nth d 0)))
))

;; Defines a sort order so that the smallest dots are drawn on top.
(defn order [a b]
  (-  (radius b) (radius a)))


  ;; Positions the dots based on data.
(defn position [dot xScale yScale radiusScale] 
  (-> dot 
(.attr "cx" (fn [d] (xScale (x d))))
(.attr " cy" (fn [d] (yScale (y d))))
(.attr "r" (fn [d] (radiusScale (radius d))))))

  ;; Finds (and possibly interpolates) the value for the specified year.
(defn  interpolateValues [values year]
  (let [i ((aget bisect "left")  values year 0 (dec (count values)))
        a (nth values i)]
(if (> i 0 
(let [b ( nth values (dec i))
t (/ (- year (nth a 0)) (- (nth b 0) (nth a 0 )))]
(+ (* (nth a 1) (-1 t)) (* (nth b 1) t))))
(nth a 1))))


  ;; Interpolates the dataset for the given (fractional) year.
(defn interpolateData [nations year]

  (as-> (js->clj nations :keywordize-keys true) x 
(map (fn [{:keys [income population lifeExpectancy] :as d}]
       (-> d 
(assoc  :income (interpolateValues income year))
(assoc  :population  (interpolateValues population year))
(assoc  :lifeExpectancy  (interpolateValues lifeExpectancy year)))
) x)))

(defn displayYear [nations label box  dot year]

(-> dot
(.data (interpolateData nations year ) data-key)
(.call position )
(.sort order))
(.text label (Math/round year)))

(defn  enableInteraction 
  [svg nations  label box dot width overlay

] 
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


