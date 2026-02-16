package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
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
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class PlaceDetailsServiceTest {
    @Mock
    PlacesClient placesClient;
    @Mock
    PlaceDetailsService.FetchBuildingDetailsCallback callback;
    @Mock
    Task<FetchPlaceResponse> fetchPlaceTask;
    @Mock
    FetchPlaceResponse fetchPlaceResponse;
    @Mock
    Task<FetchResolvedPhotoUriResponse> fetchResolvedPhotoUriTask;
    @Mock
    FetchResolvedPhotoUriResponse fetchResolvedPhotoUriResponse;
    @Mock
    FetchResolvedPhotoUriRequest fetchResolvedPhotoUriRequest;
    @Mock
    PhotoMetadata photoMetadata;
    @Mock
    Place place;
    @Mock
    Uri uri;
    @Mock
    Context context;
    @Mock
    PackageManager pm;
    @Mock
    ApplicationInfo appInfo;
    @Mock
    Bundle bundle;

    private PlaceDetailsService service;

    private final String fakePlaceId = "1234567890";
    private final String fakePlaceName = "Fake Building";
    private final String fakeAddress = "Fake street, fake city";
    private final String fakeUriLink = "https://fake.com/fake.jpg";
    private final String fakeApiKey = "FAKE_KEY";

    @Before
    public void setUp() {
        // Initialize service
        service = spy(new PlaceDetailsService(placesClient));
    }
    // Normal full data response
    @Test
    public void successful_fetch_with_full_data() {
        this.common_successful_retrieval_mock();
        // Place return values
        when(place.getDisplayName()).thenReturn(fakePlaceName);
        when(place.getFormattedAddress()).thenReturn(fakeAddress);
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
    @Test
    public void successful_fetch_with_null_data() {
        this.common_successful_retrieval_mock();

        // Place return values
        when(place.getDisplayName()).thenReturn(null);
        when(place.getFormattedAddress()).thenReturn(null);
        when(fetchResolvedPhotoUriResponse.getUri()).thenReturn(null);
        // Call the method
        service.fetchBuildingDetails(fakePlaceId, callback);

        // Assert callback was called
        ArgumentCaptor<BuildingDetailsDto> dtoCaptor =
                ArgumentCaptor.forClass(BuildingDetailsDto.class);
        verify(callback).onSuccess(any());
        verify(callback, times(1)).onSuccess(dtoCaptor.capture());

        // Assert correct Dto
        BuildingDetailsDto dto = dtoCaptor.getValue();
        assertNull(dto.getName());
        assertNull(dto.getAddress());
        assertNull(dto.getImgUri());
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
    @Test
    public void buildPhotoRequest_invalidMetadata_throwsException() {
        PlaceDetailsService service =
                new PlaceDetailsService(mock(PlacesClient.class));

        assertThrows(IllegalArgumentException.class, () ->
                service.buildPhotoRequest(photoMetadata)
        );
    }
    @Test
    public void constructor_withContext_executes() {
        try (MockedStatic<Places> mockedPlaces = mockStatic(Places.class)) {
            mockedPlaces.when(Places::isInitialized).thenReturn(true); // Skip initializing place
            Context context = mock(Context.class);

            when(context.getApplicationContext()).thenReturn(context);

            mockedPlaces.when(() -> Places.createClient(context))
                    .thenReturn(mock(PlacesClient.class));

            PlaceDetailsService service =
                    new PlaceDetailsService(context);

            assertNotNull(service);
        }
    }
    @Test
    public void fetchApiKey_returnsCorrectKey() throws Exception {
        // Mocking
        this.common_fetchApiKey_mock();
        String key = service.fetchApiKey(context);
        // Assert
        assertEquals(fakeApiKey, key);
    }
    @Test
    public void fetchApiKey_missing_metadata_throwsException() throws Exception {
        // Mock Context and PackageManager
        this.common_fetchApiKey_mock();
        // Overwrite mock to return null metadata
        appInfo.metaData = null;
        // Call the method and assert
        assertThrows(IllegalStateException.class, () -> service.fetchApiKey(context));
    }
    @Test
    public void fetchApiKey_null_key_throwsException() throws Exception {
        // Mock Context and PackageManager
        this.common_fetchApiKey_mock();
        // Overwrite mock to return null api key
        when(bundle.getString("com.google.android.geo.API_KEY")).thenReturn(null);
        // Call the method and assert
        assertThrows(IllegalStateException.class, () -> service.fetchApiKey(context));
    }@Test
    public void fetchApiKey_empty_key_throwsException() throws Exception {
        // Mock Context and PackageManager
        this.common_fetchApiKey_mock();
        // Overwrite mock to return an empty string
        when(bundle.getString("com.google.android.geo.API_KEY")).thenReturn("");
        // Call the method and assert
        assertThrows(IllegalStateException.class, () -> service.fetchApiKey(context));
    }
    @Test
    public void fetchApiKey_fail_to_load_packagemanager_throwsException() throws Exception {
        // Mock Context and PackageManager
        this.common_fetchApiKey_mock();
        // Overwrite mock to throw exception
        when(pm.getApplicationInfo("com.example", PackageManager.GET_META_DATA))
                .thenThrow(new PackageManager.NameNotFoundException());
        // Assert
        assertThrows(IllegalStateException.class, () -> service.fetchApiKey(context));
    }
    // Common mocking when every request is successful
    private void common_successful_retrieval_mock() {
        when(place.getPhotoMetadatas())
                .thenReturn(Collections.singletonList(photoMetadata));
        // Tasks
        when(placesClient.fetchPlace(any()))
                .thenReturn(fetchPlaceTask);
        when(placesClient.fetchResolvedPhotoUri(any()))
                .thenReturn(fetchResolvedPhotoUriTask);
        // Response
        when(fetchPlaceResponse.getPlace()).thenReturn(place);
        doReturn(fetchResolvedPhotoUriRequest)
                .when(service).buildPhotoRequest(any());
        // Mock Callback
        when(fetchPlaceTask.addOnSuccessListener(any()))
                .thenAnswer(invocation -> {
                    OnSuccessListener<FetchPlaceResponse> listener =
                            invocation.getArgument(0);
                    listener.onSuccess(fetchPlaceResponse);
                    return fetchPlaceTask;
                });
        when(fetchPlaceTask.addOnFailureListener(any()))
                .thenReturn(fetchPlaceTask);

        when(fetchResolvedPhotoUriTask.addOnSuccessListener(any()))
                .thenAnswer(invocation -> {
                    OnSuccessListener<FetchResolvedPhotoUriResponse> listener =
                            invocation.getArgument(0);
                    listener.onSuccess(fetchResolvedPhotoUriResponse);
                    return fetchResolvedPhotoUriTask;
                });
        when(fetchResolvedPhotoUriTask.addOnFailureListener(any())).thenReturn(fetchResolvedPhotoUriTask);
        // Mock photo response
        when(uri.toString()).thenReturn(fakeUriLink);
        when(fetchResolvedPhotoUriResponse.getUri()).thenReturn(uri);
    }
    // Common mocking for fetchApiKey test
    private void common_fetchApiKey_mock() throws PackageManager.NameNotFoundException {
        context = mock(Context.class);
        pm = mock(PackageManager.class);
        appInfo = mock(ApplicationInfo.class);
        bundle = mock(Bundle.class);
        when(bundle.getString("com.google.android.geo.API_KEY")).thenReturn(fakeApiKey);

        when(context.getPackageManager()).thenReturn(pm);
        when(context.getPackageName()).thenReturn("com.example");
        when(pm.getApplicationInfo("com.example", PackageManager.GET_META_DATA))
                .thenReturn(appInfo);
        appInfo.metaData = bundle;
    }
}