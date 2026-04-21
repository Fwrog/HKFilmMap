package com.polyu.hkfilmmap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.VH> {

    public interface OnClick { void onItemClick(MovieLocation loc); }

    private List<MovieLocation> data;
    private final OnClick listener;

    public LocationAdapter(List<MovieLocation> data, OnClick listener) {
        this.data = data;
        this.listener = listener;
    }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvMovie, tvMovieZh, tvLocation, tvYear, tvGenre, tvDistrict, tvPreview;
        ImageView ivCheck;
        public VH(View v) {
            super(v);
            tvMovie = v.findViewById(R.id.tvMovieTitle);
            tvMovieZh = v.findViewById(R.id.tvMovieTitleZh);
            tvLocation = v.findViewById(R.id.tvLocationName);
            tvYear = v.findViewById(R.id.tvYear);
            tvGenre = v.findViewById(R.id.tvGenre);
            tvDistrict = v.findViewById(R.id.tvDistrict);
            tvPreview = v.findViewById(R.id.tvScenePreview);
            ivCheck = v.findViewById(R.id.ivCheckedIn);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        MovieLocation loc = data.get(pos);
        Context ctx = h.itemView.getContext();
        h.tvMovie.setText(loc.getMovieTitle());
        h.tvMovieZh.setText(loc.getMovieTitleZh());
        h.tvLocation.setText(loc.getLocationName());
        h.tvYear.setText(String.valueOf(loc.getYear()));
        h.tvDistrict.setText(loc.getDistrict());
        h.tvGenre.setText(loc.getGenre());

        int gc;
        switch (loc.getGenre()) {
            case "Action":  gc = R.color.genre_action; break;
            case "Romance": gc = R.color.genre_romance; break;
            case "Crime":   gc = R.color.genre_crime; break;
            case "Comedy":  gc = R.color.genre_comedy; break;
            case "Sci-Fi":  gc = R.color.genre_scifi; break;
            default:        gc = R.color.genre_default; break;
        }
        h.tvGenre.getBackground().setTint(ContextCompat.getColor(ctx, gc));

        String p = loc.getSceneDescriptionZh();
        h.tvPreview.setText(p.length() > 50 ? p.substring(0, 50) + "..." : p);

        if (loc.isCheckedIn()) {
            h.ivCheck.setImageResource(R.drawable.ic_checked);
            h.ivCheck.setColorFilter(ContextCompat.getColor(ctx, R.color.checked_in));
        } else {
            h.ivCheck.setImageResource(R.drawable.ic_unchecked);
            h.ivCheck.setColorFilter(ContextCompat.getColor(ctx, R.color.text_hint));
        }

        h.itemView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { listener.onItemClick(loc); }
        });
    }

    @Override public int getItemCount() { return data.size(); }

    public void updateData(List<MovieLocation> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }
}
