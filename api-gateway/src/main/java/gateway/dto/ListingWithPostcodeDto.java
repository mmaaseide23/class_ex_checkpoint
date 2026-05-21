package gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ListingWithPostcodeDto {
    public long propertyId;
    public LocalDate listingDate;
    public long price;
    public String status;
    public String postCode;
    public String address;
}