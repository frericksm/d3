
(ns d3.nations
  (:require [cljsjs.d3]
            [clojure.string :as str]
            [d3.nations-utils :as utils]))
(enable-console-print!)


;; The x & y axes.
(def xAxis  (-> js/d3
                ( .axisBottom utils/xScale)
                ( .scale utils/xScale)
                #_(.tickFormat "d")
                (.ticks 12 (.format js/d3 ",d"))))

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
    ( .attr "y" (- utils/height 6))
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
      (.text utils/start-year)))

(def box (-> label (.node) (.getBBox)))

;; After the transition finishes, you can mouseover to change the year.

(def overlay (-> svg
                 (.append "rect")
                 (.attr "class" "overlay")
                 (.attr "x" (.-x box))
                 (.attr "y" (.-y box ))
                 (.attr "width" (.-width box ))
                 (.attr "height" (.-height box ))
                 #_(.on "mouseover" utils/enableInteraction) ;; TODO whichcalls to utils/enableInteraction to activate? 
                 ))




(defn load-data [nations]
  ;; Add a dot per nation. Initialize the data at utils/start-year (e.g 1800), and set the colors.

  ;;TODO verify: is easing working?
  (let [elin (.-easeLinear  js/d3)
        data  (utils/interpolateData nations utils/start-year )
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
    ;; Add handler to overlay

    #_(.on overlay "mouseover" (partial utils/enableInteraction svg nations label box dot utils/width overlay)) ;; TODO which calls to utils/enableInteraction to activate?

    ;; Add a title.
    (-> dot
        (.append "title" )
        (.text utils/data-key))

    ;; Start a transition that interpolates the data based on year.
    (-> svg
        (.transition)
        (.duration 3000);; TODO 30000
        (.ease elin) 
        (.tween "year" (partial utils/tween-year nations label box dot))
        ;; see https://stackoverflow.com/questions/45831942/tweening-numbers-in-d3-v4-not-working-anymore-like-in-v3        
        ;; see https://github.com/d3/d3-transition#transition_tween
        #_(.on "end" (partial utils/enableInteraction svg nations label box dot utils/width overlay)) ;;TODO which calls to utils/enableInteraction to activate?
        )))
;; Updates the display to show the specified year.
;; Entry
;; Load the data.


(-> js/d3
    (.json "nations.json" load-data ))
