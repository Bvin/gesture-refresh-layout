package cn.bvin.android.lib.widget.refresh;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;


/**
 * 手势刷新
 * Created by bvin on 2016/12/13.
 */

public class GestureRefreshLayout extends ViewGroup {

    private static final String TAG = "GestureRefreshLayout";
    private static final int MAX_ALPHA = 255;
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int SCALE_DOWN_DURATION = 150;
    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;
    private static final int ANIMATE_TO_START_DURATION = 200;
    private static final int[] LAYOUT_ATTRS = new int[]{android.R.attr.enabled};
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;
    private static final int DEFAULT_REFRESH_DISTANCE = 64;
    private static final int DEFAULT_REFRESH_ORIGINAL_POSITION = -40;

    private View mTarget;
    private OnRefreshListener mListener;
    private boolean mRefreshing = false;
    private int mTouchSlop;

    // 释放刷新距离，用于边界判断
    private float mTotalDragDistance = -1;

    private int mMediumAnimationDuration;
    private int mCurrentTargetOffsetTop;
    // Whether or not the starting offset has been determined.
    private boolean mOriginalOffsetCalculated = false;

    protected int mOriginalOffsetTop;

    // 定义的下拉End距离，用于调整回弹位置
    int mSpinnerOffsetEnd;

    private float mInitialMotionY;
    private float mInitialDownY;

    // Whether this item is scaled up rather than clipped
    private boolean mScale;

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;

    private View mRefreshView;
    private int mRefreshViewIndex = -1;
    protected int mFrom;

    private float mStartingScale;

    private Animation mScaleAnimation;
    private Animation mScaleDownAnimation;
    private Animation mScaleDownToStartAnimation;

    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;

    // 拦截到了从子View滑过
    private boolean mIsInterceptedMoveEvent;

    private OnChildScrollUpCallback mChildScrollUpCallback;
    private OnGestureStateChangeListener mGestureChangeListener;

    private boolean mNotify;

    // Whether the client has set a custom starting position;
    private boolean mUsingCustomStart;

    // 是否移动Content在下拉的过程中
    private boolean mTranslateContent;

    private OnLayoutTranslateCallback mOnLayoutTranslateCallback;

