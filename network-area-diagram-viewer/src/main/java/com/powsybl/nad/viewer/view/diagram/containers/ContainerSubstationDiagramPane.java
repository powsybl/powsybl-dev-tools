package com.powsybl.nad.viewer.view.diagram.containers;

import java.util.List;

public class ContainerSubstationDiagramPane extends ContainerDiagramPane {
    private List<String> voltageLevelIds;
    private int depth;

    public ContainerSubstationDiagramPane(List<String> voltageLevelIds, int depth) {
        this.voltageLevelIds = voltageLevelIds;
        this.depth = depth;
    }

    public List<String> getVoltageLevelIds() {
        return voltageLevelIds;
    }

    public void setVoltageLevelIds(List<String> voltageLevelIds) {
        this.voltageLevelIds = voltageLevelIds;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }
}
