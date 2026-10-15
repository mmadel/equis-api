package com.equestrian.equis.dto.riders;

import com.fasterxml.jackson.annotation.JsonProperty;

// Which way a level change goes. Any other JSON value fails reading, so Spring answers 400.
public enum Direction {
    @JsonProperty("up") UP,
    @JsonProperty("down") DOWN
}
