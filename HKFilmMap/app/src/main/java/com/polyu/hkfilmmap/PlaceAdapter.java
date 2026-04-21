package com.polyu.hkfilmmap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.polyu.hkfilmmap.data.MapPlaceItem;

import java.util.ArrayList;
import java.util.List;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {
    public interface Listener {
        void onPlaceClick(MapPlaceItem item);
    }

    private final Listener listener;
    private List<MapPlaceItem> items = new ArrayList<>();

    public PlaceAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<MapPlaceItem> nextItems) {
        items = nextItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final MapPlaceItem item = items.get(position);
        holder.poster.setImageResource(PosterUtils.getPosterRes(holder.itemView.getContext(), item.posterAsset));
        holder.placeName.setText(item.nameEn);
        holder.placeMeta.setText((item.districtEn == null ? "Unknown district" : item.districtEn) + " | " + (item.addressEn == null ? "Address pending" : item.addressEn));
        holder.placeCounts.setText(item.movieCount + " movies | " + item.sceneCount + " scenes");
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onPlaceClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView poster;
        final TextView placeName;
        final TextView placeMeta;
        final TextView placeCounts;

        ViewHolder(View itemView) {
            super(itemView);
            poster = itemView.findViewById(R.id.ivPoster);
            placeName = itemView.findViewById(R.id.tvPlaceName);
            placeMeta = itemView.findViewById(R.id.tvPlaceMeta);
            placeCounts = itemView.findViewById(R.id.tvPlaceCounts);
        }
    }
}

