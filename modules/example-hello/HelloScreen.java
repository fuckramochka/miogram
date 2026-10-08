package com.lumigram.module.hello;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.ui.ActionBar.BaseFragment;

/** Мінімальний екран модуля: жодної логіки ядра, тільки свій View. */
public final class HelloScreen extends BaseFragment {

    @Override
    public android.view.View createView(Context context) {
        actionBar.setTitle("Hello");
        FrameLayout layout = new FrameLayout(context);
        TextView text = new TextView(context);
        text.setText("Hello from LumiGram module");
        text.setTextSize(18);
        layout.addView(text, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, android.view.Gravity.CENTER));
        fragmentView = layout;
        return layout;
    }
}
