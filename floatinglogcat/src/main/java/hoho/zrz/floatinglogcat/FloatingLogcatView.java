package hoho.zrz.floatinglogcat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static android.content.Context.WINDOW_SERVICE;

//https://stackoverflow.com/questions/32224452/android-unable-to-add-window-permission-denied-for-this-window-type
//https://stackoverflow.com/questions/4481226/creating-a-system-overlay-window-always-on-top
@SuppressWarnings("CatchMayIgnoreException")
@SuppressLint("RtlHardcoded")
public class FloatingLogcatView implements Runnable {
    private boolean isMoving = false;
    private String DEFAULT_COMMAND = "logcat -d";
    private String mCommand = DEFAULT_COMMAND;
    private String mTag = "";

    private FrameLayout mRootView;
    private LinearLayout mSettingView;
    private TextView mConsoleText;
    private TextView mCommandText;
    private EditText mEditText;
    private WindowManager.LayoutParams mWindowParams;
    private GestureDetector mGestureDetector;
    private WindowManager mWindowManager;

    public FloatingLogcatView(@NonNull final Context context) {
        requireNotNull(context);
        if (requireOsVersion() && checkPermission(context)) {
            initView(context);
            mRootView.postDelayed(this, 500);
        }
    }

    private boolean requireOsVersion() {
        return Build.VERSION.SDK_INT >= 26;
    }

    private boolean checkPermission(Context context) {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
            Toast.makeText(context, "Floating logcat view need overlay permission. After granted, next time open it will work.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void requireNotNull(Object... objects) {
        if (objects != null) {
            for (Object o : objects) {
                if (o == null) {
                    throw new NullPointerException();
                }
            }
        }
    }

    private void initView(final Context context) {
        mWindowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        mRootView = new FrameLayout(context);
        mWindowParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
        mRootView.setBackgroundColor(0x99000000);
        mConsoleText = new TextView(context);
        mConsoleText.setMaxLines(5);
        mConsoleText.setTextColor(0xffffffff);
        mConsoleText.setGravity(Gravity.BOTTOM);
        mRootView.addView(mConsoleText, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mWindowManager.addView(mRootView, mWindowParams);
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            private int initY;

            @Override
            public boolean onDown(MotionEvent e) {
                initY = mWindowParams.y;
                return false;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                mWindowParams.y = (int) (initY + e2.getRawY() - e1.getRawY());
                mWindowManager.updateViewLayout(mRootView, mWindowParams);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                switchView();
            }
        });
        mRootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
                    mWindowManager.updateViewLayout(mRootView, mWindowParams);
                } else {
                    mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
                    mWindowManager.updateViewLayout(mRootView, mWindowParams);
                }
                return mGestureDetector.onTouchEvent(event);
            }
        });

        mSettingView = new LinearLayout(context);
        mSettingView.setOrientation(LinearLayout.VERTICAL);
        mCommandText = new TextView(context);
        mCommandText.setText("filter tag:");
        mCommandText.setTextColor(0xffffffff);
        mEditText = new EditText(context);
        mEditText.setTextColor(0xffffffff);
        mSettingView.addView(mCommandText);
        mSettingView.addView(mEditText);
    }

    private void switchView() {
        if (mConsoleText.isAttachedToWindow()) {
            mRootView.removeView(mConsoleText);
            mRootView.addView(mSettingView);
            mEditText.setText(mTag);

        } else {
            mRootView.removeView(mSettingView);
            mRootView.addView(mConsoleText);
            mTag = mEditText.getText().toString();
            if (TextUtils.isEmpty(mTag)) {
                mCommand = DEFAULT_COMMAND;
            } else {
                mCommand = String.format("logcat -d %s:V *:S", mTag);
            }
        }
    }

    @Override
    public void run() {
        try {
            if (!isMoving) {
                Process process = Runtime.getRuntime().exec(mCommand);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder log = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    log.append(line).append("\n");
                }
                mConsoleText.setText(log.toString());
            }
        } catch (Exception e) {
            mConsoleText.setText(e.getLocalizedMessage());
        }
        mRootView.postDelayed(this, 500);
    }
}