    private Animation.AnimationListener mRefreshListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mRefreshing) {
                // Make sure the progress view is fully visible
                // 开始刷新动画
                if (mNotify) {
                    if (mListener != null) {
                        mListener.onRefresh();
                    }
                }
            } else {
                reset();
            }
            mCurrentTargetOffsetTop = mRefreshView.getTop();
        }
    };

    public GestureRefreshLayout(Context context) {
        this(context, null);
    }

    public GestureRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);

        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();

        final DisplayMetrics metrics = getResources().getDisplayMetrics();

        // the absolute offset has to take into account that the circle starts at an offset
        mTotalDragDistance = mSpinnerOffsetEnd = (int) (DEFAULT_REFRESH_DISTANCE * metrics.density);

        mOriginalOffsetTop = mCurrentTargetOffsetTop = (int) (DEFAULT_REFRESH_ORIGINAL_POSITION * metrics.density);// refresh height

    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (mRefreshViewIndex < 0) {
            return i;
        } else if (i == childCount - 1) {
            // Draw the selected child last
            return mRefreshViewIndex;
        } else if (i >= mRefreshViewIndex) {
            // Move the children after the selected child earlier one
            return i + 1;
        } else {
            // Keep the children before the selected child the same
            return i;
        }
    }

    void reset() {
        mRefreshView.clearAnimation();
        mRefreshView.setVisibility(View.GONE);
        setColorViewAlpha(MAX_ALPHA);
        // Return the circle to its start position
        if (mScale) {
            setAnimationProgress(0 /* animation complete and view is hidden */);
        } else {
            setTargetOffsetTopAndBottom(mOriginalOffsetTop - mCurrentTargetOffsetTop,
                    true /* requires update */);
        }
        mCurrentTargetOffsetTop = mRefreshView.getTop();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled){
            reset();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mRefreshView == null) {
            return;
        }
        reset();
    }

    /**
     * The refresh indicator starting and resting position is always positioned
     * near the top of the refreshing content. This position is a consistent
     * location, but can be adjusted in either direction based on whether or not
     * there is a toolbar or actionbar present.
     * <p>
     * <strong>Note:</strong> Calling this will reset the position of the refresh indicator to
     * <code>start</code>.
     * </p>
     *
     * @param scale Set to true if there is no view at a higher z-order than where the progress
     *              spinner is set to appear. Setting it to true will cause indicator to be scaled
     *              up rather than clipped.
     * @param start The offset in pixels from the top of this view at which the
     *              progress spinner should appear.
     * @param end The offset in pixels from the top of this view at which the
     *            progress spinner should come to rest after a successful swipe
     *            gesture.
     */
    public void setRefreshViewOffset(boolean scale, int start, int end) {
        mOriginalOffsetTop = start;
        mSpinnerOffsetEnd = end;
        mUsingCustomStart = true;
        reset();
        mRefreshing = false;
    }

    public void setTranslateContent(boolean translateContent) {
        mTranslateContent = translateContent;
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    public void setOnLayoutTranslateCallback(OnLayoutTranslateCallback onLayoutTranslateCallback) {
        mOnLayoutTranslateCallback = onLayoutTranslateCallback;
    }

    /**
     * Pre API 11, alpha is used to make the progress circle appear instead of scale.
     */
    private boolean isAlphaUsedForScale() {
        return android.os.Build.VERSION.SDK_INT < 11;
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (refreshing && mRefreshing != refreshing) {
            // scale and show
            mRefreshing = refreshing;
            int endTarget = 0;
            if (!mUsingCustomStart) {
                endTarget = (mSpinnerOffsetEnd + mOriginalOffsetTop);
            } else {
                endTarget =  mSpinnerOffsetEnd;
            }
            // offset = mSpinnerOffsetEnd + mOriginalOffsetTop - mCurrentTargetOffsetTop
            // 没有使用自定义Start位置，mOriginalOffsetTop和mCurrentTargetOffsetTop相抵消
            // 就是mSpinnerOffsetEnd的位置。
            setTargetOffsetTopAndBottom(endTarget - mCurrentTargetOffsetTop,
                    true /* requires update */);
            mNotify = false;
            startScaleUpAnimation(mRefreshListener);
        } else {
            setRefreshing(refreshing, false /* notify */);
        }
    }

    private void startScaleUpAnimation(Animation.AnimationListener listener) {
        mRefreshView.setVisibility(View.VISIBLE);
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            // Pre API 11, alpha is used in place of scale up to show the
            // progress circle appearing.
            // Don't adjust the alpha during appearance otherwise.
            //mProgress.setAlpha(MAX_ALPHA);
        }
        mScaleAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(interpolatedTime);
            }
        };
        mScaleAnimation.setDuration(mMediumAnimationDuration);
        mScaleAnimation.setAnimationListener(listener);
        mRefreshView.clearAnimation();
        mRefreshView.startAnimation(mScaleAnimation);
    }

    /**
     * 更改RefreshView的透明度.
     * @param targetAlpha
     */
    private void setColorViewAlpha(int targetAlpha) {
        if (mRefreshView != null && mRefreshView.getBackground() != null) {
            mRefreshView.getBackground().setAlpha(targetAlpha);
        }
    }

    /**
     * Pre API 11, this does an alpha animation.
     * @param progress
     */
    private void setAnimationProgress(float progress) {
        if (isAlphaUsedForScale()) {
            setColorViewAlpha((int) (progress * MAX_ALPHA));
        } else {
            ViewCompat.setScaleX(mRefreshView, progress);
            ViewCompat.setScaleY(mRefreshView, progress);
        }
    }

    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mRefreshing != refreshing) {
            mNotify = notify;
            ensureTarget();
            mRefreshing = refreshing;
            if (mRefreshing) {
                animateOffsetToCorrectPosition(mCurrentTargetOffsetTop, mRefreshListener);
            } else {
                startScaleDownAnimation(mRefreshListener);
            }
        }
    }

    private void startScaleDownAnimation(Animation.AnimationListener listener) {
        mScaleDownAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(1 - interpolatedTime);
            }
        };
        mScaleDownAnimation.setDuration(SCALE_DOWN_DURATION);
        mScaleDownAnimation.setAnimationListener(listener);
        mRefreshView.clearAnimation();
        mRefreshView.startAnimation(mScaleDownAnimation);
    }

    private void ensureTarget() {
        if (mTarget == null) {
            /*if (getChildCount() > 2) {
                throw new IllegalStateException("GestureRefreshLayout can host only 2 direct child");
            } else */{
                mTarget = getChildAt(0);
                mRefreshView = getChildAt(1);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        Log.d(TAG, "onMeasure: ");
        if (mTarget == null) {
            ensureTarget();
        }

        if (mTarget == null) {
            return;
        }
        /*mTarget.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));*/
        measureChild(mTarget, widthMeasureSpec, heightMeasureSpec);

        if (mRefreshView == null) {
            return;
        }
        measureChild(mRefreshView, widthMeasureSpec, heightMeasureSpec);

        // measure other child view.
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child != mTarget && child != mRefreshView)
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
        }

        if (!mUsingCustomStart && !mOriginalOffsetCalculated) {
            mOriginalOffsetCalculated = true;
            mCurrentTargetOffsetTop = mOriginalOffsetTop = -mRefreshView.getMeasuredHeight();
            // 比RefreshView多出64px
            if (mTotalDragDistance == (int) (DEFAULT_REFRESH_DISTANCE * getResources().getDisplayMetrics().density)) {
                // 如果没有手动设置刷新距离还是默认值，就更改为RefreshView的高度加上默认值
                mTotalDragDistance = mSpinnerOffsetEnd = (int) (-mOriginalOffsetTop + mTotalDragDistance);
            }
        }
        mRefreshViewIndex = -1;
        // Get the index of the circleview.
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mRefreshView) {
                mRefreshViewIndex = index;
                break;
            }
        }
    }

    /**
     * Set the distance to trigger a sync in dips
     *
     * @param distance
     */
    public void setDistanceToTriggerSync(int distance) {
        mTotalDragDistance = distance;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.d(TAG, "onLayout: ");
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        final View child = mTarget;
        final int childLeft = getPaddingLeft();
        int childTop = getPaddingTop();
        if (mTranslateContent && mRefreshView != null) {
            childTop += mRefreshView.getMeasuredHeight() + mCurrentTargetOffsetTop;
        }
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + mTarget.getMeasuredWidth(), childTop + mTarget.getMeasuredHeight());

        if (mRefreshView == null) {
            return;
        }

        mRefreshView.layout(childLeft, mCurrentTargetOffsetTop,
                childLeft + mRefreshView.getMeasuredWidth(), mCurrentTargetOffsetTop + mRefreshView.getMeasuredHeight());

        // layout other child view
        final int count = getChildCount();
        final int parentLeft = getPaddingLeft();
        final int parentRight = r - l - getPaddingLeft();

        final int parentTop = getPaddingTop();
        final int parentBottom = b - t - getPaddingBottom();
        for (int i = 0; i < count; i++) {
            final View view = getChildAt(i);
            if (view.getVisibility() != GONE && view != mTarget && view != mRefreshView
                    &&mCurrentTargetOffsetTop==mOriginalOffsetTop) {
                //margin/gravity处理
                view.layout(parentLeft, -view.getMeasuredHeight(), parentLeft + view.getMeasuredWidth(), 0);
            }
        }

        if (mOnLayoutTranslateCallback != null) {

            mOnLayoutTranslateCallback.onLayoutTranslate(mCurrentTargetOffsetTop
            );
        }
    }

    public void setChildScrollUpCallback(OnChildScrollUpCallback childScrollUpCallback) {
        mChildScrollUpCallback = childScrollUpCallback;
    }

    public void setOnGestureChangeListener(OnGestureStateChangeListener gestureChangeListener) {
        mGestureChangeListener = gestureChangeListener;
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (mChildScrollUpCallback != null) {
            return mChildScrollUpCallback.canChildScrollUp(this, mTarget);
        }
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0 &&
                        (absListView.getFirstVisiblePosition() > 0 ||
                                absListView.getChildAt(0).getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mTarget, -1) || mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if (mRefreshView == null) {
            return false;
        }

        Log.d(TAG, "onInterceptTouchEvent: "+ev.toString());
        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            // Fail fast if we're not in a state where a swipe is possible
            mReturningToStart = false;
        }

        if (!isEnabled() || canChildScrollUp() || mReturningToStart || mRefreshing) {// 不拦截（禁止掉了 || 刷新中 ）
            return false;
        }

        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                final float initialDownY = getMotionEventY(ev, mActivePointerId);
                if (initialDownY == -1) {//如果没有有效点击，就往下传递
                    return false;
                }
                mInitialDownY = initialDownY;
                mIsInterceptedMoveEvent = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                final float y = getMotionEventY(ev, mActivePointerId);
                if (y == -1) {
                    return false;
                }
                mIsInterceptedMoveEvent = true;
                determineUserWhetherBeingDragged(y);
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingDragged;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if ((android.os.Build.VERSION.SDK_INT < 21 && mTarget instanceof AbsListView)
                || (mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    private void determineUserWhetherBeingDragged(float currentY) {
        final float yDiff = currentY - mInitialDownY;// 如果要支持反向可以用绝对值
        if (yDiff > mTouchSlop && !mIsBeingDragged) {
            mInitialMotionY = mInitialDownY + mTouchSlop;
            mIsBeingDragged = true;// 标志开始拖动
            // 这里应该暴露出接口，来定制化应当状态变化
            //mProgress.setAlpha(STARTING_PROGRESS_ALPHA);
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {// 获取最新的手指点
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (mRefreshView == null) {
            return false;
        }

        Log.d(TAG, "onTouchEvent: "+ev.toString());
        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart || canChildScrollUp()) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                if (mIsInterceptedMoveEvent)
                    mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);

                if (!mIsInterceptedMoveEvent) {
                    determineUserWhetherBeingDragged(y);
                }

                final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                if (mIsBeingDragged) {
                    if (overscrollTop > 0) {// 滑动了一定距离
                        if (mGestureChangeListener != null) {
                            mGestureChangeListener.onStartDrag(y);
                        }
                        Log.d(TAG, "move: "+overscrollTop+","+y+","+mInitialMotionY);
                        startDrag(overscrollTop);
                    } else {
                        return false;
                    }
                }
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (mActivePointerId == INVALID_POINTER) {
                    if (action == MotionEvent.ACTION_UP) {
                        Log.e(TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    }
                    return false;
                }
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                mIsBeingDragged = false;
                if (mGestureChangeListener != null) {
                    mGestureChangeListener.onFinishDrag(y);
                }
                endDrag(overscrollTop);
                mActivePointerId = INVALID_POINTER;
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param overscrollTop 可以理解为滑动的距离
     */
    private void startDrag(float overscrollTop){
        float originalDragPercent = overscrollTop / mTotalDragDistance;

        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
        float adjustedPercent = (float) Math.max(dragPercent - .4, 0) * 5 / 3;
        float extraOS = Math.abs(overscrollTop) - mTotalDragDistance;
        float slingshotDist = mUsingCustomStart ? mSpinnerOffsetEnd - mOriginalOffsetTop
                : mSpinnerOffsetEnd;
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2)
                / slingshotDist);
        float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                (tensionSlingshotPercent / 4), 2)) * 2f;
        float extraMove = (slingshotDist) * tensionPercent * 2;

        int targetY = mOriginalOffsetTop + (int) ((slingshotDist * dragPercent) + extraMove);
        if (mRefreshView != null) {
            // where 1.0f is a full circle
            if (mRefreshView.getVisibility() != View.VISIBLE) {
                mRefreshView.setVisibility(View.VISIBLE);
            }
            if (!mScale) {
                ViewCompat.setScaleX(mRefreshView, 1f);
                ViewCompat.setScaleY(mRefreshView, 1f);
            }

        }
        if (overscrollTop < mTotalDragDistance){
            // update progress
            if (mScale) {
                setAnimationProgress(Math.min(1f, overscrollTop / mTotalDragDistance));
            }
        }else {
            // 超出定义的刷新距离
        }

        float rotation = (-0.25f + .4f * adjustedPercent + tensionPercent * 2) * .5f;
        int offset = targetY - mCurrentTargetOffsetTop;//这两个值相等?
        Log.d(TAG, "startDrag: "+mCurrentTargetOffsetTop+","+offset);
        setTargetOffsetTopAndBottom(offset, true /* requires update */);

        if (mGestureChangeListener != null) {//临时拿overscrollTop去作为draggedDistance
            mGestureChangeListener.onDragging(overscrollTop, mTotalDragDistance);
        }
    }

    private void endDrag(float overscrollTop){
        Log.d(TAG, "endDrag: "+overscrollTop+","+mTotalDragDistance);
        if (overscrollTop > mTotalDragDistance){
            setRefreshing(true, true /* notify */);
        }else {
            // cancel refresh
            mRefreshing = false;
            Animation.AnimationListener listener = null;
            if (!mScale) {
                listener = new Animation.AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (!mScale) {
                            startScaleDownAnimation(null);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                };
            }
            animateOffsetToStartPosition(mCurrentTargetOffsetTop, listener);// 回程
        }
        if (mGestureChangeListener != null) {
            mGestureChangeListener.onFinishDrag(mCurrentTargetOffsetTop);
        }
    }

    private void animateOffsetToCorrectPosition(int from, Animation.AnimationListener listener) {
        mFrom = from;
        mAnimateToCorrectPosition.reset();
        mAnimateToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        mAnimateToCorrectPosition.setAnimationListener(listener);
        mRefreshView.clearAnimation();
        mRefreshView.startAnimation(mAnimateToCorrectPosition);
    }

    private void animateOffsetToStartPosition(int from, Animation.AnimationListener listener) {
        if (mScale) {
            // Scale the item back down
            startScaleDownReturnToStartAnimation(from, listener);
        } else {
            mFrom = from;
            mAnimateToStartPosition.reset();
            mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
            mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
            mAnimateToStartPosition.setAnimationListener(listener);
            mRefreshView.clearAnimation();
            mRefreshView.startAnimation(mAnimateToStartPosition);
        }
    }

    private void moveToStart(float interpolatedTime) {
        int targetTop = 0;
        targetTop = (mFrom + (int) ((mOriginalOffsetTop - mFrom) * interpolatedTime));
        int offset = targetTop - mRefreshView.getTop();
        setTargetOffsetTopAndBottom(offset, false /* requires update */);
    }

    private final Animation mAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            int endTarget = 0;
            if (!mUsingCustomStart) {
                endTarget = (int) (mSpinnerOffsetEnd - Math.abs(mOriginalOffsetTop));
            } else {
                endTarget = (int) mSpinnerOffsetEnd;
            }
            targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
            int offset = targetTop - mRefreshView.getTop();
            setTargetOffsetTopAndBottom(offset, false /* requires update */);
            //mProgress.setArrowScale(1 - interpolatedTime);
        }
    };

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);
        }
    };

    private void startScaleDownReturnToStartAnimation(int from,
                                                      Animation.AnimationListener listener) {
        mFrom = from;
        if (isAlphaUsedForScale()) {
            mStartingScale = mRefreshView.getAlpha();
        } else {
            mStartingScale = ViewCompat.getScaleX(mRefreshView);
        }
        mScaleDownToStartAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                float targetScale = (mStartingScale + (-mStartingScale  * interpolatedTime));
                setAnimationProgress(targetScale);
                moveToStart(interpolatedTime);
            }
        };
        mScaleDownToStartAnimation.setDuration(SCALE_DOWN_DURATION);
        if (listener != null) {
            mScaleDownToStartAnimation.setAnimationListener(listener);
        }
        mRefreshView.clearAnimation();
        mRefreshView.startAnimation(mScaleDownToStartAnimation);
    }

    private void setTargetOffsetTopAndBottom(int offset, boolean requiresUpdate) {
        if (mRefreshView == null) {
            ensureTarget();
        }
        mRefreshView.bringToFront();
        ViewCompat.offsetTopAndBottom(mRefreshView, offset);
        mCurrentTargetOffsetTop = mRefreshView.getTop();
        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
            invalidate();
        }
    }

    /**
     * Classes that wish to override {@link #canChildScrollUp()} method
     * behavior should implement this interface.
     */
    public interface OnChildScrollUpCallback {
        /**
         * Callback that will be called when {@link #canChildScrollUp()} method
         * is called to allow the implementer to override its behavior.
         *
         * @param parent SwipeRefreshLayout that this callback is overriding.
         * @param child The child view of SwipeRefreshLayout.
         *
         * @return Whether it is possible for the child view of parent layout to scroll up.
         */
        boolean canChildScrollUp(GestureRefreshLayout parent, @Nullable View child);
    }

    public interface OnGestureStateChangeListener {
        void onStartDrag(float startY);
        void onDragging(float draggedDistance, float releaseDistance);
        void onFinishDrag(float endY);
    }

    public interface OnLayoutTranslateCallback {
        void onLayoutTranslate(int movementTop);
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    public interface OnRefreshListener {
        public void onRefresh();
    }
}
