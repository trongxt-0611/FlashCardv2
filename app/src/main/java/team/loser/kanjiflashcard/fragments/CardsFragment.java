package team.loser.kanjiflashcard.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import team.loser.kanjiflashcard.MainActivity;
import team.loser.kanjiflashcard.R;
import team.loser.kanjiflashcard.adapters.CardAdapter;
import team.loser.kanjiflashcard.adapters.PracticeOptionAdapter;
import team.loser.kanjiflashcard.models.Card;
import team.loser.kanjiflashcard.models.PracticeOption;
import team.loser.kanjiflashcard.utils.SpacingItemDecorator;

public class CardsFragment extends Fragment {
    public static final String CARDS_FRAGMENT_NAME = CardsFragment.class.getName();
    private View mView;
    private DatabaseReference flashcardsReference, setsReference;

    private RecyclerView rcvCards;
    private CardAdapter mCardAdapter;
    private List<Card> mListCards;
    private Button btnAddCard, btnPractice;
    private ProgressDialog loader;
    private int numOfCards;
    private TextView tvNumOfCard;
    private TextToSpeech mTTSJapanese;
    private PracticeOptionAdapter mPracticeOptionAdapter;
    private ArrayList<PracticeOption> mListPracticeOptions;
    private int mPracticeOption;

