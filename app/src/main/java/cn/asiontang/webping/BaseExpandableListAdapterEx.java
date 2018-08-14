package cn.asiontang.webping;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 专门用于给可展开列表用的适配器
 *
 * @param <T_Group> 分组对象实体【当需要Group有序时，使用LinkedHashMap传递即可】
 * @param <T_Child> 分组里面的子对象实体
 * @author Asion Tang
 * @since 2014年11月4日 10:43:59
 */
public abstract class BaseExpandableListAdapterEx<T_Group, T_Child> extends BaseExpandableListAdapter
{
    private final int mGroupLayoutResId;
    private final int mChildLayoutResId;
    private final LayoutInflater mInflater;
    private final DataSetObservable mDataSetObservable = new DataSetObservable();
    protected Context mContext;
    @SuppressWarnings("unused")
    private Map<T_Group, List<T_Child>> mOriginalObjects;
    private Map<T_Group, List<T_Child>> mObjects;
    private List<T_Group> mGroupObjects;

    public BaseExpandableListAdapterEx(final Context context, final int groupLayoutResId, final int childLayoutResId)
    {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mGroupLayoutResId = groupLayoutResId;
        this.mChildLayoutResId = childLayoutResId;
    }

    /**
     * @param items 【当需要Group有序时，使用LinkedHashMap传递即可】
     */
    public BaseExpandableListAdapterEx(final Context context, final int groupLayoutResId, final int childLayoutResId//
            , final Map<T_Group, List<T_Child>> items)
    {
        this(context, groupLayoutResId, childLayoutResId);
        this.setOriginalItems(items);
    }

    @Override
    public T_Child getChild(final int groupPosition, final int childPosition)
    {
        if (this.mObjects == null)
            return null;
        return this.mObjects.get(this.getGroup(groupPosition)).get(childPosition);
    }

    /**
     * UNDONE:默认未实现，返回-1
     */
    @Override
    public long getChildId(final int groupPosition, final int childPosition)
    {
        return -1;
    }

    /**
     * 通过传递 指定组的索引 + 指定的Cihld对象 来获得Child所在的索引
     */
    public int getChildPosition(final int groupPosition, final T_Child object)
    {
        if (this.mObjects == null)
            return -1;
        return this.mObjects.get(this.getGroup(groupPosition)).indexOf(object);
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild, View convertView, final ViewGroup parent)
    {
        if (convertView == null)
            //==========================================================================
            //不能使用
            //	inflate(R.layout.?, parent);
            //否则会报错
            //	java.lang.UnsupportedOperationException: addView(View, LayoutParams)
            //	is not supported in AdapterView
            //可使用
            //	inflate(R.layout.?, null);（官方不推荐）
            //	inflate(R.layout.?, parent, false)
            //参考资料：
            //	Layout Inflation as Intended - by Dave Smith of Double Encore
            //	http://www.doubleencore.com/2013/05/layout-inflation-as-intended/
            //==========================================================================
            convertView = this.mInflater.inflate(this.mChildLayoutResId, parent, false);
        this.getChildView(groupPosition, childPosition, isLastChild, convertView, parent, this.getChild(groupPosition, childPosition));
        return convertView;
    }

    public abstract void getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent, T_Child item);

    @Override
    public int getChildrenCount(final int groupPosition)
    {
        if (groupPosition == -1 || this.mObjects == null)
            return 0;
        List<T_Child> tmp = this.mObjects.get(this.getGroup(groupPosition));
        if (tmp == null)
            return 0;
        return tmp.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T_Group getGroup(final int groupPosition)
    {
        if (this.mObjects == null)
            return null;
        //当组 的数量和缓存的不一致时，也需要重新缓存一次。
        if (this.mGroupObjects == null || this.mGroupObjects.size() != this.mObjects.size())
            this.mGroupObjects = Arrays.asList((T_Group[]) this.mObjects.keySet().toArray());
        return this.mGroupObjects.get(groupPosition);
    }

    @Override
    public int getGroupCount()
    {
        if (this.mObjects == null)
            return 0;
        return this.mObjects.keySet().size();
    }

    /**
     * UNDONE:默认未实现，返回-1
     */
    @Override
    public long getGroupId(final int groupPosition)
    {
        return -1;
    }

    /**
     * 通过传递指定的 T_Group 来获取所在的 索引
     */
    public int getGroupPosition(final T_Group group)
    {
        if (this.getGroup(0) == null)
            return -1;
        if (this.mGroupObjects == null)
            return -1;
        return this.mGroupObjects.indexOf(group);
    }

    @Override
    public View getGroupView(final int groupPosition, final boolean isExpanded, View convertView, final ViewGroup parent)
    {
        if (convertView == null)
            //==========================================================================
            //不能使用
            //	inflate(R.layout.?, parent);
            //否则会报错
            //	java.lang.UnsupportedOperationException: addView(View, LayoutParams)
            //	is not supported in AdapterView
            //可使用
            //	inflate(R.layout.?, null);（官方不推荐）
            //	inflate(R.layout.?, parent, false)
            //参考资料：
            //	Layout Inflation as Intended - by Dave Smith of Double Encore
            //	http://www.doubleencore.com/2013/05/layout-inflation-as-intended/
            //==========================================================================
            convertView = this.mInflater.inflate(this.mGroupLayoutResId, parent, false);
        this.getGroupView(groupPosition, isExpanded, convertView, parent, this.getGroup(groupPosition));
        return convertView;
    }

    public abstract void getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent, T_Group item);

    /**
     * UNDONE:没有自定义实现， 默认返回false
     */
    @Override
    public boolean hasStableIds()
    {
        return true;
    }

    @Override
    public boolean isChildSelectable(final int groupPosition, final int childPosition)
    {
        return true;
    }

    /**
     * 当“初始集合”或“当前显示集合”数据发生改变时，界面默认不会自动刷新，需要手动调用此方法来通知界面刷新显示。<br/>
     * 此函数之所以不自动触发，旨在方便以后可以精确的控制刷新的最佳时机。
     */
    public void refresh()
    {
        //必须在刷新时，把mGroupObjects的缓存重置，否则假如外界想通过引用来更新数据，就会“被缓存”起来了。
        this.mGroupObjects = null;
        if (this.mObjects != null && this.mObjects.size() > 0)
            this.mDataSetObservable.notifyChanged();
        else
            this.mDataSetObservable.notifyInvalidated();
    }

    @Override
    public void registerDataSetObserver(final DataSetObserver observer)
    {
        this.mDataSetObservable.registerObserver(observer);
    }

    /**
     * @param items 【当需要Group有序时，使用LinkedHashMap传递即可】
     */
    public void setOriginalItems(final Map<T_Group, List<T_Child>> items)
    {
        this.mGroupObjects = null;
        this.mOriginalObjects = this.mObjects = items;
    }

    @Override
    public void unregisterDataSetObserver(final DataSetObserver observer)
    {
        this.mDataSetObservable.unregisterObserver(observer);
    }
}
