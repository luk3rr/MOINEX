/*
 * Filename: SuggestionsHelper.java
 * Created on: March 15, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import lombok.Setter;

public class SuggestionsHandlerHelper<T> {
    private final ListView<T> suggestionListView;
    private final Popup suggestionsPopup;
    private final Function<T, String> displayFunction;
    private final Function<T, String> filterFunction;
    private final Consumer<T> onSelectCallback;
    private final TextField componentField;
    private ChangeListener<String> componentFieldListener;
    @Setter private List<T> suggestions = new ArrayList<>();

    public SuggestionsHandlerHelper(
            TextField componentField,
            Function<T, String> filterFunction,
            Function<T, String> displayFunction,
            Consumer<T> onSelectCallback) {
        this.suggestionListView = new ListView<>();
        this.suggestionsPopup = new Popup();

        this.componentField = componentField;
        this.filterFunction = filterFunction;
        this.displayFunction = displayFunction;
        this.onSelectCallback = onSelectCallback;

        configureListeners();
        configureSuggestionsListView();
        configureSuggestionsPopup();
    }

    public void disable() {
        componentField.textProperty().removeListener(componentFieldListener);
    }

    public void enable() {
        componentField.textProperty().addListener(componentFieldListener);
    }

    private void configureListeners() {
        // Store the listener in a variable to be able to disable and enable it
        // when needed
        componentFieldListener =
                (observable, oldValue, newValue) -> {
                    if (newValue.isBlank()) {
                        suggestionsPopup.hide();
                        return;
                    }

                    suggestionListView.getItems().clear();
                    List<T> filteredSuggestions =
                            suggestions.stream()
                                    .filter(
                                            item ->
                                                    filterFunction
                                                            .apply(item)
                                                            .toLowerCase()
                                                            .contains(newValue.toLowerCase()))
                                    .toList();

                    if (filteredSuggestions.size() > Constants.SUGGESTIONS_MAX_ITEMS) {
                        filteredSuggestions =
                                filteredSuggestions.subList(0, Constants.SUGGESTIONS_MAX_ITEMS);
                    }

                    suggestionListView.getItems().addAll(filteredSuggestions);

                    if (!filteredSuggestions.isEmpty()) {
                        adjustPopupWidth();
                        adjustPopupHeight();
                        showPopup();
                    } else {
                        suggestionsPopup.hide();
                    }
                };
    }

    private void configureSuggestionsListView() {
        suggestionListView.setCellFactory(
                param ->
                        new ListCell<>() {
                            @Override
                            protected void updateItem(T item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty || item == null) {
                                    setText(null);
                                } else {
                                    VBox cellContent = new VBox();
                                    cellContent.setSpacing(2);

                                    Label descriptionLabel = new Label(displayFunction.apply(item));

                                    cellContent.getChildren().addAll(descriptionLabel);
                                    setGraphic(cellContent);
                                }
                            }
                        });

        suggestionListView.setPrefWidth(Region.USE_COMPUTED_SIZE);
        suggestionListView.setPrefHeight(Region.USE_COMPUTED_SIZE);

        // By default, the SPACE key is used to select an item in the ListView.
        // This behavior is not desired in this case, so the event is consumed
        suggestionListView.addEventFilter(
                KeyEvent.KEY_PRESSED,
                event -> {
                    if (event.getCode() == KeyCode.SPACE) {
                        event.consume(); // Do not propagate the event
                    }
                });

        suggestionListView
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (newValue != null) {
                                onSelectCallback.accept(newValue);
                                suggestionsPopup.hide();
                            }
                        });
    }

    private void configureSuggestionsPopup() {
        suggestionsPopup.setAutoHide(true);
        suggestionsPopup.setHideOnEscape(true);
        suggestionsPopup.getContent().add(suggestionListView);
    }

    private void adjustPopupWidth() {
        suggestionListView.setPrefWidth(componentField.getWidth());
    }

    private void adjustPopupHeight() {
        int itemCount =
                Math.min(suggestionListView.getItems().size(), Constants.SUGGESTIONS_MAX_ITEMS);
        double totalHeight = itemCount * 45.0;
        suggestionListView.setPrefHeight(totalHeight);
    }

    private void showPopup() {
        suggestionsPopup.show(
                componentField,
                componentField.localToScene(0, 0).getX()
                        + componentField.getScene().getWindow().getX()
                        + componentField.getScene().getX(),
                componentField.localToScene(0, 0).getY()
                        + componentField.getScene().getWindow().getY()
                        + componentField.getScene().getY()
                        + componentField.getHeight());
    }
}
