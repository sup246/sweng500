package com.psu.sweng500.team4.parkpal;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.psu.sweng500.team4.parkpal.Models.Location;
import com.psu.sweng500.team4.parkpal.Models.User;
import com.psu.sweng500.team4.parkpal.Services.AzureServiceAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GMapFragment extends Fragment implements
        OnMapReadyCallback,
        GoogleMap.InfoWindowAdapter,
        GoogleMap.OnInfoWindowClickListener {

    private GoogleMap mMap;
    private ArrayList<Location> mLocations;
    private LayoutInflater mInflater;
    Geocoder geocoder;
    LatLng mHomeLocation = new LatLng(39.9526, -75.1652); // Philly
    private User mCurrentUser;

    public GMapFragment() {}

    public static GMapFragment newInstance() {
        GMapFragment fragment = new GMapFragment();

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mCurrentUser = (User) getArguments().getSerializable("User");
        mInflater = inflater;
        return mInflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment fragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        fragment.getMapAsync(this);
    }

    @Override
    public void onResume() { super.onResume(); }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setInfoWindowAdapter(this);
        mMap.setOnInfoWindowClickListener(this);

        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Prompt the user once explanation has been shown
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    99);
            mMap.setMyLocationEnabled(true);
        } else {
            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener()
        {
            @Override
            public boolean onMyLocationButtonClick()
            {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mHomeLocation, 7));
                return true;
            }
        });

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mHomeLocation, 7));

        if (mLocations == null) {
            mLocations = new ArrayList<Location>();

            pullLocationExample();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void test() {}

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Intent intent = new Intent(this.getActivity(), ParkDetails.class);
        intent.putExtra("Location", (Location)marker.getTag());
        intent.putExtra("User", mCurrentUser);
        startActivityForResult(intent, 999);
    }

    @Override
    public View getInfoContents(Marker marker)  {
        View v = getLayoutInflater(Bundle.EMPTY).inflate(R.layout.marker_info_layout, null);

        Location clickedLocation = (Location)marker.getTag();
        TextView tvLocation = (TextView) v.findViewById(R.id.tvLocation);
        TextView tvSnippet = (TextView) v.findViewById(R.id.tvSnippet);
        TextView tvPhone = (TextView) v.findViewById(R.id.tvPhone);
        TextView tvAddress = (TextView) v.findViewById(R.id.tvAddress);
        TextView tvAmenities = (TextView) v.findViewById(R.id.tvAmenities);
        TextView tvSeason = (TextView) v.findViewById(R.id.tvSeason);

        //get marker current location
        LatLng latLng = marker.getPosition();
        //get street address from geocoder
        Address addr = AddressFinder(latLng);

        //sets the variables created above to the current information
        tvLocation.setText(marker.getTitle());
        // Snippet returns city and state
        tvSnippet.setText(marker.getSnippet());
        tvSeason.setText("Dates Open: " + clickedLocation.getDatesOpen());
        //TODO - Add icons to represent the various amenities
        tvAmenities.setText(clickedLocation.getAmenities());
        //TODO - Add weather info

        //set Phone Number from database
        if (clickedLocation.getPhone() == null){
            tvPhone.setText("Phone Number N/A");
        }else{
            tvPhone.setText(clickedLocation.getPhone());
        }
        //sets address from the Lat/Lng from geocoder conversion
        if (addr.getAddressLine(0) == null) {
            tvAddress.setText("Address N/A");
        }else{
                tvAddress.setText(addr.getAddressLine(0));
            }

        return v;
    }

    public void createLocationMarkers() {
        for (Location l: mLocations) {
            Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(l.getLatitude(),
                    l.getLongitude())).title(l.getName())
                    .snippet(l.getTown() + "," + l.getState())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.campsite_markergrsm)));
            marker.setTag(l);
        }
    }

    private Address AddressFinder(LatLng latLong){
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        List<Address> addresses = null;
       try {
           addresses = geocoder.getFromLocation(latLong.latitude, latLong.longitude, 1);
       }
       catch (IOException exception){
       }
        return addresses.get(0);
    }

    private void pullLocationExample() {

        try {
            //Initialization of the AzureServiceAdapter to make it usable in the app.
            AzureServiceAdapter.Initialize(this.getContext());
            Log.d("INFO", "AzureServiceAdapter initialized");

        } catch (Exception e) {
            Log.e("ParkPal", "exception", e);
        }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    //Get a MobileServiceTable of the LOCATIONS table from the AzureServiceAdapter
                    final MobileServiceTable<Location> table =
                        AzureServiceAdapter.getInstance().getClient().getTable("LOCATIONS", Location.class);


                    //Get a ListenableFuture<MobileServiceList<Location>> from the MobileServiceTable,
                    //iterable like a regular list
                    final MobileServiceList<Location> results = table.where().execute().get();

                    mLocations.addAll(results);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // If we're just starting up we are waiting on getting locations so
                            // set locations and after create the markers
                            createLocationMarkers();
                        }
                    });

                    for (int i = 0; i < 10; i++) {
                        Log.d("INFO", "Result : " + results.get(i).getName() +
                                " | " + results.get(i).getLatitude() +
                                " , " + results.get(i).getLongitude());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }
}