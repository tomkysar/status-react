(ns status-im.new-group.views.contact-list
  (:require-macros [status-im.utils.views :refer [defview]])
  (:require [re-frame.core :refer [dispatch]]
            [status-im.contacts.views.contact :refer [contact-view]]
            [status-im.components.react :refer [view
                                                text
                                                list-view
                                                list-item]]
            [status-im.components.confirm-button :refer [confirm-button]]
            [status-im.components.status-bar :refer [status-bar]]
            [status-im.components.toolbar-new.view :refer [toolbar-with-search]]
            [status-im.utils.listview :refer [to-datasource]]
            [status-im.new-group.views.contact :refer [new-group-contact]]
            [status-im.new-group.views.group :refer [separator]]
            [status-im.new-group.styles :as st]
            [status-im.contacts.styles :as cst]
            [status-im.i18n :refer [label]]
            [status-im.components.toolbar-new.actions :as act]))

(defn title-with-count [title count-value]
  [view st/toolbar-title-with-count
   [text {:style st/toolbar-title-with-count-text
          :font  :toolbar-title}
    title]
   (when (pos? count-value)
     [view st/toolbar-title-with-count-container
      [text {:style st/toolbar-title-with-count-text-count
             :font  :toolbar-title}
       count-value]])])

(defview toggle-list-toolbar [title contacts-count]
  [show-search [:get-in [:toolbar-search :show]]
   search-text [:get-in [:toolbar-search :text]]]
  (toolbar-with-search
    {:show-search?       (= show-search :contact-group-list)
     :search-text        search-text
     :search-key         :contact-group-list
     :custom-title       (title-with-count title contacts-count)
     :search-placeholder (label :t/search-contacts)}))

(defn render-separator [_ row-id _]
  (list-item ^{:key row-id}
             [separator]))

(defview contact-toggle-list []
  [contacts [:all-added-group-contacts-filtered]
   selected-contacts-count [:selected-contacts-count]
   group-type [:get :group-type]]
  [view st/group-container
   [status-bar]
   [toggle-list-toolbar
    (label (if (= group-type :contact-group)
             :t/new-group
             :t/new-group-chat))
    selected-contacts-count]
   [view {:flex 1}
    [list-view
     {:dataSource                (to-datasource contacts)
      :renderRow                 (fn [row _ _]
                                  (list-item ^{:key row} [new-group-contact row]))
      :renderSeparator           render-separator
      :style                     cst/contacts-list
      :keyboardShouldPersistTaps true}]]
   (when (pos? selected-contacts-count)
     [confirm-button (label :t/next) #(dispatch [:navigate-to :new-group])])])

(defview add-contacts-toggle-list []
  [contacts [:all-group-not-added-contacts-filtered]
   group [:get-contact-group]
   selected-contacts-count [:selected-contacts-count]]
  [view st/group-container
   [status-bar]
   [toggle-list-toolbar (:name group) selected-contacts-count]
   [view {:flex 1}
    [list-view
     {:dataSource                (to-datasource contacts)
      :renderRow                 (fn [row _ _]
                                   (list-item ^{:key row} [new-group-contact row]))
      :renderSeparator           render-separator
      :style                     cst/contacts-list
      :keyboardShouldPersistTaps true}]]
   (when (pos? selected-contacts-count)
     [confirm-button (label :t/save) #(do
                                        (dispatch [:add-selected-contacts-to-group])
                                        (dispatch [:navigate-back]))])])

(defview contact-list-toolbar [title]
  [show-search [:get-in [:toolbar-search :show]]
   search-text [:get-in [:toolbar-search :text]]]
  (toolbar-with-search
    {:show-search?       (= show-search :contact-list)
     :search-text        search-text
     :search-key         :contact-list
     :title              title
     :search-placeholder (label :t/search-contacts)}))

(defn render-row [group]
  (fn [row _ _]
    (list-item
      ^{:key row}
      [contact-view {:contact        row
                     :extended?      true
                     :extend-options (when group
                                       [{:value #(dispatch [:remove-contact-from-group
                                                            (:whisper-identity row)
                                                            (:group-id group)])
                                         :text (label :t/remove-from-group)}])
                     :on-click       nil}])))

(defview contacts-list-view [group]
  [contacts [:all-added-group-contacts-filtered (:group-id group)]]
  [view {:flex 1}
   [list-view {:dataSource                (to-datasource contacts)
               :enableEmptySections       true
               :renderRow                 (render-row group)
               :bounces                   false
               :keyboardShouldPersistTaps true
               :renderSeparator           render-separator}]])

(defview edit-group-contact-list []
  [group [:get-contact-group]
   type [:get :group-type]]
  [view st/group-container
   [status-bar]
   [contact-list-toolbar (:name group)]
   [contacts-list-view group]])