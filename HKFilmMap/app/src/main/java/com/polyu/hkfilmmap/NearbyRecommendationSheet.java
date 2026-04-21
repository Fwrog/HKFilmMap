package com.polyu.hkfilmmap;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.polyu.hkfilmmap.data.NearbyFoodItem;

import java.util.List;

public final class NearbyRecommendationSheet {

    public interface NavigateListener {
        void onNavigate(NearbyFoodItem item);
    }

    private NearbyRecommendationSheet() {
    }

    public static BottomSheetDialog show(Activity activity,
                                         String placeName,
                                         double latitude,
                                         double longitude,
                                         String districtHint,
                                         String genreHint,
                                         boolean fromCheckIn,
                                         final NavigateListener listener) {
        List<NearbyFoodItem> ranked = NearbyFoodHelper.findRecommended(
                activity, latitude, longitude, districtHint, genreHint, 10);
        if (ranked.isEmpty()) {
            Toast.makeText(activity, R.string.food_none_found, Toast.LENGTH_SHORT).show();
            return null;
        }

        final BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View sheetView = activity.getLayoutInflater().inflate(R.layout.bottom_sheet_food, null);
        dialog.setContentView(sheetView);

        TextView title = sheetView.findViewById(R.id.tvFoodSheetTitle);
        title.setText(fromCheckIn
                ? activity.getString(R.string.food_sheet_title_after_checkin)
                : activity.getString(R.string.food_sheet_title));

        TextView subtitle = sheetView.findViewById(R.id.tvFoodSheetSubtitle);
        subtitle.setText(activity.getString(R.string.food_sheet_subtitle, placeName, ranked.size()));

        TextView hint = sheetView.findViewById(R.id.tvFoodSheetHint);
        hint.setText(activity.getString(R.string.food_sheet_hint));

        RecyclerView recycler = sheetView.findViewById(R.id.recyclerFood);
        recycler.setLayoutManager(new LinearLayoutManager(activity));
        NearbyFoodAdapter adapter = new NearbyFoodAdapter();
        adapter.setOnNavigateClickListener(new NearbyFoodAdapter.OnNavigateClickListener() {
            @Override
            public void onNavigateClick(NearbyFoodItem item) {
                dialog.dismiss();
                if (listener != null) {
                    listener.onNavigate(item);
                }
            }
        });
        recycler.setAdapter(adapter);
        adapter.submit(ranked);

        dialog.show();
        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setFitToContents(false);
        behavior.setHalfExpandedRatio(0.72f);
        behavior.setPeekHeight(dpToPx(activity, 280));
        behavior.setSkipCollapsed(false);
        behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        return dialog;
    }

    private static int dpToPx(Activity activity, int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }
}
