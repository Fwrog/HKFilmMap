package com.polyu.hkfilmmap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.polyu.hkfilmmap.data.NearbyFoodItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NearbyFoodAdapter extends RecyclerView.Adapter<NearbyFoodAdapter.ViewHolder> {

    private List<NearbyFoodItem> items = new ArrayList<>();
    private OnNavigateClickListener navigateListener;

    public interface OnNavigateClickListener {
        void onNavigateClick(NearbyFoodItem item);
    }

    public void setOnNavigateClickListener(OnNavigateClickListener listener) {
        this.navigateListener = listener;
    }

    public void submit(List<NearbyFoodItem> nextItems) {
        items = nextItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NearbyFoodItem item = items.get(position);
        holder.rank.setText("#" + Math.max(1, item.rank > 0 ? item.rank : position + 1));
        holder.name.setText(item.name);
        if (item.nameZh == null || item.nameZh.trim().isEmpty()) {
            holder.nameZh.setVisibility(View.GONE);
        } else {
            holder.nameZh.setVisibility(View.VISIBLE);
            holder.nameZh.setText(item.nameZh);
        }
        holder.price.setText(item.priceRange);
        holder.desc.setText(item.description);

        String distanceLabel = item.distanceKm < 1.0
                ? String.format(Locale.US, "%.0f m away", item.distanceKm * 1000)
                : String.format(Locale.US, "%.1f km away", item.distanceKm);
        StringBuilder meta = new StringBuilder();
        meta.append(item.walkMinutes > 0 ? item.walkMinutes : 1).append(" min walk");
        meta.append(" | ").append(distanceLabel);
        if (item.district != null && !item.district.trim().isEmpty()) {
            meta.append(" | ").append(item.district);
        }
        holder.distance.setText(meta.toString());
        holder.reason.setText(item.recommendationReason == null ? "" : item.recommendationReason);
        holder.reason.setVisibility(item.recommendationReason == null || item.recommendationReason.trim().isEmpty()
                ? View.GONE : View.VISIBLE);

        int iconRes;
        switch (item.category) {
            case "coffee":
                iconRes = android.R.drawable.ic_menu_report_image;
                break;
            case "dessert":
                iconRes = android.R.drawable.ic_menu_my_calendar;
                break;
            default:
                iconRes = android.R.drawable.ic_menu_compass;
                break;
        }
        holder.categoryIcon.setImageResource(iconRes);

        holder.navigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (navigateListener != null) {
                    navigateListener.onNavigateClick(item);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView rank;
        final ImageView categoryIcon;
        final TextView name;
        final TextView nameZh;
        final TextView price;
        final TextView desc;
        final TextView distance;
        final TextView reason;
        final Button navigate;

        ViewHolder(View itemView) {
            super(itemView);
            rank = itemView.findViewById(R.id.tvFoodRank);
            categoryIcon = itemView.findViewById(R.id.ivFoodCategory);
            name = itemView.findViewById(R.id.tvFoodName);
            nameZh = itemView.findViewById(R.id.tvFoodNameZh);
            price = itemView.findViewById(R.id.tvFoodPrice);
            desc = itemView.findViewById(R.id.tvFoodDesc);
            distance = itemView.findViewById(R.id.tvFoodDistance);
            reason = itemView.findViewById(R.id.tvFoodReason);
            navigate = itemView.findViewById(R.id.btnNavigateFood);
        }
    }
}
