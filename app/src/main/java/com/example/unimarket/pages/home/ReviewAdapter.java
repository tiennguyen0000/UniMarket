package com.example.unimarket.pages.home;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unimarket.R;
import com.example.unimarket.data.model.Review;
import com.example.unimarket.data.service.ReviewService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private final List<Review> items = new ArrayList<>();
    private final ReviewService reviewService = new ReviewService();

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
        holder.bind(items.get(position));
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
        private final View dividerReply;

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
            dividerReply = itemView.findViewById(R.id.dividerReply);
        }

        void bind(Review review) {
            if (review == null) return;

            if (!TextUtils.isEmpty(review.getReviewer_avatar())) {
                Glide.with(itemView.getContext())
                        .load(review.getReviewer_avatar())
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_person);
            }

            tvName.setText(!TextUtils.isEmpty(review.getReviewer_name())
                    ? review.getReviewer_name() : "Người dùng UniMarket");
            tvRating.setText(generateStars(review.getRating() != null ? review.getRating() : 0));
            tvDate.setText(formatDate(review.getCreated_at_timestamp()));
            tvTitle.setText(!TextUtils.isEmpty(review.getTitle()) ? review.getTitle() : "Đánh giá sản phẩm");
            tvContent.setText(!TextUtils.isEmpty(review.getContent()) ? review.getContent() : "Người mua chưa thêm nội dung.");
            updateHelpfulText(review);

            tvReply.setVisibility(View.GONE);
            dividerReply.setVisibility(View.GONE);
            tvHelpful.setOnClickListener(v -> markHelpful(review));
        }

        private void markHelpful(Review review) {
            int current = review.getHelpful_count() != null ? review.getHelpful_count() : 0;
            review.setHelpful_count(current + 1);
            tvHelpful.setEnabled(false);
            updateHelpfulText(review);

            reviewService.save(review, result -> {
                if (!result.isSuccess()) {
                    review.setHelpful_count(current);
                    tvHelpful.setEnabled(true);
                    updateHelpfulText(review);
                    Toast.makeText(itemView.getContext(),
                            "Không thể lưu lượt hữu ích: " + result.getError(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void updateHelpfulText(Review review) {
            int helpfulCount = review.getHelpful_count() != null ? review.getHelpful_count() : 0;
            tvHelpful.setText("Hữu ích (" + helpfulCount + ")");
        }

        private String generateStars(int rating) {
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                stars.append(i < rating ? "★" : "☆");
            }
            return stars.toString();
        }

        private String formatDate(Long timestamp) {
            if (timestamp == null) return "";

            long diff = System.currentTimeMillis() - timestamp;
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