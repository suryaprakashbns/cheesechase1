package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private ImageView imageView;
    private int delay;
    private Button centre, right, left, restartButton, exitButton;
    private FrameLayout rootLayout;
    private int collisionCount = 0;
    private ImageView tom, jerry;
    private Handler handler = new Handler();
    private Random random = new Random();
    private int score = 0;
    private int highscore = 0;
    private Set<View> checkedObstacles = new HashSet<>();
    private int screenWidth;
    private int leftEnd, center, rightEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
        centre = findViewById(R.id.centre);
        right = findViewById(R.id.right);
        left = findViewById(R.id.left);
        tom = findViewById(R.id.tomImageView);
        jerry = findViewById(R.id.jerryImageView);
        restartButton = findViewById(R.id.restartButton);
        exitButton = findViewById(R.id.exitButton);
        imageView = findViewById(R.id.backgroundImage);

        calculatePositions();

        startSpawningObstacles();
        startContinuousCollisionCheck();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        right.setOnClickListener(v -> startHorizontalAnimation(jerry, tom,(screenWidth/2)-jerry.getWidth()));
        left.setOnClickListener(v -> startHorizontalAnimation(jerry, tom,(-screenWidth/2)+jerry.getWidth()));
        centre.setOnClickListener(v -> startHorizontalAnimation(jerry, tom,0));

        restartButton.setOnClickListener(v -> resetGame());
        exitButton.setOnClickListener(v -> finish());
    }

    private void calculatePositions() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;

        leftEnd = 0;
        center = (screenWidth)/3-tom.getWidth();
        rightEnd =screenWidth/2+250;
    }

    private void startSpawningObstacles() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                spawnObstacle();
                handler.postDelayed(this, 1000);
            }
        }, 100);
    }

    private void spawnObstacle() {
        int[] marginOptions = {leftEnd,center,rightEnd};
        int randomMarginLeft = marginOptions[random.nextInt(marginOptions.length)];

        View obstacle = new View(this);
        int obstacleSize = (int) getResources().getDimension(R.dimen.obstacle_size);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(obstacleSize, obstacleSize);
        params.leftMargin = randomMarginLeft;
        obstacle.setLayoutParams(params);
        obstacle.setBackgroundResource(R.drawable.obstacle_shape);
        obstacle.setId(View.generateViewId());
        rootLayout.addView(obstacle);

        startVerticalAnimation(obstacle, 0);
    }

    private void startVerticalAnimation(final View obstacle, long delay) {
        final int screenHeight = getResources().getDisplayMetrics().heightPixels;
        obstacle.setTranslationY(-obstacle.getHeight());

        ViewPropertyAnimator animator = obstacle.animate()
                .translationY(screenHeight)
                .setDuration(5000)
                .setInterpolator(new LinearInterpolator())
                .setStartDelay(delay)
                .withEndAction(() -> {
                    rootLayout.removeView(obstacle);
                    checkedObstacles.remove(obstacle);
                });

        animator.start();
        obstacle.setTag(animator);
    }

    private void startHorizontalAnimation(final ImageView jerry, final ImageView tom, float value) {
        jerry.animate()
                .translationX(value)
                .setDuration(300)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> checkCollision(jerry))
                .start();

        tom.animate()
                .translationX(value)
                .setDuration(300)
                .setInterpolator(new LinearInterpolator())
                .start();
    }

    private void startContinuousCollisionCheck() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                checkCollision(jerry);
                handler.postDelayed(this, 100);
            }
        });
    }

    private void checkCollision(ImageView jerry) {
        int jerryLeft = (int) jerry.getX();
        int jerryRight = jerryLeft + jerry.getWidth();
        int jerryTop = (int) jerry.getY();
        int jerryBottom = jerryTop + jerry.getHeight();

        boolean collisionDetected = false;

        for (int i = 0; i < rootLayout.getChildCount(); i++) {
            View obstacle = rootLayout.getChildAt(i);

            if (obstacle.getId() == right.getId() || obstacle.getId() == left.getId() || obstacle instanceof ImageView) {
                continue;
            }

            if (checkedObstacles.contains(obstacle)) {
                continue;
            }

            int obstacleLeft = (int) obstacle.getX();
            int obstacleRight = obstacleLeft + obstacle.getWidth();
            int obstacleTop = (int) obstacle.getY();
            int obstacleBottom = obstacleTop + obstacle.getHeight();

            boolean horizontalCollision = (jerryLeft < obstacleRight && jerryRight > obstacleLeft);
            boolean verticalCollision = (jerryTop < obstacleBottom && jerryBottom > obstacleTop);
            boolean bottomTopCollision = (obstacleBottom >= jerryTop && obstacleTop < jerryTop && horizontalCollision);

            if ((horizontalCollision && verticalCollision) || bottomTopCollision) {
                handleCollision();
                collisionDetected = true;
                checkedObstacles.add(obstacle);
                break;
            }
        }

        if (!collisionDetected) {
            score++;
        }
    }

    private void handleCollision() {
        collisionCount++;

        float initialY = jerry.getTranslationY();
        if (collisionCount == 1) {
            tom.animate().translationY(initialY - 100).setDuration(50).start();
        } else if (collisionCount == 2) {
            tom.animate().translationY(-200).setDuration(50).start();
            jerry.animate().cancel();
            stopObstacles();
            showEndScreen();
        }
    }

    private void stopObstacles() {
        handler.removeCallbacksAndMessages(null);

        for (int i = 0; i < rootLayout.getChildCount(); i++) {
            View obstacle = rootLayout.getChildAt(i);

            if (obstacle.getId() != right.getId() && obstacle.getId() != left.getId() && !(obstacle instanceof ImageView)) {
                ViewPropertyAnimator animator = (ViewPropertyAnimator) obstacle.getTag();
                if (animator != null) {
                    animator.cancel();
                }
            }
        }
    }

    private void showEndScreen() {
        rootLayout.removeAllViews();

        View endScreen = getLayoutInflater().inflate(R.layout.activity_end, rootLayout, false);

        TextView scoreTextView = endScreen.findViewById(R.id.scoreTextView);
        scoreTextView.setText("Score: " + score);
        if (score > highscore) {
            highscore = score;
        }
        TextView highscoreTextView = endScreen.findViewById(R.id.highscoreTextView);
        highscoreTextView.setText("High score: " + highscore);

        Button restartButton = endScreen.findViewById(R.id.restartButton);
        Button exitButton = endScreen.findViewById(R.id.exitButton);
        ImageView endImageView = endScreen.findViewById(R.id.backgroundImage1);

        restartButton.setOnClickListener(v -> {
            resetGame();
            rootLayout.removeView(endScreen);
        });

        exitButton.setOnClickListener(v -> finish());

        rootLayout.addView(endScreen);
    }

    private void resetGame() {
        collisionCount = 0;
        score = 0;
        checkedObstacles.clear();
        tom.animate().translationY(100).setDuration(50).start();
        rootLayout.removeAllViews();

        rootLayout.addView(imageView);
        rootLayout.addView(centre);
        rootLayout.addView(right);
        rootLayout.addView(left);
        rootLayout.addView(tom);
        rootLayout.addView(jerry);

        startSpawningObstacles();
        startContinuousCollisionCheck();
    }
}
