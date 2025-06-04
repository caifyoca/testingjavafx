// --- DescriptorTreeCell.java (Modifications) ---
package com.example.narytree.ui;

import com.example.narytree.model.Descriptor;
import javafx.scene.control.TreeCell;
import java.util.Map; // Added

public class DescriptorTreeCell extends TreeCell<Descriptor> {

    private final Map<Integer, String> descriptorRuleEffects; // Added

    public DescriptorTreeCell(Map<Integer, String> descriptorRuleEffects) { // Updated constructor
        this.descriptorRuleEffects = descriptorRuleEffects;
    }

    @Override
    protected void updateItem(Descriptor item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            setStyle(""); // Reset style
        } else {
            setText(item.getLabel() + (item.isNumeric() ? " (N)" : " (Q)")); // Shorter type indicator

            String ruleEffect = descriptorRuleEffects.get(item.getId());

            if ("disabled".equals(ruleEffect)) {
                setStyle("-fx-text-fill: grey; -fx-font-style: italic; -fx-background-color: #f0f0f0;"); // Disabled look
            } else if ("hidden_revealed".equals(ruleEffect)) {
                setStyle("-fx-text-fill: darkblue;"); // Revealed look
            } else if (item.isHiddenDefaultly()) {
                // Not actively revealed by a rule, but was hidden by default.
                // This implies it should not be visible unless a rule *would* reveal it if its source was present.
                // For simplicity now, if is_hidden_defaultly is true and no rule revealed it, it's "dimmed".
                // This interaction is complex. A true "hidden" TreeItem is harder.
                // We'll rely on the controller not adding it or a more sophisticated filter if truly hidden.
                // For now, let's just style it as 'default hidden'.
                 setStyle("-fx-text-fill: lightgrey; -fx-font-style: italic;");
            } else {
                setStyle(""); // Reset to default style
            }
        }
    }
}
