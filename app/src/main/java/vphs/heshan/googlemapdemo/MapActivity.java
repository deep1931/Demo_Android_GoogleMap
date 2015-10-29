package vphs.heshan.googlemapdemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapActivity extends Activity implements GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMapClickListener{

    private GoogleMap mGoogleMap;
    private Location mLocation;
    private LatLng mSelectedPosition;
    private Context mContext;
    private GPSTracker mGpsTracker;
    private Point mPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        init();
        //open the map
        openTheMap();

        //show current location
        showCurrentLocation();
    }

    //init
    private void init() {
        mContext = getApplicationContext();
        mGpsTracker = new GPSTracker(mContext);
        mLocation = mGpsTracker.getCurrentLocation();
    }

    /* open the map */
    private void openTheMap() {
        try {
            if(mGoogleMap == null) {
                mGoogleMap=((MapFragment)getFragmentManager().findFragmentById(R.id.fragment_map))
                        .getMap();
                mGoogleMap.getUiSettings().setZoomControlsEnabled(false);
                mGoogleMap.setOnMarkerClickListener(this);
                mGoogleMap.setOnInfoWindowClickListener(this);
                mGoogleMap.setOnMapClickListener(this);
                zoomMap();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* zoom current location */
    private void zoomMap() {
        int zoomScale = 12;
        double currentLat = mLocation.getLatitude();
        double currentLon = mLocation.getLongitude();
        mGoogleMap.moveCamera(CameraUpdateFactory
                .newLatLngZoom(new LatLng(currentLat, currentLon), zoomScale));
    }

    /* show user location */
    private void showCurrentLocation() {
        try {
            if (mLocation != null) {
                double currentLat = mLocation.getLatitude();
                double currentLon = mLocation.getLongitude();
                LatLng myPoint = new LatLng(currentLat, currentLon);
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(myPoint)
                        .title("Latitude - " + currentLat + "," + " Longitude - " + currentLon)
                        .snippet("My Location")
                        .icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
                mGoogleMap.addMarker(markerOptions);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //add marker to the selected position
    private void markSelectedLocation(LatLng selectedLocation) {
        mSelectedPosition = selectedLocation;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(selectedLocation)
                .title("Latitude - " + selectedLocation.latitude + "," + " Longitude - "
                        + selectedLocation.longitude)
                .snippet("Selected Position")
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_RED));
        mGoogleMap.addMarker(markerOptions);
        routeUrl(mSelectedPosition);
        getSelectedPointAddress(mSelectedPosition);
    }

    //prepare route url
    private void routeUrl(LatLng selectedLocation) {
        String str_origin = "origin="+ mLocation.getLatitude() +","+ mLocation.getLongitude();
        String str_dest = "destination="+ selectedLocation.latitude +","+ selectedLocation.longitude;
        String parameters = str_origin+"&"+str_dest+"&"+"sensor=false";
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;
        getDirectionPointList(url);
    }

    //get direction points
    private void getDirectionPointList(final String routeUrl) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String data = "";
                InputStream iStream = null;
                HttpURLConnection urlConnection = null;

                try{
                    URL url = new URL(routeUrl);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.connect();
                    iStream = urlConnection.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
                    StringBuffer sb = new StringBuffer();
                    String line = "";
                    while( ( line = br.readLine()) != null){
                        sb.append(line);
                    }
                    data = sb.toString();
                    br.close();
                    return data;

                } catch(Exception e){
                    e.printStackTrace();
                } finally {
                    try {
                        iStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    urlConnection.disconnect();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String data) {
                drawDirection(data);
            }
        }.execute(null, null, null);
    }

    //parse json route and draw the direction
    private void drawDirection(final String routeResult) {
        new AsyncTask<String, Integer, List<List<HashMap<String,String>>> >() {
            @Override
            protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

                JSONObject jObject;
                List<List<HashMap<String, String>>> routes = null;

                try{
                    jObject = new JSONObject(routeResult);
                    DirectionsJSONParser parser = new DirectionsJSONParser();
                    // Starts parsing data
                    routes = parser.parse(jObject);
                }catch(Exception e){
                    e.printStackTrace();
                }
                return routes;
            }

            // Executes in UI thread, after the parsing process
            @Override
            protected void onPostExecute(List<List<HashMap<String, String>>> result) {
                ArrayList<LatLng> points = null;
                PolylineOptions lineOptions = null;
                MarkerOptions markerOptions = new MarkerOptions();

                // Traversing through all the routes
                for(int i=0;i<result.size();i++){
                    points = new ArrayList<LatLng>();
                    lineOptions = new PolylineOptions();

                    // Fetching i-th route
                    List<HashMap<String, String>> path = result.get(i);

                    // Fetching all the points in i-th route
                    for(int j=0;j<path.size();j++){
                        HashMap<String,String> point = path.get(j);

                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        LatLng position = new LatLng(lat, lng);

                        points.add(position);
                    }

                    // Adding all the points in the route to LineOptions
                    lineOptions.addAll(points);
                    lineOptions.width(10);
                    lineOptions.color(Color.RED);
                }

                // Drawing polyline in the Google Map for the i-th route
                mGoogleMap.addPolyline(lineOptions);
            }
        }.execute(null, null, null);
    }

    //get address of the selected location
    private void getSelectedPointAddress(final LatLng selectedPosition) {
        new AsyncTask<Void, Void, List<Address>>() {
            @Override
            protected List<Address> doInBackground(Void... params) {
                Geocoder geocoder;
                List<Address> addresses;

                try{
                    geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                    addresses = geocoder.getFromLocation(selectedPosition.latitude, selectedPosition.longitude, 1);
                    return addresses;
                } catch(Exception e){
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(List<Address> addresses) {
                if (addresses != null && addresses.size() > 0) {
                    String address = addresses.get(0).getAddressLine(0);
                    String city = addresses.get(0).getLocality();
                    String state = addresses.get(0).getAdminArea();
                    String country = addresses.get(0).getCountryName();
                    String newline = System.getProperty("line.separator");
                    updateSelectedLocation(address + newline + city + newline + state + newline + country);
                }
            }
        }.execute(null, null, null);
    }

    //update the selected location
    private void updateSelectedLocation(String address) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(mSelectedPosition)
                .title("Latitude - " + mSelectedPosition.latitude + "," + " Longitude - " + mSelectedPosition.longitude)
                .snippet("My Location")
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        mGoogleMap.addMarker(markerOptions);
        showPopup(MapActivity.this, address);
    }

    //show message
    private void showPopup(final Activity context, String address) {
        LinearLayout viewGroup = (LinearLayout) context.findViewById(R.id.popup);
        LayoutInflater layoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = layoutInflater.inflate(R.layout.pop_up_layout, viewGroup);
        TextView textView = (TextView) layout.findViewById(R.id.txt_address);
        textView.setText(address);
        // Creating the PopupWindow
        final PopupWindow popup = new PopupWindow(context);
        popup.setContentView(layout);
        popup.setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
        popup.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        popup.setFocusable(true);
        // Clear the default translucent background
        popup.setBackgroundDrawable(new BitmapDrawable());
        // Displaying the popup at the specified location, + offsets.
        popup.showAtLocation(layout, Gravity.NO_GRAVITY, mPoint.x, mPoint.y + 50 );
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        markSelectedLocation(latLng);
        mPoint = mGoogleMap.getProjection().toScreenLocation(latLng);
    }
}
