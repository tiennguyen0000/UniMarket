package com.example.unimarket.pages.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unimarket.R;
import com.example.unimarket.data.model.Review;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private final List<Review> items = new ArrayList<>();

    public void submitList(List<Review> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = items.get(position);
        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAvatar;
        private final TextView tvName;
        private final TextView tvRating;
        private final TextView tvDate;
        private final TextView tvTitle;
        private final TextView tvContent;
        private final TextView tvHelpful;
        private final TextView tvReply;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivReviewerAvatar);
            tvName = itemView.findViewById(R.id.tvReviewerName);
            tvRating = itemView.findViewById(R.id.tvReviewRating);
            tvDate = itemView.findViewById(R.id.tvReviewDate);
            tvTitle = itemView.findViewById(R.id.tvReviewTitle);
            tvContent = itemView.findViewById(R.id.tvReviewContent);
            tvHelpful = itemView.findViewById(R.id.tvHelpful);
            tvReply = itemView.findViewById(R.id.tvReply);
        }

        void bind(Review review) {
            if (review == null) return;

            // Avatar
            if (review.getReviewer_avatar() != null) {
                Glide.with(itemView.getContext())
                        .load(review.getReviewer_avatar())
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .into(ivAvatar);
            }

            // Name
            tvName.setText(review.getReviewer_name() != null ? review.getReviewer_name() : "Người dùng");

            // Rating stars
            int rating = review.getRating() != null ? review.getRating() : 0;
            tvRating.setText(generateStars(rating));

            // Date
            tvDate.setText(formatDate(review.getCreated_at_timestamp()));

            // Title & Content
            tvTitle.setText(review.getTitle() != null ? review.getTitle() : "");
            tvContent.setText(review.getContent() != null ? review.getContent() : "");

            // Helpful count
            Integer helpfulCount = review.getHelpful_count() != null ? review.getHelpful_count() : 0;
            tvHelpful.setText("👍 Hữu ích (" + helpfulCount + ")");

            // Reply button
            tvReply.setOnClickListener(v -> {
                // TODO: Implement reply functionality
            });

            tvHelpful.setOnClickListener(v -> {
                // TODO: Implement helpful functionality
            });
        }

        private String generateStars(int rating) {
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                if (i < rating) {
                    stars.append("★");
                } else {
                    stars.append("☆");
                }
            }
            return stars.toString();
        }

        private String formatDate(Long timestamp) {
            if (timestamp == null) return "";

            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            long days = TimeUnit.MILLISECONDS.toDays(diff);
            if (days > 0) {
                if (days == 1) return "1 ngày trước";
                if (days < 30) return days + " ngày trước";
                if (days < 365) return (days / 30) + " tháng trước";
                return (days / 365) + " năm trước";
            }

            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            if (hours > 0) {
                return hours + " giờ trước";
            }

            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            if (minutes > 0) {
                return minutes + " phút trước";
            }

            return "Vừa xong";
        }
    }
}
