package com.cyberlink.actiondirector.page.preview;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

// CenterScroller to move view to be at recyclerView's horizontal center
public abstract class CenterScroller {

    public abstract RecyclerView getRecyclerView();

    public final void smoothScrollToCenter(RecyclerView.ViewHolder holder) {
        smoothScrollToCenter(holder.getAdapterPosition());
    }

    public final void smoothScrollToCenter(int position) {
        RecyclerView parent = getRecyclerView();
        if (parent == null) return;

        // Peek the parent's current item positions to check the position item is visible or not
        // head = first partly visible, tail = last partly visible
        int n = parent.getChildCount();
        ChildInfo head = new ChildInfo(parent, 0);
        ChildInfo tail = new ChildInfo(parent, n - 1);

        int anchor = (parent.getLeft() + parent.getRight()) / 2;

        // Determine the view is located at which position
        int viewAt;
        if (head.adapterPos <= position && position <= tail.adapterPos) {
            // Case : Target is (partly or completely) visible within recycler
            View target = parent.getChildAt(position - head.adapterPos);
            viewAt = (target.getLeft() + target.getRight()) / 2;
        } else {
            // Case : Target is at left outside or right outside of recycler
            boolean targetAtLeft = position < head.adapterPos;
            ChildInfo info = targetAtLeft ? head : tail;
            View ref = info.view;
            int distance = onPredictScrollOffset(targetAtLeft, info, position);

            viewAt = (ref.getLeft() + ref.getRight()) / 2 + distance;
        }

        int offset = viewAt - anchor;

        parent.smoothScrollBy(offset, 0); // It is used for horizontal layout manager
    }

    protected int onPredictScrollOffset(boolean targetAtLeft, @NonNull ChildInfo info, int position) {
        // Simplest case, all the item view has same width
        ScrollInfo s = new ScrollInfo(info.view);
        return s.widthAddMargins * (position - info.adapterPos);
    }

    private Rect getMargins(View v) {
        Rect margins = new Rect();
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        margins.left = lp.leftMargin;
        margins.top = lp.topMargin;
        margins.right = lp.rightMargin;
        margins.bottom = lp.bottomMargin;
        return margins;
    }

    public final class ScrollInfo {
        public final Rect margins;
        public final int widthAddMargins;

        public ScrollInfo(View v) {
            margins = getMargins(v);
            widthAddMargins = margins.left + v.getWidth() + margins.right;
        }

        @Override
        public String toString() {
            return "widthAddMargins = " + widthAddMargins + ", Margins = " + margins;
        }
    }

    public final class ChildInfo {
        public final View view;
        public final RecyclerView.ViewHolder viewHolder;
        public final int adapterPos;

        public ChildInfo(RecyclerView parent, int childIndex) {
            view = parent.getChildAt(childIndex);
            viewHolder = parent.getChildViewHolder(view);
            adapterPos = viewHolder.getAdapterPosition();
        }

        @Override
        public String toString() {
            return String.format("#%s, view = %s", adapterPos, view);
        }
    }
}
