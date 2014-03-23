package com.sababado.circularview;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.StateSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * TODO: document your custom view class.
 */
public class CircularView extends View {
    private static final String TAG = CircularView.class.getSimpleName();
    public static final int[] PRESSED_STATE_SET = new int[]{android.R.attr.state_pressed};
    public static final int[] DE_PRESSED_STATE_SET = new int[]{-android.R.attr.state_pressed};
    private String mText; //TODO add customization for the text (style, color, etc)
    private float mExampleDimension = 0; // TODO: use a default from R.dimen...

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;

    private Paint mCirclePaint;
    private static final float CIRCLE_WEIGHT_LONG_ORIENTATION = 0.8f;
    private static final float CIRCLE_TO_MARKER_PADDING = 20f;
    private float mMarkerRadius = 40;
    private float mMarkerStartingPoint;

    private CircularViewAdapter mAdapter;
    private final AdapterDataSetObserver mAdapterDataSetObserver = new AdapterDataSetObserver();
    private OnClickListener mOnCenterCircleClickListener;

    private ArrayList<Marker> mMarkerList;
    private CircularViewObject mCircle;
    private float mHighlightedDegree;
    private boolean mAnimateMarkerOnHighlight;
    private boolean mIsAnimating;
    ObjectAnimator mObjectAnimator;

    private int paddingLeft;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;

    private int mWidth;
    private int mHeight;

    public static final int TOP = 270;
    public static final int BOTTOM = 90;
    public static final int LEFT = 180;
    public static final int RIGHT = 0;

    public CircularView(Context context) {
        super(context);
        init(null, 0);
    }

