package com.cyberlink.actiondirector.widget;

import android.content.Context;
import android.os.Vibrator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import java.util.Collections;
import java.util.List;

import static android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_DRAG;

/**
 * Simple class to wrap {@link ItemTouchHelper} for {@link android.support.v7.widget.LinearLayoutManager} as handy one.
 * Not proper animation for {@link android.support.v7.widget.GridLayoutManager}.
 * <p>
 * 1. When <b>Move</b> comes by receiving
 * {@link android.support.v7.widget.helper.ItemTouchHelper.SimpleCallback#onMove(RecyclerView, RecyclerView.ViewHolder, RecyclerView.ViewHolder)}
 * <br>, we manipulate the list by
 * {@link Collections#swap(List, int, int)}
 * and notify item moved by {@link android.support.v7.widget.RecyclerView.Adapter#notifyItemMoved(int, int)}
 * </p>
 * <p>
 * 2. When <b>Swipe</b> comes by receiving
 * {@link android.support.v7.widget.helper.ItemTouchHelper.SimpleCallback#onSwiped(RecyclerView.ViewHolder, int)}
 * <br>, we manipulate the list by
 * {@link List#remove(int)}
 * and notify item removed by
 * {@link android.support.v7.widget.RecyclerView.Adapter#notifyItemRemoved(int)}
 * </p>
 *
 * See <a href=
 * "http://stackoverflow.com/a/36196160"
 * >stack overflow</a>'s answer. Sample code is in the answer's <a href=
 * "https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf"
 * >Drag and Swipe with RecyclerView</a>
 *
 */
public abstract class SimpleItemTouchHelper {
    /** Handy toString() for the ACTION_STATES for ItemTouchHelper
     * just use as ACTION_STATES[state].
     *
     * @see ItemTouchHelper#ACTION_STATE_IDLE
     * @see ItemTouchHelper#ACTION_STATE_SWIPE
     * @see ItemTouchHelper#ACTION_STATE_DRAG
     * */
    protected static final String[] ACTION_STATES = {"IDLE", "SWIPE", "DRAG"};

    private ItemTouchHelper mHelper;
    private ItemTouchHelper.SimpleCallback mCallback;
    protected RecyclerView.Adapter mAdapter;

    public interface OrderableItemAdapter {
        ItemTouchHelper getItemTouchHelper();
    }

    /**
     * Returns <b>MODIFIABLE</b> list of the item currently we are display to user.
     */
    public abstract List getList();

    public ItemTouchHelper getHelper() {
        return mHelper;
    }

    public ItemTouchHelper.SimpleCallback getCallback() {
        return mCallback;
    }

    public SimpleItemTouchHelper(RecyclerView.Adapter adapter, int dragDirs, int swipeDirs) {
        mAdapter = adapter;
        mCallback = initCallback(dragDirs, swipeDirs);
        mHelper = new ItemTouchHelper(mCallback);
    }

    protected ItemTouchHelper.SimpleCallback initCallback(int dragDirs, int swipeDirs) {
        return new HelperCallback(dragDirs, swipeDirs);
    }

    protected class HelperCallback extends ItemTouchHelper.SimpleCallback {

        public HelperCallback(int dragDirs, int swipeDirs) {
            super(dragDirs, swipeDirs);
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            boolean isLast = viewHolder.getAdapterPosition() >= getList().size();

            if (isLast) {
                return 0; // Have no movement flags, so we cannot move this item
            } else {
                return super_getMovementFlags(recyclerView, viewHolder);
            }
        }

        /** Used for children who overwrites {@link #getMovementFlags(RecyclerView, RecyclerView.ViewHolder)}
         * @see android.support.v7.widget.helper.ItemTouchHelper.SimpleCallback#getMovementFlags(RecyclerView, RecyclerView.ViewHolder)
         * @return android.support.v7.widget.helper.ItemTouchHelper.SimpleCallback#getMovementFlags(RecyclerView, RecyclerView.ViewHolder)
         */
        protected final int super_getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return super.getMovementFlags(recyclerView, viewHolder);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();
            int last = getList().size();
            boolean reachLast = fromPosition >= last || toPosition >= last;
            if (reachLast) {
                return false; // since we no need to move
            }

            final int next = fromPosition < toPosition ? 1 : -1;
            for (int i = fromPosition; i != toPosition; i += next) {
                Collections.swap(getList(), i, i + next);
            }

            mAdapter.notifyItemMoved(fromPosition, toPosition);

            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            if (position < 0) return; // When keep clicking on delete, it keep triggering onSwiped() and it may have index = -1 = {@link RecyclerView#NO_POSITION} of previous deleted one

            getList().remove(position);
            mAdapter.notifyItemRemoved(position);
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            // TODO : use android.support.v4.widget.ViewDragHelper ?
            switch (actionState) {
                case ACTION_STATE_DRAG:
                    viewHolder.itemView.setAlpha(0.6f);
                    // Vibrate 50 ms for haptic feedback
                    Vibrator v = (Vibrator) viewHolder.itemView.getContext().getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(50);
                    break;
                default:
            }
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.setAlpha(1.0f);
        }
    }
}
