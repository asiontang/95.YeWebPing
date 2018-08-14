package cn.asiontang.webping;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * 支持N(无限)层级的树列表结构
 * //TODO:暂不支持Tree检索的功能，因为筛选完毕之后的Items会覆盖 带展开状态的当前mObjects 集合。
 *
 * <p>参考资料：</p>
 * <ul>
 * <li>
 * <a href="http://item.congci.com/item/android-wuxian-ji-shuzhuang-jiegou">Android无限级树状结构 -
 * Android
 * - 从此网</a>
 * </li>
 * </ul>
 *
 * @author AsionTang
 * @since 2016年6月1日 18:38:43
 */
@SuppressWarnings("unused")
public class NLevelTreeView extends ListView
{
    private final List<NLevelTreeNode> mExpandedNodeList = new ArrayList<>();
    private OnTreeNodeClickListener mOnTreeNodeClickListener;
    private OnTreeNodeCollapseListener mOnTreeNodeCollapseListener;
    private OnTreeNodeExpandListener mOnTreeNodeExpandListener;
    private NLevelTreeNodeAdapter mAdapter;

    public NLevelTreeView(final Context context)
    {
        super(context);
        this.init();
    }

    public NLevelTreeView(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
        this.init();
    }

    public NLevelTreeView(final Context context, final AttributeSet attrs, final int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NLevelTreeView(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.init();
    }

    /**
     * Collapse a group in the grouped list view
     */
    public void collapseGroup(final NLevelTreeNode node)
    {
        if (this.mOnTreeNodeCollapseListener != null)
            this.mOnTreeNodeCollapseListener.onTreeNodeBeforeCollapse(this, this.mExpandedNodeList, node);

        //把展开的节点，收缩起来
        this.mExpandedNodeList.remove(node);
        node.setIsExpanded(false);

        //当展开或者收缩节点后，“原始集合”和“当前展示集合”就不应该是一个集合了。所有需要拷贝一份，以便处理。
        //TODO:暂不支持Tree检索的功能，因为筛选完毕之后的Items会覆盖 带展开状态的当前mObjects 集合。
        List<NLevelTreeNode> tmpList = this.mAdapter.getItems();
        if (this.mAdapter.getOriginaItems() == tmpList)
            this.mAdapter.setItems(tmpList = new ArrayList<>(this.mAdapter.getOriginaItems()));

        final int position = tmpList.indexOf(node);
        final int nextPosition = position + 1;
        while (true)
        {
            //说明已经删除到最后一个节点了。
            if (nextPosition >= tmpList.size())
                break;

            final NLevelTreeNode tmpNode = tmpList.get(nextPosition);

            //只删除比它自己级别深的节点（如它的子、孙、重孙节点）
            if (tmpNode.getLevel() <= node.getLevel())
                break;

            tmpList.remove(tmpNode);

            //防止它的子孙节点也有可能是展开的，所有也要移除其状态。
            this.mExpandedNodeList.remove(tmpNode);
            tmpNode.setIsExpanded(false);
        }

        if (this.mOnTreeNodeCollapseListener != null)
            this.mOnTreeNodeCollapseListener.onTreeNodeAfterCollapse(this, this.mExpandedNodeList, node);

        this.mAdapter.refresh();
    }

    /**
     * Expand a group in the grouped list view
     */
    public void expandGroup(final NLevelTreeNode node)
    {
        if (this.mOnTreeNodeExpandListener != null)
            this.mOnTreeNodeExpandListener.onTreeNodeBeforeExpand(this, this.mExpandedNodeList, node);

        //把收缩的节点，展开起来
        this.mExpandedNodeList.add(node);
        node.setIsExpanded(true);

        //当展开或者收缩节点后，“原始集合”和“当前展示集合”就不应该是一个集合了。所有需要拷贝一份，以便处理。
        //TODO:暂不支持Tree检索的功能，因为筛选完毕之后的Items会覆盖 带展开状态的当前mObjects 集合。
        List<NLevelTreeNode> tmpList = this.mAdapter.getItems();
        if (this.mAdapter.getOriginaItems() == tmpList)
            this.mAdapter.setItems(tmpList = new ArrayList<>(this.mAdapter.getOriginaItems()));

        final int position = tmpList.indexOf(node);
        tmpList.addAll(position + 1, node.getChilds());

        if (this.mOnTreeNodeExpandListener != null)
            this.mOnTreeNodeExpandListener.onTreeNodeAfterExpand(this, this.mExpandedNodeList, node);

        this.mAdapter.refresh();
    }

    /**
     * 获取所有已展开的节点列表。
     */
    public List<NLevelTreeNode> getExpandedNodeList()
    {
        return this.mExpandedNodeList;
    }

    private void init()
    {
    }

    @Override
    public void onRestoreInstanceState(final Parcelable state)
    {
        final SavedState ss = (SavedState) state;

        //根据记录展开的节点的唯一ID重寻节点，然后展开
        onRestoreInstanceState(this.mAdapter.getOriginaItems(), ss.mExpandedNodeGuidList);

        super.onRestoreInstanceState(ss.getSuperState());

        //展开后，再滚动到历史位置。
        smoothScrollToPositionFromTop(ss.mFirstVisiblePosition, 0, 1);
    }

    /**
     * 递归 恢复所有历史已展开节点
     */
    private boolean onRestoreInstanceState(final List<NLevelTreeNode> list, final List<String> expandedNodeGuidList)
    {
        for (final NLevelTreeNode node : list)
        {
            if (expandedNodeGuidList.contains(node.getNodeGuid()))
            {
                expandGroup(node);

                //当前已展开节点已经等于历史展开数量时，说明已经恢复完毕了。就不用再递归循环了。
                if (this.mExpandedNodeList.size() == expandedNodeGuidList.size())
                    return true;
            }
            if (node.getChilds().size() > 0)
                if (onRestoreInstanceState(node.getChilds(), expandedNodeGuidList))
                    return true;
        }
        return false;
    }

    @Override
    public Parcelable onSaveInstanceState()
    {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState ss = new SavedState(superState);

        //只要记录展开的节点的唯一ID即可
        for (final NLevelTreeNode node : this.mExpandedNodeList)
            ss.mExpandedNodeGuidList.add(node.getNodeGuid());

        //记录滚动时第一行显示的索引，方便恢复滚动位置。
        ss.mFirstVisiblePosition = getFirstVisiblePosition();
        return ss;
    }

    @Override
    public boolean performItemClick(final View view, final int position, final long id)
    {
        final boolean handled = super.performItemClick(view, position, id);
        performItemClick(position);
        return handled;
    }

    private void performItemClick(final int position)
    {
        final NLevelTreeNode item = this.mAdapter.getItem(position);
        if (this.mExpandedNodeList.contains(item))
        {
            collapseGroup(item);
        }
        else
        {
            //没有子节点，则不允许展开
            if (item.getChilds().size() == 0)
            {
                //默认只支持叶子节点的Click事件
                if (this.mOnTreeNodeClickListener != null)
                    this.mOnTreeNodeClickListener.onTreeNodeClick(this, item);
                return;
            }
            expandGroup(item);
        }
    }

    public void setAdapter(final NLevelTreeNodeAdapter adapter)
    {
        this.mAdapter = adapter;
        super.setAdapter(adapter);
    }

    /**
     * 必须使用继承自 NLevelTreeNodeAdapter 的 适配器，否则会出现异常。
     */
    @Override
    public void setAdapter(final ListAdapter adapter)
    {
        if (adapter instanceof NLevelTreeNodeAdapter)
            this.setAdapter((NLevelTreeNodeAdapter) adapter);
        else
            throw new RuntimeException("For NLevelTreeView, use setAdapter(NLevelTreeNodeAdapter) instead of setAdapter(ListAdapter)");
    }

    /**
     * 不支持使用此回调方式
     */
    @Override
    @Deprecated
    public void setOnItemClickListener(final OnItemClickListener listener)
    {
        //实际的事件回调在setAdapter里设置，由 setOnTreeNodeClickListener 处理。
        //super.setOuterOnItemClickListener(listener);

        throw new RuntimeException("For NLevelTreeView, use setOnTreeNodeClickListener() instead of setOnItemClickListener()");
    }

    /**
     * 默认只支持叶子节点的Click事件
     */
    public void setOnTreeNodeClickListener(final OnTreeNodeClickListener listener)
    {
        this.mOnTreeNodeClickListener = listener;
    }

    public void setOnTreeNodeCollapseListener(final OnTreeNodeCollapseListener onTreeNodeCollapseListener)
    {
        this.mOnTreeNodeCollapseListener = onTreeNodeCollapseListener;
    }

    public void setOnTreeNodeExpandListener(final OnTreeNodeExpandListener onTreeNodeExpandListener)
    {
        this.mOnTreeNodeExpandListener = onTreeNodeExpandListener;
    }

    public interface OnTreeNodeCollapseListener
    {
        void onTreeNodeAfterCollapse(NLevelTreeView view, List<NLevelTreeNode> expandedNodeList, NLevelTreeNode node);

        void onTreeNodeBeforeCollapse(NLevelTreeView view, List<NLevelTreeNode> expandedNodeList, NLevelTreeNode node);
    }

    public interface OnTreeNodeExpandListener
    {
        void onTreeNodeAfterExpand(NLevelTreeView view, List<NLevelTreeNode> expandedNodeList, NLevelTreeNode node);

        void onTreeNodeBeforeExpand(NLevelTreeView view, List<NLevelTreeNode> expandedNodeList, NLevelTreeNode node);
    }

    /**
     * 默认只支持叶子节点的Click事件
     */
    public interface OnTreeNodeClickListener
    {
        /**
         * 默认只支持叶子节点的Click事件
         */
        void onTreeNodeClick(NLevelTreeView view, NLevelTreeNode node);
    }

    static class SavedState extends BaseSavedState
    {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>()
        {
            @Override
            public SavedState createFromParcel(final Parcel in)
            {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(final int size)
            {
                return new SavedState[size];
            }
        };
        public final List<String> mExpandedNodeGuidList = new ArrayList<>();
        public int mFirstVisiblePosition;

        /**
         * Constructor called from {@link AbsListView#onSaveInstanceState()}
         */
        SavedState(final Parcelable superState)
        {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(final Parcel in)
        {
            super(in);
            in.readStringList(this.mExpandedNodeGuidList);
            this.mFirstVisiblePosition = in.readInt();
        }

        @Override
        public void writeToParcel(final Parcel out, final int flags)
        {
            super.writeToParcel(out, flags);
            out.writeStringList(this.mExpandedNodeGuidList);
            out.writeInt(this.mFirstVisiblePosition);
        }
    }

    /**
     * @author AsionTang
     * @since 2016年6月1日 18:38:43
     */
    @SuppressWarnings("unused")
    public static class NLevelTreeNode
    {
        private final List<NLevelTreeNode> mChilds = new ArrayList<>();
        private CharSequence mId;
        private int mLevel = 0;
        private CharSequence mName;
        private NLevelTreeNode mParentNode;
        private boolean mIsExpanded;

        public NLevelTreeNode()
        {
        }

        public NLevelTreeNode(final NLevelTreeNode parentNode, final int level, final CharSequence id, final CharSequence name)
        {
            this.setParentNode(parentNode);
            this.setLevel(level);
            this.setID(id);
            this.setName(name);
        }

        public NLevelTreeNode(final int level, final CharSequence id, final CharSequence name)
        {
            this(null, level, id, name);
        }

        public NLevelTreeNode(final CharSequence id, final CharSequence name)
        {
            this(null, 0, id, name);
        }

        public NLevelTreeNode(final CharSequence name)
        {
            this(null, 0, name, name);
        }

        /**
         * @return 返回 祖父节点 + '分隔符' + 父节点 形式
         */
        public static String getNodeGuid(final NLevelTreeView.NLevelTreeNode node, final String delimiter)
        {
            if (node.getParentNode() == null)
                return node.getID().toString();
            return getNodeGuid(node.getParentNode(), delimiter) + delimiter + node.getID().toString();
        }

        /**
         * 为此Node添加一个子节点
         */
        public NLevelTreeNode addChild(final NLevelTreeNode child)
        {
            if (!this.mChilds.contains(child))
            {
                this.mChilds.add(child);
                child.setParentNode(this);
            }
            return this;
        }

        /**
         * 设置此Node所属的所有子节点
         */
        public NLevelTreeNode addChilds(final List<NLevelTreeNode> childs)
        {
            for (final NLevelTreeNode child : childs)
                this.addChild(child);
            return this;
        }

        /**
         * 获取此Node指定位置的子节点
         */
        public NLevelTreeNode getChild(final int index)
        {
            return this.mChilds.get(index);
        }

        /**
         * 获取此Node所属的所有子节点
         */
        public List<NLevelTreeNode> getChilds()
        {
            return this.mChilds;
        }

        /**
         * 获取当前Node 唯一标识符（当此Node被点击时，可供区分被点击的是谁）
         */
        public CharSequence getID()
        {
            return this.mId;
        }

        /**
         * 设置当前Node 唯一标识符（当此Node被点击时，可供区分被点击的是谁）
         */
        public NLevelTreeNode setID(final CharSequence id)
        {
            this.mId = id;
            return this;
        }

        /**
         * 获取当前Node所属哪个层级；一般从0级（根节点）开始递增。
         */
        public int getLevel()
        {
            return this.mLevel;
        }

        /**
         * 设置当前Node所在的层级；一般从0级（根节点）开始递增。
         */
        public NLevelTreeNode setLevel(final int level)
        {
            this.mLevel = level;

            //必须立即更新子节点的级别，否则就乱套了。
            for (final NLevelTreeNode child : this.mChilds)
                child.setLevel(level + 1);
            return this;
        }

        /**
         * 获取当前Node 名字
         */
        public CharSequence getName()
        {
            return this.mName;
        }

        /**
         * 设置当前Node 名字
         */
        public NLevelTreeNode setName(final CharSequence name)
        {
            this.mName = name;
            return this;
        }

        /**
         * 默认设置了以点为分隔符得到节点全局唯一ID（能在一棵树中唯一的标识当前节点是谁，方便拿来定位）。
         *
         * @see #getNodeGuid(NLevelTreeNode, String)
         */
        public String getNodeGuid()
        {
            return getNodeGuid(this, ".");
        }

        /**
         * 获取 此Note 的父节点
         */
        public NLevelTreeNode getParentNode()
        {
            return this.mParentNode;
        }

        /**
         * 设置 此Note 的父节点
         */
        public NLevelTreeNode setParentNode(final NLevelTreeNode parentNode)
        {
            this.mParentNode = parentNode;
            if (parentNode != null)
            {
                parentNode.addChild(this);
                this.setLevel(parentNode.getLevel() + 1);
            }
            return this;
        }

        public boolean isExpanded()
        {
            return this.mIsExpanded;
        }

        public void setIsExpanded(final boolean isExpanded)
        {
            this.mIsExpanded = isExpanded;
        }
    }

    /**
     * @author AsionTang
     * @since 2016年6月1日 18:38:43
     */
    @SuppressWarnings("unused")
    public abstract static class NLevelTreeNodeAdapter extends BaseAdapterEx3<NLevelTreeNode>
    {
        public NLevelTreeNodeAdapter(final Context context, final int itemLayoutResId)
        {
            super(context, itemLayoutResId);
        }

        public NLevelTreeNodeAdapter(final Context context, final int itemLayoutResId, final List<NLevelTreeNode> objects)
        {
            super(context, itemLayoutResId, objects);
        }

        @Override
        public void filter(final Object... constraintArgs)
        {
            //TODO:暂不支持Tree检索的功能，因为筛选完毕之后的Items会覆盖 带展开状态的当前mObjects 集合。
            throw new RuntimeException("//TODO:暂不支持Tree检索的功能，因为筛选完毕之后的Items会覆盖 带展开状态的当前mObjects 集合。");
            //super.filter(constraintArgs);
        }

        @Override
        protected List<NLevelTreeNode> performFiltering(final List<NLevelTreeNode> originalItems, final CharSequence constraint, final Object... args)
        {
            //TODO:暂不支持Tree检索的功能，因为筛选完毕之后的Items会覆盖 带展开状态的当前mObjects 集合。
            throw new RuntimeException("//TODO:暂不支持Tree检索的功能，因为筛选完毕之后的Items会覆盖 带展开状态的当前mObjects 集合。");
            //return super.performFiltering(originalItems, constraint, args);
        }
    }
}
