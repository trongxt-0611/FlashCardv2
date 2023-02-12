package team.loser.kanjiflashcard.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.wajahatkarim3.easyflipview.EasyFlipView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import team.loser.kanjiflashcard.R;
import team.loser.kanjiflashcard.adapters.ResultAdapter;
import team.loser.kanjiflashcard.models.Card;
import team.loser.kanjiflashcard.models.Question;
import team.loser.kanjiflashcard.models.ResultItem;
import team.loser.kanjiflashcard.utils.IOnBackPressed;
import team.loser.kanjiflashcard.utils.SpacingItemDecorator;

public class QuizFragment extends Fragment implements IOnBackPressed {
    public static final String QUIZ_FRAGMENT_NAME = QuizFragment.class.getName();
    private View mView;
    private TextView btnOption1, btnOption2, btnOption3, btnOption4, tvPronunciation, tvExamples;
    private MaterialCardView bgOption1, bgOption2, bgOption3, bgOption4;
    private TextView tvQuesIndex, tvQuestion;
    private DatabaseReference flashCardRoot;

    private ArrayList<Card> mListCards;
    private ArrayList<Question> mListQuestions;
    private ArrayList<String> mListOptions;
    private ArrayList<ResultItem> mListResultItems;
    private ArrayList<Question> mListIncorrectQuestions;
    int questNum = 0;
    int correctAns = 0;
    boolean isFirstChoiceCorrect = true;
    boolean isReversed = false;
    boolean isShuffleQues = false;
    private Question currentQues;
    private EasyFlipView easyFlipView;
    private TextView tvPressToFlip;
    private ImageView btnSpeaker;
    private TextToSpeech mTTSJapanese;
    private String setsReference;
    private HashMap<String, String> config;
    int selectedOption = -1;

