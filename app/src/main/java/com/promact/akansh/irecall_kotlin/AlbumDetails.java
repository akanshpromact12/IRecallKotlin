package com.promact.akansh.irecall_kotlin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Created by Akansh on 21-08-2017.
 */

public class AlbumDetails implements Comparable, Serializable {
    public String AlbumId;
    public String Date;
    public String Filename;
    public String Latitude;
    public String Longitude;
    public String MediaId;
    public String caption;
    public String thumbnail;
    public String MediaType;

    @JsonCreator
    public AlbumDetails(@JsonProperty("AlbumId") String AlbumId, @JsonProperty("Date") String Date,
                        @JsonProperty("Filename") String Filename, @JsonProperty("Latitude") String Latitude,
                        @JsonProperty("Longitude") String Longitude, @JsonProperty("MediaId") String MediaId,
                        @JsonProperty("caption") String caption,
                        @JsonProperty("Thumbnail") String thumbnail,
                        @JsonProperty("MediaType") String MediaType) {
        this.AlbumId = AlbumId;
        this.Date = Date;
        this.Filename = Filename;
        this.Latitude = Latitude;
        this.Longitude = Longitude;
        this.MediaId = MediaId;
        this.caption = caption;
        this.thumbnail = thumbnail;
        this.MediaType = MediaType;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
