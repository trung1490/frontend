(ns frontend.components.pieces.popover
  (:require [clojure.string :as string]
            [devcards.core :as dc :refer-macros [defcard]]
            [frontend.components.pieces.icon :as icon]
            [goog.string :as gstring]
            [om.dom :as om-dom]
            [om.next :as om-next :refer-macros [defui]])
  (:require-macros [frontend.utils :refer [component html]]))

(defn- with-positioned-element
  "A component responsible for ensuring an element is positioned
   appropriately relative to its anchor element(s).

  children
   The anchor element(s) to position against.

  :placement
   The position of the element relative to the anchor element.
   Can be one of [:top :bottom :left :right].
   (default: :top)

  :element
   The element to position against the anchor."
  [{:keys [placement element]} children]
  (component
   (html
    [:div
     [:.positioned-element
      {:class (name placement)}
      element]
     children])))

(defn- card
  "A popover card component that draws a static popover card that
   can transition itself in and out on visibility change.

  :title (optional)
   The popover title.

  :body
   The content to display in the popover.

  :placement
   The position of the popover relative to the trigger element.
   Can be one of [:top :bottom :left :right].
   (default: :top)

  :visible?
   Popover card transitions in when changed from false to true,
    and transitions out when changed from true to false.
   (default: :false)"
  [{:keys [title body placement visible?]
    :or {placement :top visible? false}}]
  (component
   (js/React.createElement
    js/React.addons.CSSTransitionGroup
    #js {:transitionName "transition"
         :transitionEnterTimeout 200
         :transitionLeaveTimeout 200}
    (when visible?
      (html
       [:.cci-popover
        {:class (name placement)}
        [:.popover-inner
         [:.content
          (when title
            [:.title
             [:span title]])
          [:.body
           body]]]])))))

(defn- handle-document-click [popover event]
  (let [click-target (.-target event)
        popover-node (om-dom/node popover)
        popover-clicked? (.contains popover-node click-target)
        visible? (-> popover om-next/get-state :visible-by-click?)]
    (when (and visible?
               (not popover-clicked?))
      (om-next/update-state! popover assoc :visible-by-click? false))))

(defui ^:once Popover
  "A popover component that attaches a popover to the specified
   trigger element(s), passed in as children.

  children
   The trigger element(s) to attach the popover to.

  :title (optional)
   The popover title.

  :body
   The content to display in the popover.

  :placement
   The position of the popover relative to the trigger element.
   Can be one of [:top :bottom :left :right].
   (default: :top)

  :trigger-mode
   The type of events on the trigger element that affect the
    visibility of the popover.
   A popover in hover mode will also respond to clicks, but not
    vice versa. This enables touch interactions for a popover in
    hover mode and allows users to pin it in place.
   Can be one of [:click :hover].
   (default: :click)"
  Object
  (initLocalState [_]
    {:visible-by-click? false
     :visible-by-hover? false})
  (componentDidMount [this]
    (let [document-click-handler #(handle-document-click this %)]
      (set! (.-documentClickHandler this) document-click-handler)
      (js/document.addEventListener
       "click"
       document-click-handler
       true)))
  (componentDidUpdate [this prev-props prev-state]
    ;; workaround for Safari iOS not firing click events
    ;; reference: https://github.com/kentor/react-click-outside/issues/4#issuecomment-266644870
    (when (exists? js/document.documentElement.ontouchstart)
      (let [visible? (:visible-by-click? (om-next/get-state this))
            prev-visible? (:visible-by-click? prev-state)]
        (when (not= visible? prev-visible?)
          (if visible?
            (set! (.-cursor document.body.style) "pointer")
            (set! (.-cursor document.body.style) nil))))))
  (componentWillUnmount [this]
    (js/document.removeEventListener
     "click"
     (.-documentClickHandler this)
     true))
  (render [this]
    (component
     (let [{:keys [title body placement trigger-mode]
            :or {placement :top trigger-mode :click}}
           (om-next/props this)

           {:keys [visible-by-click? visible-by-hover?]}
           (om-next/get-state this)

           hover-props
           {:on-mouse-enter
            #(om-next/update-state! this assoc :visible-by-hover? true)
            :on-mouse-leave
            #(om-next/update-state! this assoc :visible-by-hover? false)}

           click-props
           {:on-click
            #(om-next/update-state! this update :visible-by-click? not)}]
       (html
        [:div (merge click-props
                     (when (= :hover trigger-mode)
                       hover-props))
         (with-positioned-element
          {:placement placement
           :element (card {:title title
                           :body body
                           :placement placement
                           :visible? (or visible-by-click?
                                         visible-by-hover?)})}
          (om-next/children this))])))))

(def popover (om-next/factory Popover))

(defn tooltip
  "A tooltip component that attaches a tooltip to the specified
   trigger element(s), passed in as children.

  children
   The trigger element(s) to attach the tooltip to.

  :body
   The content to display in the tooltip.

  :placement
   The position of the tooltip relative to the trigger element.
   Can be one of [:top :bottom :left :right].
   (default: :top)"
  [{:keys [body placement]
    :or {placement :top}}
   children]
  (popover {:title nil
            :body body
            :placement placement
            :trigger-mode :hover}
           children))

(dc/do
  (def trigger
    (html
     [:div {:style {:width 32}}
      (icon/settings)]))

  (defn vary-placements
    [component props]
    (for [placement [:top :left :right :bottom]
          :let [body (gstring/format
                          "%s %s"
                          (if-let [trigger-mode (:trigger-mode props)]
                            (-> trigger-mode
                                name
                                string/capitalize)
                            "Hover")
                          (name placement))]]
      (component (assoc props
                        :placement placement
                        :body body)
                 trigger)))

  (defcard popover-card
    (fn [state]
      (html
       [:div {:style {:display "flex"
                      :justify-content "space-around"
                      :padding-left 150
                      :padding-right 150
                      :padding-top 100
                      :padding-bottom 200}}
        (with-positioned-element
         {:placement :left
          :element (card {:body "Without title"
                          :placement :left
                          :trigger-mode :click
                          :visible? true})}
         trigger)
        (with-positioned-element
         {:placement :top
          :element (card {:title "With title"
                          :body "Content"
                          :placement :top
                          :trigger-mode :click
                          :visible? true})}
         trigger)
        (with-positioned-element
         {:placement :bottom
          :element (card {:title "With multi-line content and multi-line title"
                          :body "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                          :placement :bottom
                          :trigger-mode :click
                          :visible? true})}
         trigger)
        (with-positioned-element
         {:placement :right
          :element (card {:title "With rich content"
                          :body
                          (html
                            [:div
                             [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."]
                             [:p [:a {:href "#"} "Click here!"]]])
                          :placement :right
                          :trigger-mode :click
                          :visible? true})}
         trigger)])))

  (defcard tooltip
    (fn [state]
      (html
       [:div {:style {:display "flex"
                      :justify-content "space-between"
                      :padding 50}}
        (vary-placements tooltip
                         {:body "Content"})])))

  (defcard popover
    (fn [state]
      (html
       [:div {:style {:display "flex"
                      :justify-content "space-between"
                      :padding 50}}
        (vary-placements popover
                         {:title "Title"
                          :body "Content"
                          :trigger-mode :click})]))))