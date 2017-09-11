package com.cyberlink.actiondirector.page.storyboard;

import android.graphics.Rect;
import android.os.Handler;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.cyberlink.actiondirector.R;
import com.cyberlink.actiondirector.page.preview.CenterScroller;
import com.cyberlink.actiondirector.page.preview.ViewSelectionTool;
import com.cyberlink.actiondirector.widget.SimpleItemTouchHelper;

import java.util.ArrayList;
import java.util.List;

public class EditorStoryboardItemAdapter
        extends RecyclerView.Adapter<EditorStoryboardItemAdapter.ViewHolder>
        implements SimpleItemTouchHelper.OrderableItemAdapter {

    public interface OnItemClickListener {
        void onItemClick(ViewHolder item, @ViewType.StoryboardViewType int viewType, int itemIndex);
    }

    public interface OnItemTouchListener {
        void onItemDragStart(int clipIndex);
        void onItemMove(int fromClip, int toClip);
        void onItemDragEnd(int startClipIndex, int endClipIndex);
    }

    public interface TransitionOwner {
        boolean hasTransition(int clipIndex);
    }

    public interface ItemEventListener extends OnItemClickListener, OnItemTouchListener, TransitionOwner {}

    private List<StoryboardItem> mItems;
    private EditorItemTouchHelper mHelper;
    private ItemEventListener mOnEvent;
    /** The item id currently storyboard selected */
    private int mSelectedAdapterPos = -1;

    private ViewSelectionTool<ViewHolder> mViewSelector = new ViewSelectionTool<>();
    private CenterScroller mCenterScroller;

    public EditorStoryboardItemAdapter(List<StoryboardItem> items
            , final RecyclerView parent
            , ItemEventListener eventListener) {
        mItems = items != null ? items : new ArrayList<StoryboardItem>();
        mOnEvent = eventListener;
        mCenterScroller = new EditorCenterScroller() {
            @Override
            public RecyclerView getRecyclerView() {
                return parent;
            }
        };
        mHelper = new EditorItemTouchHelper(this, ItemTouchHelper.LEFT | ItemTouchHelper. RIGHT, 0) {
            @Override
            public List getList() {
                return mItems;
            }

            @Override
            protected void onItemDragStart(int clipIndex) {
                if (mOnEvent != null) {
                    mOnEvent.onItemDragStart(clipIndex);
                }
            }

            @Override
            protected void onItemMove(int fromClip, int toClip) {
                if (mOnEvent != null) {
                    mOnEvent.onItemMove(fromClip, toClip);
                }
            }

            @Override
            protected void onItemDragEnd(int startIndex, int endIndex) {
                if (mOnEvent != null) {
                    mOnEvent.onItemDragEnd(startIndex, endIndex);
                }
            }
        };
    }

    @Override
    public @ViewType.StoryboardViewType int getItemViewType(int position) {
        if (position == getItemCount() - 1) {
            return ViewType.ADD;
        } else if (position % 2 == 0) {
            return ViewType.TRANSITION;
        } else {
            return ViewType.CLIP;
        }
    }

    @Override
    public EditorStoryboardItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, @ViewType.StoryboardViewType int viewType) {
        @LayoutRes int res;
        switch (viewType) {
            case ViewType.TRANSITION:
                res = R.layout.list_transition_item;
                break;
            case ViewType.CLIP:
                res = R.layout.list_storyboard_item;
                break;
            default:
            case ViewType.ADD:
                res = R.layout.list_storyboard_add_clip;
                break;
        }

        View v = LayoutInflater.from(parent.getContext()).inflate(res, parent, false);
        return new EditorStoryboardItemAdapter.ViewHolder(v, viewType);
    }

    @Override
    public void onBindViewHolder(EditorStoryboardItemAdapter.ViewHolder holder, int position) {
        switch (holder.type) {
            case ViewType.CLIP:
                StoryboardItem item = mItems.get(position / 2);
                holder.itemView.setContentDescription("[AID]Editor_Clip_"+ (position / 2));
                holder.setItem(item);

                Glide.with(holder.itemView.getContext())
                        .load(item.getPath())
                        .placeholder(R.drawable.thumbnail_video_default_n)
                        .centerCrop()
                        .animate(R.anim.fadein)
                        .into(holder.thumbnail);

                boolean selected = mSelectedAdapterPos == holder.getAdapterPosition();

                if (selected) {
                    mViewSelector.setItemSelected(holder, true);
                } else {
                    mViewSelector.updateSelectionState(holder, false);
                }

                if (item.exists()) {
                    holder.mask.setVisibility(View.GONE);
                } else {
                    holder.mask.setVisibility(View.VISIBLE);
                }

                break;
            case ViewType.TRANSITION:
                boolean hasTrx = mOnEvent.hasTransition(position / 2);
                holder.itemView.setContentDescription("[AID]Editor_Tx_" + (position / 2));
                holder.enableTransition = hasTrx;
                holder.itemView.setSelected(hasTrx);
                break;
            default:
                break;
        }
    }

    public void notifyClipChanged(int pos) {
        notifyItemRangeChanged(2 * pos, 3);
    }

    public void selectItem(int indexOfItems) {
        int indexOfAdapter = 2 * indexOfItems + 1;
        mSelectedAdapterPos = indexOfAdapter;
        notifyItemChanged(indexOfItems);
        smoothScrollToCenter.run();
        notifyItemChanged(indexOfAdapter);
    }

    private static final Handler sNavigateHandler = new Handler();
    private Runnable smoothScrollToCenter = new Runnable() {
        private final int LIMIT = 50;
        private int tried = 0;
        @Override
        public void run() {
            boolean canScrollNow = mCenterScroller.getRecyclerView().getChildCount() >= 2;
            if (canScrollNow) {
                tried = 0;
                mCenterScroller.smoothScrollToCenter(mSelectedAdapterPos);
            } else if (tried >= LIMIT) {
                // failed...
                sNavigateHandler.removeCallbacks(this);
            } else {
                tried++;
                sNavigateHandler.postDelayed(this, 50);
            }
        }
    };

    @Override
    public int getItemCount() {
        int n = mItems.size();
        if (n == 0) {
            return 1;
        } else {
            return 2*n + 2;
        }
    }

    @Override
    public ItemTouchHelper getItemTouchHelper() {
        return mHelper.getHelper();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private StoryboardItem item;
        private final ImageView thumbnail;
        private final View mask;
        private final int type;
        private boolean enableTransition;

        ViewHolder(View v, int viewType) {
            super(v);
            type = viewType;
            thumbnail = (ImageView) v.findViewById(R.id.storyboardItemThumbnail);
            mask = v.findViewById(R.id.storyboardItemMask);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int index = getAdapterPosition() / 2;

                    ViewHolder thiz = ViewHolder.this;
                    if (type == ViewType.CLIP) {
                        mViewSelector.setItemSelected(thiz, true);
                        mSelectedAdapterPos = thiz.getAdapterPosition();
                    } else if (type == ViewType.TRANSITION) {
                        // No handle selection here, TransitionArranger handle for us
                        // Handled by storyItemView().setSelected() in TransitionArranger#setTransition()
                    }

                    if (mOnEvent != null) {
                        mOnEvent.onItemClick(ViewHolder.this, type, index);
                    }
                }
            });
        }

        private void setItem(StoryboardItem m) {
            item = m;
        }
    }

    private abstract class EditorCenterScroller extends CenterScroller {
        private final String TAG = "ECS"; // ECS = EditorCenterScroller
        private final boolean DEBUG_PSO = false; // PSO = Predict Scroll Offset

        @Override
        protected int onPredictScrollOffset(boolean targetAtLeft, @NonNull CenterScroller.ChildInfo info, int position) {
            int n = getRecyclerView().getChildCount();
            LogF("atLeft = %s, pos = %s, info = %s", targetAtLeft, position, info);

            boolean infoIsClip = ViewType.CLIP == info.viewHolder.getItemViewType();
            // The visible item follow info
            // If target is at left, info's adapter index is 0, so follow is index 1
            // If target is ar right, info's adapter index is n-1, so follow is index n-2
            ChildInfo follow = new ChildInfo(getRecyclerView(), targetAtLeft ? 1 : n-2);

            ChildInfo clip = infoIsClip ? info : follow;
            ChildInfo tran = infoIsClip ? follow : info;
            LogF("infoIsClip = %s", infoIsClip);
            LogF("clip = %s", clip);
            LogF("tran = %s", tran);

            // Uses two kinds of view to predict scroll offset
            final ScrollInfo clipSI = new ScrollInfo(clip.view);
            final ScrollInfo tranSI = new ScrollInfo(tran.view);
            final int clipW = clip.view.getWidth();
            final int tranW = tran.view.getWidth();
            final Rect clipMg = clipSI.margins;
            final Rect tranMg = tranSI.margins;
            int clipN = (position - clip.adapterPos) / 2; // Count of full units of clips
            int tranN = (position - tran.adapterPos) / 2; // Count of full units of transitions
            LogF("Mgs clip = %s, tran = %s", clipMg, tranMg);

            LogF("old N clip = %s, tran = %s", clipN, tranN);

            // Adding shift uf the referred view is transition
            int shiftToClipCenter = 0;
            if (!infoIsClip) { // Since the scrolling center is transition's center, so we need shift to clip's center
                final int toHalf = clipW + tranW;
                int normal;
                if (targetAtLeft) {
                    tranN = clipN; // before change, => clipN = transN - 1, transN <= 0. (E.g. clipN = -9, transN = -8)
                    normal = tranMg.left + clipMg.left + clipMg.right;
                } else {
                    clipN = tranN; // before change, => clipN = transN + 1, transN >= 0. (E.g. clipN = 11, transN = 10)
                    normal = tranMg.right + clipMg.left;
                }
                shiftToClipCenter = normal + toHalf / 2;
                LogF("new N clip = %s, tran = %s", clipN, tranN, shiftToClipCenter);
            }
            LogF("W clip = %s, tran = %s", clipW, tranW);
            LogF("N clip = %s, tran = %s", clipN, tranN);

            final int clipOffset = clipSI.widthAddMargins * clipN;
            final int tranOffset = tranSI.widthAddMargins * tranN;
            LogF("scroll = %s + %s + %s", clipOffset, tranOffset, shiftToClipCenter);

            return clipOffset + tranOffset + shiftToClipCenter;
        }

        private void LogF(String format, Object... args) {
            if (DEBUG_PSO) Log.i(TAG, String.format(format, args));
        }
    }

    public void moveStoryboardItem(int fromIndex, int toIndex) {
        StoryboardItem insertItem = mItems.remove(fromIndex);
        mItems.add(toIndex, insertItem);
    }
}
