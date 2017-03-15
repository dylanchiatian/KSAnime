package com.daose.ksanime.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

public class OverflowView extends AppCompatTextView {

    public OverflowView(Context context) {
        super(context);
    }

    public OverflowView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverflowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        if(h > oldh) {
            int lastLineIndex = h / getLineHeight();
            setMaxLines(lastLineIndex);
        }
    }
}


