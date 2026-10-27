package io.github.lukmnh.toolbox.json.viewer.domain;

public enum NodeType {
    OBJECT,
    ARRAY,
    STRING,
    NUMBER,
    BOOLEAN,
    NULL;

    public boolean isContainer() {
        return this == OBJECT || this == ARRAY;
    }
}
