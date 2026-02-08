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
    private String fetchApiKey(Context context){
        try{
            ApplicationInfo applicationInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = applicationInfo.metaData;
            return bundle.getString("com.google.android.geo.API_KEY");
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
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
                    dto.setImgUri(uri.toString());;
                    callback.onSuccess(dto);
                })
                .addOnFailureListener(callback::onFailure);
    }
    protected FetchResolvedPhotoUriRequest buildPhotoRequest(PhotoMetadata photoMetadata) {
        return FetchResolvedPhotoUriRequest.builder(photoMetadata)
                .setMaxWidth(1200)
                .setMaxHeight(600)
                .build();
    }


    public interface FetchBuildingDetailsCallback {
        void onSuccess(BuildingDetailsDto buildingDetailsDto);
        void onFailure(Exception e);
    }

}
