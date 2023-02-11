package team.loser.kanjiflashcard.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import team.loser.kanjiflashcard.MainActivity;
import team.loser.kanjiflashcard.QuizActivity;
import team.loser.kanjiflashcard.R;
import team.loser.kanjiflashcard.adapters.PracticeOptionAdapter;
import team.loser.kanjiflashcard.adapters.SetAdapter;
import team.loser.kanjiflashcard.models.Category;
import team.loser.kanjiflashcard.models.PracticeOption;
import team.loser.kanjiflashcard.models.Set;
import team.loser.kanjiflashcard.utils.IOnBackPressed;
import team.loser.kanjiflashcard.utils.SpacingItemDecorator;

public class SetsFragment extends Fragment {
    public static final String SETS_FRAGMENT_NAME = SetsFragment.class.getName();
    private View mView;
    private DatabaseReference cateRef, setsRef;
    public DatabaseReference getCateRef(){
        return this.cateRef;
    };

    private TextView tvNumOfSets;
    private RecyclerView rcvSets;
    private SetAdapter mSetAdapter;
    private List<Set> mListSets;
    private FloatingActionButton btnAddSet;
    private ProgressDialog loader;
    private PracticeOptionAdapter mPracticeOptionAdapter;
    private ArrayList<PracticeOption> mListPracticeOptions;
    private int mPracticeOption;

