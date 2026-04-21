package com.polyu.hkfilmmap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.polyu.hkfilmmap.data.MovieListItem;

import java.util.ArrayList;
import java.util.List;

public class MovieCatalogAdapter extends RecyclerView.Adapter<MovieCatalogAdapter.ViewHolder> {
    public interface Listener {
        void onMovieClick(long movieId);
    }

    private final Listener listener;
    private List<MovieListItem> items = new ArrayList<>();

    public MovieCatalogAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<MovieListItem> nextItems) {
        items = nextItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final MovieListItem item = items.get(position);
        holder.poster.setImageResource(PosterUtils.getPosterRes(holder.itemView.getContext(), item.posterAsset));
        holder.title.setText(item.titleEn);
        String year = item.year == null ? "Year TBD" : String.valueOf(item.year);
        String director = item.director == null ? "Director TBD" : item.director;
        holder.meta.setText(year + " | " + director + " | " + item.genreGroup);
        holder.counts.setText(item.sceneCount + " scenes | " + item.mapVisibleCount + " map-ready");
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onMovieClick(item.movieId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView poster;
        final TextView title;
        final TextView meta;
        final TextView counts;

        ViewHolder(View itemView) {
            super(itemView);
            poster = itemView.findViewById(R.id.ivMovieCatalogPoster);
            title = itemView.findViewById(R.id.tvMovieCatalogTitle);
            meta = itemView.findViewById(R.id.tvMovieCatalogMeta);
            counts = itemView.findViewById(R.id.tvMovieCatalogCounts);
        }
    }
}