    public QuizFragment(String setsReference) {
        // Required empty public constructor
        this.setsReference = setsReference;
    }
    public QuizFragment(HashMap<String, String> config) {
        // Required empty public constructor
        this.config = config;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_quiz, container, false);
        setControls();
        getAllCards();
        setEvents();
        return mView;
    }
    private void setEvents() {
        btnOption1.setOnClickListener(view -> {
            selectedOption = 0;
            checkAnswer(selectedOption);
        });
        btnOption2.setOnClickListener(view -> {
            selectedOption = 1;
            checkAnswer(selectedOption);
        });
        btnOption3.setOnClickListener(view -> {
            selectedOption = 2;
            checkAnswer(selectedOption);
        });
        btnOption4.setOnClickListener(view -> {
            selectedOption = 3;
            checkAnswer(selectedOption);
        });
        btnSpeaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = currentQues.getQuestion();
                mTTSJapanese.speak(text, TextToSpeech.QUEUE_FLUSH, null,  ""  );
            }
        });
    }
    private void setControls() {
        btnOption1 = mView.findViewById(R.id.btn_option1);
        btnOption2 = mView.findViewById(R.id.btn_option2);
        btnOption3 = mView.findViewById(R.id.btn_option3);
        btnOption4 = mView.findViewById(R.id.btn_option4);
        bgOption1 = mView.findViewById(R.id.background_option1);
        bgOption2 = mView.findViewById(R.id.background_option2);
        bgOption3 = mView.findViewById(R.id.background_option3);
        bgOption4 = mView.findViewById(R.id.background_option4);
        tvQuesIndex = mView.findViewById(R.id.tv_question_index);
        tvQuestion = mView.findViewById(R.id.tv_question);
        tvPronunciation = mView.findViewById(R.id.tv_howtoread_quiz);
        tvExamples = mView.findViewById(R.id.tv_examples_quiz);
        easyFlipView = mView.findViewById(R.id.efv_question);
        mListCards = new ArrayList<>();
        mListOptions = new ArrayList<>();
        mListQuestions = new ArrayList<>();
        mListResultItems = new ArrayList<>();
        mListIncorrectQuestions = new ArrayList<>();
        tvPressToFlip = mView.findViewById(R.id.tv_press_to_flip);
        btnSpeaker = mView.findViewById(R.id.btn_speaker);
        mTTSJapanese = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i != TextToSpeech.ERROR){
                    mTTSJapanese.setLanguage(Locale.JAPAN);
                }
            }
        });


        setsReference = config.get("SET_REF_URL");
        isReversed = Boolean.parseBoolean(config.get("IS_REVERSED"));
        isShuffleQues = Boolean.parseBoolean(config.get("IS_SHUFFLE"));
        flashCardRoot = FirebaseDatabase.getInstance().getReferenceFromUrl(setsReference.toString()).child("flashCards");

    }

    @Override
    public void onDestroy() {
        if(mTTSJapanese != null){
            mTTSJapanese.stop();
            mTTSJapanese.shutdown();
        }
        super.onDestroy();
    }

    private void checkAnswer(int selectedOption) {

        if (selectedOption == mListQuestions.get(questNum).getCorrectAns()) {
            //correct
            if (selectedOption == 0) {
                btnOption1.setBackgroundColor(getResources().getColor(R.color.correct));
                btnOption1.setTextColor(ContextCompat.getColorStateList(getContext(),R.color.onCorrect));
                btnOption2.setEnabled(false);
                btnOption3.setEnabled(false);
                btnOption4.setEnabled(false);
            }
            if (selectedOption == 1) {
                btnOption2.setBackgroundColor(getResources().getColor(R.color.correct));
                btnOption2.setTextColor(ContextCompat.getColorStateList(getContext(),R.color.onCorrect));
                btnOption1.setEnabled(false);
                btnOption3.setEnabled(false);
                btnOption4.setEnabled(false);
            }
            if (selectedOption == 2) {
                btnOption3.setBackgroundColor(getResources().getColor(R.color.correct));
                btnOption3.setTextColor(ContextCompat.getColorStateList(getContext(),R.color.onCorrect));
                btnOption1.setEnabled(false);
                btnOption2.setEnabled(false);
                btnOption4.setEnabled(false);
            }
            if (selectedOption == 3) {
                btnOption4.setBackgroundColor(getResources().getColor(R.color.correct));
                btnOption4.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.onCorrect));
                btnOption1.setEnabled(false);
                btnOption3.setEnabled(false);
                btnOption2.setEnabled(false);
            }
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                // do something
                if (isFirstChoiceCorrect) correctAns++;
                changeQuestion();
            }, 1000);
        } else {
            //wrong
            isFirstChoiceCorrect = false;
            if(currentQues!=null){
                if(!mListIncorrectQuestions.contains(currentQues)){
                    mListIncorrectQuestions.add(currentQues);
                    getResults(currentQues.getQuestion(), currentQues.getDefinition());
                }
            }
            if (selectedOption == 0) {
                btnOption1.setBackgroundColor(getResources().getColor(R.color.incorrect));
                btnOption1.setTextColor(ContextCompat.getColorStateList(getContext(),R.color.onIncorrect));
            }
            if (selectedOption == 1) {
                btnOption2.setBackgroundColor(getResources().getColor(R.color.incorrect));
                btnOption2.setTextColor(ContextCompat.getColorStateList(getContext(),R.color.onIncorrect));
            }
            if (selectedOption == 2) {
                btnOption3.setBackgroundColor(getResources().getColor(R.color.incorrect));
                btnOption3.setTextColor(ContextCompat.getColorStateList(getContext(),R.color.onIncorrect));
            }
            if (selectedOption == 3) {
                btnOption4.setBackgroundColor(getResources().getColor(R.color.incorrect));
                btnOption4.setTextColor(ContextCompat.getColorStateList(getContext(),R.color.onIncorrect));
            }

        }
    }
    private void changeQuestion() {
        isFirstChoiceCorrect = true;
        btnOption1.setBackgroundColor(0x00000000);
        btnOption2.setBackgroundColor(0x00000000);
        btnOption3.setBackgroundColor(0x00000000);
        btnOption4.setBackgroundColor(0x00000000);
        btnOption1.setTextColor(getResources().getColor(R.color.primary));
        btnOption2.setTextColor(getResources().getColor(R.color.primary));
        btnOption3.setTextColor(getResources().getColor(R.color.primary));
        btnOption4.setTextColor(getResources().getColor(R.color.primary));
        btnOption1.setEnabled(true);
        btnOption3.setEnabled(true);
        btnOption2.setEnabled(true);
        btnOption4.setEnabled(true);
        if (questNum < mListQuestions.size() - 1) {
            questNum++;
            setQuestion(questNum);
        } else {
            //end quiz
            Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_result_quiz);

            TextView tvCorrectAns = dialog.findViewById(R.id.tv_correct_count);
            TextView tvIncorrectAns = dialog.findViewById(R.id.tv_incorrect_count);
            Button btnViewResult = dialog.findViewById(R.id.btn_view_result_dialog);
            Button btnDone = dialog.findViewById(R.id.btn_done_dialog);

            tvCorrectAns.setText(correctAns + "");
            tvIncorrectAns.setText((questNum + 1 - correctAns) + "");
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
    }
    private void getAllCards() {
        flashCardRoot.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Card card = dataSnapshot.getValue(Card.class);
                    mListCards.add(card);
                    if (isReversed) {
                        mListOptions.add(card.getTerm());
                    } else {
                        mListOptions.add(card.getDefinition());
                    }
                }
                mListQuestions = getQuestionListForQuiz(isReversed);
                if (isShuffleQues) {
                    Collections.shuffle(mListQuestions);
                    easyFlipView.setEnabled(false);
                    tvPressToFlip.setVisibility(View.GONE);
                    btnSpeaker.setVisibility(View.GONE);
                }
                else {
                    easyFlipView.setEnabled(true);
                    tvPressToFlip.setVisibility(View.VISIBLE);
                    btnSpeaker.setVisibility(View.VISIBLE);
                }
                setQuestion(questNum);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GET CARDS", "Get list of cards failed");
            }
        });
    }
    private void setQuestion(int i) {
        tvQuestion.setText(mListQuestions.get(i).getQuestion());
        btnOption1.setText(mListQuestions.get(i).getOption1());
        btnOption2.setText(mListQuestions.get(i).getOption2());
        btnOption3.setText(mListQuestions.get(i).getOption3());
        btnOption4.setText(mListQuestions.get(i).getOption4());
        tvQuesIndex.setText(i + 1 + "/" + mListQuestions.size());
        tvPronunciation.setText(mListQuestions.get(i).getHowToRead());
        tvExamples.setText(mListQuestions.get(i).getExamples());
        currentQues = mListQuestions.get(i); // get current question
    }
    private ArrayList<Question> getQuestionListForQuiz(boolean reversed) {
        if (mListCards.size() == 0) return null;
        ArrayList<Question> listQues = new ArrayList<>();
        for (Card card : mListCards) {
            String ques;
            String ans;
            if (reversed == true) {
                ques = card.getDefinition();
                ans = card.getTerm();
            } else {
                ques = card.getTerm();
                ans = card.getDefinition();
            }
            String read = card.getHowtoread();
            String ex = card.getExamples();
            Question question = makeOneQuestion(ques, ans, read, ex, mListOptions);
            listQues.add(question);
        }
        return listQues;
    }
    private Question makeOneQuestion(String ques, String ans, String read, String ex, ArrayList<String> allOptions) {
        Question resQuestion = new Question();
        resQuestion.setQuestion(ques); // question
        resQuestion.setDefinition(ans);
        resQuestion.setHowToRead(read);
        resQuestion.setExamples(ex);
        String[] options = new String[4];
        Random generator = new Random();
        int indexOfAnswer = generator.nextInt(4);
        resQuestion.setCorrectAns(indexOfAnswer);// answer
        options[indexOfAnswer] = ans;
        if (indexOfAnswer == 0) {
            resQuestion.setOption1(ans);
            ArrayList<Integer> indexOfAnotherOptions = getAnotherOptions(ans, mListOptions);
            resQuestion.setOption2(mListOptions.get(indexOfAnotherOptions.get(0)));
            resQuestion.setOption3(mListOptions.get(indexOfAnotherOptions.get(1)));
            resQuestion.setOption4(mListOptions.get(indexOfAnotherOptions.get(2)));
        }
        if (indexOfAnswer == 1) {
            resQuestion.setOption2(ans);
            ArrayList<Integer> indexOfAnotherOptions = getAnotherOptions(ans, mListOptions);
            resQuestion.setOption1(mListOptions.get(indexOfAnotherOptions.get(0)));
            resQuestion.setOption3(mListOptions.get(indexOfAnotherOptions.get(1)));
            resQuestion.setOption4(mListOptions.get(indexOfAnotherOptions.get(2)));
        }
        if (indexOfAnswer == 2) {
            resQuestion.setOption3(ans);
            ArrayList<Integer> indexOfAnotherOptions = getAnotherOptions(ans, mListOptions);
            resQuestion.setOption2(mListOptions.get(indexOfAnotherOptions.get(0)));
            resQuestion.setOption1(mListOptions.get(indexOfAnotherOptions.get(1)));
            resQuestion.setOption4(mListOptions.get(indexOfAnotherOptions.get(2)));
        }
        if (indexOfAnswer == 3) {
            resQuestion.setOption4(ans);
            ArrayList<Integer> indexOfAnotherOptions = getAnotherOptions(ans, mListOptions);
            resQuestion.setOption2(mListOptions.get(indexOfAnotherOptions.get(0)));
            resQuestion.setOption3(mListOptions.get(indexOfAnotherOptions.get(1)));
            resQuestion.setOption1(mListOptions.get(indexOfAnotherOptions.get(2)));
        }

        return resQuestion;
    }
    private ArrayList<Integer> getAnotherOptions(String answer, ArrayList<String> allOptions) {
        ArrayList<Integer> numbers = new ArrayList<Integer>();
        Random randomGenerator = new Random();
        while (numbers.size() < 3) {

            int random = randomGenerator.nextInt(allOptions.size());
            if (!numbers.contains(random) && allOptions.get(random) != answer) {
                numbers.add(random);
            }
        }
        return numbers;
    }
    private void getResults(String term, String def){
        ResultItem resultItem = new ResultItem(term, def);
        mListResultItems.add(resultItem);
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }
}
