package org.polaric.colorpicker;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;


@SuppressWarnings({"FieldCanBeLocal", "ConstantConditions"})
public class ColorChooserView extends FrameLayout implements View.OnClickListener, View.OnLongClickListener {

    private int mCircleSize;
    private ColorPickerCallback mCallback=null;
    private GridView mGrid;
    private int mPreselect;
    private boolean mSetPreselectionColor = false;
    private int mSubIndex=-1;
    private int mTopIndex=-1;
    private boolean mInSub=false;

    @NonNull
    private int[] mColorsTop;
    @Nullable
    private int[][] mColorsSub;
    
    public void setColorCallback(ColorPickerCallback callback) {
        mCallback = callback;
    }

    public ColorChooserView(Context context) {
        super(context);
        init(context);
    }

    public ColorChooserView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ColorChooserView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(21)
    public ColorChooserView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    @NonNull
    public void preselect(@ColorInt int preselect) {
        mPreselect = preselect;
        mSetPreselectionColor = true;
    }

    public void hideDoneButton() {
        findViewById(R.id.done).setVisibility(View.GONE);
    }

    private void invalidateSelf() {
        if (mGrid.getAdapter() == null) {
            mGrid.setAdapter(new ColorGridAdapter());
            mGrid.setSelector(ResourcesCompat.getDrawable(getResources(), R.drawable.md_transparent, null));
        } else ((BaseAdapter) mGrid.getAdapter()).notifyDataSetChanged();
    }

    private void init(Context context) {
        inflate(context,R.layout.colorchooser,this);
        findViewById(R.id.done).setOnClickListener(this);
        findViewById(R.id.back).setOnClickListener(this);
        generateColors();
        int preselectColor;
        if (mSetPreselectionColor) {
            preselectColor = mPreselect;
            if (preselectColor != 0) {
                for (int topIndex = 0; topIndex < mColorsTop.length; topIndex++) {
                    if (mColorsTop[topIndex] == preselectColor) {
                        topIndex(topIndex);
                        if (mColorsSub != null) {
                            findSubIndexForColor(topIndex, preselectColor);
                        } else {
                            subIndex(5);
                        }
                        break;
                    }
                    if (mColorsSub != null) {
                        for (int subIndex = 0; subIndex < mColorsSub[topIndex].length; subIndex++) {
                            if (mColorsSub[topIndex][subIndex] == preselectColor) {
                                topIndex(topIndex);
                                subIndex(subIndex);
                                break;
                            }
                        }
                    }
                }
            }
        }
        mCircleSize = getResources().getDimensionPixelSize(R.dimen.colorchooser_circlesize);
        mGrid = (GridView) findViewById(R.id.grid);
        invalidateSelf();
    }

    private boolean isInSub() {
        return mInSub;
    }

    private void isInSub(boolean value) {
        mInSub=value;
    }

    private int topIndex() {
        return mTopIndex;
    }

    private void topIndex(int value) {
        if (topIndex() != value && value > -1)
            findSubIndexForColor(value, mColorsTop[value]);
        mTopIndex=value;
    }

    private int subIndex() {
        if (mColorsSub == null) return -1;
        return mSubIndex;
    }

    private void subIndex(int value) {
        if (mColorsSub == null) return;
        mSubIndex=value;
    }

    private void generateColors() {
        mColorsTop = ColorPalette.PRIMARY_COLORS;
        mColorsSub = ColorPalette.PRIMARY_COLORS_SUB;
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() != null) {
            final String[] tag = ((String) v.getTag()).split(":");
            final int index = Integer.parseInt(tag[0]);
            final int color = Integer.parseInt(tag[1]);

            if (isInSub()) {
                subIndex(index);
            } else {
                topIndex(index);
                if (mColorsSub != null && index < mColorsSub.length) {
                    isInSub(true);
                }
            }
            if (mCallback!=null) {
                mCallback.onColorSelection(color);
            }
        }
        if (v.getId()==R.id.done) {
            if (mCallback!=null) {
                mCallback.onDone();
            }
        } else if (v.getId()==R.id.back) {
            isInSub(false);
        }
        invalidateSelf();
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getTag() != null) {
            final String[] tag = ((String) v.getTag()).split(":");
            final int color = Integer.parseInt(tag[1]);
            ((CircleView) v).showHint(color);
            return true;
        }
        return false;
    }

    public interface ColorPickerCallback {
        void onColorSelection(@ColorInt int selectedColor);
        void onDone();
    }

    private void findSubIndexForColor(int topIndex, int color) {
        if (mColorsSub == null || mColorsSub.length - 1 < topIndex)
            return;
        int[] subColors = mColorsSub[topIndex];
        for (int subIndex = 0; subIndex < subColors.length; subIndex++) {
            if (subColors[subIndex] == color) {
                subIndex(subIndex);
                break;
            }
        }
    }

    private class ColorGridAdapter extends BaseAdapter {

        public ColorGridAdapter() {

        }

        @Override
        public int getCount() {
            if (isInSub()) return mColorsSub[topIndex()].length;
            else return mColorsTop.length;
        }

        @Override
        public Object getItem(int position) {
            if (isInSub()) return mColorsSub[topIndex()][position];
            else return mColorsTop[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @SuppressWarnings("ResourceAsColor")
        @SuppressLint("DefaultLocale")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new CircleView(getContext());
                convertView.setLayoutParams(new GridView.LayoutParams(mCircleSize, mCircleSize));
            }
            CircleView child = (CircleView) convertView;
            final int color = isInSub() ? mColorsSub[topIndex()][position] : mColorsTop[position];
            child.setBackgroundColor(color);
            if (isInSub())
                child.setSelected(subIndex() == position);
            else child.setSelected(topIndex() == position);
            child.setTag(String.format("%d:%d", position, color));
            child.setOnClickListener(ColorChooserView.this);
            child.setOnLongClickListener(ColorChooserView.this);
            return convertView;
        }
    }

}
