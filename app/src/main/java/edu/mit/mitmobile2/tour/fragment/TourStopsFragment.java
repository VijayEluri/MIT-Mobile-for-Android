package edu.mit.mitmobile2.tour.fragment;

import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.Iterator;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import edu.mit.mitmobile2.Constants;
import edu.mit.mitmobile2.R;
import edu.mit.mitmobile2.tour.adapters.TourStopViewPagerAdapter;
import edu.mit.mitmobile2.tour.callbacks.TourStopCallback;
import edu.mit.mitmobile2.tour.model.MITTour;
import edu.mit.mitmobile2.tour.model.MITTourStop;

public class TourStopsFragment extends Fragment {

    @InjectView(R.id.tour_stop_view_pager)
    ViewPager tourStopViewpager;

    private TourStopViewPagerAdapter tourStopViewPagerAdapter;
    private List<MITTourStop> mainLoopStops;
    private MITTour tour;
    private int currentPosition;
    private TourStopCallback callback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_tour_stop, container, false);
        ButterKnife.inject(this, view);

        callback = (TourStopCallback) getActivity();

        tour = getArguments().getParcelable(Constants.TOURS);

        mainLoopStops = tour.getStops();
        Iterator<MITTourStop> iterator = mainLoopStops.iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().getType().equals(Constants.MAIN_LOOP)) {
                iterator.remove();
            }
        }

        tourStopViewPagerAdapter = new TourStopViewPagerAdapter(getFragmentManager(), mainLoopStops);
        tourStopViewpager.setAdapter(tourStopViewPagerAdapter);
        tourStopViewpager.setCurrentItem(getArguments().getInt(Constants.CURRENT_MAIN_LOOP_STOP));

        tourStopViewpager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                int realPosition = tourStopViewPagerAdapter.getRealPosition(position);
                currentPosition = realPosition;
                int mainLoopStopNum = currentPosition + 1;
                callback.setMainLoopActionBarTitle(mainLoopStopNum, mainLoopStops.size());
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        return view;
    }
}