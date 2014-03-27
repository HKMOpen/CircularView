package com.sababado.circularview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO Document
 */
public class CircularViewObject {
    private static final AtomicInteger sAtomicIdCounter = new AtomicInteger(0);
    protected int id;
    protected float radius;
    protected float radiusPadding;
    protected float x;
    protected float y;
    private Paint paint;
    protected Context context;
    protected Drawable drawable;
    protected CircularView.AdapterDataSetObserver mAdapterDataSetObserver;
    private boolean fitToCircle;

    /**
     * Use this value to make sure that no color shows.
     */
    public static final int NO_COLOR = -1;

    /**
     * Create a new CircularViewObject with the current context.
     *
     * @param context Current context.
     */
    public CircularViewObject(final Context context) {
        this.context = context;
        id = sAtomicIdCounter.getAndAdd(1);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(NO_COLOR);
        setRadiusPadding(5f);
        fitToCircle = false;
    }

    CircularViewObject(final Context context, final float radiusPadding, final int centerBackgroundColor) {
        this(context);
        setRadiusPadding(radiusPadding);
        paint.setColor(centerBackgroundColor);
    }

    protected void init(final float x, final float y, final float radius, final CircularView.AdapterDataSetObserver adapterDataSetObserver) {
        this.x = x;
        this.y = y;
        setRadius(radius);
        this.mAdapterDataSetObserver = adapterDataSetObserver;
    }

    protected void draw(final Canvas canvas) {
        if (paint.getColor() != NO_COLOR) {
            canvas.drawCircle(x, y, radius, paint);
        }
        if (drawable != null) {
            float leftOffset = -radius + radiusPadding;
            float topOffset = -radius + radiusPadding;
            float rightOffset = radius - radiusPadding;
            float bottomOffset = radius - radiusPadding;
            if (fitToCircle) {
                final double extraOffset = distanceFromCenter(x+leftOffset, y+topOffset) - radius;
                leftOffset += extraOffset;
                topOffset += extraOffset;
                rightOffset -= extraOffset;
                bottomOffset -= extraOffset;
            }
            drawable.setBounds(
                    (int) (x + leftOffset),
                    (int) (y + topOffset),
                    (int) (x + rightOffset),
                    (int) (y + bottomOffset)
            );
            drawable.draw(canvas);
        }
    }

    /**
     * Check to see if a point is in the center circle or not.
     * This simply uses the distance formula to get the distance from the center of the circle
     * to the given point and then compares that to the circle's radius.
     *
     * @param x X coordinate.
     * @param y Y coordinate.
     * @return True if the point is within the circle, false if not.
     */
    public boolean isInCenterCircle(final float x, final float y) {
        final double c = distanceFromCenter(x, y);
        return c <= radius;
    }

    /**
     * Get the distance from the given point to the center of this object.
     *
     * @param x X coordinate.
     * @param y Y coordinate.
     * @return Distance from the given point to the center of this object.
     */
    public double distanceFromCenter(final float x, final float y) {
        return Math.sqrt(Math.pow(x - this.x, 2) + Math.pow(y - this.y, 2));
    }

    /**
     * Get this Object's unique ID. The ID is generated atomically on initialization.
     *
     * @return Atomically generated ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Set the object's visual as a bitmap.
     *
     * @param bitmap Bitmap to display.
     */
    public void setSrc(Bitmap bitmap) {
        setSrc(new BitmapDrawable(context.getResources(), bitmap));
    }

    /**
     * Set the object's visual by using a resource id.
     *
     * @param resId Resource id of the drawable to display.
     */
    public void setSrc(final int resId) {
//        setSrc(BitmapFactory.decodeResource(context.getResources(), resId));
        setSrc(context.getResources().getDrawable(resId));
    }

    /**
     * Set the object's visual as a drawable.
     *
     * @param drawable Drawable to display.
     */
    public void setSrc(final Drawable drawable) {
        this.drawable = drawable;
        if (mAdapterDataSetObserver != null) {
            mAdapterDataSetObserver.onInvalidated();
        }
    }

    void setCallback(final View view) {
        if (drawable != null) {
            drawable.setCallback(view);
        }
    }

    /**
     * Specify a set of states for the drawable. These are use-case specific, so see the relevant documentation. As an example, the background for widgets like Button understand the following states: [state_focused, state_pressed].<br/>
     * If the new state you are supplying causes the appearance of the Drawable to change, then it is responsible for calling invalidateSelf() in order to have itself redrawn, and true will be returned from this function.<br/>
     * Note: The Drawable holds a reference on to stateSet until a new state array is given to it, so you must not modify this array during that time.
     *
     * @param stateSet The new set of states to be displayed.
     * @return Returns true if this change in state has caused the appearance of the Drawable to change (hence requiring an invalidate), otherwise returns false.
     */
    public boolean setState(final int[] stateSet) {
        boolean appearanceChange = false;
        if (drawable != null) {
            appearanceChange = drawable.setState(stateSet);
            if (appearanceChange) {
                drawable.invalidateSelf();
            }
        }
        return appearanceChange;
    }

