package com.powsybl.ad.viewer.view.diagram.containers;

public abstract class AbstractSelectableContainer {
    protected final String id;

    protected final String name;

    AbstractSelectableContainer(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
