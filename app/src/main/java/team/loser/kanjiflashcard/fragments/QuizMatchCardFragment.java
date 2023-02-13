package team.loser.kanjiflashcard.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import team.loser.kanjiflashcard.MainActivity;
import team.loser.kanjiflashcard.R;
import team.loser.kanjiflashcard.adapters.CardAdapter;
import team.loser.kanjiflashcard.adapters.CardMatchAdapter;
import team.loser.kanjiflashcard.adapters.PracticeOptionAdapter;
import team.loser.kanjiflashcard.adapters.ResultAdapter;
import team.loser.kanjiflashcard.models.Card;
import team.loser.kanjiflashcard.models.CardMatch;
import team.loser.kanjiflashcard.models.PracticeOption;
import team.loser.kanjiflashcard.models.ResultItem;
import team.loser.kanjiflashcard.utils.SpacingItemDecorator;

public class QuizMatchCardFragment extends Fragment {
    public static final String QUIZ_MATCH_CARD_FRAGMENT_NAME = QuizMatchCardFragment.class.getName();
    private View mView;
    private DatabaseReference flashcardsReference, setsReference;
    private ArrayList<Card> mListAllCards;
    private RecyclerView rcvCardsL, rcvCardsR;
    private CardMatchAdapter mCardAdapterL, mCardAdapterR;
    private ArrayList<CardMatch> mListCardsMatchL, mListCardsMatchR;
    private ProgressDialog loader;
    private int leftSelectedPosition = -1;
    private int rightSelectedPosition = -1;
    private CardMatch currentCard;
    private String leftSelectedId ="", rightSelectedId= "";
    private boolean isIncorrect = false;
    private ArrayList<ResultItem> mListResultItems;
    boolean isFirstChoiceCorrect = true;
    private int correctAns = 0;
    int questNum = 0;

