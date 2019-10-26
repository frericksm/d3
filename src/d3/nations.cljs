(ns d3.nations
  (:require [cljsjs.d3]
            [clojure.string :as str]
[d3.nations-utils :as utils]))
(enable-console-print!)

;;Chart dimensions.
(def  margin  {:top 19.5 :right 19.5 :bottom 19.5 :left 39.5})
(def width (- 960 (:right margin)))
(def height (- 500 (:top margin) (:bottom margin)))

;; Various scales. These domains make assumptions of data, naturally.

(def xScale (-> js/d3
(.scaleLog #js [300, 1e5] #js[0, width])))
(def yScale (-> js/d3
(.scaleLinear #js [10 85] #js[height 0 ])))
(def radiusScale (-> js/d3
(.scaleSqrt #js [0 5e8] #js[0 40 ])) )
(def colorScale (-> js/d3
                    (.scaleOrdinal #js [0 10] (aget js/d3 "schemeCategory10" ))))

;; The x & y axes.
(def xAxis  (-> js/d3
( .axisBottom xScale)
;;( .scale xScale)
(.tickFormat "d")
#_(.ticks 12 (.format js/d3 ",d"))))

(def yAxis  (-> js/d3
( .axisLeft yScale)))

;; Create the SVG container and set the origin.

(def svg (-> js/d3
(.select "#chart")
(.append "svg")
(.attr "width" (+ width (:left margin) (:right margin)))
(.attr "height" (+ height (:top margin) (:bottom margin)))
(.append "g")
(.attr "transform" (str "translate(" (:left margin) "," (:top margin) ")"))))

;; Add the x-axis.
(-> svg
(.append "g")
(.attr "class" "x axis" )
(.attr "transform" (str "translate (0" "," height  ")"))
(.call xAxis))

;; Add the y-axis.
(-> svg
(.append "g")
(.attr "class" "y axis" )
(.call yAxis))

;; Add an x-axis label.
(-> svg
(.append "text")
(.attr "class" "x label")
( .attr "text-anchor" "end")
( .attr "x" width)
( .attr"y" (- height 6))
( .text"income per capita, inflation-adjusted (dollars)")
)

;; Add a y-axis label.
(-> svg
(.append "text")
(.attr "class" "y label")
( .attr "text-anchor" "end")
( .attr "y" 6)
( .attr"dy" ".75em")
( .attr"transform" "rotate(-90)")
( .text"life expectancy (years)")  
)
;; Add the year label; the value is set on transition.
(def label
(-> svg
(.append "text")
(.attr "class" "year label")
(.attr "text-anchor" "end")
(.attr "x" width)
(.attr "y" (- height 24))
(.text 1800)))

(def box (-> label (.node) (.getBBox)))

;; After the transition finishes, you can mouseover to change the year.

(def overlay (-> svg
(.append "rect")
(.attr "class" "overlay")
  (.attr "x" (aget box "x"))
  (.attr "y" (aget box "y"))
  (.attr "width" (aget box "width"))
  (.attr "height" (aget box "height"))
  (.on "mouseover" utils/enableInteraction)))





(defn load-data [nations]
  ;; Add a dot per nation. Initialize the data at 1800, and set the colors.

  (let [dot (-> svg 
(.append "g")
(.attr "class" "dots")
(.selectAll ".dot")
(.data (utils/interpolateData nations 1800))
(.enter)
(.append "circle")
(.attr "class" "dot")
(.style "fill" ( fn [d] (.ordinal colorScale d )))
(.call utils/position)
(.sort utils/order))]
;; Add a title.
  (-> dot
(.append "title" )
(.text (fn [d] (aget d "name"))))
)
)





(defn year [year] 
  (.interpolateNumber js/d3 1800 2009))

  ;; Tweens the entire chart by first tweening the year, and then the data.
  ;; For the interpolated data, the dots and label are redrawn.
(defn  tweenYear [nations label box dot ]
  (fn [t] (utils/displayYear nations label box dot (year t))))
  ;; Updates the display to show the specified year.







;; Start a transition that interpolates the data based on year.

#_(-> svg
(.transition)
(.duration 30000)
(.ease (aget js/d3 "easeLinear"))
(.tween "year" tweenYear)
(.each "end" enableInteraction))


;; Entry
;; Load the data.

(-> js/d3
(.json "nations.json" load-data ))
