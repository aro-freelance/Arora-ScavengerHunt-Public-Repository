package com.aro.aroscavengerhunt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aro.aroscavengerhunt.helpers.CameraPermissionHelper;
import com.aro.aroscavengerhunt.samplerender.SampleRender;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


//I am currently working on getting the AR camera to display. (done)
// I have a working GPS map which finds nearby points of interest and then on button push (in menu) it will return a random
// nearby point of interest


// Next get user position data and determine direction and distance to destination at start or game
// Each X time calculate direction and distance to destination
// Use this to determine if the user is moving towards the destination (done)
// TODO fix the AR display so that the menu still shows
// Give user feedback based on directional information. (done)
// Set an endgame condition and feedback (done)
// Clean the UI up so that it is not displaying any of the backend data (done)
// Start a new game as soon as the app launches (done)
// hide new game button until game is ended and then again when it is pressed (done)
// Add instructions for user (done)
// Fix types so that they can be interacted with... turns out we were getting Place.Type data and needed to convert to String (done)
// Build the framework to generate hints from type data and display it to the initial alert message (done)
// Added framework for bounding based on range from user. (done)
// Added framework for bounding based on type category by user. (done)
// Added all the types to the switch. (done)
// Added a clue after the clueString to display the first letter of the location (done)
// add a button to display clues again (done)
// add clues to the type switch (done)

//TODO after clues are added test, package and release.
// Need a release file that can be sent via internet without having to publish if possible for maximum speed.

/**
 * UPDATE PLANS
 */
// TODO fine tune clues and add more clues
// TODO add a way for the user to set the radius of the game
// TODO add a way for the user to see hints in increasing escalation ... distance, direction, address
// TODO add a way for user to select the type they want before the game is started (i.e random bakery)
      //to do this we will want to return the types first,
      // then display them in a listview/recyclerview
      // then allow the user to click one to select it
      // then using that selection get a random location with that type


