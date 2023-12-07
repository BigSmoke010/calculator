package com.walid.calculator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.udojava.evalex.Expression;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TextView textView, resulttextView;
    private HorizontalScrollView horizontalScrollView;
    private String SHARED_NAME = "History";
    private ArrayList<String> allInputs = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private boolean hasCalculated = false;
    private boolean closeOnNextOperator = false;
    private boolean isLongPress = false;
    private final Handler handler = new Handler();
    private Runnable delayedAction;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(SHARED_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        setupNightMode();

        String textViewText = sharedPreferences.getString("allInputsText", "");
        String allInputsSharedPreferences = sharedPreferences.getString("allInputsArr", "");

        Type listType = new TypeToken<ArrayList<String>>() {
        }.getType();
        allInputs = new Gson().fromJson(allInputsSharedPreferences, listType);

        if (allInputs == null) {
            allInputs = new ArrayList<>();
        }
        textView = findViewById(R.id.nums);
        textView.setText(textViewText);
        resulttextView = findViewById(R.id.resultnum);
        horizontalScrollView = findViewById(R.id.horizscroll_nums);

        Button[] btnList = {findViewById(R.id.one), findViewById(R.id.two), findViewById(R.id.three),
                findViewById(R.id.four), findViewById(R.id.five), findViewById(R.id.six),
                findViewById(R.id.seven), findViewById(R.id.eight), findViewById(R.id.nine),
                findViewById(R.id.zero)};

        Button[] operations = {findViewById(R.id.plus), findViewById(R.id.minus),
                findViewById(R.id.division), findViewById(R.id.multiplication)};

        setupButtonListeners(btnList, operations);
        setupMenuButton();
        setupEqualsButton();
        setupDotButton();
        setupConstantsButtons();
        setupParenthesesButtons();
        setupFunctionButtons();
        setupDelete();
        setupPowButton();
    }

    @Override
    protected  void onStop() {
        super.onStop();
        editor.putString("allInputsText", "");
        editor.putString("allInputsArr", "");
        editor.apply();
    }
    private void setupConstantsButtons() {
        Button pi = findViewById(R.id.pi);
        pi.setOnClickListener(view -> handleInput("π"));
    }

    private void setupNightMode() {
        boolean nightMode = sharedPreferences.getBoolean("night", false);
        int nightModeFlag = nightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(nightModeFlag);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDelete() {
        ImageButton deleteButton = findViewById(R.id.Delete);
        deleteButton.setOnTouchListener((view, motionEvent) -> {
            handleLongClick(deleteButton, motionEvent);
            return false;
        });
    }

    private void startScaleAlphaAnimation(View view, float scale, float alpha) {
        Animator scaleAnimatorX = ObjectAnimator.ofFloat(view, "scaleX", scale);
        Animator scaleAnimatorY = ObjectAnimator.ofFloat(view, "scaleY", scale);
        Animator opacityAnimator = ObjectAnimator.ofFloat(view, "alpha", alpha);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(scaleAnimatorX).with(scaleAnimatorY).with(opacityAnimator);
        animatorSet.setDuration(100);
        animatorSet.start();
    }

    private void handleLongClick(ImageButton deleteButton, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startScaleAlphaAnimation(deleteButton, 0.8f, 0.4f);
                isLongPress = true;
                hideResultIfNeeded();
                handleDelete();
                delayedAction = () -> {
                    if (isLongPress) {
                        allInputs.clear();
                        textView.setText("");
                    }
                };
                handler.postDelayed(delayedAction, 500);
                return;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isLongPress) {
                    startScaleAlphaAnimation(deleteButton, 1.0f, 1.0f);
                    isLongPress = false;
                }
                handler.removeCallbacks(delayedAction);
        }
    }

    private void handleDelete() {
        hideResultIfNeeded();
        if (!allInputs.isEmpty()) {
            System.out.println(allInputs);
            String lastInput = allInputs.get(allInputs.size() - 1);
            if (isNumber(lastInput) && Double.parseDouble(lastInput) != Math.PI){
                allInputs.remove(allInputs.size() - 1);
                if (lastInput.length() > 1) {
                    allInputs.add(lastInput.substring(0, lastInput.length() - 1));
                }
            } else {
                if (lastInput.equals("cos(") || lastInput.equals("sin(") || lastInput.equals("tan(")) {
                    allInputs.remove(allInputs.size() - 1);
                } else if (lastInput.equals("sqrt(") || lastInput.equals("sqrt")) {
                    closeOnNextOperator = false;
                    allInputs.remove(allInputs.size() - 1);
                    if (allInputs.get(allInputs.size() - 1).equals("*")) {
                        allInputs.remove(allInputs.size() - 1);
                    }
                } else {
                    allInputs.remove(allInputs.size() - 1);
                }
            }
            updateTextView("del");
        }

    }

    private void autoCloseParentheses() {
        int openParenthesesCount = 0;
        int closeParenthesesCount = 0;

        for (String input : allInputs) {
            if (input.equals("(") || input.equals("sqrt(") || input.equals("cos(") || input.equals("sin(") || input.equals("tan(")) {
                openParenthesesCount++;
            } else if (input.equals(")")) {
                closeParenthesesCount++;
            }
        }

        if (openParenthesesCount > closeParenthesesCount) {
            for (int i = closeParenthesesCount; i < openParenthesesCount; i++) {
                allInputs.add(")");
            }
        }
    }

    private void setupButtonListeners(Button[] btnList, Button[] operations) {
        for (Button btn : btnList) {
            btn.setOnClickListener(view -> {
                hideResultIfNeeded();
                handleInput(btn.getText().toString());
            });
        }

        for (Button operation : operations) {
            operation.setOnClickListener(view -> {
                hideResultIfNeeded();
                if (canAddOperator()) {
                    if (closeOnNextOperator) {
                        allInputs.add(")");
                        closeOnNextOperator = false;
                    }
                    allInputs.add(operation.getText().toString());
                    updateTextView(operation.getText().toString());
                }
            });
        }
    }

    private void setupMenuButton() {
        ImageButton menuBtn = findViewById(R.id.menuBtn);
        menuBtn.setOnClickListener(view -> showPopupMenu(menuBtn));
    }

    private void setupEqualsButton() {
        Button equals = findViewById(R.id.equal);
        equals.setOnClickListener(view -> calculateAndShowResult());
    }

    private void setupDotButton() {
        Button dot = findViewById(R.id.dot);
        dot.setOnClickListener(view -> handleDotInput());
    }

    private void hideResultIfNeeded() {
        if (hasCalculated) {
            hideResult();
            hasCalculated = false;
        }
    }

    private void setupParenthesesButtons() {
        Button openParentheses = findViewById(R.id.opPar);
        Button closeParentheses = findViewById(R.id.closePar);
        openParentheses.setOnClickListener(view -> handleInput("("));
        closeParentheses.setOnClickListener(view -> handleInput(")"));
    }

    private void setupFunctionButtons() {
        Button squareRoot = findViewById(R.id.root);
        Button cos = findViewById(R.id.cos);
        Button sin = findViewById(R.id.sin);
        Button tan = findViewById(R.id.tan);
        squareRoot.setOnClickListener(view -> handleInput("√"));
        cos.setOnClickListener(view -> handleInput("cos("));
        sin.setOnClickListener(view -> handleInput("sin("));
        tan.setOnClickListener(view -> handleInput("tan("));
    }

    private void setupPowButton() {
        Button pow = findViewById(R.id.power);
        pow.setOnClickListener(view -> handleInput("^"));
    }


    private void handleDotInput() {
        if (allInputs.size() > 0) {
            String lastNum = allInputs.get(allInputs.size() - 1);

            if (isNumber(lastNum) && !lastNum.contains(".")) {
                allInputs.set(allInputs.size() - 1, lastNum + ".");
                updateTextView(".");
            }

            if (hasCalculated && !resulttextView.getText().toString().contains(".")) {
                hideResult();
                textView.setText(textView.getText() + ".");
                hasCalculated = false;
            }
        }
    }

    private void handleInput(String input) {
        if (hasCalculated) {
            hideResult();
            hasCalculated = false;
        }
        boolean updateTextViewnext = true;
        if (!allInputs.isEmpty() && isNumber(allInputs.get(allInputs.size() - 1))) {
            switch (input) {
                case "√":
                    if (allInputs.get(allInputs.size() - 1).contains(".")) {
                        updateTextViewnext = false;
                    }
                    if (!allInputs.get(allInputs.size() - 1).contains(".") && !allInputs.get(allInputs.size() - 1).contains("sqrt")) {
                        allInputs.add("*");
                        allInputs.add("sqrt");
                    }
                    break;
                case "π":
                    allInputs.add("*");
                    allInputs.add(String.valueOf(Math.PI));
                    break;
                case "^":
                    if (isNumber(allInputs.get(allInputs.size() - 1))) {
                        allInputs.add("^");
                    } else {
                        updateTextViewnext = false;
                        System.out.println("updateNo");
                    }
                    break;
                case "(":
                case "cos(":
                case "sin(":
                case "tan(":
                    if (isNumber(allInputs.get(allInputs.size() - 1))) {
                        allInputs.add("*");
                        allInputs.add(input);
                    } else if (allInputs.get(allInputs.size() - 1).equals("sqrt")) {
                        allInputs.remove(allInputs.size() - 1);
                        allInputs.add("sqrt(");
                    }
                    break;
                case ")":
                    allInputs.add(")");
                    break;
                default:
                    if (allInputs.get(allInputs.size() - 1).equals("sqrt")) {
                        closeOnNextOperator = true;
                        allInputs.remove(allInputs.size() - 1);
                        allInputs.add("sqrt(");
                    }
                    String num = allInputs.get(allInputs.size() - 1);
                    num += input;
                    allInputs.set(allInputs.size() - 1, num);
                    break;
            }
        } else {
            if (!allInputs.isEmpty() && input.equals("√") && !allInputs.get(allInputs.size() - 1).contains(".") && !allInputs.get(allInputs.size() - 1).contains("sqrt")) {
                allInputs.add("sqrt");
            } else if (input.equals("√") && allInputs.isEmpty()) {
                allInputs.add("sqrt");
            } else if (input.equals("^")) {
                if (!allInputs.isEmpty() && isNumber(allInputs.get(allInputs.size() - 1))) {
                    allInputs.add("^");
                } else {
                    updateTextViewnext = false;
                }
            } else if (input.equals("π")) {
                if (!allInputs.isEmpty() && isNumber(allInputs.get(allInputs.size() - 1))) {
                    allInputs.add("*");
                    allInputs.add(String.valueOf(Math.PI));
                } else {
                    allInputs.add(String.valueOf(Math.PI));
                }
            } else {
                if (allInputs.size() > 0) {
                    if (allInputs.get(allInputs.size() - 1).equals("sqrt")) {
                        closeOnNextOperator = true;
                        allInputs.remove(allInputs.size() - 1);
                        allInputs.add("sqrt(");
                    }
                }
                if (!input.equals("^")) {
                    System.out.println("entered num");
                    allInputs.add(input);
                }
            }
        }
        if (updateTextViewnext) {
            updateTextView(input);
        }
    }

    private boolean canAddOperator() {
        return allInputs.size() > 0 && !isOperator(String.valueOf(allInputs.get(allInputs.size() - 1).charAt(allInputs.get(allInputs.size() - 1).length() - 1)))
                && !hasCalculated && lastInputIsNotDot();
    }

    private boolean lastInputIsNotDot() {
        String lastInput = allInputs.get(allInputs.size() - 1);
        return lastInput.charAt(lastInput.length() - 1) != '.';
    }

    private void updateTextView(String input) {
        if (input.equals("del")) {
            textView.setText(textView.getText().toString().substring(0, textView.getText().toString().length() - 1));
        } else {
            textView.setText(textView.getText().toString() + input);
        }
        textView.post(() -> horizontalScrollView.fullScroll(View.FOCUS_RIGHT));
    }

    private void showPopupMenu(View anchorView) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.historyBtn) {
                openHistoryViewer();
            } else {
                showThemeDialog();
            }
            return false;
        });
        popupMenu.show();
    }

    private void openHistoryViewer() {
        String json = new Gson().toJson(allInputs);
        editor.putString("allInputsArr", json);
        editor.putString("allInputsText", textView.getText().toString());
        editor.apply();
        Intent intent = new Intent(MainActivity.this, HistoryViewer.class);
        startActivity(intent);
    }

    private void showThemeDialog() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(MainActivity.this);

        dialogBuilder.setSingleChoiceItems(new String[]{"Light", "Dark"}, sharedPreferences.getBoolean("night", false) ? 1 : 0, (dialogInterface, i) -> {
            switch (i) {
                case 0:
                    setNightMode(false);
                    break;
                case 1:
                    setNightMode(true);
                    break;
            }

            dialogInterface.dismiss();
        }).setTitle("Theme");
        dialogBuilder.show();
    }

    private void setNightMode(boolean nightMode) {
        editor.putBoolean("night", nightMode);
        editor.apply();
        AppCompatDelegate.setDefaultNightMode(nightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void calculateAndShowResult() {
        if (allInputs.size() > 0 && !isOperator(allInputs.get(allInputs.size() - 1))) {
            System.out.println(allInputs);
            autoCloseParentheses();
            hasCalculated = true;
            closeOnNextOperator = false;
            String result = calculateExpression(allInputs);
            saveResultToHistory(removeTrailingZero(result));
            updateInputsWithResult(result);
            showResultAnimation();
            allInputs.clear();

            String[] splitResults = removeTrailingZero(result).split("(?=[-+*/])");
            for (String splitResult : splitResults) {
                if (!splitResult.isEmpty()) {
                    allInputs.add(splitResult);
                }
            }
        }
    }

    private void saveResultToHistory(String result) {
        editor.putString(textView.getText().toString(), result);
        editor.apply();
    }

    private void updateInputsWithResult(String result) {
        allInputs.clear();
        allInputs.add(removeTrailingZero(result));
        resulttextView.setText(removeTrailingZero(result));
    }

    private void showResultAnimation() {
        float currentFontSize = textView.getTextSize() / getResources().getDisplayMetrics().scaledDensity;

        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0.5f);
        ObjectAnimator textSizeAnimator = ObjectAnimator.ofFloat(textView, "textSize", currentFontSize, 42f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alphaAnimator, textSizeAnimator);
        animatorSet.setDuration(1000);
        animatorSet.start();

        ObjectAnimator alphaAnimatorup = ObjectAnimator.ofFloat(resulttextView, "alpha", 0.5f, 1f);
        ObjectAnimator textSizeAnimatorup = ObjectAnimator.ofFloat(resulttextView, "textSize", 42f, currentFontSize);

        AnimatorSet animatorSetup = new AnimatorSet();
        animatorSetup.playTogether(alphaAnimatorup, textSizeAnimatorup);
        animatorSetup.setDuration(1000);
        animatorSetup.start();
    }

    private void hideResult() {
        float currentFontSize = resulttextView.getTextSize() / getResources().getDisplayMetrics().scaledDensity;

        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(resulttextView, "alpha", 1f, 0.5f);
        ObjectAnimator textSizeAnimator = ObjectAnimator.ofFloat(resulttextView, "textSize", currentFontSize, 42f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alphaAnimator, textSizeAnimator);
        animatorSet.setDuration(1000);
        animatorSet.start();

        ObjectAnimator alphaAnimatorup = ObjectAnimator.ofFloat(textView, "alpha", 1f, 1f);
        ObjectAnimator textSizeAnimatorup = ObjectAnimator.ofFloat(textView, "textSize", currentFontSize, currentFontSize);

        AnimatorSet animatorSetup = new AnimatorSet();
        animatorSetup.playTogether(alphaAnimatorup, textSizeAnimatorup);
        animatorSetup.setDuration(1000);
        animatorSetup.start();

        resulttextView.setVisibility(View.INVISIBLE);
        textView.setVisibility(View.INVISIBLE);
        Animation slideInAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        textView.setText(String.join("", allInputs));
        textView.startAnimation(slideInAnimation);

        new Handler().postDelayed(() -> {
            resulttextView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            resulttextView.setText("");
        }, 250);
    }

    public static String calculateExpression(ArrayList<String> arr) {
        String input = String.join("", arr);
        Expression expression = new Expression(input);
        BigDecimal result = expression.eval();
        DecimalFormat decimalFormat = new DecimalFormat("0.#####");
        return decimalFormat.format(result);
    }

    public static String removeTrailingZero(String number) {
        return number.endsWith(".0") ? number.substring(0, number.length() - 2) : number;
    }

    private static boolean isNumber(String token) {
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isOperator(String token) {
        if (token.equals("+") || token.equals("-") || token.equals("/") || token.equals("*")) {
            return true;
        } else {
            return false;
        }
    }

}