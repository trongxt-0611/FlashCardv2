package team.loser.kanjiflashcard;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // do something
                nextActivity();
            }
        }, 1000);
    }

    private void nextActivity() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        Intent intent;
        if(firebaseUser == null){
            //chua login
            intent = new Intent(this, SignInActivity.class);
        }
        else{
            intent = new Intent(this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }
}