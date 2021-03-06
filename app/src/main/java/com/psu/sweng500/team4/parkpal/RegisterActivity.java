package com.psu.sweng500.team4.parkpal;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
//import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

//import com.google.android.gms.auth.api.Auth;
//import com.google.android.gms.common.ConnectionResult;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;
import com.psu.sweng500.team4.parkpal.Models.User;
import com.psu.sweng500.team4.parkpal.Models.UserPrefs;
import com.psu.sweng500.team4.parkpal.Queries.AsyncResponse;
import com.psu.sweng500.team4.parkpal.Queries.UserPrefsInsertTask;
import com.psu.sweng500.team4.parkpal.Queries.UserPrefsUpdateTask;
import com.psu.sweng500.team4.parkpal.Services.AzureServiceAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;

public class RegisterActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;
    private UserRegistrationTask mAuthTask2 = null;
    // UI references.
//    private AutoCompleteTextView mEmailView;
    private EditText mEmailView;
    private EditText mPasswordView;
    private EditText mConfirmPasswordView;
    private EditText mBirthDateView;
    private EditText mUsernameView;
    private EditText mFirstNameView;
    private EditText mLastNameView;
    private EditText mZipCodeView;
    private Switch mKids;
    private Switch mDogs;
    private Switch mWatersports;
    private Switch mHiking;
    private Switch mCamping;
    private Switch mForest;
    private Switch mDesert;
    private Switch mMountain;
    private Switch mBeach;
    private View mProgressView;
    private View mRegistrationFormView;
    private String userChoosenTask;

    private UserPrefs userPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Set up the registration form.
