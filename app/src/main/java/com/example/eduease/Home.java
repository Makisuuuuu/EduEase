package com.example.eduease;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import android.media.MediaPlayer;

public class Home extends AppCompatActivity implements QuizAdapter.QuizClickListener {

    private EditText searchQuiz;
    private QuizAdapter quizAdapter;
    private List<Quiz> quizList;  // Original quiz list from Firestore
    private List<Quiz> filteredQuizList;  // Filtered quiz list for search
    private FirebaseFirestore db;
    private MediaPlayer mediaPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        boolean skipMusic = getIntent().getBooleanExtra("SKIP_MUSIC", false);
        if (!skipMusic) {
            playOpeningSound(); // Play the opening sound only if skipMusic is false
        }

        applyEdgeToEdgePadding();
        loadProfileImage();
        setupProfileClickListener();
        setupCreateButtonClickListener();

        RecyclerView quizzesRecyclerView = findViewById(R.id.quizzes_recycler_view);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2); // 2 columns
        quizzesRecyclerView.setLayoutManager(gridLayoutManager);

        searchQuiz = findViewById(R.id.search_quiz);
        quizList = new ArrayList<>();
        filteredQuizList = new ArrayList<>();
        quizAdapter = new QuizAdapter(filteredQuizList, this);
        quizzesRecyclerView.setAdapter(quizAdapter);

        db = FirebaseFirestore.getInstance();
        loadQuizzesFromFirestore();

        setupSearchListener();
    }

    private void playOpeningSound() {
        mediaPlayer = MediaPlayer.create(this, R.raw.opening_music); // Replace with your sound file in res/raw
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release(); // Release MediaPlayer resources
            mediaPlayer = null;
        }
    }


    private void applyEdgeToEdgePadding() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadProfileImage() {
        String profileImageUrl = getIntent().getStringExtra("PROFILE_IMAGE_URL");
        ImageView profileImageView = findViewById(R.id.profile_image_view);

        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(this).load(profileImageUrl).circleCrop().into(profileImageView);
        } else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl().toString()).circleCrop().into(profileImageView);
            }
        }
    }

    private void setupProfileClickListener() {
        ImageButton profileImageView = findViewById(R.id.profile_image_view);
        profileImageView.setOnClickListener(v -> {
            vibrate(); // Trigger vibration
            startActivity(new Intent(Home.this, Settings.class));
        });
    }

    private void setupCreateButtonClickListener() {
        ImageButton createButton = findViewById(R.id.create_btn);
        createButton.setOnClickListener(v -> {
            vibrate(); // Trigger vibration
            showCreateOrImportDialog();
        });
    }

    private void showCreateOrImportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose an option")
                .setItems(new CharSequence[] {
                        "Create Quiz",
                        "Import Quiz"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0: // Create Quiz
                            startActivity(new Intent(Home.this, CreateQuiz.class));
                            break;
                        case 1: // Import Quiz
                            break;
                    }
                })
                .show();
    }


    private void setupSearchListener() {
        searchQuiz.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterQuizzes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Add an OnClickListener to vibrate on click
        searchQuiz.setOnClickListener(v -> vibrate());
    }


    private void filterQuizzes(String query) {
        if (query.isEmpty()) {
            filteredQuizList.clear();
            filteredQuizList.addAll(quizList);  // Show all quizzes if query is empty
        } else {
            filteredQuizList.clear();
            String lowerCaseQuery = query.toLowerCase();
            filteredQuizList.addAll(
                    quizList.stream()
                            .filter(quiz -> quiz.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                                    quiz.getDescription().toLowerCase().contains(lowerCaseQuery))
                            .collect(Collectors.toList())
            );
        }
        quizAdapter.notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadQuizzesFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w("Home", "User not logged in.");
            return;
        }

        String userId = currentUser.getUid();
        CollectionReference quizzesRef = db.collection("quizzes");

        quizzesRef.whereEqualTo("creatorId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        Log.w("Home", "Error fetching quizzes", e);
                        return;
                    }

                    quizList.clear();
                    filteredQuizList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Quiz quiz = doc.toObject(Quiz.class);
                            quiz.setId(doc.getId()); // Set document ID for editing
                            quizList.add(quiz);
                        }
                    }
                    filteredQuizList.addAll(quizList);  // Initially display all quizzes
                    quizAdapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onQuizClick(Quiz quiz) {
        vibrate();
        showQuizOptionsDialog(quiz);
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    private void showQuizOptionsDialog(Quiz quiz) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose an option")
                .setItems(new CharSequence[]{
                        "Edit",
                        "Delete",
                        "Review",
                        "Quiz"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0: // Edit Quiz
                            Intent editIntent = new Intent(Home.this, CreateQuiz.class);
                            editIntent.putExtra("QUIZ_ID", quiz.getId());
                            startActivity(editIntent);
                            break;
                        case 1: // Delete Quiz
                            deleteQuiz(quiz);
                            break;
                        case 2: // Review (Flashcards)
                            break;
                        case 3: // Take Quiz
                            showQuizTypeDialog(quiz);
                            break;
                    }
                })
                .show();
    }

    private void showQuizTypeDialog(Quiz quiz) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Quiz Type")
                .setItems(new CharSequence[]{
                        "Identification",
                        "Multiple Choice",
                        "True or False"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0: // Identification
                            Intent takeIdentificationIntent = new Intent(Home.this, TakeQuiz.class);
                            takeIdentificationIntent.putExtra("QUIZ_ID", quiz.getId());
                            takeIdentificationIntent.putExtra("QUIZ_TYPE", "Identification");
                            startActivity(takeIdentificationIntent);
                            break;
                        case 1: // Multiple Choice
                            break;
                        case 2: // True or False
                            break;
                    }
                })
                .show();
    }


    private void deleteQuiz(Quiz quiz) {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this);
        confirmDialog.setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this quiz?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("quizzes").document(quiz.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Remove the quiz from both lists
                                quizList.remove(quiz);
                                filteredQuizList.remove(quiz);
                                quizAdapter.notifyDataSetChanged();
                                Log.d("Home", "Quiz successfully deleted!");
                            })
                            .addOnFailureListener(e -> Log.e("Home", "Error deleting quiz", e));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
