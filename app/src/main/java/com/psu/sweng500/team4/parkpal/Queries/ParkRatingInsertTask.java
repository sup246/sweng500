package com.psu.sweng500.team4.parkpal.Queries;

import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.psu.sweng500.team4.parkpal.Models.ParkRating;
import com.psu.sweng500.team4.parkpal.Services.AzureServiceAdapter;

import java.util.Date;

/**
 * Created by brhoads on 7/30/2017.
 */

public class ParkRatingInsertTask extends DBQueryTask {
    private ParkRating parkRating;

    private static final int SUCCESS = 200;

    public ParkRatingInsertTask(AsyncResponse delegate, long parkId, String username, int userId, int rating){
        super();

        this.delegate = delegate;
        parkRating = new ParkRating(parkId, username, userId, rating);

    }

    @Override
    protected Object doInBackground(Object[] params) {
        try {
            //Get a MobileServiceTable of the PARK_ratings table from the AzureServiceAdapter
            final MobileServiceTable<ParkRating> table =
                    AzureServiceAdapter.getInstance().getClient().getTable("PARK_RATINGS", ParkRating.class);

            table.insert(parkRating).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SUCCESS;
    }
}