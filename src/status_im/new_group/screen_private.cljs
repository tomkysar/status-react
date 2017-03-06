(ns status-im.new-group.screen-private
  (:require-macros [status-im.utils.views :refer [defview]])
  (:require [re-frame.core :refer [subscribe dispatch]]
            [status-im.resources :as res]
            [status-im.contacts.views.contact :refer [contact-view]]
            [status-im.contacts.styles :as cst]
            [status-im.components.react :refer [view
                                                text
                                                image
                                                icon
                                                touchable-highlight
                                                list-view
                                                list-item]]
            [status-im.components.text-field.view :refer [text-field]]
            [status-im.components.confirm-button :refer [confirm-button]]
            [status-im.components.styles :refer [color-blue color-gray5 color-light-blue]]
            [status-im.components.status-bar :refer [status-bar]]
            [status-im.components.toolbar-new.view :refer [toolbar]]
            [status-im.utils.platform :refer [platform-specific]]
            [status-im.utils.listview :refer [to-datasource]]
            [status-im.new-group.views.contact :refer [new-group-contact]]
            [status-im.new-group.styles :as st]
            [status-im.new-group.views.group :refer [group-toolbar
                                                     group-name-view
                                                     add-btn
                                                     more-btn
                                                     delete-btn
                                                     separator]]
            [status-im.new-group.validations :as v]
            [status-im.i18n :refer [label]]
            [cljs.spec :as s]))

(def contacts-limit 3)

(defview group-contacts-view [group]
  [contacts [:all-added-group-contacts-with-limit (:group-id group) contacts-limit]
   contacts-count [:all-added-group-contacts-count (:group-id group)]]
  [view
   (when (pos? contacts-count)
     [separator])
   [view
    (doall
      (map (fn [row]
             ^{:key row}
             [view
              [contact-view
               {:contact        row
                :on-click       nil
                :extend-options [{:value #(dispatch [:remove-contact-from-group
                                                     (:whisper-identity row)
                                                     (:group-id group)])
                                  :text (label :t/remove-from-group)}]
                :extended?      true}]
              (when-not (= row (last contacts))
                [separator])])
           contacts))]
   (when (< contacts-limit contacts-count)
     [more-btn group contacts-limit contacts-count])])

(defn save []
  (dispatch [:set-group-name]))

(defview edit-group []
  [group-name [:get :new-chat-name]
   group [:get-contact-group]
   type [:get :group-type]]
  (let [save-btn-enabled? (and (s/valid? ::v/name group-name)
                               (not= group-name (:name group)))]
    [view st/group-container
     [group-toolbar type (boolean group)]
     [group-name-view]
     [add-btn]
     [group-contacts-view group]
     [view st/separator]
     [delete-btn #(do
                    (dispatch [:update-group (assoc group :pending? true)])
                    (dispatch [:navigate-to-clean :contact-list]))]
     [view {:flex 1}]
     (when save-btn-enabled?
       [confirm-button (label :t/save) save])]))

(defn render-separator [_ row-id _]
  (list-item ^{:key row-id}
             [separator]))

(defn render-row [row _ _]
  (list-item
    ^{:key row}
    [contact-view {:contact   row
                   :extended? false
                   :on-click  nil}]))

(defview new-group []
  [contacts [:selected-group-contacts]
   group-name [:get :new-chat-name]
   group-type [:get :group-type]]
  (let [save-btn-enabled? (and (s/valid? ::v/name group-name) (pos? (count contacts)))]
    [view st/group-container
     [group-toolbar group-type false]
     [group-name-view]
     [view {:flex 1}
      [list-view {:dataSource                (to-datasource contacts)
                  :enableEmptySections       true
                  :renderRow                 render-row
                  :bounces                   false
                  :keyboardShouldPersistTaps true
                  :renderSeparator           render-separator}]]
     (when save-btn-enabled?
       [confirm-button (label :t/save)
        (if (= group-type :contact-group)
          #(dispatch [:create-new-group group-name])
          #(dispatch [:create-new-group-chat group-name]))])]))
