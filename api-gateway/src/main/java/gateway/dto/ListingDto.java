package gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ListingDto {
    public int id;
    public long propertyId;
    public LocalDate listingDate;
    public long price;
    public String status;
}