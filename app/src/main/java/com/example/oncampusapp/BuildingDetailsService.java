package com.example.oncampusapp;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;
import java.util.List;

public class BuildingDetailsService {
    private final PlacesClient placesClient;

    public BuildingDetailsService(Context context) {
        Context appContext = context.getApplicationContext();
        if (!Places.isInitialized()){
            Places.initializeWithNewPlacesApiEnabled(appContext, fetchApiKey(appContext));
        }
        placesClient = Places.createClient(appContext);
    }
    public BuildingDetailsService(PlacesClient client){
        this.placesClient = client;
    }
    
    public PlacesClient getPlacesClient() {
        return placesClient;
    }
    
    /**
     * Retrieves the Google Maps API key from the application's AndroidManifest.xml metadata.
     * 
     * @param context the application context
     * @return the API key string
     * @throws IllegalStateException if the API key is missing, empty, or metadata cannot be read
     */
    protected String fetchApiKey(Context context){
        String apiKey;
        try{
            ApplicationInfo applicationInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = applicationInfo.metaData;
            if (bundle == null) {
                throw new IllegalStateException(
                        "Missing application meta-data. Please configure com.google.android.geo.API_KEY in AndroidManifest.xml.");
            }
            apiKey = bundle.getString("com.google.android.geo.API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalStateException(
                        "Missing or empty API key. Please configure MAPS_API_KEY in local.properties.");
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(
                    "Unable to load application info to read com.google.android.geo.API_KEY from AndroidManifest.xml.", e);
        }
        return apiKey;
    }
    
    /**
     * Fetches building details from Google Places API using a place ID.
     * Retrieves the building's name, address, and photo (if available), then returns them via callback.
     * 
     * @param placeId the Google Places ID of the building
     * @param callback the callback to invoke with results or errors
     */
    public void fetchBuildingDetails(String placeId, FetchBuildingDetailsCallback callback){
        if (placeId == null || placeId.isEmpty()){
            callback.onFailure(new IllegalArgumentException("Invalid placeId"));
            return;
        }
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.PHOTO_METADATAS
        );
        final FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId,fields);
        placesClient.fetchPlace(request)
                .addOnSuccessListener(fetchPlaceResponse -> processFetchPlaceResponse(fetchPlaceResponse, callback))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Processes the response from the Places API fetch request.
     * Extracts place details and fetches the first photo URI if available, then invokes the callback.
     * 
     * @param response the FetchPlaceResponse containing place information
     * @param callback the callback to invoke with the constructed BuildingDetailsDto
     */
    private void processFetchPlaceResponse(FetchPlaceResponse response, FetchBuildingDetailsCallback callback){

        final Place place = response.getPlace();
        // Constructing Dto
        BuildingDetailsDto dto = new BuildingDetailsDto();
        dto.setName(place.getDisplayName());
        dto.setAddress(place.getFormattedAddress());
        final List<PhotoMetadata> metadata = place.getPhotoMetadatas();

        // If there's no image
        if (metadata == null || metadata.isEmpty()) {
            callback.onSuccess(dto);
            return;
        }
        // If there's image
        final PhotoMetadata photoMetadata = metadata.get(0);
        final FetchResolvedPhotoUriRequest photoRequest = buildPhotoRequest(photoMetadata);

        placesClient.fetchResolvedPhotoUri(photoRequest)
                .addOnSuccessListener(fetchResolvedPhotoUriResponse -> {
                    Uri uri = fetchResolvedPhotoUriResponse.getUri();
                    if (uri != null) {  
                        dto.setImgUri(uri.toString());  
                    }
                    callback.onSuccess(dto);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Builds a request to fetch a resolved photo URI with specified dimensions.
     * 
     * @param photoMetadata the photo metadata from the place details
     * @return a FetchResolvedPhotoUriRequest configured with max width and height
     */
    protected FetchResolvedPhotoUriRequest buildPhotoRequest(PhotoMetadata photoMetadata) {
        return FetchResolvedPhotoUriRequest.builder(photoMetadata)
                .setMaxWidth(1200)
                .setMaxHeight(600)
                .build();
    }

    /**
     * Callback interface for handling building details fetch results.
     * Provides methods to handle both successful and failed fetch operations.
     */
    public interface FetchBuildingDetailsCallback {
        void onSuccess(BuildingDetailsDto buildingDetailsDto);
        void onFailure(Exception e);
    }

}
