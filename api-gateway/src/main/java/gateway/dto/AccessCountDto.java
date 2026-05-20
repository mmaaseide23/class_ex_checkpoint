package gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessCountDto {
    public int id;
    public String accessType;
    public String accessValue;
    public int count;
    public LocalDateTime lastAccessed;
}