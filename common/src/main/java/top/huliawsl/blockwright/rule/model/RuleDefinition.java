package top.huliawsl.blockwright.rule.model;

import com.google.gson.JsonObject;

import java.nio.file.Path;

public final class RuleDefinition {
    public String id;
    public String executor;
    public JsonObject config = new JsonObject();
    public transient Path sourcePath;
}
