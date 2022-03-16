package com.powsybl.ad.viewer.view.diagram.containers;

public abstract class AbstractSelectableContainer
{
    protected final String id;

    protected final String name;

    protected boolean checkedProperty = false;

    protected boolean saveDiagrams = true;

    AbstractSelectableContainer(String id, String name)
    {
        this.id = id;
        this.name = name;
        addListenerOnCheck();
    }

    protected abstract void addListenerOnCheck();

    protected abstract void removeDiagramTab();

    abstract void addDiagramTab();

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public boolean checkedProperty()
    {
        return checkedProperty;
    }

    public void setCheckedProperty(boolean b)
    {
        checkedProperty = b;
    }

    public void setSaveDiagrams(boolean saveDiagrams)
    {
        this.saveDiagrams = saveDiagrams;
    }
}
