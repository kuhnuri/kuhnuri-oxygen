package de.axxepta.oxygen.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Resource {

    public final String name;
    public final BaseXType type;
    public final boolean locked;

    public Resource(@JsonProperty("name") String name,
                    @JsonProperty("type") BaseXType type,
                    @JsonProperty("locked") boolean locked) {
        this.name = name;
        this.type = type;
        this.locked = locked;
    }
}
