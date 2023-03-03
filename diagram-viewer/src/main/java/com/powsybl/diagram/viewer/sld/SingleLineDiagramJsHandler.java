package com.powsybl.diagram.viewer.sld;

import com.powsybl.iidm.network.Container;
import com.powsybl.sld.svg.GraphMetadata;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

public class SingleLineDiagramJsHandler {

    private final TreeView<Container<?>> substationsTree;

    private Consumer<String> operateSwitch;

    private String metadata;

    public SingleLineDiagramJsHandler(TreeView<Container<?>> substationsTree) {
        this.substationsTree = substationsTree;
    }

    /**
     * Called when the JS side detect sld-breaker or sld-disconnector selection.
     * @param svgId the svg identity of svg element selected
     */
    public void handleSwitchPositionChange(String svgId) {
        // Get SwitchId from svgId using metadata
        GraphMetadata graphMetadata = GraphMetadata.parseJson(new ByteArrayInputStream(this.metadata.getBytes(StandardCharsets.UTF_8)));
        GraphMetadata.NodeMetadata node = graphMetadata.getNodeMetadata(svgId);
        String swId = node.getEquipmentId();
        // Execute action on switch
        operateSwitch.accept(swId);
    }

    /**
     * Called when the JS side detect sld-top-feeder or sld-bottom-feeder selection.
     * @param svgId the svg identity of svg element selected
     */
    public void handleSelectionChange(String svgId) {
        GraphMetadata graphMetadata = GraphMetadata.parseJson(new ByteArrayInputStream(this.metadata.getBytes(StandardCharsets.UTF_8)));
        GraphMetadata.NodeMetadata node = graphMetadata.getNodeMetadata(svgId);
        if (Objects.nonNull(node)) {
            this.substationsTree.getRoot().getChildren().forEach(child -> {
                TreeItem<Container<?>> found = findTreeViewItem(child, node.getNextVId());
                if (found != null) {
                    this.substationsTree.getSelectionModel().select(found);
                    this.substationsTree.scrollTo(this.substationsTree.getSelectionModel().getSelectedIndex());
                }
            });
        }
    }

    /**
     * Recursive search into TreeView
     * @param item tree item
     * @param vlId voltageLevel id
     */
    private TreeItem<Container<?>> findTreeViewItem(TreeItem<Container<?>> item, String vlId) {
        if (item != null) {
            if (Objects.equals(item.getValue().getId(), vlId)) {
                return item;
            }
            for (TreeItem<Container<?>> child : item.getChildren()) {
                TreeItem<Container<?>> s = findTreeViewItem(child, vlId);
                if (s != null) {
                    return s;
                }
            }
        }
        return null;
    }

    public SingleLineDiagramJsHandler setMetadata(String metadata) {
        this.metadata = metadata;
        return this;
    }

    public SingleLineDiagramJsHandler setOperateSwitch(Consumer<String> operateSwitch) {
        this.operateSwitch = operateSwitch;
        return this;
    }
}
