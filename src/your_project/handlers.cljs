(ns your-project.handlers
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx reg-fx ->interceptor dispatch]]
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [your-project.db :as db :refer [app-db]]))

;; -- Interceptors ----------------------------------------------------------
;;
;; See https://github.com/Day8/re-frame/blob/develop/docs/Interceptors.md
;;
(defn check-and-throw
      "Throw an exception if db doesn't have a valid spec."
      [spec db]
      (when-not (s/valid? spec db)
                (let [explain-data (s/explain-data spec db)]
                     (throw (ex-info (str "Spec check failed: " explain-data) explain-data)))))

(def validate-spec
  (if goog.DEBUG
    (->interceptor
      :id :validate-spec
      :after (fn [context]
                 (let [db (-> context :effects :db)]
                      (check-and-throw ::db/app-db db)
                      context)))
    ->interceptor))

;; -- Handlers --------------------------------------------------------------

(reg-event-db
  :initialize-db
  [validate-spec]
  (fn [_ _]
      app-db))

(reg-event-db
  :set-greeting
  [#_validate-spec]
  (fn [db [_ value]]
      (assoc db :greeting value)))

(reg-fx
  :fetch
  (fn [{:keys [url]}]
      (-> (js/fetch url)
          (.then #(.json %))
          (.then #(dispatch [:set-greeting (str/join ";" (map :word (:list (js->clj % :keywordize-keys true))))]))
          (.catch #(dispatch [:set-greeting (str "you suck " %)])))))

(reg-event-fx
  :fetch-random
  []
  (fn [coeffects event]
      {:fetch {:url "http://api.urbandictionary.com/v0/random"}}))
