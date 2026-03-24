(ns cjl-enterprises.bowling-score.core)

(defn init []
  ;; Locate the `#app` div and replace its content)
  (let [app-el (.getElementById js/document "app")]
    (set! (.-innerHTML app-el)
          "<h1>🎳 Hello from ClojureScript!</h1>")))

(defn on-js-reload []
  ;; Called by shadow-cljs after every reload
  (init))
