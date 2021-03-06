package com.psu.sweng500.team4.parkpal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.psu.sweng500.team4.parkpal.Models.User;
import com.psu.sweng500.team4.parkpal.Queries.AsyncResponse;
import com.psu.sweng500.team4.parkpal.Queries.UserQueryTask;

import com.microsoft.windowsazure.notifications.NotificationsManager;
import com.psu.sweng500.team4.parkpal.Services.AzureServiceAdapter;
import com.psu.sweng500.team4.parkpal.Services.NotificationService;


public class MainActivity extends AppCompatActivity {

    private static final String SELECTED_ITEM = "opened_fragment";
    private BottomNavigationView mBottomNavigationView;
    private User mCurrentUser;
    private Menu mOptionMenu;
    private String defaultFragment = "GMapFragment";
    public static final String SENDER_ID = "654566160966";
    public static MobileServiceClient mClient;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            // Handle navigation view item clicks here.
            int id = item.getItemId();

            Fragment fragment = null;

            Bundle bundle = new Bundle();
            bundle.putSerializable("User", mCurrentUser);

            if (id == R.id.navigation_map) {
                fragment = GMapFragment.newInstance();
                fragment.setArguments(bundle);
            } else if (id == R.id.navigation_recommendations) {
                fragment = new RecommendationsFragment();
                fragment.setArguments(bundle);
            }

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.content, fragment);
            transaction.commit();

            return true;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getLoggedInUser();

        defaultFragment = getIntent().getStringExtra("fragment");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        NotificationsManager.handleNotifications(this, SENDER_ID, NotificationService.class);

        mClient = AzureServiceAdapter.getInstance().getClient();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_ITEM, 0);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);

        mOptionMenu = menu;

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        switch (item.getItemId()) {
            case R.id.option_edit_profile:
                //Retrieve the logged in user's email and pass it to the ProfileFragment
                Bundle bundle = new Bundle();
                bundle.putSerializable("User", mCurrentUser);

                ProfileFragment fragment = new ProfileFragment();
                fragment.setArguments(bundle);

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.content, fragment);
                transaction.commit();

                return true;
            case R.id.option_logout:
                logout();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String getLoggedInUser() {
        SharedPreferences settings = getSharedPreferences("PARKPAL", Context.MODE_PRIVATE);
        String email = settings.getString("loggedInEmail", "");

        // Couldn't find user for some reason, logout
        if (email == null || email.isEmpty())
        {
            logout();
            return null;
        }

        setCurrentUser(email);

        return email;
    }

    private void setCurrentUser(String email) {
        // Get park notes
        UserQueryTask asyncQuery = new UserQueryTask(new AsyncResponse(){
            @Override
            public void processFinish(Object result){
                Log.d("INFO", "Result : " + result);

                // Couldn't find user for some reason, logout
                if (result == null)
                {
                    logout();
                    return;
                }

                mCurrentUser = (User) result;
                NotificationService.setCurrentUser(mCurrentUser);

                MenuItem profileName = mOptionMenu.findItem(R.id.option_username);
                profileName.setTitle(mCurrentUser.getFirstName() + " " + mCurrentUser.getLastName());

                Fragment fragment = null;

                if (defaultFragment != null && defaultFragment.equals("recommendations")){
                    fragment = new RecommendationsFragment();
                }
                else {
                    fragment = GMapFragment.newInstance();
                }

                //Manually displaying the first fragment - one time only
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                Bundle bundle = new Bundle();
                bundle.putSerializable("User", mCurrentUser);
                fragment.setArguments(bundle);
                transaction.replace(R.id.content, fragment);
                transaction.commit();
            }
        }, email);

        asyncQuery.execute();
    }

    private void logout() {
        SharedPreferences sharedpreferences = getSharedPreferences("PARKPAL", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.clear();
        editor.commit();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }
}