    public SetsFragment(DatabaseReference cateRef ) {
        this.cateRef = cateRef;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_sets, container, false);
        setControls();
        setEvents();
        getNumberOfSet();
        getListSetsFromRealtimeDataBase();
        initPracticeOptions();
        return mView;
    }

    private void setEvents() {
        btnAddSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addNewSet();
            }
        });
    }
    private void setControls() {
        //recycler view
        setsRef = cateRef.child("sets");
        tvNumOfSets = mView.findViewById(R.id.tv_num_of_sets);
        rcvSets = mView.findViewById(R.id.rcv_list_sets);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        rcvSets.setLayoutManager(linearLayoutManager);
        SpacingItemDecorator itemDecorator = new SpacingItemDecorator(20, true, false);
        rcvSets.addItemDecoration(itemDecorator);



        mListSets = new ArrayList<>();
        mSetAdapter = new SetAdapter(mListSets, this, new SetAdapter.IClickListener() {
            @Override
            public void onClickUpdateItem(Set set) {
                onClickUpdateSet(set);
            }
            @Override
            public void onClickDeleteItem(Set set) {
                onClickDeleteSet(set);
            }
            @Override
            public void onClickItemSet(DatabaseReference setRef) {
                ((MainActivity)getActivity()).goToCardsFragment(setRef);
            }
            @Override
            public void onClickStartReview(DatabaseReference setRef) {
                Intent intent = new Intent(getContext(), QuizActivity.class);
                intent.putExtra("SET_REF_URL", setRef.toString());
                intent.putExtra("IS_REVERSED", false);
                intent.putExtra("IS_SHUFFLE", false);
                startActivity(intent);
            }
            @Override
            public void onClickStartPractice(DatabaseReference setRef) {
                makePractice(setRef);
            }
        });
        rcvSets.setAdapter(mSetAdapter);
        loader = new ProgressDialog(getContext());
        btnAddSet = mView.findViewById(R.id.btn_add_set);
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
                        startActivity(setUpIntentPractice(setRef.toString(), false, true));
                        break;
                    case 1:
                        startActivity(setUpIntentPractice(setRef.toString(), true, true));
                        break;
                    default:
                        break;
                }
            }
        });

    }
    private void initPracticeOptions() {
        mListPracticeOptions = new ArrayList<PracticeOption>();
        PracticeOption option2 = new PracticeOption("1", getString(R.string.quiz_practice_title), getString(R.string.quiz_practice_des));
        PracticeOption option3 = new PracticeOption("2", getString(R.string.quiz_practice_reversed_title),getString(R.string.quiz_practice_reversed_des));
        this.mListPracticeOptions.add(option2);
        this.mListPracticeOptions.add(option3);
    }
    private Intent setUpIntentPractice(String setRefUrl, boolean isReversed, boolean isShuffle){
        Intent intent = new Intent(getContext(), QuizActivity.class);
        intent.putExtra("SET_REF_URL", setRefUrl);
        intent.putExtra("IS_REVERSED", isReversed);
        intent.putExtra("IS_SHUFFLE", isShuffle);
        return intent;
    }
    private void getNumberOfSet() {
        setsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot i : snapshot.getChildren()) {
                    count++;
                }
                if (count == 0) {
                    tvNumOfSets.setText("Add some sets to start learning!");
                }
                else{
                    tvNumOfSets.setText("ALL SETS: "+ count);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private void addNewSet() {
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_category_detail);
        Window window = dialog.getWindow();
        if(window == null) return;
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams windowAttributes = window.getAttributes();
        windowAttributes.gravity = Gravity.CENTER;
        window.setAttributes(windowAttributes);
        dialog.setCancelable(true);
        TextView tvDialogTitle = dialog.findViewById(R.id.tv_dialog_title);
        tvDialogTitle.setText("ADD NEW SET");

        EditText edCategoryName = dialog.findViewById(R.id.ed_category_name);
        EditText edDescription = dialog.findViewById(R.id.ed_category_description);
        Button btnSave = dialog.findViewById(R.id.btn_save);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String setName = edCategoryName.getText().toString().trim();
                String description = edDescription.getText().toString().trim();
                String setId = cateRef.push().getKey().toString() + "-set";
                String timeStamp = new SimpleDateFormat("dd-MM-yyy HH:mm:ss").format(new Date());
                if(setName.isEmpty()){
                    edCategoryName.setError("Category name is required");
                    return;
                }
                if(description.isEmpty()){
                    description = "No description";
                }
                loader.setMessage("Adding...");
                loader.setCanceledOnTouchOutside(false);
                loader.show();
                Category newCategory = new Category(setId, setName, description, timeStamp);

                setsRef.child(setId).setValue(newCategory).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(getActivity(), "Add successfully!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                        else {
                            String err =  task.getException().toString();
                            Toast.makeText(getActivity()  , "Add failed! " + err, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }

                        loader.dismiss();
                    }
                });
            }
        });
        dialog.show();
    }
    private void getListSetsFromRealtimeDataBase(){
        setsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Set set = snapshot.getValue(Set.class);
                if(set != null){
                    mListSets.add(0,set);
                    mSetAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Set set = snapshot.getValue(Set.class);
                if( set==null || mListSets.isEmpty() || mListSets == null) return;
                for(int i=0; i < mListSets.size(); i++){
                    if(set.getId() == mListSets.get(i).getId()){
                        mListSets.set(i, set);
                        break;
                    }
                }
                mSetAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Set set = snapshot.getValue(Set.class);
                if( set==null || mListSets.isEmpty() || mListSets == null) return;
                for(int i=0; i <mListSets.size(); i++){
                    if(set.getId() == mListSets.get(i).getId()){
                        mListSets.remove( mListSets.get(i));
                        break;
                    }
                }
                mSetAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private void onClickDeleteSet(Set set){
        new AlertDialog.Builder(getContext())
                .setTitle("Remove Set")
                .setMessage("All cards belong to this set will be delete! Are you sure to remove it?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        cateRef.child("sets").child(set.getId()).removeValue(new DatabaseReference.CompletionListener() {
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
    private void onClickUpdateSet(Set set){
        final Dialog editDialog = new Dialog(getActivity());
        editDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        editDialog.setContentView(R.layout.dialog_category_detail);
        Window window = editDialog.getWindow();
        if(window == null) return;
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams windowAttributes = window.getAttributes();
        windowAttributes.gravity = Gravity.CENTER;
        window.setAttributes(windowAttributes);
        editDialog.setCancelable(true);

        TextView tvDialogTitle = editDialog.findViewById(R.id.tv_dialog_title);
        tvDialogTitle.setText("UPDATE CATEGORY");

        EditText edCategoryName = editDialog.findViewById(R.id.ed_category_name);
        EditText edDescription = editDialog.findViewById(R.id.ed_category_description);
        Button btnUpdate = editDialog.findViewById(R.id.btn_save);
        Button btnCancel = editDialog.findViewById(R.id.btn_cancel);

        edCategoryName.setText(set.getName());
        edCategoryName.setSelection(set.getName().length());
        edDescription.setText(set.getDescription());
        edDescription.setSelection(set.getDescription().length());
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editDialog.dismiss();
            }
        });
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String cateName = edCategoryName.getText().toString().trim();
                String desc = edDescription.getText().toString().trim();
                String timeStamp = new SimpleDateFormat("dd-MM-yyy HH:mm:ss").format(new Date());
                Map<String, Object> map = new HashMap<>();
                map.put("name", cateName);
                map.put("description", desc);
                map.put("timeStamp", timeStamp);
                loader.setMessage("Updating...");
                loader.setCanceledOnTouchOutside(false);
                loader.show();
                cateRef.child(set.getId()).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(getActivity(), "update successful!", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            String err = task.getException().toString();
                            Toast.makeText(getActivity(), "update failed!", Toast.LENGTH_SHORT).show();
                        }
                        loader.dismiss();
                    }
                });
                editDialog.dismiss();
            }
        });
        editDialog.show();
    }

}