    public CircularView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CircularView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.CircularView, defStyle, 0);

        mText = a.getString(
                R.styleable.CircularView_text);
        final int centerBackgroundColor = a.getColor(
                R.styleable.CircularView_centerBackgroundColor,
                CircularViewObject.NO_COLOR);
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mExampleDimension = a.getDimension(
                R.styleable.CircularView_exampleDimension,
                mExampleDimension);

        Drawable circleDrawable = null;
        if (a.hasValue(R.styleable.CircularView_centerDrawable)) {
            circleDrawable = a.getDrawable(
                    R.styleable.CircularView_centerDrawable);
            circleDrawable.setCallback(this);
        }

        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();

        mCirclePaint = new Paint();
        mCirclePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setStyle(Paint.Style.FILL);
        mCirclePaint.setColor(Color.RED);

        mHighlightedDegree = 0f;
        mAnimateMarkerOnHighlight = false;
        mIsAnimating = false;

        mCircle = new CircularViewObject(getContext(),CIRCLE_TO_MARKER_PADDING, centerBackgroundColor);
        mCircle.setSrc(circleDrawable);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // init circle dimens
        final int shortDimension = Math.min(
                mHeight = getMeasuredHeight(),
                mWidth = getMeasuredWidth());
        final float circleRadius = (shortDimension * CIRCLE_WEIGHT_LONG_ORIENTATION - mMarkerRadius * 4f - CIRCLE_TO_MARKER_PADDING * 2f) / 2f;
        final float circleCenter = shortDimension / 2f;
        mCircle.init(circleCenter, circleCenter, circleRadius, mAdapterDataSetObserver);
    }

    /**
     * Make sure a degree value is less than or equal to 360 and greater than or equal to 0.
     *
     * @param degree Degree to normalize
     * @return Return a positive degree value
     */
    private float normalizeDegree(float degree) {
        if (degree < 0f) {
            degree = 360f + degree;
        }
        return degree % 360f;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        paddingLeft = getPaddingLeft();
        paddingTop = getPaddingTop();
        paddingRight = getPaddingRight();
        paddingBottom = getPaddingBottom();
        setupMarkerList();
    }

    private void setupMarkerList() {
        if (mAdapter != null) {
            // init marker dimens
            final int markerCount = mAdapter.getCount();
            assert (markerCount >= 0);
            if (mMarkerList == null) {
                mMarkerList = new ArrayList(markerCount);
            }
            final int markerViewListSize = mMarkerList.size();
            final float degreeInterval = 360.0f / markerCount;
            final float radiusFromCenter = mCircle.getRadius() + CIRCLE_TO_MARKER_PADDING + mMarkerRadius;
            int position = 0;
            // loop clockwise
            for (float i = 0; i < 360f; i += degreeInterval) {
                final boolean positionHasExistingMarkerInList = position < markerViewListSize;
                final float actualDegree = normalizeDegree(i + 90f);
                final double rad = Math.toRadians(actualDegree);
                final float sectionMin = actualDegree - degreeInterval / 2f;

                // get the old marker view if it exists.
                final Marker oldMarker;
                if (positionHasExistingMarkerInList) {
                    oldMarker = mMarkerList.get(position);
                } else {
                    oldMarker = null;
                }

                // get the new marker view.
                final Marker newMarker = mAdapter.getMarker(position, oldMarker);
                assert (newMarker != null);

                // Initialize all other necessary values
                newMarker.init(
                        (float) (radiusFromCenter * Math.cos(rad)) + mCircle.getX(),
                        (float) (radiusFromCenter * Math.sin(rad)) + mCircle.getY(),
                        mMarkerRadius,
                        normalizeDegree(sectionMin),
                        normalizeDegree(sectionMin + degreeInterval) - 0.001f,
                        mAdapterDataSetObserver);
                // Make sure it's drawable has the callback set
                newMarker.setCallback(this);

                // Add to list
                if (positionHasExistingMarkerInList) {
                    mMarkerList.set(position, newMarker);
                } else {
                    mMarkerList.add(newMarker);
                }
                position++;
            }
            mMarkerList.trimToSize();
        }
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mExampleDimension);
//        mTextWidth = mTextPaint.measureText(mText);
        mTextWidth = mTextPaint.measureText(String.valueOf(mHighlightedDegree));

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
    }

    //TODO always draw the animating markers on top.
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int contentWidth = mWidth - paddingLeft - paddingRight;
        int contentHeight = mHeight - paddingTop - paddingBottom;

        mCirclePaint.setStyle(Paint.Style.FILL);
        mCirclePaint.setColor(Color.RED);
        // Draw CircularViewObject
        mCircle.draw(canvas);
        // Draw Markers
        if (mMarkerList != null && !mMarkerList.isEmpty()) {
            mCirclePaint.setStyle(Paint.Style.STROKE);
            mCirclePaint.setColor(Color.BLUE);
            for (final Marker marker : mMarkerList) {
                if ((mIsAnimating || mAnimateMarkerOnHighlight) && marker.hasInSection(mHighlightedDegree % 360)) {
                    if (!marker.isAnimating()) {
                        marker.animateBounce();
                    }
                    mCirclePaint.setStyle(Paint.Style.FILL);
                    marker.draw(canvas);
                    mCirclePaint.setStyle(Paint.Style.STROKE);
                } else {
                    marker.draw(canvas);
                }
            }
        }

        // Draw line
        if (mIsAnimating) {
            final float radiusFromCenter = mCircle.getRadius() + CIRCLE_TO_MARKER_PADDING + mMarkerRadius;
            final float x = (float) Math.cos(Math.toRadians(mHighlightedDegree)) * radiusFromCenter + mCircle.getX();
            final float y = (float) Math.sin(Math.toRadians(mHighlightedDegree)) * radiusFromCenter + mCircle.getY();
            canvas.drawLine(mCircle.getX(), mCircle.getY(), x, y, mCirclePaint);
        }


        // Draw the text.
        if (mIsAnimating) {
            canvas.drawText(String.valueOf(mHighlightedDegree),
                    mCircle.getX() - mTextWidth / 2f,
                    mCircle.getY() - mTextHeight / 2f,
//                paddingLeft + (contentWidth - mTextWidth) / 2,
//                paddingTop + (contentHeight + mTextHeight) / 2,
                    mTextPaint);
        }
    }

    /**
     * Set the adapter to use on this view.
     *
     * @param adapter Adapter to set.
     */
    public void setAdapter(final CircularViewAdapter adapter) {
        mAdapter = adapter;
        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(mAdapterDataSetObserver);
        }
    }

    /**
     * Get the adapter that has been set on this view.
     *
     * @return
     * @see #setAdapter(CircularViewAdapter)
     */
    public CircularViewAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Gets the text for this view.
     *
     * @return The text for this view.
     */
    public String getText() {
        return mText;
    }

    /**
     * Sets the view's text.
     *
     * @param text The view's text.
     */
    public void setText(String text) {
        mText = text;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example dimension attribute value.
     *
     * @return The example dimension attribute value.
     */
    public float getExampleDimension() {
        return mExampleDimension;
    }

    /**
     * Sets the view's example dimension attribute value. In the example view, this dimension
     * is the font size.
     *
     * @param exampleDimension The example dimension attribute value to use.
     */
    public void setExampleDimension(float exampleDimension) {
        mExampleDimension = exampleDimension;
        invalidateTextPaintAndMeasurements();
    }

    public float getHighlightedDegree() {
        return mHighlightedDegree;
    }

    /**
     * Set the degree that will trigger highlighting a marker.
     *
     * @param highlightedDegree Value in degrees.
     */
    public void setHighlightedDegree(final float highlightedDegree) {
        this.mHighlightedDegree = highlightedDegree;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    /**
     * Check if a marker should animate when it is highlighted. By default this is false and when it is
     * set to true the marker will constantly be animating.
     *
     * @return True if a marker should animate when it is highlighted, false if not.
     * @see #setHighlightedDegree(float)
     */
    public boolean isAnimateMarkerOnHighlight() {
        return mAnimateMarkerOnHighlight;
    }

    /**
     * If set to true the marker that is highlighted with {@link #setHighlightedDegree(float)} will
     * animate continuously. This is set to false by default.
     *
     * @param animateMarkerOnHighlight True to continuously animate, false to turn it off.
     */
    public void setAnimateMarkerOnHighlight(boolean animateMarkerOnHighlight) {
        this.mAnimateMarkerOnHighlight = animateMarkerOnHighlight;
    }

    /**
     * Get the center circle object.
     * @return The center circle object.
     */
    public CircularViewObject getCenterCircle() {
        return mCircle;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;
        final int action = event.getAction();
        final boolean isEventInCenterCircle =
                mCircle == null ? false : mCircle.isInCenterCircle(event.getX(), event.getY());
        if (action == MotionEvent.ACTION_DOWN) {
            // check if center
            if (isEnabled() && isEventInCenterCircle) {
                setCenterCircleState(PRESSED_STATE_SET);
                handled = true;
            }
        } else if (action == MotionEvent.ACTION_UP) {
            if (isEventInCenterCircle) {
                setCenterCircleState(StateSet.NOTHING);
                if (mOnCenterCircleClickListener != null) {
                    mOnCenterCircleClickListener.onClick(this);
                }
                handled = true;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (!isEventInCenterCircle) {
                setCenterCircleState(StateSet.NOTHING);
                handled = true;
            }
        }
        return handled || super.onTouchEvent(event);
    }

    void setCenterCircleState(final int[] stateSet) {
        if (mCircle != null) {
            mCircle.setState(stateSet);
        }
    }

    /**
     * Set the click listener that will receive a callback when the center circle is clicked.
     *
     * @param l Listener to receive a callback.
     */
    public void setOnCenterCircleClickListener(OnClickListener l) {
        mOnCenterCircleClickListener = l;
    }

    /**
     * Start animating the highlighted degree. This will cancel any current animations of this type.
     * Pass <code>true</code> to {@link #setAnimateMarkerOnHighlight(boolean)} in order to see individual
     * marker animations when the highlighted degree reaches each marker.
     *
     * @param startDegree Degree to start the animation at.
     * @param endDegree   Degree to end the animation at.
     * @param duration    Duration the animation should be.
     */
    public void animateHighlightedDegree(final float startDegree, final float endDegree, final long duration) {
        mHighlightedDegree = startDegree;
        if (mObjectAnimator != null) {
            mObjectAnimator.cancel();
        }
        mObjectAnimator = ObjectAnimator.ofFloat(CircularView.this, "highlightedDegree", startDegree, endDegree)
                .setDuration(duration);
        mObjectAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mIsAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mIsAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mIsAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        mObjectAnimator.start();
    }

    /**
     * Get the starting point for the markers.
     *
     * @return The starting point for the markers.
     */
    public float getMarkerStartingPoint() {
        return mMarkerStartingPoint;
    }

    /**
     * Set the starting point for the markers
     *
     * @param startingPoint Starting point for the markers
     */
    public void setMarkerStartingPoint(final float startingPoint) {
        mMarkerStartingPoint = startingPoint;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Remove all callback references from the center circle
        mCircle.setCallback(null);
        // Remove all callback references from the markers
        if (mMarkerList != null) {
            for (final Marker marker : mMarkerList) {
                marker.setCallback(null);
            }
        }
        // Unregister adapter observer
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mAdapterDataSetObserver);
        }
    }

    class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            invalidate();
        }

        /**
         * Does the same thing as {@link #onChanged()}.
         */
        @Override
        public void onInvalidated() {
            onChanged();
        }
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    }

}