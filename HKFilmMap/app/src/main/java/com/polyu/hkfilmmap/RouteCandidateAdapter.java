package com.polyu.hkfilmmap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.polyu.hkfilmmap.data.RouteCandidateItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RouteCandidateAdapter extends RecyclerView.Adapter<RouteCandidateAdapter.ViewHolder> {
    public interface Listener {
        void onToggleRoute(RouteCandidateItem item);
        void onViewDetail(RouteCandidateItem item);
        void onFocus(RouteCandidateItem item);
    }

    private final Listener listener;
    private List<RouteCandidateItem> items = new ArrayList<>();

    public RouteCandidateAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<RouteCandidateItem> nextItems) {
        items = nextItems == null ? new ArrayList<RouteCandidateItem>() : nextItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route_candidate, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final RouteCandidateItem item = items.get(position);
        holder.poster.setImageResource(PosterUtils.getPosterRes(holder.itemView.getContext(), item.posterAsset));
        holder.place.setText(item.placeName == null ? "Film place" : item.placeName);
        holder.movie.setText(buildMovieLabel(item));
        holder.meta.setText(buildMetaLabel(item));
        holder.status.setText(item.isInRoute == 1
                ? holder.itemView.getContext().getString(R.string.route_candidate_in_route)
                : holder.itemView.getContext().getString(R.string.route_candidate_ready));
        holder.route.setImageResource(item.isInRoute == 1 ? R.drawable.ic_in_route : R.drawable.ic_add_route);
        holder.route.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { listener.onToggleRoute(item); }
        });
        holder.detail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { listener.onViewDetail(item); }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { listener.onFocus(item); }
        });
    }

    private String buildMovieLabel(RouteCandidateItem item) {
        String title = item.topMovieTitle == null || item.topMovieTitle.trim().isEmpty()
                ? "Featured movie" : item.topMovieTitle;
        if (item.topMovieTitleZh != null && !item.topMovieTitleZh.trim().isEmpty()) {
            return title + " / " + item.topMovieTitleZh;
        }
        return title;
    }

    private String buildMetaLabel(RouteCandidateItem item) {
        String district = item.districtEn == null || item.districtEn.trim().isEmpty()
                ? "District pending" : item.districtEn;
        String genre = item.genreGroup == null || item.genreGroup.trim().isEmpty()
                ? "Film" : item.genreGroup;
        return String.format(Locale.US, "%s | %s | %d movies, %d scenes",
                district, genre, Math.max(item.movieCount, 1), Math.max(item.sceneCount, 1));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView poster;
        final TextView place;
        final TextView movie;
        final TextView meta;
        final TextView status;
        final ImageButton route;
        final ImageButton detail;

        ViewHolder(View itemView) {
            super(itemView);
            poster = itemView.findViewById(R.id.ivCandidatePoster);
            place = itemView.findViewById(R.id.tvCandidatePlace);
            movie = itemView.findViewById(R.id.tvCandidateMovie);
            meta = itemView.findViewById(R.id.tvCandidateMeta);
            status = itemView.findViewById(R.id.tvCandidateStatus);
            route = itemView.findViewById(R.id.btnCandidateRoute);
            detail = itemView.findViewById(R.id.btnCandidateDetail);
        }
    }
}
