package io.github.lukmnh.toolbox.common.tool;

/** Kategori tool, menentukan pengelompokan di Home dan sidebar. Urutan enum = urutan tampil. */
public enum ToolCategory {
    PDF("category.pdf"),
    JSON("category.json"),
    DEV_UTILS("category.devutils"),
    PRODUCTIVITY("category.productivity");

    private final String messageKey;

    ToolCategory(String messageKey) {
        this.messageKey = messageKey;
    }

    /** Key di {@code messages.properties} untuk nama kategori. */
    public String messageKey() {
        return messageKey;
    }

    /** Style class untuk warna aksen kategori, misalnya {@code category-pdf}. */
    public String styleClass() {
        return "category-" + messageKey.substring(messageKey.indexOf('.') + 1);
    }
}
