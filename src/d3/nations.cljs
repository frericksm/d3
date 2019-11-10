(ns d3.nations
  (:require [cljsjs.d3]
            [clojure.string :as str]
            [d3.nations-utils :as utils]))
(enable-console-print!)



;; The x & y axes.
(def xAxis  (-> js/d3
                ( .axisBottom utils/xScale)
                ;;( .scale xScale)
                (.tickFormat "d")
                #_(.ticks 12 (.format js/d3 ",d"))))

(def yAxis  (-> js/d3
                ( .axisLeft utils/yScale)))

;; Create the SVG container and set the origin.

(def svg (-> js/d3
             (.select "#chart")
             (.append "svg")
             (.attr "width" (+ utils/width (:left utils/margin) (:right utils/margin)))
             (.attr "height" (+ utils/height (:top utils/margin) (:bottom utils/margin)))
             (.append "g")
             (.attr "transform" (str "translate(" (:left utils/margin) "," (:top utils/margin) ")"))))

;; Add the x-axis.
(-> svg
    (.append "g")
    (.attr "class" "x axis" )
    (.attr "transform" (str "translate (0" "," utils/height  ")"))
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
    ( .attr "x" utils/width)
    ( .attr"y" (- utils/height 6))
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
      (.attr "x" utils/width)
      (.attr "y" (- utils/height 24))
      (.text 1800)))

(def box (-> label (.node) (.getBBox)))

;; After the transition finishes, you can mouseover to change the year.

(def overlay (-> svg
                 (.append "rect")
                 (.attr "class" "overlay")
                 (.attr "x" (aget box "x"))
                 (.attr "y" (aget box "y"))
                 (.attr "width" (.-width box ))
                 (.attr "height" (.-height box ))
                 #_(.on "mouseover" utils/enableInteraction) ;; TODO
                 ))




(defn load-data [nations]
  ;; Add a dot per nation. Initialize the data at 1800, and set the colors.

  (let [elin (.-easeLinear  js/d3)
        data  (utils/interpolateData nations 1800)
        dot 
        (-> svg
            (.append "g")

            (.attr "class" "dots")
            (.selectAll ".dot")
            (.data data)
            (.enter)
            (.append "circle")
            (.attr "class" "dot")
            (.style "fill" ( fn [d]
                            (let [c (utils/color d)
                                  c-scaled (utils/colorScale (utils/color d))] c-scaled)))
            (.call utils/position)
            (.sort utils/order))]


    ;; Add a title.
    (-> dot
        (.append "title" )
        (.text utils/data-key))

    ;; Start a transition that interpolates the data based on year.
    (-> svg
        (.transition)
        (.duration 30000)
        (.ease elin)
        (.tween "year" (partial utils/tweenYear nations label box dot ))
        (.on "end" (partial utils/enableInteraction svg nations label box dot utils/width overlay))
        )))
  ;; Updates the display to show the specified year.
  ;; Entry
  ;; Load the data.


  (-> js/d3
      (.json "nations.json" load-data ))
