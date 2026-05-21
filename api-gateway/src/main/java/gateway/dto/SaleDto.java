package gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaleDto {
    public long propertyId;
    public String councilName;
    public long purchasePrice;
    public String address;
    public String postCode;
    public String propertyType;
    public double area;
    public LocalDate contractDate;
    public String zoning;
    public String primaryPurpose;
}