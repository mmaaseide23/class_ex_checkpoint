package listing;

import io.javalin.http.Context;

import java.util.List;

public class ListingController {

    private final ListingDAO listings;

    public ListingController(ListingDAO listings) {
        this.listings = listings;
    }

    public void createListing(Context ctx) {
        Listing listing = ctx.bodyValidator(Listing.class).get();
        if (listings.newListing(listing)) {
            ctx.status(201).json("Listing Created");
        } else {
            ctx.status(400).json("Failed to add listing");
        }
    }

    public void getAllListings(Context ctx) {
        List<Listing> all = listings.getAllListings();
        if (all.isEmpty()) {
            ctx.status(404).json("No Listings Found");
        } else {
            ctx.status(200).json(all);
        }
    }

    public void getListingsByProperty(Context ctx, String propertyId) {
        List<Listing> history = listings.getListingsByPropertyId(propertyId);
        if (history.isEmpty()) {
            ctx.status(404).json("No listings for property " + propertyId);
        } else {
            ctx.status(200).json(history);
        }
    }

    public void getListingsByPostCode(Context ctx, String postCode) {
        List<ListingWithPostcode> matches = listings.getListingsByPostCode(postCode);
        ctx.status(200).json(matches);
    }
}