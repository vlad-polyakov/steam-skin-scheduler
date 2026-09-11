package com.steam.skin.scheduler.content.entity.vdf;

import java.util.List;
import java.util.Optional;

public final class VdfNode {

    private final String key;
    private final String value;
    private final boolean object;
    private final List<VdfNode> children;

    private VdfNode(
            String key,
            String value,
            boolean object,
            List<VdfNode> children
    ) {
        this.key = key;
        this.value = value;
        this.object = object;
        this.children = children;
    }

    public static VdfNode value(String key, String value) {
        return new VdfNode(
                key,
                value,
                false,
                List.of()
        );
    }

    public static VdfNode object(String key, List<VdfNode> children) {
        return new VdfNode(
                key,
                null,
                true,
                List.copyOf(children)
        );
    }

    public String key() {
        return key;
    }

    public String value() {
        return value;
    }

    public boolean isValue() {
        return !object;
    }


    public List<VdfNode> children() {
        return children;
    }

    public Optional<VdfNode> first(String key) {
        return children.stream()
                .filter(child -> child.key.equalsIgnoreCase(key))
                .findFirst();
    }

    public List<VdfNode> findAll(String key) {
        return children.stream()
                .filter(child -> child.key.equalsIgnoreCase(key))
                .toList();
    }

    @Override
    public String toString() {
        if (isValue()) {
            return key + " = " + value;
        }

        return key + " {" +
                " children=" + children.size() +
                " }";
    }
}