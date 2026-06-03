package top.huliawsl.blockwright.pcg;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class PcgEdge {
    private final String from;
    private final String to;

    public PcgEdge(String from, String to) {
        this.from = from == null ? "" : from;
        this.to = to == null ? "" : to;
    }

    public static PcgEdge fromArray(JsonArray array) {
        if (array.size() < 2) {
            return new PcgEdge("", "");
        }
        return new PcgEdge(array.get(0).getAsString(), array.get(1).getAsString());
    }

    public static PcgEdge fromObject(JsonObject object) {
        return new PcgEdge(
                object.has("from") ? object.get("from").getAsString() : "",
                object.has("to") ? object.get("to").getAsString() : ""
        );
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }
}
