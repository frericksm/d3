(ns d3.core
  (:require [cljsjs.d3]
            [clojure.string :as str]))

(enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; data

(def data {2005 {:natural-gas 0.2008611514256557
                 :coal        0.48970650816857986
                 :nuclear     0.19367190804075465
                 :renewables  0.02374724819670379}
           2015 {:natural-gas 0.33808321253456974
                 :coal        0.3039492492908485
                 :nuclear     0.1976276775179704
                 :renewables  0.08379872568702211}})
(def height 450)
(def width 540)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; helpers
(def height-scale
  (-> js/d3
      (.scaleLinear)
      (.domain #js [0 1])
      (.range #js [(- height 15) 0])))
(defn format-percent [value]
  ((.format js/d3 ".2%") value))

(defn format-name [name-str]
  (-> name-str
      (str/replace "-" " ")
      (str/capitalize)))
(defn attrs [el m]
  (doseq [[k v] m]
    (.attr el k v)))

(defn data-join [parent tag class data]
  (let [join  (-> parent
                  (.selectAll (str tag "." class))
                  (.data (into-array data)))
        enter (-> join
                  (.enter)
                  (.append tag)
                  (.classed class true))]
    (-> join (.exit) (.remove))
    (.merge join enter)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; draw functions
(defn append-svg []
  (-> js/d3
      (.select "#slopegraph")
      (.append "svg")
      (.attr "height" height)
      (.attr "width" width)))


(defn remove-svg []
  (-> js/d3
      (.selectAll "#slopegraph svg")
      (.remove)))


(def column-1-start (/ width 4))
(def column-space (* 3 (/ width 4)))

(defn draw-header [svg years]
  (-> svg
      (data-join "text" "slopegraph-header" years)
      (.text (fn [data _] (str data)))
      (attrs {"x" (fn [_ index]
                    (+ 50 (* index column-space)))
              "y" 15})))

(defn draw-line [svg data-col-1 data-col-2]
  (-> svg
      (data-join "line" "slopegraph-line" data-col-1)
      (attrs {"x1" (+ 5 column-1-start)
              "x2" (- column-space 5)
              "y1" (fn [[_ v]]
                     (height-scale v))
              "y2" (fn [[k _]]
                     (height-scale (get data-col-2 k)))})))

(defn draw-column [svg data-col index custom-attrs]
  (-> svg
      (data-join "text" (str "text.slopegraph-column-" index) data-col)
      (.text (fn [[k v]] (str (format-name (name k)) " " (format-percent v))))
      (attrs (assoc custom-attrs
                    "y" (fn [[_ v]] (height-scale v))))))

(defn draw-slopegraph [svg data]
  (let [data-2005 (get data 2005)
        data-2015 (get data 2015)]

    (draw-column svg data-2005 1 {"x" column-1-start})
    (draw-column svg data-2015 2 {"x" column-space})

    (draw-header svg [2005 2015])
    (draw-line svg data-2005 data-2015)))

,,,

(defn ^:export main []
  (let [svg (append-svg)]
    (draw-slopegraph svg data)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Lifecycle



(defn ^:export main []
  (remove-svg)
  (let [svg (append-svg)]
    (draw-slopegraph svg data)))

(main)
