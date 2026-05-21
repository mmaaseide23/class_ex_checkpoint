package gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PurchaserWithPreferencesDto {
    public int id;
    public String firstName;
    public String lastName;
    public String email;
    public List<String> postcodes = new ArrayList<>();
}