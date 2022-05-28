package com.powsybl.ad.viewer.view.diagram.containers;

public class ContainerVoltageDiagramPane extends ContainerDiagramPane {
    private String voltageLevelId;
    private int depth;

    public ContainerVoltageDiagramPane(String voltageLevelId, int depth) {
        this.voltageLevelId = voltageLevelId;
        this.depth = depth;
    }

    public String getVoltageLevelId() {
        return voltageLevelId;
    }

    public void setVoltageLevelId(String voltageLevelId) {
        this.voltageLevelId = voltageLevelId;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }
}
