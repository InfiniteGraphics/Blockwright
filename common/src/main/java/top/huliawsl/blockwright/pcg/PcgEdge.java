package top.huliawsl.blockwright.pcg;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class PcgEdge {
    private final String from;
    private final String to;
    private final String fromPort;
    private final String toPort;

    public PcgEdge(String from, String to) {
        this(from, to, "output", "input");
    }

    public PcgEdge(String from, String to, String fromPort, String toPort) {
        this.from = from == null ? "" : from;
        this.to = to == null ? "" : to;
        this.fromPort = fromPort == null || fromPort.isBlank() ? "output" : fromPort;
        this.toPort = toPort == null || toPort.isBlank() ? "input" : toPort;
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
                object.has("to") ? object.get("to").getAsString() : "",
                object.has("fromPort") ? object.get("fromPort").getAsString() : "output",
                object.has("toPort") ? object.get("toPort").getAsString() : "input"
        );
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getFromPort() {
        return fromPort;
    }

    public String getToPort() {
        return toPort;
    }
}
