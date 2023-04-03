#!/usr/bin/env bb

(require '[clojure.tools.cli :refer [parse-opts]]
         '[clojure.pprint :refer [pprint]]
         '[clojure.data.csv :as csv]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

;;;; Default values

(def default-destination "./converted.csv")

(def supported-formats
  {"https://mtg.dragonshield.com" "DragonShield",
   "https://www.mtggoldfish.com" "Goldfish"})

(def supported-formats-help
  (str
    "\nSupported formats: \n"
    (with-out-str
     (pprint supported-formats))))

;;;; CLI

(def cli-options
  [["-s" "--source format" "Format of the source file you have already generated."]
   ["-r" "--result format" "Format you want convert to."]
   ["-f" "--file path" "Path to source file."]
   ["-d" "--dest path" "Path to the resulting converted file."
    :default default-destination]
   ["-h" "--help"]])

(defn error-msg [errors]
  (str "Something went wrong:\n\n" (str/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))


(defn validate-args
  [args]
  (let [{:keys [options errors summary]} (parse-opts args cli-options)
        {:keys [source result file dest help]} options
        summary (str/join \newline [summary supported-formats-help])]
    (cond
      help
      {:exit-message summary :ok? true}
      errors
      {:exit-message (error-msg errors)}
      (nil? source)
      {:exit-message (error-msg ["You didn't provide the format of source."])}
      (not (contains? (set (vals supported-formats)) source))
      {:exit-message (error-msg [(str "Format " source " is not supported.")
                                 "Use --help flag to see formats and corresponding names."])}
      (nil? result)
      {:exit-message (error-msg ["You didn't provide the format of result."])}
      (not (contains? (set (vals supported-formats)) result))
      {:exit-message (error-msg [(str "Format " result " is not supported.")
                                 "Use --help flag to see formats and corresponding names."])}
      (nil? file)
      {:exit-message (error-msg ["You didn't provide the source file."])}
      :else 
      {:source source
       :result result
       :file file
       :dest dest})))

;;;; CSV Utils

(defn read-csv
  [file]
  (with-open [reader (io/reader file)]
    (let [content
          (doall
            (csv/read-csv reader))]
      (if (re-matches #"sep=." (first (first content)))
        (rest content)
        content))))

(defn read-column [filename column-index]
  (with-open [reader (io/reader filename)]
    (let [data (read-csv reader)]
      (mapv #(nth % column-index) data))))

(defn columns->rows
  [data]
  (vec
    (for [idx (range 0 (count (first data)))]
      (mapv
        (fn [column]
          (get column idx ""))
        data))))

;;;; Conversion logic

(defn get-columns-mapping
  [source result]
  (condp = [source result]
    ["DragonShield" "Goldfish"]
    {"Card Name" "Card"
     "Set Code"  "Set ID"
     "Set Name"  "Set Name"
     "Quantity"  "Quantity"
     "Printing"  "Foil"
     ;; XXX this column is incompatable and can't be adequately replaced
     "Condition" "Variation"}

    ["Goldfish" "DragonShield"]
    {"Card" "Card Name"
     "Set ID"  "Set Code"
     "Set Name"  "Set Name"
     "Quantity"  "Quantity"
     "Foil"  "Printing"
     ;; XXX this column is incompatable and can't be adequately replaced
     "Variation" "Condition"}))

(defn get-columns-conversion-fns
  [source result]
  (condp = [source result]
    ["DragonShield" "Goldfish"]
    {["Printing" "Foil"]
     (fn [data]
       (case data
         "Normal" "REGULAR"
         "Foil"   "FOIL"
         "REGULAR"))

     ["Condition" "Variation"]
     (constantly "")}

    ["Goldfish" "DragonShield"]
    {["Foil" "Printing"]
     (fn [data]
       (case data
         "REGULAR" "Normal"
         "FOIL"   "Foil"
         "Normal"))

     ["Variation" "Condition"]
     (constantly "")}))

(defn get-conversion-meta
  [source result]
  {:columns-mapping (get-columns-mapping source result)
   :columns-conversion-fns (get-columns-conversion-fns source result)})

(defn process-conversion
  [source result file]
  (let [source-data (read-csv file)
        source-columns (first source-data)
        {:keys [columns-mapping columns-conversion-fns]}
        (get-conversion-meta source result)]
    (->> (count source-columns)
         (range 0)
         (keep
           (fn [col-idx]
             (let [column (read-column file col-idx)
                   column-name (first column)
                   corresponding-column-name (get columns-mapping column-name)
                   conversion-fn (get columns-conversion-fns
                                      [column-name corresponding-column-name]
                                      ;; or
                                      identity)]
               (when (some? corresponding-column-name)
                 (vec
                   (concat [corresponding-column-name]
                           (map #(conversion-fn %) (rest column))))))))
         (vec))))

(defn convert
  [source-format result-format source destination]
  (with-open [writer (io/writer destination)]
    (->> source
         (process-conversion source-format result-format)
         (columns->rows)
         (csv/write-csv writer))
    (println (str "Written result file to " destination))))

;;;; Main

(defn ^:export -main [args]
  (let [{:keys [source result file dest
                exit-message ok?]}
        (validate-args args)]
    (if exit-message
       (exit (if ok? 0 1) exit-message)
       (convert source result file dest))))


(-main *command-line-args*)
