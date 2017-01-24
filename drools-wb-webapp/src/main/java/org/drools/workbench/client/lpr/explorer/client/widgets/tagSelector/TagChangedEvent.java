package org.drools.workbench.client.lpr.explorer.client.widgets.tagSelector;

public class TagChangedEvent {
    private String tag;

    public TagChangedEvent( String tag ) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public void setTag( String tag ) {
        this.tag = tag;
    }
}
