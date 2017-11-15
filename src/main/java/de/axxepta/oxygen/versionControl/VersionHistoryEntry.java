package de.axxepta.oxygen.versioncontrol;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URL;
import java.time.LocalDateTime;

/**
 * @author Markus on 01.02.2016.
 */
public class VersionHistoryEntry {

    private URL url;
    private int version;
    private int revision;
    private LocalDateTime changeDate;

    public VersionHistoryEntry(@JsonProperty("url") URL url,
                               @JsonProperty("version") int version,
                               @JsonProperty("revision") int revision,
                               @JsonProperty("changeDate") LocalDateTime changeDate) {
        this.url = url;
        this.version = version;
        this.revision = revision;
        this.changeDate = changeDate;
    }

    Object[] getDisplayVector() {
        return new Object[]{version, revision, changeDate };
    }

    protected URL getURL() {
        return url;
    }

}
