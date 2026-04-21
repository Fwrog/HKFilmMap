package com.polyu.hkfilmmap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {
    private List<AchievementItem> items = new ArrayList<>();

    public void submit(List<AchievementItem> nextItems) {
        items = nextItems == null ? new ArrayList<AchievementItem>() : nextItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_achievement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AchievementItem item = items.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvDescription.setText(item.description);
        holder.tvProgress.setText(item.current + "/" + item.target);
        holder.ivIcon.setImageResource(item.iconResId);
        holder.tvStatus.setText(item.unlocked
                ? holder.itemView.getContext().getString(R.string.achievement_status_unlocked)
                : holder.itemView.getContext().getString(R.string.achievement_status_locked));
        int statusColor = ContextCompat.getColor(
                holder.itemView.getContext(),
                item.unlocked ? R.color.checked_in : R.color.text_secondary
        );
        holder.tvStatus.setTextColor(statusColor);
        int cardColor = ContextCompat.getColor(
                holder.itemView.getContext(),
                item.unlocked ? R.color.surface_card : R.color.surface
        );
        holder.cardView.setCardBackgroundColor(cardColor);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final CardView cardView;
        final ImageView ivIcon;
        final TextView tvTitle;
        final TextView tvDescription;
        final TextView tvStatus;
        final TextView tvProgress;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardAchievement);
            ivIcon = itemView.findViewById(R.id.ivAchievementIcon);
            tvTitle = itemView.findViewById(R.id.tvAchievementTitle);
            tvDescription = itemView.findViewById(R.id.tvAchievementDescription);
            tvStatus = itemView.findViewById(R.id.tvAchievementStatus);
            tvProgress = itemView.findViewById(R.id.tvAchievementProgress);
        }
    }
}
