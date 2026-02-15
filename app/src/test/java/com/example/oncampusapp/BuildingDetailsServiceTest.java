package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest;
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class BuildingDetailsServiceTest {
    @Mock
    PlacesClient placesClient;

    @Mock
    BuildingDetailsService.FetchBuildingDetailsCallback callback;

    @Mock
    Task<FetchPlaceResponse> fetchPlaceTask;

    @Mock
    FetchPlaceResponse fetchPlaceResponse;
    @Mock
    Task<FetchResolvedPhotoUriResponse> fetchResolvedPhotoUriTask;
    @Mock
    Place place;

    @Mock
    Uri uri;

    private BuildingDetailsService service;

    private final String fakePlaceId = "1234567890";
    private final String fakePlaceName = "Fake Building";
    private final String fakeAddress = "Fake street, fake city";
    private final String fakeUriLink = "https://fake.com/fake.jpg";

    @Before
    public void setUp() throws Exception {
        // Initialize service
        service = spy(new BuildingDetailsService(placesClient));
    }

    // Normal full data response
    @Test
    public void successful_retrieval_with_full_data() {
        // Setup
        doReturn(mock(FetchResolvedPhotoUriRequest.class))
                .when(service).buildPhotoRequest(any());
        // Tasks
        when(placesClient.fetchPlace(any()))
                .thenReturn(fetchPlaceTask);
        when(placesClient.fetchResolvedPhotoUri(any()))
                .thenReturn(fetchResolvedPhotoUriTask);
        // Place return values
        when(place.getDisplayName()).thenReturn(fakePlaceName);
        when(place.getFormattedAddress()).thenReturn(fakeAddress);
        PhotoMetadata photoMetadata = mock(PhotoMetadata.class);
        when(place.getPhotoMetadatas())
                .thenReturn(Collections.singletonList(photoMetadata));

        // Mock place response
        when(fetchPlaceResponse.getPlace()).thenReturn(place);
        when(fetchPlaceTask.addOnSuccessListener(any()))
                .thenAnswer(invocation -> {
                    OnSuccessListener<FetchPlaceResponse> listener =
                            invocation.getArgument(0);
                    listener.onSuccess(fetchPlaceResponse);
                    return fetchPlaceTask;
                });
        when(fetchPlaceTask.addOnFailureListener(any()))
                .thenReturn(fetchPlaceTask);

        // Mock photo response
        when(placesClient.fetchResolvedPhotoUri(any())).thenReturn(fetchResolvedPhotoUriTask);
        FetchResolvedPhotoUriResponse photoResponse = mock(FetchResolvedPhotoUriResponse.class);
        when(uri.toString()).thenReturn(fakeUriLink);
        when(photoResponse.getUri()).thenReturn(uri);
        when(fetchResolvedPhotoUriTask.addOnSuccessListener(any()))
                .thenAnswer(invocation -> {
                    OnSuccessListener<FetchResolvedPhotoUriResponse> listener =
                            invocation.getArgument(0);
                    listener.onSuccess(photoResponse);
                    return fetchResolvedPhotoUriTask;
                });
        when(fetchResolvedPhotoUriTask.addOnFailureListener(any())).thenReturn(fetchResolvedPhotoUriTask);

        // Call the method
        service.fetchBuildingDetails(fakePlaceId, callback);

        // Assert callback was called
        ArgumentCaptor<BuildingDetailsDto> dtoCaptor =
                ArgumentCaptor.forClass(BuildingDetailsDto.class);
        verify(callback).onSuccess(any());
        verify(callback, times(1)).onSuccess(dtoCaptor.capture());

        // Assert correct Dto
        BuildingDetailsDto dto = dtoCaptor.getValue();
        assertEquals(fakePlaceName, dto.getName());
        assertEquals(fakeAddress, dto.getAddress());
        assertEquals(fakeUriLink,dto.getImgUri());
    }

    // Illegal Argument Exception due to null placeId
    @Test
    public void handling_null_placeId_input() {
        // Call method with null placeId
        service.fetchBuildingDetails(null, callback);

        // Capture the exception passed to callback
        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback, times(1)).onFailure(captor.capture());
        verify(callback, never()).onSuccess(any());

        // Assert the exception type and message
        Exception captured = captor.getValue();
        assertTrue(captured instanceof IllegalArgumentException);
        assertEquals("Invalid placeId", captured.getMessage());
    }

    // Illegal Argument Exception due to empty placId
    @Test
    public void handling_empty_placeId_string() {
        // Call method with empty placeId
        service.fetchBuildingDetails("", callback);

        // Capture the exception passed to callback
        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback, times(1)).onFailure(captor.capture());
        verify(callback, never()).onSuccess(any());

        // Assert the exception type and message
        Exception captured = captor.getValue();
        assertTrue(captured instanceof IllegalArgumentException);
        assertEquals("Invalid placeId", captured.getMessage());
    }

    // In case of failure on Google's end, just have onFailure called
    @Test
    public void fetchPlace_failure_handling() {
        //Set up
        Exception exception = new RuntimeException("Network error");

        when(placesClient.fetchPlace(any()))
                .thenReturn(fetchPlaceTask);
        // Mock place response
        when(fetchPlaceTask.addOnSuccessListener(any()))
                .thenReturn(fetchPlaceTask);
        when(fetchPlaceTask.addOnFailureListener(any()))
                .thenAnswer(invocation -> {
                    OnFailureListener listener = invocation.getArgument(0);
                    listener.onFailure(exception);
                    return fetchPlaceTask;
                });

        // Call method
        service.fetchBuildingDetails("invalidPlaceId", callback);

        // Assert callback
        verify(callback, times(1)).onFailure(exception);
        verify(callback, never()).onSuccess(any());
    }

    // Success but no photo metadata (not sure if this even happens)
    @Test
    public void success_with_null_photo_metadata() {
        // Setup
        when(placesClient.fetchPlace(any()))
                .thenReturn(fetchPlaceTask);

        when(fetchPlaceResponse.getPlace()).thenReturn(place);
        when(place.getDisplayName()).thenReturn(fakePlaceName);
        when(place.getFormattedAddress()).thenReturn(fakeAddress);
        when(place.getPhotoMetadatas()).thenReturn(null); // Null photo metadata


        when(fetchPlaceTask.addOnSuccessListener(any()))
                .thenAnswer(invocation -> {
                    OnSuccessListener<FetchPlaceResponse> listener =
                            invocation.getArgument(0);
                    listener.onSuccess(fetchPlaceResponse);
                    return fetchPlaceTask;
                });

        when(fetchPlaceTask.addOnFailureListener(any()))
                .thenReturn(fetchPlaceTask);

        // Call the method
        service.fetchBuildingDetails(fakePlaceId, callback);

        // Assert callback
        ArgumentCaptor<BuildingDetailsDto> dtoCaptor =
                ArgumentCaptor.forClass(BuildingDetailsDto.class);
        verify(callback).onSuccess(any());

        verify(callback, times(1)).onSuccess(dtoCaptor.capture());

        // Assert correct Dto
        BuildingDetailsDto dto = dtoCaptor.getValue();
        assertEquals(fakePlaceName, dto.getName());
        assertEquals(fakeAddress, dto.getAddress());
        assertNull(dto.getImgUri());
    }

    // Success with empty list of photo metadata
    @Test
    public void success_with_empty_photo_metadata_list() {
        // Setup
        when(placesClient.fetchPlace(any()))
                .thenReturn(fetchPlaceTask);

        when(fetchPlaceResponse.getPlace()).thenReturn(place);
        when(place.getDisplayName()).thenReturn(fakePlaceName);
        when(place.getFormattedAddress()).thenReturn(fakeAddress);
        when(place.getPhotoMetadatas()).thenReturn(Collections.emptyList()); // Empty list of photometadata


        when(fetchPlaceTask.addOnSuccessListener(any()))
                .thenAnswer(invocation -> {
                    OnSuccessListener<FetchPlaceResponse> listener =
                            invocation.getArgument(0);
                    listener.onSuccess(fetchPlaceResponse);
                    return fetchPlaceTask;
                });

        when(fetchPlaceTask.addOnFailureListener(any()))
                .thenReturn(fetchPlaceTask);

        // Call the method
        service.fetchBuildingDetails(fakePlaceId, callback);

        // Assert callback
        ArgumentCaptor<BuildingDetailsDto> dtoCaptor =
                ArgumentCaptor.forClass(BuildingDetailsDto.class);
        verify(callback).onSuccess(any());
        verify(callback, times(1)).onSuccess(dtoCaptor.capture());

        // Assert correct Dto
        BuildingDetailsDto dto = dtoCaptor.getValue();
        assertEquals(fakePlaceName, dto.getName());
        assertEquals(fakeAddress, dto.getAddress());
        assertNull(dto.getImgUri());
    }


}