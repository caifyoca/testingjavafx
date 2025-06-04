package com.example.narytree.ui;

import com.example.narytree.db.DatabaseManager;
import com.example.narytree.model.ApplicabilityRule;
import com.example.narytree.model.Descriptor;
import com.example.narytree.model.DescriptorState;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node; // Added
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class MainController {

    @FXML private TreeView<Descriptor> descriptorTreeView;
    @FXML private VBox detailsPane;
    private DatabaseManager dbManager;
    private Map<Integer, TreeItem<Descriptor>> treeItemMap = new HashMap<>();
    private Map<Integer, String> descriptorRuleEffects = new HashMap<>();

    private static final String STATE_PRESENT = "present";
    private static final String STATE_UNKNOWN = "unknown";
    private static final String STATE_INAPPLICABLE = "inapplicable";
    private static final String STATE_OUI = "oui";
    private static final String STATE_TRUE = "true"; // For generic boolean states like 'selected'
    private static final String STATE_FALSE = "false";

    private static final String EFFECT_DISABLED = "disabled_by_rule";
    private static final String EFFECT_HIDDEN_REVEALED = "hidden_revealed";


    public void setDatabaseManager(DatabaseManager dbManager) { this.dbManager = dbManager; }
    @FXML public void initialize() {
        descriptorTreeView.setCellFactory(tv -> new DescriptorTreeCell(descriptorRuleEffects));
        descriptorTreeView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showDescriptorDetails(newValue)
        );
    }
    public void loadTreeData() {
        if (dbManager == null) { showAlert("Error", "Database connection not available.", Alert.AlertType.ERROR); return; }
        treeItemMap.clear();
        descriptorRuleEffects.clear();
        try {
            Descriptor visualRootData = new Descriptor(0, "Descriptors", null, false, false, false, false);
            TreeItem<Descriptor> visualRoot = new TreeItem<>(visualRootData);
            descriptorTreeView.setRoot(visualRoot); treeItemMap.put(0, visualRoot); visualRoot.setExpanded(true);
            List<Descriptor> rootDescriptors = dbManager.getRootDescriptors();
            for (Descriptor rootDescriptor : rootDescriptors) {
                TreeItem<Descriptor> rootItem = new TreeItem<>(rootDescriptor);
                visualRoot.getChildren().add(rootItem); treeItemMap.put(rootDescriptor.getId(), rootItem);
                addChildrenToTreeItem(rootItem);
            }
            if (rootDescriptors.isEmpty()) System.out.println("No root descriptors found.");
            System.out.println("Tree data loaded. treeItemMap size: " + treeItemMap.size());
            applyInitialRuleEffectsFromLoadedStates();
        } catch (SQLException e) { e.printStackTrace(); showAlert("Database Error", "Failed to load: " + e.getMessage(), Alert.AlertType.ERROR); }
    }
    private void applyInitialRuleEffectsFromLoadedStates() {
        System.out.println("Applying initial rule effects based on loaded states...");
        boolean anyRuleApplied = false;
        for (TreeItem<Descriptor> item : treeItemMap.values()) {
            Descriptor descriptor = item.getValue();
            if (descriptor == null || descriptor.getId() == 0) continue;
            try {
                DescriptorState state = dbManager.getDescriptorState(descriptor.getId());
                if (state != null) {
                    String stateValue = state.getStateValue();
                    boolean isSourceCurrentlyPresent = false;
                    boolean isParentInDb = !dbManager.getChildrenOfDescriptor(descriptor.getId()).isEmpty();

                    if (descriptor.isNumeric() && (descriptor.getParentId() == null || isParentInDb)) {
                        if (STATE_PRESENT.equals(stateValue)) isSourceCurrentlyPresent = true;
                    } else if (!descriptor.isNumeric() && descriptor.isTypeOfPresence()) {
                        if (STATE_OUI.equals(stateValue)) isSourceCurrentlyPresent = true;
                    }
                    if (isSourceCurrentlyPresent) {
                        System.out.println("  Init: Desc " + descriptor.getLabel() + " is 'present', evaluating rules.");
                        evaluateApplicabilityRules(descriptor.getId(), true, false);
                        anyRuleApplied = true;
                    }
                }
            } catch (SQLException e) { System.err.println("Error init state for " + descriptor.getLabel() + ": " + e.getMessage()); }
        }
        if(anyRuleApplied) Platform.runLater(() -> descriptorTreeView.refresh());
        System.out.println("Finished applying initial rule effects.");
    }
    private void addChildrenToTreeItem(TreeItem<Descriptor> parentItem) throws SQLException {
        Descriptor parentDescriptor = parentItem.getValue();
        if (parentDescriptor == null || parentDescriptor.getId() == 0) { return; }
        List<Descriptor> children = dbManager.getChildrenOfDescriptor(parentDescriptor.getId());
        for (Descriptor child : children) {
            TreeItem<Descriptor> childItem = new TreeItem<>(child);
            parentItem.getChildren().add(childItem); treeItemMap.put(child.getId(), childItem);
            addChildrenToTreeItem(childItem);
        }
    }

    private void showDescriptorDetails(TreeItem<Descriptor> treeItem) {
        detailsPane.getChildren().clear();

        if (treeItem == null || treeItem.getValue() == null) {
             detailsPane.getChildren().add(new Label("Select a descriptor to see details."));
            return;
        }
        Descriptor descriptor = treeItem.getValue();
        if (descriptor.getId() == 0 && "Descriptors".equals(descriptor.getLabel())) {
            detailsPane.getChildren().add(new Label("Top-level container. Select a descriptor."));
            return;
        }

        detailsPane.setPadding(new Insets(10)); detailsPane.setSpacing(8);
        detailsPane.getChildren().add(new Label("ID: " + descriptor.getId()));
        detailsPane.getChildren().add(new Label("Label: " + descriptor.getLabel()));
        detailsPane.getChildren().add(new Label("Parent ID: " + (descriptor.getParentId() == null ? "N/A (Root)" : descriptor.getParentId())));


        boolean isParentInDB = false;
        try { if(dbManager != null) isParentInDB = !dbManager.getChildrenOfDescriptor(descriptor.getId()).isEmpty(); }
        catch (SQLException e) { System.err.println("Error checking parent status: " + e.getMessage());}

        String ruleEffect = descriptorRuleEffects.get(descriptor.getId());
        boolean isDisabledByRule = EFFECT_DISABLED.equals(ruleEffect);

        DescriptorState savedState = null; String currentVal = null;
        if (dbManager != null) {
            try { savedState = dbManager.getDescriptorState(descriptor.getId()); if (savedState != null) currentVal = savedState.getStateValue(); }
            catch (SQLException e) { System.err.println("Error loading state for " + descriptor.getId() + ": " + e.getMessage()); }
        }

        if (descriptor.isNumeric()) {
            detailsPane.getChildren().add(new Label("Type: Numeric/Quantitative"));
            if (descriptor.getParentId() == null || isParentInDB) {
                HBox numericParentControls = new HBox(10); numericParentControls.setId("numericParentControls");
                CheckBox presentCb = new CheckBox("Present"); presentCb.setId("presentCb");
                CheckBox unknownCb = new CheckBox("Unknown"); unknownCb.setId("unknownCb");
                CheckBox inapplicableCb = new CheckBox("Inapplicable"); inapplicableCb.setId("inapplicableCb");
                if (currentVal != null) { presentCb.setSelected(STATE_PRESENT.equals(currentVal)); unknownCb.setSelected(STATE_UNKNOWN.equals(currentVal)); inapplicableCb.setSelected(STATE_INAPPLICABLE.equals(currentVal)); }
                presentCb.selectedProperty().addListener((obs, oldV, newV) -> { if (newV) { unknownCb.setSelected(false); inapplicableCb.setSelected(false); } evaluateApplicabilityRules(descriptor.getId(), newV, true); });
                unknownCb.selectedProperty().addListener((obs, oldV, newV) -> { if (newV) { presentCb.setSelected(false); inapplicableCb.setSelected(false); }});
                inapplicableCb.selectedProperty().addListener((obs, oldV, newV) -> { if (newV) { presentCb.setSelected(false); unknownCb.setSelected(false); }});
                presentCb.setDisable(isDisabledByRule); unknownCb.setDisable(isDisabledByRule); inapplicableCb.setDisable(isDisabledByRule);
                numericParentControls.getChildren().addAll(presentCb, unknownCb, inapplicableCb); detailsPane.getChildren().add(numericParentControls);
            } else {
                HBox numericLeafControls = new HBox(10); numericLeafControls.setId("numericLeafControls");
                TextField valueField = new TextField(); valueField.setId("valueField");
                if (currentVal != null) valueField.setText(currentVal);
                valueField.setPromptText("Enter numeric value"); valueField.setDisable(isDisabledByRule);
                numericLeafControls.getChildren().addAll(new Label(descriptor.getLabel() + " Value:"), valueField); detailsPane.getChildren().add(numericLeafControls);
            }
        } else {
            detailsPane.getChildren().add(new Label("Type: Nominal/Qualitative"));
            if (descriptor.isTypeOfPresence()) {
                HBox typeOfPresenceControls = new HBox(10); typeOfPresenceControls.setId("typeOfPresenceControls");
                CheckBox ouiCb = new CheckBox("Oui (Present)"); ouiCb.setId("ouiCb");
                CheckBox inconnuCb = new CheckBox("Inconnu (Unknown)"); inconnuCb.setId("inconnuCb");
                CheckBox jamaisCb = new CheckBox("Jamais (Inapplicable)"); jamaisCb.setId("jamaisCb");
                if (currentVal != null) { ouiCb.setSelected(STATE_OUI.equals(currentVal)); inconnuCb.setSelected(STATE_UNKNOWN.equals(currentVal)); jamaisCb.setSelected(STATE_INAPPLICABLE.equals(currentVal)); }
                ouiCb.selectedProperty().addListener((obs, oldV, newV) -> { if (newV) { inconnuCb.setSelected(false); jamaisCb.setSelected(false); } evaluateApplicabilityRules(descriptor.getId(), newV, true); });
                inconnuCb.selectedProperty().addListener((obs, oldV, newV) -> { if (newV) { ouiCb.setSelected(false); jamaisCb.setSelected(false); }});
                jamaisCb.selectedProperty().addListener((obs, oldV, newV) -> { if (newV) { ouiCb.setSelected(false); inconnuCb.setSelected(false); }});
                ouiCb.setDisable(isDisabledByRule); inconnuCb.setDisable(isDisabledByRule); jamaisCb.setDisable(isDisabledByRule);
                typeOfPresenceControls.getChildren().addAll(ouiCb, inconnuCb, jamaisCb); detailsPane.getChildren().add(typeOfPresenceControls);
            } else {
                 boolean isLeafInTreeView = treeItem.isLeaf();
                 if(isLeafInTreeView && !isParentInDB){
                     CheckBox selectedCb = new CheckBox("Selected"); selectedCb.setId("selectedCb");
                     if (currentVal != null) selectedCb.setSelected(STATE_TRUE.equals(currentVal));
                     selectedCb.setDisable(isDisabledByRule); detailsPane.getChildren().add(selectedCb);
                 }
            }
            if (isParentInDB) detailsPane.getChildren().add(new Label("Allows multiple selections from children: " + descriptor.isMultistate()));
        }
        detailsPane.getChildren().add(new Label("Hidden by Default: " + descriptor.isHiddenDefaultly()));
        if (EFFECT_HIDDEN_REVEALED.equals(ruleEffect)) detailsPane.getChildren().add(new Label("Status: Was hidden, now revealed by a rule."));
        else if (EFFECT_DISABLED.equals(ruleEffect)) detailsPane.getChildren().add(new Label("Status: Disabled by an applicability rule."));
        else if (descriptor.isHiddenDefaultly()) detailsPane.getChildren().add(new Label("Status: Hidden by default."));


        Button updateButton = new Button("Save Current State");
        updateButton.setOnAction(e -> handleUpdateState(descriptor));
        updateButton.setDisable(isDisabledByRule);
        detailsPane.getChildren().add(updateButton);
    }

    private void evaluateApplicabilityRules(int sourceDescriptorId, boolean sourceIsPresent, boolean refreshDetailsPaneIfNeeded) {
        System.out.println("Evaluating rules for source ID: " + sourceDescriptorId + ", Present: " + sourceIsPresent);
        if (dbManager == null) return;
        try {
            List<ApplicabilityRule> rules = dbManager.getApplicabilityRulesBySourceId(sourceDescriptorId);
            boolean changedEffect = false;
            for (ApplicabilityRule rule : rules) {
                TreeItem<Descriptor> targetItem = treeItemMap.get(rule.getTargetId());
                if (targetItem != null && targetItem.getValue() != null) {
                    String oldEffect = descriptorRuleEffects.get(targetItem.getValue().getId());
                    applyRuleEffect(targetItem, rule, sourceIsPresent);
                    String newEffect = descriptorRuleEffects.get(targetItem.getValue().getId());
                    if (oldEffect == null ? newEffect != null : !oldEffect.equals(newEffect)) changedEffect = true;
                } else System.err.println("Target TreeItem not found for ID: " + rule.getTargetId());
            }
            if (changedEffect) Platform.runLater(() -> descriptorTreeView.refresh());
            if (refreshDetailsPaneIfNeeded) {
                TreeItem<Descriptor> selectedItem = descriptorTreeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.getValue() != null) {
                    boolean selectedIsSource = (selectedItem.getValue().getId() == sourceDescriptorId);
                    boolean selectedIsTarget = false;
                    if(!selectedIsSource) { // check if selected is one of the targets
                        for(ApplicabilityRule r : rules) {
                            if(r.getTargetId() == selectedItem.getValue().getId()){
                                selectedIsTarget = true;
                                break;
                            }
                        }
                    }
                    if(selectedIsSource || selectedIsTarget) Platform.runLater(() -> showDescriptorDetails(selectedItem));
                }
            }
        } catch (SQLException e) { System.err.println("Error applying rules: " + e.getMessage()); e.printStackTrace(); }
    }

    private void applyRuleEffect(TreeItem<Descriptor> targetItem, ApplicabilityRule rule, boolean sourceIsPresent) {
        Descriptor targetDescriptor = targetItem.getValue();
        int targetId = targetDescriptor.getId();
        String currentEffect = descriptorRuleEffects.get(targetId);
        if (sourceIsPresent) {
            if (rule.isApplicableIf()) {
                if (targetDescriptor.isHiddenDefaultly()) descriptorRuleEffects.put(targetId, EFFECT_HIDDEN_REVEALED);
                else if (EFFECT_DISABLED.equals(currentEffect)) descriptorRuleEffects.remove(targetId);
            } else descriptorRuleEffects.put(targetId, EFFECT_DISABLED);
        } else {
            if (rule.isApplicableIf() && targetDescriptor.isHiddenDefaultly() && EFFECT_HIDDEN_REVEALED.equals(currentEffect)) descriptorRuleEffects.remove(targetId);
            else if (!rule.isApplicableIf() && EFFECT_DISABLED.equals(currentEffect)) descriptorRuleEffects.remove(targetId);
        }
    }

    private void handleUpdateState(Descriptor descriptor) {
        if (descriptor == null || dbManager == null) {
            showAlert("Error", "Cannot save state. No descriptor selected or database unavailable.", Alert.AlertType.ERROR);
            return;
        }
        System.out.println("Attempting to save state for: " + descriptor.getLabel());
        String stateToSave = null;
        boolean isPresentState = false;

        for (Node node : detailsPane.getChildren()) {
            if (node instanceof HBox) {
                HBox hbox = (HBox) node;
                String hboxId = hbox.getId(); // Relies on IDs set in showDescriptorDetails
                if ("numericParentControls".equals(hboxId)) {
                    CheckBox presentCb = (CheckBox) hbox.lookup("#presentCb");
                    CheckBox unknownCb = (CheckBox) hbox.lookup("#unknownCb");
                    CheckBox inapplicableCb = (CheckBox) hbox.lookup("#inapplicableCb");
                    if (presentCb.isSelected()) { stateToSave = STATE_PRESENT; isPresentState = true; }
                    else if (unknownCb.isSelected()) stateToSave = STATE_UNKNOWN;
                    else if (inapplicableCb.isSelected()) stateToSave = STATE_INAPPLICABLE;
                    break;
                } else if ("typeOfPresenceControls".equals(hboxId)) {
                    CheckBox ouiCb = (CheckBox) hbox.lookup("#ouiCb");
                    CheckBox inconnuCb = (CheckBox) hbox.lookup("#inconnuCb");
                    CheckBox jamaisCb = (CheckBox) hbox.lookup("#jamaisCb");
                    if (ouiCb.isSelected()) { stateToSave = STATE_OUI; isPresentState = true; }
                    else if (inconnuCb.isSelected()) stateToSave = STATE_UNKNOWN;
                    else if (jamaisCb.isSelected()) stateToSave = STATE_INAPPLICABLE;
                    break;
                } else if ("numericLeafControls".equals(hboxId)) {
                    TextField valueField = (TextField) hbox.lookup("#valueField");
                    stateToSave = valueField.getText();
                    // isPresentState for numeric leaf values is typically always true if a value is entered,
                    // but numeric leaves are not usually rule sources in this model.
                    // Setting to true if not empty, false otherwise for this example.
                    isPresentState = stateToSave != null && !stateToSave.trim().isEmpty();
                    break;
                }
            } else if (node instanceof CheckBox && "selectedCb".equals(node.getId())) {
                CheckBox selectedCb = (CheckBox) node;
                stateToSave = selectedCb.isSelected() ? STATE_TRUE : STATE_FALSE;
                isPresentState = selectedCb.isSelected();
                break;
            }
        }

        try {
            if (stateToSave != null) {
                DescriptorState newState = new DescriptorState(descriptor.getId(), stateToSave);
                dbManager.saveDescriptorState(newState);
                showAlert("Success", "State for '" + descriptor.getLabel() + "' saved as: '" + stateToSave + "'.", Alert.AlertType.INFORMATION);
            } else {
                // If no specific state was selected (e.g., all checkboxes off for a group), delete the state.
                boolean deleted = dbManager.deleteDescriptorState(descriptor.getId());
                if (deleted) {
                    showAlert("State Cleared", "State for '" + descriptor.getLabel() + "' has been cleared.", Alert.AlertType.INFORMATION);
                } else {
                    // This case might occur if no controls were matched or no state was actively selected
                    showAlert("Save State", "No specific state selected or identified to save for '" + descriptor.getLabel() + "'. No changes made to DB.", Alert.AlertType.WARNING);
                }
                isPresentState = false; // Explicitly not present if state is cleared
            }
            // Re-evaluate rules based on the new state (or absence of state)
            evaluateApplicabilityRules(descriptor.getId(), isPresentState, true);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to save or clear state: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML private void handleLoad() {
        System.out.println("Load action triggered.");
        loadTreeData();
        showAlert("Load", "Data reloaded from the database.", Alert.AlertType.INFORMATION);
    }
    @FXML private void handleSave() {
        showAlert("Save", "Use 'Save Current State' button in details view. Global save not implemented.", Alert.AlertType.INFORMATION);
    }
    @FXML private void handleExit() {
        System.out.println("Exit action triggered.");
        javafx.application.Platform.exit();
    }
    @FXML private void handleAddDescriptor() {
        System.out.println("Add Descriptor action triggered.");
        showAlert("Add Descriptor", "Add Descriptor functionality requires a dialog (not yet implemented).", Alert.AlertType.INFORMATION);
    }
    @FXML private void handleDeleteSelected() {
        System.out.println("Delete Selected action triggered.");
        TreeItem<Descriptor> selectedItem = descriptorTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null || (selectedItem.getValue().getId() == 0 && "Descriptors".equals(selectedItem.getValue().getLabel()))) {
            showAlert("Delete", "Please select a valid descriptor to delete.", Alert.AlertType.WARNING);
            return;
        }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Descriptor: " + selectedItem.getValue().getLabel());
        confirmAlert.setContentText("Are you sure you want to delete this descriptor and all its children (if any)? This action relies on database cascade delete for states and rules.");
        if (confirmAlert.showAndWait().orElse(null) == javafx.scene.control.ButtonType.OK) {
            try {
                boolean deleted = dbManager.deleteDescriptor(selectedItem.getValue().getId());
                if(deleted) {
                    loadTreeData();
                    showAlert("Delete", "Descriptor deleted successfully. Tree refreshed.", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Delete Error", "Could not delete the descriptor from DB.", Alert.AlertType.ERROR);
                }
            } catch (SQLException e) {
                showAlert("Delete Error", "Database error during deletion: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }
    @FXML private void handleAbout() {
        System.out.println("About action triggered.");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About N-ary Tree Descriptor Editor");
        alert.setHeaderText("N-ary Tree Descriptor Editor v0.4"); // Version bump
        alert.setContentText("This application helps manage n-ary tree descriptors with applicability rules and state persistence.\nDeveloped by AI.");
        alert.showAndWait();
    }
    private void showAlert(String title, String content, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
