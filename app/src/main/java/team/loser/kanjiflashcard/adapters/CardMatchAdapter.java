package team.loser.kanjiflashcard.adapters;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import team.loser.kanjiflashcard.R;
import team.loser.kanjiflashcard.fragments.QuizMatchCardFragment;
import team.loser.kanjiflashcard.models.CardMatch;

public class CardMatchAdapter extends RecyclerView.Adapter<CardMatchAdapter.CardMatchViewHolder> {
    private List<CardMatch> mListOptions;
    private ViewBinderHelper viewBinderHelper = new ViewBinderHelper();
    private IClickListener mIClickListener;
    public int row_index = -1;
    public boolean isIncorrect = false;
    int primary_color, surface_color, on_surface_color, on_primary_color;
    int incorrect, onIncorrect;

    public interface IClickListener {
        void onClickItemCard(CardMatch cardMatch, int position);
    }

    public CardMatchAdapter(ArrayList<CardMatch> mListOptions ,IClickListener iClickListener) {
        this.mListOptions = mListOptions;
        this.mIClickListener = iClickListener;
    }

    @NonNull
    @Override
    public CardMatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_match_item, parent, false);
        primary_color = ContextCompat.getColor(view.getContext(), R.color.primary);
        surface_color = ContextCompat.getColor(view.getContext(), R.color.surface);
        on_surface_color = ContextCompat.getColor(view.getContext(), R.color.onSurface);
        on_primary_color = ContextCompat.getColor(view.getContext(), R.color.onPrimary);
        incorrect = ContextCompat.getColor(view.getContext(), R.color.incorrect);
        onIncorrect = ContextCompat.getColor(view.getContext(), R.color.onIncorrect);
        return new CardMatchViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull CardMatchViewHolder holder, int position) {
        CardMatch item = mListOptions.get(position);
        if (item == null) {
            return;
        };
        holder.tvText.setText(item.getText());
        holder.cardViewBackground.setOnClickListener(view -> {
            mIClickListener.onClickItemCard(item, holder.getAbsoluteAdapterPosition());
            row_index = holder.getAbsoluteAdapterPosition();
            notifyDataSetChanged();
        });

        if (row_index == position){
            holder.cardViewBackground.setCardBackgroundColor(primary_color);
            holder.tvText.setTextColor(on_primary_color);
            if(isIncorrect){
                holder.cardViewBackground.setCardBackgroundColor(incorrect);
                holder.tvText.setTextColor(onIncorrect);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        holder.cardViewBackground.setCardBackgroundColor(surface_color);
                        holder.tvText.setTextColor(on_surface_color);
                    }
                }, 1000);
            }
        }else{
            holder.cardViewBackground.setCardBackgroundColor(surface_color);
            holder.tvText.setTextColor(on_surface_color);
        }
    }


    @Override
    public int getItemCount() {
        if (mListOptions != null) {
            return mListOptions.size();
        }
        return 0;
    }

    public class CardMatchViewHolder extends RecyclerView.ViewHolder {
        private TextView tvText;
        private MaterialCardView cardViewBackground;
        public CardMatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tv_card_match);
            cardViewBackground = itemView.findViewById(R.id.cv_card_match_background);
        }
    }
}
