package edu.mit.mitmobile2.shuttles;

import edu.mit.mitmobile2.Constants;
import edu.mit.mitmobile2.MITModuleActivity;
import edu.mit.mitmobile2.MitMobileApplication;
import edu.mit.mitmobile2.R;
import edu.mit.mitmobile2.Schema;
import edu.mit.mitmobile2.shuttles.model.MITShuttleRoute;
import edu.mit.mitmobile2.shuttles.adapter.MITShuttleAdapter;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ShuttlesActivity extends MITModuleActivity implements ShuttleAdapterCallback, LoaderManager.LoaderCallbacks<Cursor> {

    int contentLayoutId = R.layout.content_shuttles;

    private final static long REFRESH_TIME = 1000;

    private MITShuttleAdapter mitShuttleAdapter;

    @InjectView(R.id.shuttle_refresh_layout)
    SwipeRefreshLayout shuttleRefreshLayout;
    @InjectView(R.id.shuttle_listview)
    ListView shuttleListView;

    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.setContentLayoutId(R.layout.content_shuttles);
        super.onCreate(savedInstanceState);

        ButterKnife.inject(this);
        initialShuttleView();

        final Cursor cursor = getContentResolver().query(MITShuttlesProvider.ALL_ROUTES_URI, Schema.Route.ALL_COLUMNS, null, null, null);

        List<MITShuttleRoute> routes = new ArrayList<>();
        ShuttlesDatabaseHelper.generateRouteObjects(routes, cursor);
        cursor.close();

        if (routes.size() > 0) {
            routes = sortShuttleRoutesByStatus(routes);
        }

        mitShuttleAdapter = new MITShuttleAdapter(this, routes);
        shuttleListView.setAdapter(mitShuttleAdapter);

        Bundle bundle = new Bundle();
        bundle.putString("module", Constants.SHUTTLES);
        bundle.putString("path", Constants.Shuttles.ALL_ROUTES_PATH);
        bundle.putString("uri", MITShuttlesProvider.ALL_ROUTES_URI.toString());

        // FORCE THE SYNC - No matter what
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);

        ContentResolver.requestSync(MitMobileApplication.mAccount, MitMobileApplication.AUTHORITY, bundle);

        shuttleRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                shuttleRefreshLayout.setRefreshing(true);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        shuttleRefreshLayout.setRefreshing(false);
                        //TODO: refreshing shuttle adapter
                    }
                }, REFRESH_TIME);
            }
        });

        getSupportLoaderManager().initLoader(0, null, this);

        timer = new Timer();
        startTimerTask();
    }

    private void startTimerTask() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mitShuttleAdapter.getCount() == 0) {
                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        shuttleRefreshLayout.setRefreshing(true);
                    }
                });

                Bundle bundle = new Bundle();
                bundle.putString("module", Constants.SHUTTLES);
                bundle.putString("path", Constants.Shuttles.PREDICTIONS_PATH);
                bundle.putString("uri", MITShuttlesProvider.PREDICTIONS_URI.toString());

                String mitTuples = mitShuttleAdapter.getRouteStopTuples("mit");
                String crTuples = mitShuttleAdapter.getRouteStopTuples("charles-river");

                bundle.putString("mitTuples", mitTuples);
                bundle.putString("crTuples", crTuples);

                Timber.d("Requesting Predictions");

                ContentResolver.requestSync(MitMobileApplication.mAccount, MitMobileApplication.AUTHORITY, bundle);
            }
        }, 10000, 15000);
    }

    private void initialShuttleView() {
        View footer = ((LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.shuttle_list_footer, null, false);
        shuttleListView.addFooterView(footer);
        initialListViewFooter(footer);
//        updateShuttleView();
    }


    /*public void updateShuttleView() {
        apiClient.get(Constants.SHUTTLES, Constants.Shuttles.ALL_ROUTES_PATH, null, null,
                new Callback<List<MITShuttleRoute>>() {
                    @Override
                    public void success(final List<MITShuttleRoute> mitShuttleRoutes, Response response) {
                        setShuttleRoutesStopDistance(mitShuttleRoutes);
                        sortShuttleRoutesByDistance(mitShuttleRoutes);
                        for (MITShuttleRoute shuttleRoute : mitShuttleRoutes) {
                            final MITShuttle mitShuttle = new MITShuttle();
                            setShuttleRoutesStop(mitShuttle, shuttleRoute);
                        }
                        sortShuttleRoutesByStatus();
                        mitShuttleAdapter.notifyDataSetChanged();
                        new PersistRoutesInDbTask().execute(mitShuttleRoutes);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                    }
                });
    }*/

    /*public void setShuttleRoutesStopDistance(List<MITShuttleRoute> mitShuttleRoutes) {
        for (MITShuttleRoute mitShuttleRoute : mitShuttleRoutes) {
            if (mitShuttleRoute.isPredictable()) {
                for (MITShuttleStopWrapper mitShuttleStopWrapper : mitShuttleRoute.getStops()) {
                    if (location != null) {
                        Location stopLocation = new Location("");
                        stopLocation.setLatitude(mitShuttleStopWrapper.getLat());
                        stopLocation.setLongitude(mitShuttleStopWrapper.getLon());
                        mitShuttleStopWrapper.setDistance(location.distanceTo(stopLocation));
                    }
                }
            }
        }
    }*/

    /*public void sortShuttleRoutesByDistance(List<MITShuttleRoute> mitShuttleRoutes) {
        for (MITShuttleRoute mitShuttleRoute : mitShuttleRoutes) {
            Collections.sort(mitShuttleRoute.getStops(), new Comparator<MITShuttleStopWrapper>() {
                @Override
                public int compare(MITShuttleStopWrapper lhs, MITShuttleStopWrapper rhs) {
                    return (int) (lhs.getDistance() - rhs.getDistance());
                }
            });
        }
    }*/

    /*public void setShuttleRoutesStop(MITShuttle mitShuttle, MITShuttleRoute shuttleRoute) {
        mitShuttle.setRouteName(shuttleRoute.getTitle());
        mitShuttle.setPredicable(shuttleRoute.isPredictable());
        mitShuttle.setScheduled(shuttleRoute.isScheduled());
        mitShuttle.setRouteID(shuttleRoute.getId());
        mitShuttle.setFirstStopName(shuttleRoute.getStops().get(0).getTitle());
        mitShuttle.setFirstStopID(shuttleRoute.getStops().get(0).getId());
        mitShuttle.setSecondStopName(shuttleRoute.getStops().get(1).getTitle());
        mitShuttle.setSecondStopID(shuttleRoute.getStops().get(1).getId());
        if (mitShuttle.isPredicable()) {
            updateShuttleRoutesStopView(mitShuttle, true);
            updateShuttleRoutesStopView(mitShuttle, false);
        }
        mitshuttles.add(mitShuttle);
    }*/

    /*public void updateShuttleRoutesStopView(final MITShuttle mitShuttle, final boolean isFirstStop) {
        HashMap<String, String> stopParams = new HashMap<>();

        stopParams.put("route", mitShuttle.getRouteID());
        if (isFirstStop) {
            stopParams.put("stop", mitShuttle.getFirstStopID());
        } else {
            stopParams.put("stop", mitShuttle.getSecondStopID());
        }

        apiClient.get(Constants.SHUTTLES, Constants.Shuttles.STOP_INFO_PATH,
                stopParams, null, new Callback<MITShuttleStopWrapper>() {
                    @Override
                    public void success(MITShuttleStopWrapper mitShuttleStopWrapper, Response response) {
                        if (mitShuttleStopWrapper.getPredictions() != null && mitShuttleStopWrapper.getPredictions().size() > 0) {
                            if (isFirstStop) {
                                mitShuttle.setFirstMinute(mitShuttleStopWrapper.getPredictions().get(0).
                                        getSeconds() / 60 + "m");
                            } else {
                                mitShuttle.setSecondMinute(mitShuttleStopWrapper.getPredictions().get(0).
                                        getSeconds() / 60 + "m");
                            }
                        }
                        mitShuttleAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void failure(RetrofitError error) {
                    }
                });
    }*/

    /*public void sortShuttleRoutesByStatus() {
        List<MITShuttle> shuttleRouteStatusInService = new ArrayList<>();
        List<MITShuttle> shuttleRouteStatusNotInservice = new ArrayList<>();
        List<MITShuttle> shuttleRouteStatusUnknown = new ArrayList<>();
        for (MITShuttle mitShuttle : mitshuttles) {
            if (mitShuttle.isPredicable()) {
                shuttleRouteStatusInService.add(mitShuttle);
            } else if (mitShuttle.isScheduled()) {
                shuttleRouteStatusUnknown.add(mitShuttle);
            } else {
                shuttleRouteStatusNotInservice.add(mitShuttle);
            }
        }
        mitshuttles.clear();
        for (MITShuttle shuttleRoute : shuttleRouteStatusInService) {
            mitshuttles.add(shuttleRoute);
        }
        for (MITShuttle shuttleRoute : shuttleRouteStatusUnknown) {
            mitshuttles.add(shuttleRoute);
        }
        for (MITShuttle shuttleRoute : shuttleRouteStatusNotInservice) {
            mitshuttles.add(shuttleRoute);
        }
    }*/

    public List<MITShuttleRoute> sortShuttleRoutesByStatus(List<MITShuttleRoute> routes) {
        List<MITShuttleRoute> shuttleRouteStatusInService = new ArrayList<>();
        List<MITShuttleRoute> shuttleRouteStatusNotInservice = new ArrayList<>();
        List<MITShuttleRoute> shuttleRouteStatusUnknown = new ArrayList<>();

        for (MITShuttleRoute route : routes) {
            if (route.isPredictable()) {
                shuttleRouteStatusInService.add(route);
            } else if (route.isScheduled()) {
                shuttleRouteStatusUnknown.add(route);
            } else {
                shuttleRouteStatusNotInservice.add(route);
            }
        }

        routes.clear();

        for (MITShuttleRoute shuttleRoute : shuttleRouteStatusInService) {
            routes.add(shuttleRoute);
        }
        for (MITShuttleRoute shuttleRoute : shuttleRouteStatusUnknown) {
            routes.add(shuttleRoute);
        }
        for (MITShuttleRoute shuttleRoute : shuttleRouteStatusNotInservice) {
            routes.add(shuttleRoute);
        }

        return routes;
    }

    public void initialListViewFooter(View footer) {
        final RelativeLayout parkOffice = (RelativeLayout) footer.findViewById(R.id.park_office);
        final RelativeLayout saferide = (RelativeLayout) footer.findViewById(R.id.saferide);
        final LinearLayout realTimeBusArrivals = (LinearLayout) footer.findViewById(R.id.real_time_bus_arrivals);
        final LinearLayout realTimeTrainArribals = (LinearLayout) footer.findViewById(R.id.real_time_train_arrivals);
        final LinearLayout googleTransit = (LinearLayout) footer.findViewById(R.id.google_transit);

        parkOffice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPhoneCall(getResources().getString(R.string.parking_office_number));
            }
        });
        saferide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPhoneCall(getResources().getString(R.string.saferide_number));
            }
        });
        realTimeBusArrivals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMBTAUrl(getResources().getString(R.string.real_time_bus_url));
            }
        });
        realTimeTrainArribals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMBTAUrl(getResources().getString(R.string.real_time_train_url));
            }
        });
        googleTransit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMBTAUrl(getResources().getString(R.string.real_time_google_transit_url));
            }
        });
    }

    public void setPhoneCall(String phoneNumber) {
        String uri = "tel:" + phoneNumber.trim();
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse(uri));
        startActivity(intent);
    }

    public void openMBTAUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No application can handle this request. " +
                    "Please install a map app.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void shuttleRouteClick(String routeID) {
        HashMap<String, String> routeParams = new HashMap<>();
        routeParams.put("route", routeID);
        apiClient.get(Constants.SHUTTLES, Constants.Shuttles.ROUTE_INFO_PATH, routeParams, null,
                new Callback<MITShuttleRoute>() {
                    @Override
                    public void success(final MITShuttleRoute mitShuttleRouteWrapper, Response response) {
                        Intent intent = new Intent(getApplicationContext(), ShuttleRouteActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("route", mitShuttleRouteWrapper);
                        startActivity(intent);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                    }
                });
    }

    @Override
    public void shuttleStopClick(String stopID) {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        MitCursorLoader cursorLoader = new MitCursorLoader(this, MITShuttlesProvider.ALL_ROUTES_URI);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Timber.d("Notified!");
        List<MITShuttleRoute> routes = new ArrayList<>();
        ShuttlesDatabaseHelper.generateRouteObjects(routes, data);
        routes = sortShuttleRoutesByStatus(routes);
        mitShuttleAdapter.updateListItems(routes);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                shuttleRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        timer = new Timer();
        startTimerTask();
    }

    @Override
    protected void onStop() {
        timer.cancel();
        super.onStop();
    }
}
