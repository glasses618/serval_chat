package org.servalproject.servalchat.views;

import android.os.AsyncTask;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import org.servalproject.mid.IObservableList;
import org.servalproject.mid.ListObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeremy on 8/08/16.
 */
public abstract class ScrollingAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> implements ListObserver<T> {
    private LinearLayoutManager layoutManager;
    private IObservableList<T, ?> list;
    private List<T> past = new ArrayList<>();
    private List<T> future = new ArrayList<>();
    private boolean hasMore = true;
    private boolean fetching = false;

    public ScrollingAdapter(IObservableList<T, ?> list){
        this.list = list;
    }

    protected abstract void bind(VH holder, T item);

    @Override
    public void onBindViewHolder(VH holder, int position) {
        bind(holder, getItem(position));
    }

    protected void addItem(T item, boolean inPast){
        if (inPast){
            past.add(item);
            notifyItemInserted(past.size() + future.size() - 1);
        }else{
            future.add(item);
            notifyItemInserted(0);
            if (layoutManager!=null)
                layoutManager.scrollToPosition(0);
        }
    }

    @Override
    public int getItemCount() {
        int count = past.size() + future.size();
        if (hasMore)
            count++;
        return count;
    }

    protected abstract int getItemType(T item);

    protected T getItem(int position){
        if (position<0)
            return null;
        int futureSize = future.size();
        if (position < futureSize)
            return future.get(futureSize - 1 - position);
        position -= futureSize;
        if (position<past.size())
            return past.get(position);
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        return getItemType(getItem(position));
    }

    private void testPosition(){
        if (fetching || !hasMore || layoutManager==null)
            return;

        int lastVisible = layoutManager.findLastVisibleItemPosition();
        final int fetchCount = lastVisible + 15 - (past.size() + future.size());

        if (fetchCount<=0)
            return;

        fetching = true;

        final AsyncTask<Void, T, Void> fetch = new AsyncTask<Void, T, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try{
                    for (int i=0;i<fetchCount;i++){
                        T msg = list.next();
                        publishProgress(msg);
                        if (msg == null)
                            break;
                    }
                }catch (Exception e){
                    throw new IllegalStateException(e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                fetching = false;
                testPosition();
            }

            @Override
            protected void onProgressUpdate(T... values) {
                super.onProgressUpdate(values);
                T msg = values[0];
                if (msg == null) {
                    hasMore = false;
                    notifyItemRemoved(past.size() + future.size());
                }else
                    addItem(msg, true);
            }
        };
        fetch.execute();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                testPosition();
            }
        });
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        layoutManager = null;
    }

    public void onVisible(){
        list.observe(this);
        testPosition();
    }

    public void onHidden(){
        list.stopObserving(this);
    }

    @Override
    public void added(T obj) {
        addItem(obj, false);
    }

    @Override
    public void removed(T obj) {

    }

    @Override
    public void updated(T obj) {

    }

    @Override
    public void reset() {

    }

    public void clear(){
        list.close();
        past.clear();
        future.clear();
    }
}