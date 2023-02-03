package team.loser.kanjiflashcard.adapters;

import static team.loser.kanjiflashcard.MainActivity.onlineUserID;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import team.loser.kanjiflashcard.R;
import team.loser.kanjiflashcard.fragments.SetsFragment;
import team.loser.kanjiflashcard.models.Category;
import team.loser.kanjiflashcard.models.Set;

public class SetAdapter extends RecyclerView.Adapter<SetAdapter.SetViewHolder> {
    private List<Set> mListSets;
    private ViewBinderHelper viewBinderHelper = new ViewBinderHelper();
    private IClickListener mIClickListener;
    private DatabaseReference mCateReference;
    private SetsFragment setsFragment;

    public interface IClickListener {
        void onClickUpdateItem(Set set);
        void onClickDeleteItem(Set set);
        void onClickItemSet(DatabaseReference setRef);
        void onClickStartReview(Set set);
        void onClickStartPractice(Set set);
    }

    public SetAdapter(List<Set> mListSets, SetsFragment fragment, IClickListener iClickListener) {
        this.mListSets = mListSets;
        this.mIClickListener = iClickListener;
        this.setsFragment = fragment;
        mCateReference = this.setsFragment.getCateRef();
    }

    @NonNull
    @Override
    public SetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.set_item, parent, false);

        return new SetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SetViewHolder holder, int position) {
        Set set = mListSets.get(position);
        if (set == null) return;

        viewBinderHelper.bind(holder.swipeRevealLayout, set.getId());
        holder.tvTimeStamp.setText(set.getTimeStamp());
        holder.tvCateName.setText(set.getName());
        holder.tvDescription.setText(set.getDescription());
        countNumberOfCardInSet(set, holder.tvNumOfCards, holder.btnReview_Add, holder.btnPractice); // must call before set practice/review button
        try {
            long days = countDays(set.getTimeStamp());
            holder.tvDayCount.setText(days < 1 ? "recently" : days + " days ago");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        // actions listener
        holder.btnEdit.setOnClickListener(view -> mIClickListener.onClickUpdateItem(set));
        holder.btnRemove.setOnClickListener(view -> mIClickListener.onClickDeleteItem(set));
        holder.btnAddCard.setOnClickListener(view -> mIClickListener.onClickItemSet(mCateReference.child("sets").child(set.getId())));
        holder.btnReview_Add.setOnClickListener(view -> {
            if (holder.btnReview_Add.getText() == "ADD CARDS") {
                mIClickListener.onClickItemSet(mCateReference.child("sets").child(set.getId()));
            } else {
                mIClickListener.onClickStartReview(set);
            }
        });
        holder.btnPractice.setOnClickListener(view -> mIClickListener.onClickStartPractice(set));
        holder.tvCateName.setOnClickListener(view -> mIClickListener.onClickItemSet(mCateReference.child("sets").child(set.getId())));
        //layout click
        holder.tvNumOfCards.setOnClickListener(view -> mIClickListener.onClickItemSet(mCateReference.child("sets").child(set.getId())));
        holder.loButtons.setOnClickListener(view -> mIClickListener.onClickItemSet(mCateReference.child("sets").child(set.getId())));
        holder.tvDescription.setOnClickListener(view -> mIClickListener.onClickItemSet(mCateReference.child("sets").child(set.getId())));
    }


    @Override
    public int getItemCount() {
        if (mListSets != null) {
            return mListSets.size();
        }
        return 0;
    }

    public class SetViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDayCount, tvTimeStamp, tvCateName, tvNumOfCards, tvDescription;
        private SwipeRevealLayout swipeRevealLayout;
        private ImageButton btnEdit, btnRemove, btnAddCard;
        private Button btnReview_Add, btnPractice;
        private LinearLayout loButtons;

        public SetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayCount = itemView.findViewById(R.id.tv_day_count);
            tvTimeStamp = itemView.findViewById(R.id.tv_timestamp);
            tvCateName = itemView.findViewById(R.id.tv_set_name);
            tvNumOfCards = itemView.findViewById(R.id.tv_cards_or_card);
            tvDescription = itemView.findViewById(R.id.tv_description);
            swipeRevealLayout = itemView.findViewById(R.id.swipe_layout);
            btnEdit = itemView.findViewById(R.id.btn_edit_set_item);
            btnRemove = itemView.findViewById(R.id.btn_remove_set_item);
            btnAddCard = itemView.findViewById(R.id.btn_add_card_set_item);
            btnReview_Add = itemView.findViewById(R.id.btn_review_or_add_set_item);
            btnPractice = itemView.findViewById(R.id.btn_practice_set_item);
            //layout
            loButtons = itemView.findViewById((R.id.layout_buttons));
        }
    }

    public long countDays(String fromDate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyy HH:mm:ss", Locale.ENGLISH);
        Date firstDate = sdf.parse(fromDate);
        Date secondDate = sdf.parse(new SimpleDateFormat("dd-MM-yyy HH:mm:ss").format(new Date()));

        long diffInMilliSeconds = Math.abs(secondDate.getTime() - firstDate.getTime());
        long diff = TimeUnit.DAYS.convert(diffInMilliSeconds, TimeUnit.MILLISECONDS);

        return diff;
    }

    private void countNumberOfCardInSet(Set set, TextView textView, Button btnReview, Button btnPractice) {
        DatabaseReference cardRef = mCateReference.child("sets").child(set.getId()).child("flashCards");
        cardRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot i : snapshot.getChildren()) {
                    count++;
                }
                if (count < 2) {
                    textView.setText(count + " card");
                } else {
                    textView.setText(count + " cards");
                }
                if (count < 5) {
                    btnPractice.setVisibility(View.INVISIBLE);
                    btnReview.setText("ADD CARDS");
                } else {
                    btnPractice.setVisibility(View.VISIBLE);
                    btnReview.setText("REVIEW");
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                textView.setText("error");
            }
        });
    }
}