    /**
     * Get the object's drawable.
     *
     * @return The drawable.
     */
    public Drawable getDrawable() {
        return drawable;
    }

    /**
     * Get the y position.
     *
     * @return The y position.
     */
    public float getY() {
        return y;
    }

    /**
     * Set the y position.
     *
     * @param y The new y position.
     */
    public void setY(float y) {
        this.y = y;
        if (mAdapterDataSetObserver != null) {
            mAdapterDataSetObserver.onInvalidated();
        }
    }

    /**
     * Get the x position.
     *
     * @return The x position.
     */
    public float getX() {
        return x;
    }

    /**
     * Set the x position.
     *
     * @param x The new x position.
     */
    public void setX(float x) {
        this.x = x;
        if (mAdapterDataSetObserver != null) {
            mAdapterDataSetObserver.onInvalidated();
        }
    }

    /**
     * Get the radius of the object.
     *
     * @return The radius.
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Set the radius of the object.
     *
     * @param radius The new radius.
     */
    public void setRadius(float radius) {
        this.radius = radius;
        if (mAdapterDataSetObserver != null) {
            mAdapterDataSetObserver.onInvalidated();
        }
    }

    /**
     * Get the object's visual padding from the radius.
     *
     * @return The object's visual padding from the radius.
     */
    public float getRadiusPadding() {
        return radiusPadding;
    }

    /**
     * Set the object's visual padding from the radius.
     *
     * @param radiusPadding The object's visual padding from the radius.
     */
    public void setRadiusPadding(float radiusPadding) {
        this.radiusPadding = radiusPadding;
        if (mAdapterDataSetObserver != null) {
            mAdapterDataSetObserver.onInvalidated();
        }
    }

    /**
     * Gets the center background color attribute value.
     *
     * @return The center background color attribute value.
     */
    public int getCenterBackgroundColor() {
        return paint.getColor();
    }

    /**
     * Sets the view's center background color attribute value.
     *
     * @param centerBackgroundColor The color attribute value to use.
     */
    public void setCenterBackgroundColor(int centerBackgroundColor) {
        paint.setColor(centerBackgroundColor);
        if (mAdapterDataSetObserver != null) {
            mAdapterDataSetObserver.onInvalidated();
        }
    }

    CircularView.AdapterDataSetObserver getAdapterDataSetObserver() {
        return mAdapterDataSetObserver;
    }

    void setAdapterDataSetObserver(CircularView.AdapterDataSetObserver adapterDataSetObserver) {
        this.mAdapterDataSetObserver = adapterDataSetObserver;
    }

    /**
     * True if the object's drawable should fit inside the center circle. False if it will not.
     * @return True if the object's drawable should fit inside the center circle. False if it will not.
     */
    public boolean isFitToCircle() {
        return fitToCircle;
    }

    /**
     * Set to true if this object's drawable should fit inside of the center circle and false if not.
     * @param fitToCircle Flag to determine if this drawable should fit inside the center circle.
     */
    public void setFitToCircle(boolean fitToCircle) {
        this.fitToCircle = fitToCircle;
        if(mAdapterDataSetObserver != null) {
            mAdapterDataSetObserver.onInvalidated();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CircularViewObject that = (CircularViewObject) o;

        if (id != that.id) return false;
        if (Float.compare(that.radius, radius) != 0) return false;
        if (Float.compare(that.radiusPadding, radiusPadding) != 0) return false;
        if (Float.compare(that.x, x) != 0) return false;
        if (Float.compare(that.y, y) != 0) return false;
        if (context != null ? !context.equals(that.context) : that.context != null) return false;
        if (drawable != null ? !drawable.equals(that.drawable) : that.drawable != null)
            return false;
        if (mAdapterDataSetObserver != null ? !mAdapterDataSetObserver.equals(that.mAdapterDataSetObserver) : that.mAdapterDataSetObserver != null)
            return false;
        if (paint != null ? !paint.equals(that.paint) : that.paint != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (radius != +0.0f ? Float.floatToIntBits(radius) : 0);
        result = 31 * result + (radiusPadding != +0.0f ? Float.floatToIntBits(radiusPadding) : 0);
        result = 31 * result + (x != +0.0f ? Float.floatToIntBits(x) : 0);
        result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
        result = 31 * result + (paint != null ? paint.hashCode() : 0);
        result = 31 * result + (context != null ? context.hashCode() : 0);
        result = 31 * result + (drawable != null ? drawable.hashCode() : 0);
        result = 31 * result + (mAdapterDataSetObserver != null ? mAdapterDataSetObserver.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CircularViewObject{" +
                "id=" + id +
                ", radius=" + radius +
                ", radiusPadding=" + radiusPadding +
                ", x=" + x +
                ", y=" + y +
                ", paint=" + paint +
                ", context=" + context +
                ", drawable=" + drawable +
                ", mAdapterDataSetObserver=" + mAdapterDataSetObserver +
                '}';
    }
}