    public QuizMatchCardFragment(DatabaseReference setsReference) {
        this.setsReference = setsReference;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_quiz_match_2_cards, container, false);
        setControls();
        getAllCards();
        setEvents();
        return mView;
    }
    private void setEvents() {
    }
    private void setControls() {
        mListResultItems = new ArrayList<>();
        flashcardsReference = setsReference.child("flashCards");
        loader = new ProgressDialog(getContext());
        rcvCardsL = mView.findViewById(R.id.rcv_cards_match_left);
        rcvCardsR = mView.findViewById(R.id.rcv_cards_match_right);

        SpacingItemDecorator itemDecorator = new SpacingItemDecorator(25, true, true);
        rcvCardsL.addItemDecoration(itemDecorator);
        rcvCardsL.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvCardsR.addItemDecoration(itemDecorator);
        rcvCardsR.setLayoutManager(new LinearLayoutManager(getContext()));

        mListAllCards = new ArrayList<>();
        mListCardsMatchL = new ArrayList<>();
        mListCardsMatchR = new ArrayList<>();
        mCardAdapterL = new CardMatchAdapter(mListCardsMatchL,new CardMatchAdapter.IClickListener() {
            @Override
            public void onClickItemCard(CardMatch cardMatch, int position) {
                leftSelectedId = cardMatch.getId();
                leftSelectedPosition = position;
                currentCard = cardMatch;
                mCardAdapterL.isIncorrect = !checkMatchingCards(leftSelectedId, rightSelectedId);
            }
        });
        rcvCardsL.setAdapter(mCardAdapterL);

        mCardAdapterR = new CardMatchAdapter(mListCardsMatchR, new CardMatchAdapter.IClickListener() {
            @Override
            public void onClickItemCard(CardMatch cardMatch, int position) {
                rightSelectedId = cardMatch.getId();
                rightSelectedPosition = position;
                mCardAdapterR.isIncorrect = !checkMatchingCards(leftSelectedId, rightSelectedId);
            }
        });
        rcvCardsR.setAdapter(mCardAdapterR);

    }
    @SuppressLint("NotifyDataSetChanged")
    private boolean checkMatchingCards(String card1ID, String card2ID){
        if(!card1ID.isEmpty() && !card2ID.isEmpty()){
            if(card1ID.equals(card2ID)){
                if (isFirstChoiceCorrect){
                    correctAns++;
                }
                mListCardsMatchL.remove(leftSelectedPosition);
                mListCardsMatchR.remove(rightSelectedPosition);

                mCardAdapterR.row_index = -1;
                mCardAdapterL.row_index = -1;
                mCardAdapterR.notifyDataSetChanged();
                mCardAdapterL.notifyDataSetChanged();
                leftSelectedId="";
                rightSelectedId="";
                leftSelectedPosition=-1;
                rightSelectedPosition=-1;
                isFirstChoiceCorrect=true;
                if(mListCardsMatchL.size()==0){
                    Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.dialog_result_quiz);

                    TextView tvCorrectAns = dialog.findViewById(R.id.tv_correct_count);
                    TextView tvIncorrectAns = dialog.findViewById(R.id.tv_incorrect_count);
                    Button btnViewResult = dialog.findViewById(R.id.btn_view_result_dialog);
                    Button btnDone = dialog.findViewById(R.id.btn_done_dialog);

                    tvCorrectAns.setText(correctAns + "");
                    tvIncorrectAns.setText((questNum - correctAns) + "");
                    btnDone.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                            if(getActivity().getSupportFragmentManager() != null){
                                getActivity().getSupportFragmentManager().popBackStack();
                            }
                        }
                    });
                    btnViewResult.setOnClickListener(view -> {
                        final Dialog resultsDialog = new Dialog(getContext());
                        resultsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        resultsDialog.setContentView(R.layout.dialog_results);
                        Window window = resultsDialog.getWindow();
                        if(window == null){
                            return;
                        }
                        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        WindowManager.LayoutParams windowAttributes = window.getAttributes();
                        windowAttributes.gravity = Gravity.CENTER;
                        window.setAttributes(windowAttributes);
                        resultsDialog.setCancelable(true);

                        Button btnBack = resultsDialog.findViewById(R.id.btn_back_result_dialog);
                        RecyclerView rcvResults = resultsDialog.findViewById(R.id.rcv_results_dialog);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
                        rcvResults.setLayoutManager(linearLayoutManager);
                        SpacingItemDecorator itemDecorator = new SpacingItemDecorator(10, true, false);
                        rcvResults.addItemDecoration(itemDecorator);

                        ResultAdapter resultAdapter = new ResultAdapter(mListResultItems);
                        rcvResults.setAdapter(resultAdapter);

                        btnBack.setOnClickListener(view1 -> resultsDialog.dismiss());
                        resultsDialog.show();
                    });
                    dialog.show();
                }
                return true;
            }
            isFirstChoiceCorrect=false;
            ResultItem incorrectWord = new ResultItem(currentCard.getText(), currentCard.getHiddenMatchingText());
            if(!mListResultItems.contains(incorrectWord))
                mListResultItems.add(incorrectWord);
            return false;
        }
        return true;
    }
    @SuppressLint("NotifyDataSetChanged")
    private void setUpTwoListCards(){
        for (Card card : mListAllCards){
            String id = UUID.randomUUID().toString();
            CardMatch cardMatchL = new CardMatch(id, card.getTerm(), card.getDefinition());
            mListCardsMatchL.add(cardMatchL);
            CardMatch cardMatchR = new CardMatch(id, card.getDefinition(), card.getTerm());
            mListCardsMatchR.add(cardMatchR);
        }
        Collections.shuffle(mListCardsMatchL);
        Collections.shuffle(mListCardsMatchR);
        mCardAdapterL.notifyDataSetChanged();
        mCardAdapterR.notifyDataSetChanged();
        questNum = mListCardsMatchL.size();
    }
    private void getAllCards() {
        flashcardsReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Card card = dataSnapshot.getValue(Card.class);
                    mListAllCards.add(card);
                }
                setUpTwoListCards();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GET CARDS", "Get list of cards failed");
            }
        });
    }


}