//        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mEmailView = (EditText) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);

        String email = getIntent().getStringExtra("email");
        String password = getIntent().getStringExtra("password");

        mEmailView.setText(email);
        mPasswordView.setText(password);

        mConfirmPasswordView = (EditText) findViewById(R.id.conPassword);
        mFirstNameView = (EditText) findViewById(R.id.firstName);
        mLastNameView = (EditText) findViewById(R.id.lastName);
        mBirthDateView = (EditText) findViewById(R.id.birthdate);
        mUsernameView = (EditText) findViewById(R.id.userName);
        mZipCodeView = (EditText) findViewById(R.id.zipCode);

        mZipCodeView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.submit || id == EditorInfo.IME_NULL) {
                    // Start the registration activity
                    attemptRegistration();
                }
                return false;
            }
        });

        mKids = (Switch) findViewById(R.id.prefs_kids);
        mDogs = (Switch) findViewById(R.id.prefs_dogs);
        mWatersports = (Switch) findViewById(R.id.prefs_watersports);
        mHiking = (Switch) findViewById(R.id.prefs_hiking);
        mCamping = (Switch) findViewById(R.id.prefs_camping);
        mForest = (Switch) findViewById(R.id.prefs_forest);
        mDesert = (Switch) findViewById(R.id.prefs_desert);
        mMountain = (Switch) findViewById(R.id.prefs_mountain);
        mBeach = (Switch) findViewById(R.id.prefs_beach);

   /*     try {
            //Initialization of the AzureServiceAdapter to make it usable in the app.
            AzureServiceAdapter.Initialize(this);
            Log.d("INFO", "AzureServiceAdapter initialized");
        } catch (Exception e) {
            Log.e("ParkPal", "exception", e);
        }*/

        Button mPhotoButton = (Button) findViewById(R.id.btnSelectPhoto);
        mPhotoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });

        Button mRegistrationButton = (Button) findViewById(R.id.submit);
        mRegistrationButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegistration();
            }
        });

        mRegistrationFormView = findViewById(R.id.registration_form);
        mProgressView = findViewById(R.id.registration_progress);
        /* TO DO add google and facebook sign up. */
    }

    /**
     * Attempts to register the account specified by the register form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual submit attempt is made.
     */
    private void attemptRegistration() {
        if (mAuthTask2 != null) {
            return;
        }
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);
        mConfirmPasswordView.setError(null);
        mBirthDateView.setError(null);
        mUsernameView.setError(null);
        mFirstNameView.setError(null);
        mLastNameView.setError(null);
        mZipCodeView.setError(null);


        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String conPassword = mConfirmPasswordView.getText().toString();
        String birthDate = mBirthDateView.getText().toString();
        String username = mUsernameView.getText().toString();
        String firstName = mFirstNameView.getText().toString();
        String lastName = mLastNameView.getText().toString();
        String zipCode = mZipCodeView.getText().toString();

        userPrefs = new UserPrefs(username);
        userPrefs.setPrefs_kids(mKids.isChecked());
        userPrefs.setPrefs_dogs(mDogs.isChecked());
        userPrefs.setPrefs_watersports(mWatersports.isChecked());
        userPrefs.setPrefs_camping(mCamping.isChecked());
        userPrefs.setPrefs_hiking(mHiking.isChecked());
        userPrefs.setPrefs_forest(mForest.isChecked());
        userPrefs.setPrefs_mountain(mMountain.isChecked());
        userPrefs.setPrefs_desert(mDesert.isChecked());
        userPrefs.setPrefs_beach(mBeach.isChecked());

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid confirmed password, if the user entered one.
        if (!TextUtils.isEmpty(conPassword) && !isPasswordValid(conPassword) && conPassword != password) {
            mConfirmPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mConfirmPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        // Check for a valid birthdate
        if (!TextUtils.isEmpty(birthDate) && !isBirthDateValid(birthDate)) {
            mBirthDateView.setError(getString(R.string.error_field_required));
            focusView = mBirthDateView;
            cancel = true;
        }

        // Check for a valid username
        if (!TextUtils.isEmpty(username) && !isUsernameValid(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        // Check for a valid first name
        if (!TextUtils.isEmpty(firstName) && !isFirstNameValid(firstName)) {
            mFirstNameView.setError(getString(R.string.error_field_required));
            focusView = mFirstNameView;
            cancel = true;
        }

        // Check for a valid last name
        if (!TextUtils.isEmpty(lastName) && !isLastNameValid(lastName)) {
            mLastNameView.setError(getString(R.string.error_field_required));
            focusView = mLastNameView;
            cancel = true;
        }

        // Check for a valid zip code
        if (!TextUtils.isEmpty(zipCode) && !isZipCodeValid(zipCode)) {
            mZipCodeView.setError(getString(R.string.error_field_required));
            focusView = mZipCodeView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask2 = new UserRegistrationTask(email, password, conPassword, birthDate,
                    username, firstName, lastName, zipCode);
            mAuthTask2.execute((Void) null);

            // Populate prefs
            UserPrefsInsertTask asyncQuery = new UserPrefsInsertTask(new AsyncResponse(){
                @Override
                public void processFinish(Object result){
                }
            }, userPrefs);

            asyncQuery.execute();
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");

    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    private boolean isBirthDateValid(String birthdate) {
        //TODO: Replace this with your own logic
        SimpleDateFormat simpleDate = new SimpleDateFormat("mm/dd/yyyy");
        Date birthDate = null;
        Date testDate = null;
        try {
            birthDate = simpleDate.parse(birthdate);
            testDate = simpleDate.parse("01/01/2010");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return birthDate.before(testDate);
    }

    private boolean isUsernameValid(String username) {
        //TODO: Replace this with your own logic
        return username.length() > 6;
    }

    private boolean isFirstNameValid(String firstName) {
        //TODO: Replace this with your own logic
        return firstName.length() >= 4;
    }

    private boolean isLastNameValid(String lastName) {
        //TODO: Replace this with your own logic
        return lastName.length() >= 4;
    }

    private boolean isZipCodeValid(String zipCode) {
        //TODO: Replace this with your own logic
        return zipCode.length() > 4;
    }


    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mRegistrationFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mRegistrationFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mRegistrationFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mRegistrationFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), RegisterActivity.ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(RegisterActivity.ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

//        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

/*    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(RegisterActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }*/

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }


    /**
     * This will be used to validate information
     */

    public class UserRegistrationTask extends AsyncTask<Object, Object, Integer> {

        private final String mEmail;
        private final String mPassword;
        private final String mConfirmPassword;
        private final String mBirthDate;
        private final String mUsername;
        private final String mFirstName;
        private final String mLastName;
        private final String mZipCode;

        private static final int REGISTER_USER = 9001;
        private static final int INCORRECT_EMAIL = 9002;
        private static final int INCORRECT_USER = 9003;

        UserRegistrationTask(String email, String password, String conPassword, String birthDate,
                             String username, String firstName, String lastName, String zipCode) {
            mEmail = email;
            mPassword = password;
            mConfirmPassword = conPassword;
            mBirthDate = birthDate;
            mUsername = username;
            mFirstName = firstName;
            mLastName = lastName;
            mZipCode = zipCode;
        }

        @Override
        protected Integer doInBackground(Object... params) {
            // Check for Duplicate email, username
            // If none put in database
            /*try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return -1;
            }*/
            final MobileServiceTable<User> table =
                    AzureServiceAdapter.getInstance().getClient().getTable("USERS", User.class);

            try {
                final MobileServiceList<User> email_results = table.where().field("email").eq(mEmail).execute().get();
                final MobileServiceList<User> username_results = table.where().field("username").eq(mUsername).execute().get();
                if (email_results.size() != 0) {
                    return INCORRECT_EMAIL;
                } else if (username_results.size() != 0) {
                    return INCORRECT_USER;
                } else {
                    try {
                        User newUser = new User();
                        newUser.setUsername(mUsername);
                        newUser.setEmail(mEmail);
                        newUser.setBirthdate(mBirthDate);
                        newUser.setFirstName(mFirstName);
                        newUser.setLastName(mLastName);
                        newUser.setPassword(mPassword);
                        newUser.setZipCode(mZipCode);
                        //SUBMIT INFO
                        table.insert(newUser).get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return REGISTER_USER;
        }

        ///////////////////////////////////////////////////////////
 /*       private void createUser(){

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
//Get a MobileServiceTable of the USERS table from the AzureServiceAdapter
                    final MobileServiceTable<User> table =
                            AzureServiceAdapter.getInstance().getClient().getTable("USERS", User.class);

                    try {
                        User newUser = new User();
                        newUser.setUsername("testUser");
                        newUser.setBirthdate("1/1/2001");
                        newUser.setFirstname("test");
                        newUser.setLastname("User");
                        newUser.setEmail("testUser@test.com");
                        newUser.setPassword("hello");
                        newUser.setZipCode("17011");

                        table
                                .insert(newUser)
                                .get();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute();

        }*/
/////////////////////////////////////////////////////////////////
        @Override
        protected void onPostExecute(final Integer success) {
            mAuthTask2 = null;
            showProgress(false);

            if (success == INCORRECT_EMAIL) {
                mEmailView.setError(getString(R.string.error_field_required));
                mEmailView.requestFocus();
            } else if (success == INCORRECT_USER) {
                mUsernameView.setError(getString(R.string.error_field_required));
                mUsernameView.requestFocus();
            } else {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask2 = null;
            showProgress(false);
        }
    }

    private void selectImage() {
        final CharSequence[] items = {"Take Photo", "Choose from Library",
                "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result = checkPermission(RegisterActivity.this);
                if (items[item].equals("Take Photo")) {
                    userChoosenTask = "Take Photo";
                    if (result)
                        cameraIntent();
                } else if (items[item].equals("Choose from Library")) {
                    userChoosenTask = "Choose from Library";
                    if (result)
                        galleryIntent();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void cameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 1337);
    }

    private void galleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"), 1337);
    }
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        public static boolean checkPermission(final Context context)
        {
            int currentAPIVersion = Build.VERSION.SDK_INT;
            if(currentAPIVersion>=android.os.Build.VERSION_CODES.M)
            {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                        alertBuilder.setCancelable(true);
                        alertBuilder.setTitle("Permission necessary");
                        alertBuilder.setMessage("External storage permission is necessary");
                        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions((Activity) context, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                            }
                        });
                        AlertDialog alert = alertBuilder.create();
                        alert.show();
                     } else {
                        ActivityCompat.requestPermissions((Activity) context, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                    return false;
                } else {
                    return true;
                }
            } else {
                return true;
            }
        }
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            switch (requestCode) {
                case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if(userChoosenTask.equals("Take Photo"))
                            cameraIntent();
                        else if(userChoosenTask.equals("Choose from Library"))
                            galleryIntent();
                    } else {
                        //code for deny
                    }
                    break;
            }
        }
}