    public CardsFragment(DatabaseReference setsReference) {
        // Required empty public constructor
        this.setsReference = setsReference;
        //TODO: set data
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_cards, container, false);
        setControls();
        getNumberOfCards();
        setEvents();
        getListCardFromRealtimeDataBase();
        initPracticeOptions();
        return mView;
    }
    private void setEvents() {
        btnAddCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addNewCard();
            }
        });
        btnPractice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makePractice(setsReference);
            }
        });
    }
    private void setControls() {
        flashcardsReference = setsReference.child("flashCards");
        btnAddCard = mView.findViewById(R.id.btn_add_card_cards_fragment);
        btnPractice = mView.findViewById(R.id.btn_practice);
        loader = new ProgressDialog(getContext());
        rcvCards = mView.findViewById(R.id.rcv_list_cards_cards_fragment);
        tvNumOfCard = mView.findViewById(R.id.tv_num_of_card_card_fragment);
        rcvCards.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        mTTSJapanese = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i != TextToSpeech.ERROR){
                    mTTSJapanese.setLanguage(Locale.JAPAN);
                }
            }
        });
        mListCards = new ArrayList<>();
        mCardAdapter = new CardAdapter(mListCards, new CardAdapter.IClickListener() {
            @Override
            public void onClickEditCard(Card card) {
                CardsFragment.this.onCLickEditCard(card);
            }

            @Override
            public void onClickRemoveCard(Card card) {
                CardsFragment.this.onClickRemoveCard(card);
            }

            @Override
            public void onClickToSpeech(Card card) {
                //TODO: text to speech
                String text = card.getTerm();
                mTTSJapanese.speak(text, TextToSpeech.QUEUE_FLUSH, null,  ""  );
            }
        });
        rcvCards.setAdapter(mCardAdapter);

    }

    @Override
    public void onDestroy() {
        if(mTTSJapanese != null){
            mTTSJapanese.stop();
            mTTSJapanese.shutdown();
        }
        super.onDestroy();
    }

    private void initPracticeOptions() {
        mListPracticeOptions = new ArrayList<PracticeOption>();
        PracticeOption option1 = new PracticeOption("0", getString(R.string.quiz_review_title),getString(R.string.quiz_review_des));
        PracticeOption option2 = new PracticeOption("1", getString(R.string.quiz_practice_title), getString(R.string.quiz_practice_des));
        PracticeOption option3 = new PracticeOption("2", getString(R.string.quiz_practice_reversed_title),getString(R.string.quiz_practice_reversed_des));
        PracticeOption option4 = new PracticeOption("3", getString(R.string.quiz_match_card_title),getString(R.string.quiz_match_card_des));
        this.mListPracticeOptions.add(option1);
        this.mListPracticeOptions.add(option2);
        this.mListPracticeOptions.add(option3);
        this.mListPracticeOptions.add(option4);

    }
    private void getNumberOfCards() {
        flashcardsReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot card : snapshot.getChildren()) {
                    count++;
                }
                numOfCards = count;
                if (count < 5) {
                    Toast.makeText(getContext(), "Please add at least 5 cards", Toast.LENGTH_SHORT).show();
                }
                if (count == 0) {
                    tvNumOfCard.setText("Add at least 5 cards to start learning");
                }
                else{
                    tvNumOfCard.setText("ALL CARDS: "+ count);
                }
                if(numOfCards < 5){
                    btnPractice.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private void getListCardFromRealtimeDataBase() {
        flashcardsReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Card card = snapshot.getValue(Card.class);
                if (card != null) {
                    mListCards.add(card);
                    mCardAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Card card = snapshot.getValue(Card.class);
                if (card == null || mListCards.isEmpty() || mListCards == null) return;
                for (int i = 0; i < mListCards.size(); i++) {
                    if (card.getId() == mListCards.get(i).getId()) {
                        mListCards.set(i, card);
                        break;
                    }
                }
                mCardAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Card card = snapshot.getValue(Card.class);
                if (card == null || mListCards.isEmpty() || mListCards == null) return;
                for (int i = 0; i < mListCards.size(); i++) {
                    if (card.getId() == mListCards.get(i).getId()) {
                        mListCards.remove(mListCards.get(i));
                        break;
                    }
                }
                mCardAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    //Remove card
    private void onClickRemoveCard(Card card) {
        new AlertDialog.Builder(getContext())
                .setTitle("Remove Card")
                .setMessage("Are you sure you want to remove this Card?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        flashcardsReference.child(card.getId()).removeValue(new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                Toast.makeText(getContext(), "delete successful", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
    //Edit card
    private void onCLickEditCard(@NonNull Card card) {
        Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_card_detail);
        TextView edTitle = dialog.findViewById(R.id.dialog_title);
        edTitle.setText("EDIT CARD");

        EditText edTerm = dialog.findViewById(R.id.ed_term_card_detail_dialog);
        EditText edDefinition = dialog.findViewById(R.id.ed_definition_card_detail_dialog);
        EditText edHowToRead = dialog.findViewById(R.id.ed_read_card_detail_dialog);
        EditText edExamples = dialog.findViewById(R.id.ed_examples_card_detail_dialog);
        ImageButton btnCloseDialog = dialog.findViewById(R.id.btn_close_dialog_card_detail);
        ImageButton btnOk = dialog.findViewById(R.id.btn_ok_card_detail_dialog);
        Button btnAddNextCard = dialog.findViewById(R.id.btn_add_next_card_card_detail_dialog);
        btnAddNextCard.setVisibility(View.INVISIBLE);
        //set view
        edTerm.setText(card.getTerm());
        edTerm.setSelection(card.getTerm().length());
        edDefinition.setText(card.getDefinition());
        edDefinition.setSelection(card.getDefinition().length());
        edHowToRead.setText(card.getHowtoread());
        edHowToRead.setSelection(card.getHowtoread().length());
        edExamples.setText(card.getExamples());
        edExamples.setSelection(card.getExamples().length());
        btnCloseDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        btnOk.setOnClickListener(view -> {
            String term = edTerm.getText().toString().trim();
            String definition = edDefinition.getText().toString().trim();
            String howtoread = edHowToRead.getText().toString().trim();
            String examples = edExamples.getText().toString().trim();
            String cardId = card.getId(); //old id
            if (term.isEmpty()) {
                edTerm.setError("Term is required");
                return;
            }
            if (definition.isEmpty()) {
                edDefinition.setError("Definition is required");
                return;
            }
            if (howtoread.isEmpty()) howtoread = "pronunciation was not added";
            if (examples.isEmpty()) howtoread = "add some examples to learn!";
            loader.setMessage("Updating...");
            loader.setCanceledOnTouchOutside(false);
            loader.show();
            Card updatedCard = new Card(cardId, term, definition, howtoread, examples);
            flashcardsReference.child(cardId).setValue(updatedCard).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(getActivity(), "update successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        String err = task.getException().toString();
                        Toast.makeText(getActivity(), "update failed! " + err, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    loader.dismiss();
                }
            });
        });
        dialog.show();
    }
    // Add new card
    private void addNewCard() {
        Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_card_detail);
        TextView edTitle = dialog.findViewById(R.id.dialog_title);
        edTitle.setText("ADD NEW CARD");

        EditText edTerm = dialog.findViewById(R.id.ed_term_card_detail_dialog);
        EditText edDefinition = dialog.findViewById(R.id.ed_definition_card_detail_dialog);
        EditText edHowToRead = dialog.findViewById(R.id.ed_read_card_detail_dialog);
        EditText edExamples = dialog.findViewById(R.id.ed_examples_card_detail_dialog);
        ImageButton btnCloseDialog = dialog.findViewById(R.id.btn_close_dialog_card_detail);
        ImageButton btnOk = dialog.findViewById(R.id.btn_ok_card_detail_dialog);
        Button btnAddNextCard = dialog.findViewById(R.id.btn_add_next_card_card_detail_dialog);
        btnCloseDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                closeKeyboard();
            }
        });
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String term = edTerm.getText().toString().trim();
                String definition = edDefinition.getText().toString().trim();
                String howtoread = edHowToRead.getText().toString().trim();
                String examples = edExamples.getText().toString().trim();
                String cardId = flashcardsReference.push().getKey();
                if (term.isEmpty()) {
                    edTerm.setError("Term is required");
                    return;
                }
                if (definition.isEmpty()) {
                    edDefinition.setError("Definition is required");
                    return;
                }
                loader.setMessage("Adding...");
                loader.setCanceledOnTouchOutside(false);
                loader.show();
                Card newCard = new Card(cardId, term, definition, howtoread, examples);
                flashcardsReference.child(cardId).setValue(newCard).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getActivity(), "Add successfully!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            String err = task.getException().toString();
                            Toast.makeText(getActivity(), "Add failed! " + err, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }

                        loader.dismiss();
                    }
                });
                closeKeyboard();
            }
        });
        btnAddNextCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String term = edTerm.getText().toString().trim();
                String definition = edDefinition.getText().toString().trim();
                String howtoread = edHowToRead.getText().toString().trim();
                String examples = edExamples.getText().toString().trim();
                String cardId = flashcardsReference.push().getKey();
                if (term.isEmpty()) {
                    edTerm.setError("Term is required");
                    return;
                }
                if (definition.isEmpty()) {
                    edDefinition.setError("Definition is required");
                    return;
                }
                loader.setMessage("Adding...");
                loader.setCanceledOnTouchOutside(false);
                loader.show();
                Card newCard = new Card(cardId, term, definition, howtoread, examples);
                flashcardsReference.child(cardId).setValue(newCard).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getActivity(), "Add successfully!", Toast.LENGTH_SHORT).show();
                            edTerm.setText("");
                            edDefinition.setText("");
                            edExamples.setText("");
                            edHowToRead.setText("");
                            edTerm.requestFocus();

                        } else {
                            String err = task.getException().toString();
                            Toast.makeText(getActivity(), "Add failed! " + err, Toast.LENGTH_SHORT).show();
                        }

                        loader.dismiss();
                    }
                });
            }
        });
        dialog.show();
    }
    private void makePractice(DatabaseReference setRef){
        Dialog optionsDialog = new Dialog(getContext());
        optionsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        optionsDialog.setContentView(R.layout.dialog_practice_modes);
        Window window = optionsDialog.getWindow();
        if (window == null) return;
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams windowAttributes = window.getAttributes();
        windowAttributes.gravity = Gravity.CENTER;
        window.setAttributes(windowAttributes);
        optionsDialog.setCancelable(true);

        RecyclerView rcvOptions = optionsDialog.findViewById(R.id.rcv_practice_options_dialog);
        LinearLayoutManager linearLayoutManager  = new LinearLayoutManager(getContext());
        rcvOptions.setLayoutManager(linearLayoutManager);
        SpacingItemDecorator itemDecorator = new SpacingItemDecorator(10, true, false);
        rcvOptions.addItemDecoration(itemDecorator);

        mPracticeOptionAdapter  = new PracticeOptionAdapter(mListPracticeOptions, new PracticeOptionAdapter.IClickListener() {
            @Override
            public void onClickItemPracticeOption(int index) {
                mPracticeOption = index;
            }
        });
        rcvOptions.setAdapter(mPracticeOptionAdapter);
        optionsDialog.show();
        Button btnStart = optionsDialog.findViewById(R.id.btn_start_practice_options_dialog);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (mPracticeOption){
                    case 0:
                        optionsDialog.dismiss();
                        ((MainActivity)getActivity()).goToQuizFragment(setConfigForQuiz(setRef.toString(), false, false));
                        break;
                    case 1:
                        optionsDialog.dismiss();
                        ((MainActivity)getActivity()).goToQuizFragment(setConfigForQuiz(setRef.toString(), false, true));
                        break;
                    case 2:
                        optionsDialog.dismiss();
                        ((MainActivity)getActivity()).goToQuizFragment(setConfigForQuiz(setRef.toString(), true, true));
                      break;
                    case 3:
                        optionsDialog.dismiss();
                        ((MainActivity)getActivity()).goToQuizMatchCardsFragment(setRef);
                    default:
                        break;
                }
            }
        });

    }
    private HashMap<String, String> setConfigForQuiz(String setRefUrl, boolean isReversed, boolean isShuffle){
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("SET_REF_URL", setRefUrl);
        map.put("IS_REVERSED", String.valueOf(isReversed));
        map.put("IS_SHUFFLE", String.valueOf(isShuffle));
        return map;
    }
    private void closeKeyboard()
    {
        // this will give us the view
        // which is currently focus
        // in this layout
        View view = this.getActivity().getCurrentFocus();

        // if nothing is currently
        // focus then this will protect
        // the app from crash
        if (view != null) {

            // now assign the system
            // service to InputMethodManager
            InputMethodManager manager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
