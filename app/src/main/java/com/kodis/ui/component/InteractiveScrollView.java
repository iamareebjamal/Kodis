package com.kodis.ui.component;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;
import com.kodis.listener.OnBottomReachedListener;
import com.kodis.listener.OnScrollListener;

public class InteractiveScrollView extends ScrollView {
    OnBottomReachedListener onBottomReachedListener;
    OnScrollListener onScrollListener;

    public InteractiveScrollView(Context context, AttributeSet attrs,
                                 int defStyle) {
        super(context, attrs, defStyle);
    }

    public InteractiveScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InteractiveScrollView(Context context) {
        super(context);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        onScrollListener.onScrolled();

        if (t > oldt)
            onScrollListener.onScrolledDown();
        else
            onScrollListener.onScrolledUp();

        View view = getChildAt(getChildCount() - 1);
        int diff = (view.getBottom() - (getHeight() + getScrollY()));

        if (diff <= 20 && onBottomReachedListener != null) {
            onBottomReachedListener.onBottomReached();
        }

        super.onScrollChanged(l, t, oldl, oldt);
    }

    public OnBottomReachedListener getOnBottomReachedListener() {
        return onBottomReachedListener;
    }

    public void setOnBottomReachedListener(
            OnBottomReachedListener onBottomReachedListener) {
        this.onBottomReachedListener = onBottomReachedListener;
    }

    public OnScrollListener getOnScrollListener() {
        return onScrollListener;
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

}