public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback ,
        SampleRender.Renderer,
        FragmentOnAttachListener,
        BaseArFragment.OnTapArPlaneListener,
        BaseArFragment.OnSessionConfigurationListener,
        ArFragment.OnViewCreatedListener{



    private static final String TAG = MainActivity.class.getSimpleName();

    private ArFragment arFragment;
    private Renderable model;
    private ViewRenderable viewRenderable;
    private GoogleMap map;
    private CameraPosition cameraPosition;

    private Button newGameButton;
    private Button hintButton;

    // The entry point to the Places API.
    private PlacesClient placesClient;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient fusedLocationProviderClient;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng defaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location lastKnownLocation;
    private Location userLocationAtStartOfGame;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    // Used for selecting the current place.
    private static final int M_MAX_ENTRIES = 5;
    private String[] likelyPlaceNames;
    private String[] likelyPlaceAddresses;
    private List[] likelyPlaceAttributions;
    private List<Place.Type>[] likelyPlaceTypes;
    private LatLng[] likelyPlaceLatLngs;

    private String destinationName;
    private String destinationAddress;
    private List destinationAttributes;
    private List destinationTypes;
    private LatLng destinationLatLng;

    private float originalDistance;
    private float newDistance;
    private float originalBearingToDestination;
    private float newBearingToDestination;
    private float currentBearing;

    private String clueString = "";

    private boolean gameRunning = false;
    private final int delay = 10000;
    private final int endGameDistance = 5;

    private boolean isCategorySelected = false;
    private String selectedCategoryString = "";

    private List<String> clueList = new ArrayList<>();

    //default range is about .75 miles
    private int gameRadius = 1200;
    private boolean isInRange = false;
    private boolean isCorrectCategory = false;

    private int clueListMaxSize = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_main);

        newGameButton = findViewById(R.id.new_game_button);
        hintButton = findViewById(R.id.hint_button);

        getSupportFragmentManager().addFragmentOnAttachListener(this);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }



        loadModels();

        // Construct a PlacesClient
        Places.initialize(getApplicationContext(), BuildConfig.API_KEY);
        placesClient = Places.createClient(this);

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.d(TAG, "ERROR: mapfragment is null");
        }



        newGameButton.setOnClickListener(v -> readyPlaceInfoAndStartGame());
        hintButton.setOnClickListener(v -> displayClues());

    }

    @Override
    protected void onResume() {
        super.onResume();

        // ARCore requires camera permission to operate.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this);
            return;
        }



        //get user's change in position every X seconds
        final Handler ha = new Handler();
        ha.postDelayed(new Runnable() {

            @Override
            public void run() {

                if(gameRunning){
                    Log.d("testing", "ping");
                    updateDeltaPosition();

                    //distance is in meters..
                    //when distance is less than 5 end the game
                    if(newDistance < endGameDistance){
                        endGame();
                    }
                }

                ha.postDelayed(this, delay);
            }
        }, delay);

    }


    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (map != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, map.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("testing", "options menu created");
        getMenuInflater().inflate(R.menu.current_place_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.option_get_place) {
            readyPlaceInfoAndStartGame();
        }
        if(item.getItemId() == R.id.option_get_hint){
            giveUserDirectionalFeedback();
        }
        return true;
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        Log.d("testing", "on map ready.");

        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        this.map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                Log.d("testing", "get info contents");
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                        (FrameLayout) findViewById(R.id.map), false);

                TextView title = infoWindow.findViewById(R.id.title);
                title.setText(marker.getTitle());

                TextView snippet = infoWindow.findViewById(R.id.snippet);
                snippet.setText(marker.getSnippet());

                return infoWindow;
            }
        });

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        if(!gameRunning){
            Log.d("testing", "game is not running, starting game");
            //TODO: turn on when done testing
            readyPlaceInfoAndStartGame();
        }
    }


    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        Log.d("testing", "get device location");
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                Log.d("testing", "location permission granted");
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));

                                currentBearing = lastKnownLocation.getBearing();

                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            } else {
                Log.d("testing", "location permission denied");
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }


    }


    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        Log.d("testing", "get location permission");
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        Log.d("testing", "on request permissions result");

        /**
         * Location Permission
         */
        locationPermissionGranted = false;
        if (requestCode
                == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        updateLocationUI();


        /**
         * AR Permission
         */
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }

    }

    /**
     * Prompts the user to select the current place from a list of likely places, and shows the
     * current place on the map - provided the user has granted location permission.
     */
    private void readyPlaceInfoAndStartGame() {

        if (map == null) {
            return;
        }

        if (locationPermissionGranted) {
            Log.d("testing", "show current place... permission");
            // Use fields to define the data types to return.
            List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.TYPES,
                    Place.Field.LAT_LNG);


            // Use the builder to create a FindCurrentPlaceRequest.
            FindCurrentPlaceRequest request =
                    FindCurrentPlaceRequest.newInstance(placeFields);



            // Get the likely places - that is, the businesses and other points of interest that
            // are the best match for the device's current location.
            @SuppressWarnings("MissingPermission") final
            Task<FindCurrentPlaceResponse> placeResult =
                    placesClient.findCurrentPlace(request);
            placeResult.addOnCompleteListener (new OnCompleteListener<FindCurrentPlaceResponse>() {
                @Override
                public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FindCurrentPlaceResponse likelyPlaces = task.getResult();

                        // Set the count, handling cases where less than 5 entries are returned.
                        int count;
                        if (likelyPlaces.getPlaceLikelihoods().size() < M_MAX_ENTRIES) {
                            count = likelyPlaces.getPlaceLikelihoods().size();
                        } else {
                            count = M_MAX_ENTRIES;
                        }

                        int i = 0;
                        likelyPlaceNames = new String[count];
                        likelyPlaceAddresses = new String[count];
                        likelyPlaceAttributions = new List[count];
                        likelyPlaceTypes = new List[count];
                        likelyPlaceLatLngs = new LatLng[count];

                        for (PlaceLikelihood placeLikelihood : likelyPlaces.getPlaceLikelihoods()) {
                            // Build a list of likely places
                            likelyPlaceNames[i] = placeLikelihood.getPlace().getName();
                            likelyPlaceAddresses[i] = placeLikelihood.getPlace().getAddress();
                            likelyPlaceAttributions[i] = placeLikelihood.getPlace()
                                    .getAttributions();
                            likelyPlaceTypes[i] = placeLikelihood.getPlace().getTypes();
                            likelyPlaceLatLngs[i] = placeLikelihood.getPlace().getLatLng();

                            i++;
                            if (i > (count - 1)) {
                                break;
                            }
                        }

                        //Select a Random Destination
                        MainActivity.this.selectDestination();

//                        // Show a dialog offering the user the list of likely places, and add a
//                        // marker at the selected place.
//                        MainActivity.this.openPlacesDialog();
                    }
                    else {
                        Log.e(TAG, "Exception: %s", task.getException());
                    }
                }
            });
        } else {
            // The user has not granted permission.
            Log.d("testing", "show current place.. no permission");
            Log.i(TAG, "The user did not grant location permission.");

            // Add a default marker, because the user hasn't selected a place.
            map.addMarker(new MarkerOptions()
                    .title(getString(R.string.default_info_title))
                    .position(defaultLocation)
                    .snippet(getString(R.string.default_info_snippet)));

            // Prompt the user for permission.
            getLocationPermission();
        }
    }

    /**
     * Select a destination from the list of nearby points of interest
     */
    private void selectDestination(){

        newGameButton.setVisibility(View.INVISIBLE);

        gameRunning = true;
        userLocationAtStartOfGame = lastKnownLocation;

        Random random = new Random();
        int randomInt = random.nextInt(likelyPlaceNames.length);
                //ThreadLocalRandom.current().nextInt(0, likelyPlaceNames.length);

        destinationLatLng = likelyPlaceLatLngs[randomInt];
        destinationTypes =  likelyPlaceTypes[randomInt];

        //check if the selected point is within the range set by the user
        checkSelection();

        //keep selecting new points while the point selected is not in range and the correct category
        int safeLimit = 10;
        int counter = 0;

        while(!isInRange || !isCorrectCategory){
            if(counter < safeLimit){
                randomInt = random.nextInt(likelyPlaceNames.length);
                destinationLatLng = likelyPlaceLatLngs[randomInt];
                destinationTypes =  likelyPlaceTypes[randomInt];
                checkSelection();
                counter++;
            }
            else{
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("Could not find a suitable destination.")
                        .show();
                cancelGame();

                newGameButton.setVisibility(View.VISIBLE);

                return;
            }
        }


        destinationName = likelyPlaceNames[randomInt];
        destinationAddress = likelyPlaceAddresses[randomInt];
        destinationAttributes = likelyPlaceAttributions[randomInt];





        Log.d("testing",
                destinationName + " " + destinationAddress + " " + destinationAttributes + " "
                        + destinationTypes + " " + destinationLatLng);



        //convert type list to string list
        List<String> destinationTypeStringList = new ArrayList();
        for (Object type: destinationTypes
             ) {
            String s = type.toString().toLowerCase();
            destinationTypeStringList.add(s);
        }

        //set a clue based on destination type and name
        clueGenerator(destinationTypeStringList);

//        //show the clue to user
//        displayClues();

        //use the clue list to create the cluestring
        if(clueList.size() > 0){
            for (int i = 0; i < clueList.size(); i++) {
                clueString = clueString + clueList.get(i);
            }
        } else{
            clueString = "The location is " + originalDistance + " meters from you.";
        }


        String welcomeMessageTitle = "Your next adventure is about to begin!";
        String welcomeMessageBody = "Click on flat surfaces to follow the tiger to your destination." +
                "\n \nClue: \n \n"
                + clueString + "\n \n"
                + "The location name starts with the letter " + destinationName.toUpperCase().charAt(0);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(welcomeMessageTitle)
                .setMessage(welcomeMessageBody)
                .setOnDismissListener(dialog1 -> {
                    hintButton.setVisibility(View.VISIBLE);
                })
                .show();

    }

    private void checkSelection(){
        //create location object for destination
        Location destinationLocation = new Location(LocationManager.GPS_PROVIDER);

        if(destinationLatLng != null){
            destinationLocation.setLatitude(destinationLatLng.latitude);
            destinationLocation.setLongitude(destinationLatLng.longitude);

            //distances
            originalDistance = userLocationAtStartOfGame.distanceTo(destinationLocation);
        }

        if(originalDistance > gameRadius || originalDistance < endGameDistance ){
            Log.d("testing", "Point Selected is not in the correct range. d = " + originalDistance);

            isInRange = false;
        } else {
            Log.d("testing", "Point Selected is in range. d = " + originalDistance);
            isInRange = true;
        }


        if(isCategorySelected){
            Log.d("testing", "there is a specified category");
            for (int i = 0; i < likelyPlaceNames.length; i++) {

                //convert the current type list to strings
                List<String> stringTypeList = new ArrayList<>();
                for (Object type: likelyPlaceTypes[i]
                ) {
                    String s = type.toString().toLowerCase();
                    stringTypeList.add(s);
                }

                //type list contains the category user selected
                if(stringTypeList.contains(selectedCategoryString)){
                    Log.d("testing", "selection is the correct category");
                    isCorrectCategory = true;
                }
                else{
                    Log.d("testing", "selection is not the correct category");
                    isCorrectCategory = false;
                }
            }
        }
        //the user didn't specify a category
        else{
            Log.d("testing", "there not is a specified category");
            isCorrectCategory = true;
        }

    }

    private void updateDeltaPosition(){

        //this will update the value of lastKnownLocation
        getDeviceLocation();

        //create location object for destination
        Location destinationLocation = new Location(LocationManager.GPS_PROVIDER);

        if(destinationLatLng != null){
            destinationLocation.setLatitude(destinationLatLng.latitude);
            destinationLocation.setLongitude(destinationLatLng.longitude);

            //distances
            originalDistance = userLocationAtStartOfGame.distanceTo(destinationLocation);
            newDistance = lastKnownLocation.distanceTo(destinationLocation);

            //direction to the destination
            // bearingTo. Returns the approximate initial
            // bearing in degrees east of true north when traveling along the shortest
            // path between this location and the given location.
            originalBearingToDestination = userLocationAtStartOfGame.bearingTo(destinationLocation);
            newBearingToDestination = lastKnownLocation.bearingTo(destinationLocation);
        }



        String bearingString1 = "";

        if(currentBearing > 15 && currentBearing < 75){
            bearingString1 = "You are facing North East";
        }
        else if(currentBearing >= 345 || currentBearing <= 15){
            bearingString1 = "You are facing North";
        }
        else if(currentBearing > 285 && currentBearing < 345){
            bearingString1 = "You are facing North West";
        }
        else if(currentBearing <= 285 && currentBearing >= 255){
            bearingString1 = "You are facing West";
        }
        else if(currentBearing < 255 && currentBearing > 195){
            bearingString1 = "You are facing South West";
        }
        else if(currentBearing <= 195 && currentBearing >= 165){
            bearingString1 = "You are facing South";
        }
        else if(currentBearing < 165 && currentBearing > 105 ){
            bearingString1 = "You are facing South East";
        }
        else if(currentBearing <= 105 && currentBearing >= 75){
            bearingString1 = "You are facing East";
        }
        else{
            bearingString1 = "Unknown Direction";
        }

        Log.d("testing", "Current Bearing: " + bearingString1 + " " + currentBearing);


        String bearingString2 = "";
        if(gameRunning){
            if(newBearingToDestination > 15 && newBearingToDestination < 75){
                bearingString2 = "North East";
            }
            else if(newBearingToDestination >= 345 || newBearingToDestination <= 15){
                bearingString2 = "North";
            }
            else if(newBearingToDestination > 285 && newBearingToDestination < 345){
                bearingString2 = "North West";
            }
            else if(newBearingToDestination <= 285 && newBearingToDestination >= 255){
                bearingString2 = "West";
            }
            else if(newBearingToDestination < 255 && newBearingToDestination > 195){
                bearingString2 = "South West";
            }
            else if(newBearingToDestination <= 195 && newBearingToDestination >= 165){
                bearingString2 = "South";
            }
            else if(newBearingToDestination < 165 && newBearingToDestination > 105 ){
                bearingString2 = "South East";
            }
            else if(newBearingToDestination <= 105 && newBearingToDestination >= 75){
                bearingString2 = "East";
            }
            else{
                bearingString2 = "Unknown Direction";
            }

            Log.d("testing", "Destination to the " + bearingString2 + " " + newBearingToDestination);
        }

//        String feedback = "You are facing " + bearingString1 + ". Destination is " + bearingString2 +".";
//        // Display the dialog.
//        AlertDialog dialog = new AlertDialog.Builder(this)
//                .setTitle("Testing Feedback")
//                .setMessage(feedback)
//                .show();

    }

    private void giveUserDirectionalFeedback(){

        updateDeltaPosition();

        //TODO: use originalDistance, NewDistance, originalBearing, NewBearing
        // to set the user interface with feedback (correct or incorrect progress)

        String hintString = "";
        //bearing
        Log.d("testing", "Original Bearing: " + originalBearingToDestination + " . New Bearing: " + newBearingToDestination + ".");
        Log.d("testing","original: " + originalDistance + " . new: " + newDistance + " .");
        //distance
        if(newDistance < originalDistance){
            hintString = "moving towards destination";
        }
        if(originalDistance > newDistance){
            hintString = "moving away from destination";
        }
        if(originalDistance == newDistance){
            hintString = "no progress detected";
        }

        DecimalFormat df = new DecimalFormat("0.0");

        // Display the dialog.
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(hintString + ". O: d - " + df.format(originalDistance) +
                        " , b - " + df.format(originalBearingToDestination) + ". N: d - " + df.format(newDistance) +
                        ", b - " + df.format(newBearingToDestination) + ".")
                .show();


    }


    ///generate the clueString based on the Google Places Type data returned
    private void clueGenerator(List<String> types) {
        Log.d("types", "clue generator");

        //remove generic types
        if(types.contains("point_of_interest")){
            Log.d("types", "contains P O I");
            types.remove("point_of_interest");
        }
        if(types.contains("establishment")){
            Log.d("types", "contains E");
            types.remove("establishment");
        }

        destinationTypes = types;

        //make clues based on types
        for (int i = 0; i < destinationTypes.size(); i++) {
            String typeString = types.get(i);
            Log.d("types", "typestring = "+typeString);
            List<String> l = new ArrayList<>();
            Random r = new Random();
            switch(typeString){

//                case "point_of_interest" :
//                    l.clear();
//                    l.add("Your destination is an interesting point");
//                    l.add("A point...?");
//                    l.add("Interesting...");
//                    l.add("You seek a point of interest");
//                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
//                    break;
//
//                case "establishment" :
//                    l.clear();
//                    l.add("Your destination is established");
//                    l.add("A reputable establishment");
//                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
//                    break;

//TODO: START HERE FOR DATA ENTRY
                case "airport" :
                    Log.d("types", "type switch: airport");
                    l.clear();
                    l.add("Your destination is an aerial launchpad.");
                    l.add("Where we're going we don't need roads.");
                    l.add("You are cleared for takeoff.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "amusement_park" :
                    Log.d("types", "type switch: amusement park");
                    l.clear();
                    l.add("The happiest place on Earth?");
                    l.add("Your destination would be an amusing place to spend a day.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "aquarium" :
                    Log.d("types", "type switch: aquarium");
                    l.clear();
                    l.add("You seek a building filled with walls of glass, brine and color.");
                    l.add("Your destination houses many marine creatures of the deep.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "art_gallery" :
                    Log.d("types", "type switch: art gallery");
                    l.clear();
                    l.add("You seek a building walled with history, culture and beauty.");
                    l.add("Your journey will lead you to paint and stone.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "bakery" :
                    Log.d("types", "type switch: bakery");
                    l.clear();
                    l.add("Your journey will lead you delicious pastries.");
                    l.add("You seek the smell of fresh bread.");
                    l.add("Cakes, and pastries, and bread ... oh my.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "bar" :
                    Log.d("types", "type switch: bar");
                    l.clear();
                    l.add("You journey to a place where everyone knows your name.");
                    l.add("Your destination is a part of the local nightlife.");
                    l.add("When you get there you could get a drink if you are old enough.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "beauty_salon" :
                    Log.d("types", "type switch: beauty salon");
                    l.clear();
                    l.add("Your destination is a house of beauty.");
                    l.add("Hair, nails and makeup.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "bicycle_store" :
                    Log.d("types", "type switch: bicycle store");
                    l.clear();
                    l.add("Queen sang: I want to ride my...");
                    l.add("No training wheels needed to get to this destination.");
                    l.add("Walk to this destination... and ride away?");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "book_store" :
                    Log.d("types", "type switch: book store");
                    l.clear();
                    l.add("Your journey will lead you to the home of a million adventures");
                    l.add("Your destination is shelved with imagination");
                    l.add("Your journey leads to knowledge, adventure and mystery in analog");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "bowling_alley" :
                    Log.d("types", "type switch: bowling alley");
                    l.clear();
                    l.add("The alley you seek is well lit and striking");
                    l.add("If you have some spare time you should strike out to this destination.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "cafe" :
                    Log.d("types", "type switch: cafe");
                    l.clear();
                    l.add("A good place to get a coffee and read a book.");
                    l.add("Every adventurer should stop at the local tavern.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "casino" :
                    Log.d("types", "type switch: casino");
                    l.clear();
                    l.add("Head to the house that always wins.");
                    l.add("Your journey to the house of chance.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;


                case "church" :
                    Log.d("types", "type switch: church");
                    l.clear();
                    l.add("You journey to a house of worship.");
                    l.add("Your adventure leads to a holy place.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "city_hall" :
                    Log.d("types", "type switch: city hall");
                    l.clear();
                    l.add("Your destination is the center of local power.");
                    l.add("The community is steered by this hall of power.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "clothing_store" :
                    Log.d("types", "type switch: clothing store");
                    l.clear();
                    l.add("Fashion or function?");
                    l.add("You journey to a building of cloth and color.");
                    l.add("Time for a new look adventurer?");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "convenience_store" :
                    Log.d("types", "type switch: convenience store");
                    l.clear();
                    l.add("You journey to a convenient community destination.");
                    l.add("They have a little bit of everything at this accessible destination.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "courthouse" :
                    Log.d("types", "type switch: courthouse");
                    l.clear();
                    l.add("Your destination decides the futures of many.");
                    l.add("You journey to a house of law.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "dentist" :
                    Log.d("types", "type switch: dentist");
                    l.clear();
                    l.add("You journey to an oral healer.");
                    l.add("Your destination is dreaded by many.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "doctor" :
                    Log.d("types", "type switch: doctor");
                    l.clear();
                    l.add("You seek a medic.");
                    l.add("You seek a healer.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "drugstore" :
                    Log.d("types", "type switch: drugstore");
                    l.clear();
                    l.add("Your destination provides medical supplies.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "electrician" :
                    Log.d("types", "type switch: electrician");
                    l.clear();
                    l.add("You seek a bringer of light.");
                    l.add("You seek a provider of power to the people.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "electronics_store" :
                    Log.d("types", "type switch: electronics store");
                    l.clear();
                    l.add("Appliances and gadgets and games... oh my.");
                    l.add("You journey to a hub of technology");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "fire_station" :
                    Log.d("types", "type switch: fire station");
                    l.clear();
                    l.add("You seek a band of local heroes who harness the power of water.");
                    l.add("You seek a band of local heroes who battle the inferno. ");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "florist" :
                    Log.d("types", "type switch: florist");
                    l.clear();
                    l.add("You seek the beauty of nature arranged.");
                    l.add("You journey to a house of flowers.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "gas_station" :
                    Log.d("types", "type switch: gas station");
                    l.clear();
                    l.add("Time to refuel.");
                    l.add("Time for a pit stop.");
                    l.add("You journey to the watering hole for mechanical steeds.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "gym" :
                    Log.d("types", "type switch: gym");
                    l.clear();
                    l.add("Your destination is a training facility for strength, speed and stamina.");
                    l.add("You journey to a place of training.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "hair_care" :
                    Log.d("types", "type switch: hair care");
                    l.clear();
                    l.add("Your journey takes you to a place of style, cut and color.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "hindu_temple" :
                    Log.d("types", "type switch: hindu temple");
                    l.clear();
                    l.add("You seek a place of worship.");
                    l.add("You journey to a holy place.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "hospital" :
                    Log.d("types", "type switch: hospital");
                    l.clear();
                    l.add("You journey to a place of healing.");
                    l.add("Your destination is a house of healing.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "jewelry_store" :
                    Log.d("types", "type switch: jewelry store");
                    l.clear();
                    l.add("You journey to a place of filled with sparkling treasure.");
                    l.add("You seek a trove of gold and gems.");
                    l.add("Your destination is filled with treasure.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "library" :
                    Log.d("types", "type switch: library");
                    l.clear();
                    l.add("You seek a public house of knowledge.");
                    l.add("You seek a place of learning.");
                    l.add("Your journey leads to a place of great knowledge.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "light_rail_station" :
                    Log.d("types", "type switch: light rail station");
                    l.clear();
                    l.add("You seek a place of speed and travel.");
                    l.add("Your destination is a stop on the way to another destination.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "locksmith" :
                    Log.d("types", "type switch: locksmith");
                    l.clear();
                    l.add("Your journey leads to a master of barring and opening the way");
                    l.add("The craftsman you seek specializes in security");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "lodging" :
                    Log.d("types", "type switch: lodging");
                    l.clear();
                    l.add("You seek a local inn.");
                    l.add("Your journey leads to a place of rest.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "meal_delivery" :
                    Log.d("types", "type switch: meal_delivery");
                    l.clear();
                    l.add("Your destination delivers sustenance.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "mosque" :
                    Log.d("types", "type switch: mosque");
                    l.clear();
                    l.add("Your destination is a holy place.");
                    l.add("You journey to a place of worship.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "movie_theater" :
                    Log.d("types", "type switch: movie theater");
                    l.clear();
                    l.add("Your destination is a place of spectacle");
                    l.add("Lights, Camera, Action!");
                    l.add("When you get there you could get some popcorn and check out the latest blockbuster.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "museum" :
                    Log.d("types", "type switch: museum");
                    l.clear();
                    l.add("You seek a place of history and learning.");
                    l.add("Your journey lead you to historical relics and natural marvels.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "night_club" :
                    Log.d("types", "type switch: night club");
                    l.clear();
                    l.add("Your destination is a nocturnal gathering place.");
                    l.add("Looking for a party adventurer?");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "park" :
                    Log.d("types", "type switch: park");
                    l.clear();
                    l.add("Your destination is a place of fresh air and recreation.");
                    l.add("Fields, swings and benches might be found at your destination");
                    l.add("You seek a field of outdoor entertainment for the community.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "pet_store" :
                    Log.d("types", "type switch: pet store");
                    l.clear();
                    l.add("Your destination is filled with everything a furry friend might need.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "post_office" :
                    Log.d("types", "type switch: post office");
                    l.clear();
                    l.add("Your destination is an outpost for couriers.");
                    l.add("Time to go postal.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "primary_school" :
                    Log.d("types", "type switch: primary school");
                    l.clear();
                    l.add("The place where learning begins for many.");
                    l.add("A place of learning and play for youth.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "restaurant" :
                    Log.d("types", "type switch: restaurant");
                    l.clear();
                    l.add("Your journey leads to a culinary destination.");
                    l.add("After your long journey, you could check out the local cuisine");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "school" :
                    Log.d("types", "type switch: school");
                    l.clear();
                    l.add("The place of learning.");
                    l.add("A place of learning and play for the young.");
                    l.add("Your destination is a place of enlightenment");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "secondary_school" :
                    Log.d("types", "type switch: secondary school");
                    l.clear();
                    l.add("The place of learning.");
                    l.add("A place of learning and play for the young.");
                    l.add("Your destination is a place of enlightenment");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "shoe_store" :
                    Log.d("types", "type switch: shoe store");
                    l.clear();
                    l.add("Perhaps you will leave better shod than you arrive at this destination.");
                    l.add("You seek a store filled with an important piece of attire for a long journey.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "spa" :
                    Log.d("types", "type switch: spa");
                    l.clear();
                    l.add("Your journey leads to a destination for health and relaxation.");
                    l.add("You seek a place of rest and beauty.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "stadium" :
                    Log.d("types", "type switch: stadium");
                    l.clear();
                    l.add("Your journey leads to a staging ground for athletes.");
                    l.add("You seek a place of sports and entertainment.");
                    l.add("Let's play ball!");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "store" :
                    Log.d("types", "type switch: store");
                    l.clear();
                    l.add("Your journey leads to a place that makes sales.");
                    l.add("A place of commerce.");
                    l.add("Your destination is a place where goods can be purchased.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "subway_station" :
                    Log.d("types", "type switch: subway station");
                    l.clear();
                    l.add("You journey to a place where journeys happen.");
                    l.add("The underground.");
                    l.add("You seek a place of travel.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "synagogue" :
                    Log.d("types", "type switch: sysnagogue");
                    l.clear();
                    l.add("A holy place.");
                    l.add("Your journey leads to a place of worship.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "tourist_attraction" :
                    Log.d("types", "type switch: tourist attraction");
                    l.clear();
                    l.add("Your destination is designed to attract travelers.");
                    l.add("You seek a place of tourism.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "train_station" :
                    Log.d("types", "type switch: train station");
                    l.clear();
                    l.add("You have a train to catch!");
                    l.add("You seek a place of travel.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "university" :
                    Log.d("types", "type switch: university");
                    l.clear();
                    l.add("Your journey leads to a place of learning.");
                    l.add("Your destination is a place of knowledge.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "veterinary_care" :
                    Log.d("types", "type switch: veterinary care");
                    l.clear();
                    l.add("You seek a place where our best friends are made whole.");
                    l.add("You journey to a place of healing and furry friends.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;

                case "zoo" :
                    Log.d("types", "type switch: zoo");
                    l.clear();
                    l.add("You seek a place of natural wonder.");
                    l.add("You journey to a place where you can glimpse other parts of the world.");
                    l.add("Your destination is filled with wondrous beasts.");
                    clueList.add(l.get(r.nextInt(l.size())) + "\n \n");
                    break;


                default:
                    break;

            }


        }

        //if cluelist is long select a number of random clues from it and make a new list
        if(clueList.size() > clueListMaxSize){

            Random rand = new Random();

            List<String> tempList = new ArrayList<>();

            for (int i = 0; i < clueListMaxSize; i++) {

                int x = rand.nextInt(clueListMaxSize);

                tempList.add(clueList.get(x));

            }

            //replace the cluelist with the shorter temp list
            clueList.clear();
            for (String string : tempList
                 ) {
                clueList.add(string);
            }

        }

        Log.d("types", "cluelist : " + clueList);

    }


    ///TODO
    public void displayClues(){
        //put the clue(s) generated on the UI (call this from a button on the UI)

        if(clueList.size() == 0){
            clueString = "The location is " + newDistance + " meters from you.";
        }

        String s = "Clue: \n \n"
                + clueString + "\n \n"
                + "The location name starts with the letter " + destinationName.toUpperCase().charAt(0);

        // Display the dialog.
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Hint")
                .setMessage(s)
                .show();

        //TODO: add more hint options to this dialog

    }



    /**
     * this will be called when the user arrives at the destination
     */
    private void endGame(){

        //display a model
        //TODO: display a prize model?

        //pop up an alert
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Your Journey is completed!")
                .setMessage("You have arrived at the destination!")
                .show();

        gameRunning = false;
        clueList.clear();
        clueString = "";

        hintButton.setVisibility(View.INVISIBLE);
        newGameButton.setVisibility(View.VISIBLE);

    }

    private void cancelGame(){
        gameRunning = false;
        clueList.clear();
        clueString = "";

    }


    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    @Override
    public void onSurfaceCreated(SampleRender render) {

    }

    @Override
    public void onSurfaceChanged(SampleRender render, int width, int height) {

    }

    @Override
    public void onDrawFrame(SampleRender render) {

    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {

        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
            arFragment.setOnViewCreatedListener(this);
            arFragment.setOnTapArPlaneListener(this);

        }
    }
    @Override
    public void onSessionConfiguration(Session session, Config config) {
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.setDepthMode(Config.DepthMode.AUTOMATIC);
        }
    }

    @Override
    public void onViewCreated(ArSceneView arSceneView) {
        arFragment.setOnViewCreatedListener(null);

        // Fine adjust the maximum frame rate
        arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL);
    }

    public void loadModels() {
        WeakReference<MainActivity> weakActivity = new WeakReference<>(this);
        ModelRenderable.builder()
                .setSource(this, Uri.parse("https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb"))
                .setIsFilamentGltf(true)
                .setAsyncLoadEnabled(true)
                .build()
                .thenAccept(model -> {
                    MainActivity activity = weakActivity.get();
                    if (activity != null) {
                        activity.model = model;
                    }
                })
                .exceptionally(throwable -> {
                    Toast.makeText(
                            this, "Unable to load model", Toast.LENGTH_LONG).show();
                    return null;
                });
        ViewRenderable.builder()
                .setView(this, R.layout.view_model_title)
                .build()
                .thenAccept(viewRenderable -> {
                    MainActivity activity = weakActivity.get();
                    if (activity != null) {
                        activity.viewRenderable = viewRenderable;
                    }
                })
                .exceptionally(throwable -> {
                    Toast.makeText(this, "Unable to load model", Toast.LENGTH_LONG).show();
                    return null;
                });
    }


    @Override
    public void onTapPlane(HitResult hitResult, Plane plane, MotionEvent motionEvent) {

        updateDeltaPosition();

        if (model == null || viewRenderable == null) {
            Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the Anchor.
        Anchor anchor = hitResult.createAnchor();
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        // Create the transformable model and add it to the anchor.
        TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
        model.setParent(anchorNode);
        model.setRenderable(this.model)
                .animate(true).start();
        model.getScaleController().setMinScale(0.2f);
        model.getScaleController().setMaxScale(0.25f);


        //y rotation is what i need
        //next I need to rotate the model to point the direction of the destination
        //to do this I need to be able to take 0f = direction user is facing
        // and modify that by the bearing to the destination (which is degrees east of north)

        // so face the model north first means (-bearing)
        // then from north face it in the direction of the bearing to destination (+newBearingToDestination)


        // model N - 180 E - 90 S - 0 W - 270
        // bearing data n-0 e-90 s-180 w- 270

        //the mathematical difference in degrees between where the user is heading and where the destination is
        float bearingDifference = newBearingToDestination - currentBearing;

        float linearDifference = 180 - bearingDifference;

        if(linearDifference < 0){
            linearDifference = 360 + linearDifference;
        }
        if(linearDifference > 360){
            linearDifference = linearDifference - 360;
        }


        //set rotation in direction (x,y,z) in degrees 90
        model.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), linearDifference));

        model.select();

        }
}