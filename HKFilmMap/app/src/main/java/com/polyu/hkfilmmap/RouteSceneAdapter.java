package com.polyu.hkfilmmap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.polyu.hkfilmmap.data.RouteSceneItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RouteSceneAdapter extends RecyclerView.Adapter<RouteSceneAdapter.ViewHolder> {

    public interface OnFoodClickListener { void onFoodClick(RouteSceneItem item); }
    public interface OnRemoveClickListener { void onRemoveClick(RouteSceneItem item); }
    public interface OnCheckInClickListener { void onCheckInClick(RouteSceneItem item); }
    public interface OnStopClickListener { void onStopClick(RouteSceneItem item); }

    private OnFoodClickListener foodListener;
    private OnRemoveClickListener removeListener;
    private OnCheckInClickListener checkInListener;
    private OnStopClickListener stopListener;

    private List<RouteSceneItem> items = new ArrayList<>();

    public void setOnFoodClickListener(OnFoodClickListener l) { foodListener = l; }
    public void setOnRemoveClickListener(OnRemoveClickListener l) { removeListener = l; }
    public void setOnCheckInClickListener(OnCheckInClickListener l) { checkInListener = l; }
    public void setOnStopClickListener(OnStopClickListener l) { stopListener = l; }

    public void submit(List<RouteSceneItem> nextItems) {
        items = new ArrayList<>(nextItems);
        notifyDataSetChanged();
    }

    public void moveItem(int from, int to) {
        Collections.swap(items, from, to);
        notifyItemMoved(from, to);
    }

    public List<Long> getSceneIds() {
        List<Long> ids = new ArrayList<>();
        for (RouteSceneItem item : items) { ids.add(item.sceneId); }
        return ids;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route_scene, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final RouteSceneItem item = items.get(position);
        holder.order.setText(String.valueOf(position + 1));
        holder.poster.setImageResource(PosterUtils.getPosterRes(holder.itemView.getContext(), item.posterAsset));
        holder.movie.setText(item.movieTitle);
        holder.place.setText(item.placeName + (item.latitude == null ? " · pending" : ""));
        holder.scene.setText(item.sceneTitleEn);

        holder.checkIn.setImageResource(item.isCheckedIn == 1 ? R.drawable.ic_check_in_done : R.drawable.ic_check_in);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { if (stopListener != null) stopListener.onStopClick(item); }
        });
        holder.checkIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { if (checkInListener != null) checkInListener.onCheckInClick(item); }
        });
        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { if (removeListener != null) removeListener.onRemoveClick(item); }
        });
        holder.food.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { if (foodListener != null) foodListener.onFoodClick(item); }
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView order;
        final ImageView poster;
        final TextView movie;
        final TextView place;
        final TextView scene;
        final ImageButton checkIn;
        final ImageButton remove;
        final ImageButton food;

        ViewHolder(View itemView) {
            super(itemView);
            order = itemView.findViewById(R.id.tvRouteOrder);
            poster = itemView.findViewById(R.id.ivRoutePoster);
            movie = itemView.findViewById(R.id.tvRouteMovie);
            place = itemView.findViewById(R.id.tvRoutePlace);
            scene = itemView.findViewById(R.id.tvRouteScene);
            checkIn = itemView.findViewById(R.id.btnRouteCheckIn);
            remove = itemView.findViewById(R.id.btnRemoveRouteStop);
            food = itemView.findViewById(R.id.btnNearbyFood);
        }
    }
}
