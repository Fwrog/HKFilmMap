package com.polyu.hkfilmmap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.polyu.hkfilmmap.data.MovieSceneItem;

import java.util.ArrayList;
import java.util.List;

public class MovieSceneAdapter extends RecyclerView.Adapter<MovieSceneAdapter.ViewHolder> {
    public interface Listener {
        void onToggleCheckIn(long sceneId);
        void onToggleRoute(long sceneId);
        void onOpenPlace(long placeId);
    }

    private final Listener listener;
    private List<MovieSceneItem> items = new ArrayList<>();

    public MovieSceneAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<MovieSceneItem> nextItems) {
        items = nextItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie_scene, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final MovieSceneItem item = items.get(position);
        holder.place.setText(item.placeName);
        holder.meta.setText((item.districtEn == null ? "" : item.districtEn) + " | " + (item.addressEn == null ? "" : item.addressEn));
        holder.description.setText(item.descriptionEn == null ? item.sceneTitleEn : item.descriptionEn);

        holder.checkIn.setImageResource(item.isCheckedIn == 1 ? R.drawable.ic_check_in_done : R.drawable.ic_check_in);
        holder.route.setImageResource(item.isInRoute == 1 ? R.drawable.ic_in_route : R.drawable.ic_add_route);

        StringBuilder status = new StringBuilder();
        if (item.isCheckedIn == 1) status.append(holder.itemView.getContext().getString(R.string.action_remove_check_in));
        if (item.isInRoute == 1) {
            if (status.length() > 0) status.append(" · ");
            status.append(holder.itemView.getContext().getString(R.string.route_candidate_in_route));
        }
        holder.statusText.setText(status.toString());
        holder.statusText.setVisibility(status.length() > 0 ? View.VISIBLE : View.GONE);

        holder.checkIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { listener.onToggleCheckIn(item.sceneId); }
        });
        holder.route.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { listener.onToggleRoute(item.sceneId); }
        });
        holder.openPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { listener.onOpenPlace(item.placeId); }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView place;
        final TextView meta;
        final TextView description;
        final ImageButton checkIn;
        final ImageButton route;
        final ImageButton openPlace;
        final TextView statusText;

        ViewHolder(View itemView) {
            super(itemView);
            place = itemView.findViewById(R.id.tvMovieScenePlace);
            meta = itemView.findViewById(R.id.tvMovieSceneMeta);
            description = itemView.findViewById(R.id.tvMovieSceneDescription);
            checkIn = itemView.findViewById(R.id.btnMovieSceneCheckIn);
            route = itemView.findViewById(R.id.btnMovieSceneRoute);
            openPlace = itemView.findViewById(R.id.btnMovieScenePlace);
            statusText = itemView.findViewById(R.id.tvMovieSceneStatus);
        }
    }
}
