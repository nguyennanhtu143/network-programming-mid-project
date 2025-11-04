package dev.p3ntest.zombies.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapInfo {
    private String id;
    private JsonNode level;
    private String name;
    private Boolean verified;
    private Boolean published;
}


