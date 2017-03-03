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
            [status-im.new-group.validations :as v]
            [status-im.i18n :refer [label]]
            [cljs.spec :as s]))

(def contacts-limit 3)

(defview group-name-input []
  [new-group-name [:get :new-chat-name]]
  [view
   [text-field
    {:error            (when
                         (not (s/valid? ::v/not-illegal-name new-group-name))
                         (label :t/illegal-group-chat-name))
     :error-color       color-blue
     :wrapper-style     st/group-chat-name-wrapper
     :line-color        color-gray5
     :focus-line-color  color-light-blue
     :focus-line-height st/group-chat-focus-line-height
     :label-hidden?     true
     :input-style       st/group-chat-name-input
     :auto-focus        true
     :on-change-text    #(dispatch [:set :new-chat-name %])
     :value             new-group-name}]])

(defview group-toolbar [type edit?]
  [view
   [status-bar]
   [toolbar
    {:title (label
              (if (= type :contact-group)
                (if edit? :t/edit-group :t/new-group)
                (if edit? :t/edit-group :t/new-group-chat)))
     :actions [{:image :blank}]}]])

(defn group-name-view []
  [view st/chat-name-container
   [text {:style st/group-name-text}
    (label :t/name)]
   [group-name-input]])

(defn add-btn []
  [view st/add-button-container
   [touchable-highlight {:on-press #(dispatch [:navigate-forget :contact-toggle-list])}
    [view st/add-container
     [view st/add-icon-container
      [icon :add_blue st/add-icon]]
     [text {:style st/add-text}
      (label :t/add-members)]]]])

(defn delete-btn [on-press]
  [touchable-highlight {:on-press on-press}
   [view st/delete-group-container
    [view st/delete-icon-container
     [icon :close_red st/add-icon]]
    [view st/delete-group-text-container
     [text {:style st/delete-group-text}
      (label :t/delete-group)]
     [text {:style st/delete-group-prompt-text}
      (label :t/delete-group-prompt)]]]])

(defn more-btn [contacts-limit contacts-count]
  [view
   [view cst/contact-item-separator-wrapper
    [view cst/contact-item-separator]]
   [view cst/show-all
    [touchable-highlight {:on-press #(dispatch [:navigate-forget :contact-toggle-list])}
     [view
      [text {:style cst/show-all-text
             :uppercase? (get-in platform-specific [:uppercase?])
             :font (get-in platform-specific [:component-styles :contacts :show-all-text-font])}
       (str (- contacts-count contacts-limit) " " (label :t/more))]]]]])

(defview group []
  [contacts [:selected-group-contacts]
   group-name [:get :new-chat-name]
   group [:get :contact-group]
   type [:get :group-type]]
  (let [save-btn-enabled? (and (s/valid? ::v/name group-name) (pos? (count contacts)))
        contacts-count (count contacts)
        limited-contacts (take contacts-limit contacts)]
    [view st/new-group-container
     [group-toolbar type (boolean group)]
     [group-name-view]
     [add-btn]
     (when (pos? contacts-count)
       [view cst/contact-item-separator-wrapper
        [view cst/contact-item-separator]])
     [view
      (doall
        (map (fn [row]
               ^{:key row}
               [view
                [contact-view
                 {:contact        row
                  :on-click       nil
                  :extend-options [{:value #(dispatch [:deselect-contact (:whisper-identity row)])
                                    :text (label :t/remove-from-group)}]
                  :extended?      true}]
                (when-not (= row (last limited-contacts))
                  [view cst/contact-item-separator-wrapper
                   [view cst/contact-item-separator]])])
             limited-contacts))]
     (when (< contacts-limit contacts-count)
       [more-btn contacts-limit contacts-count])
     [view st/separator]
     (when group
        [delete-btn #(do
                       (dispatch [:update-group (assoc group :pending? true)])
                       (dispatch [:navigate-to-clean :contact-list]))])
     [view {:flex 1}]
     (when save-btn-enabled?
       [confirm-button (label :t/save)
        (if (= type :contact-group)
          (if group
            #(dispatch [:update-group-after-edit group group-name])
            #(dispatch [:create-new-group group-name]))
          #(dispatch [:create-new-group-chat group-name]))])]))
