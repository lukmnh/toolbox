package io.github.lukmnh.toolbox.json.beautify.domain;

/** @param truncated {@code true} bila file lebih besar dari batas yang dibaca */
public record TextPreview(String text, boolean truncated) {}
