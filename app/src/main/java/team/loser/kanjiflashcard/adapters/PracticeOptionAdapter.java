package team.loser.kanjiflashcard.adapters;

import static team.loser.kanjiflashcard.MainActivity.onlineUserID;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import team.loser.kanjiflashcard.R;
import team.loser.kanjiflashcard.models.PracticeOption;
import team.loser.kanjiflashcard.models.PracticeOption;

public class PracticeOptionAdapter extends RecyclerView.Adapter<PracticeOptionAdapter.PracticeOptionViewHolder> {
    private List<PracticeOption> mListOptions;
    private ViewBinderHelper viewBinderHelper = new ViewBinderHelper();
    private IClickListener mIClickListener;
    private int row_index = 0;
    int primary_color, surface_color, on_surface_color, on_primary_color;

    public interface IClickListener {
        void onClickItemPracticeOption(int index);
    }

    public PracticeOptionAdapter(ArrayList<PracticeOption> mListOptions, IClickListener iClickListener) {
        this.mListOptions = mListOptions;
        this.mIClickListener = iClickListener;

    }

    @NonNull
    @Override
    public PracticeOptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.practice_option, parent, false);
        primary_color = ContextCompat.getColor(view.getContext(), R.color.primary);
        surface_color = ContextCompat.getColor(view.getContext(), R.color.surface);
        on_surface_color = ContextCompat.getColor(view.getContext(), R.color.onSurface);
        on_primary_color = ContextCompat.getColor(view.getContext(), R.color.onPrimary);
        return new PracticeOptionViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull PracticeOptionViewHolder holder, int position) {
        PracticeOption option = mListOptions.get(position);
        if (option == null) return;
        holder.tvOptionTitle.setText(option.getTitle());
        holder.tvDescription.setText(option.getDescription());
        holder.llPracticeOptions.setOnClickListener(view -> {
            mIClickListener.onClickItemPracticeOption(holder.getAbsoluteAdapterPosition());
            row_index = holder.getAbsoluteAdapterPosition();
            notifyDataSetChanged();
        });

        if (row_index == position){
            holder.cardViewPracticeOption.setCardBackgroundColor(primary_color);
            holder.tvOptionTitle.setTextColor(on_primary_color);
            holder.tvDescription.setTextColor(on_primary_color);
        }else{
            holder.cardViewPracticeOption.setCardBackgroundColor(surface_color);
            holder.tvOptionTitle.setTextColor(on_surface_color);
            holder.tvDescription.setTextColor(on_surface_color);
        }
    }


    @Override
    public int getItemCount() {
        if (mListOptions != null) {
            return mListOptions.size();
        }
        return 0;
    }

    public class PracticeOptionViewHolder extends RecyclerView.ViewHolder {
        private TextView tvOptionTitle, tvDescription;
        private LinearLayout llPracticeOptions;
        private MaterialCardView cardViewPracticeOption;
        public PracticeOptionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOptionTitle = itemView.findViewById(R.id.option_title);
            tvDescription = itemView.findViewById(R.id.option_description);
            llPracticeOptions = itemView.findViewById(R.id.layout_practice_options);
            cardViewPracticeOption = itemView.findViewById(R.id.practice_option_background);
        }
    }
}
