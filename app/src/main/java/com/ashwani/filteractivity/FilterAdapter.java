package com.ashwani.filteractivity;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by hp on 7/6/2017.
 */

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.ViewHolder>{

    List<Filters> filterList;
    Context mContext;
    RecyclerViewItemClickListener clickListener;

    public FilterAdapter(Context context, List<Filters> filterList, RecyclerViewItemClickListener recyclerViewItemClickListener){
        this.mContext = context;
        this.filterList = filterList;
        this.clickListener = recyclerViewItemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.card_filter,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Filters filters = filterList.get(position);
        holder.tvFilterName.setText(filters.getName());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filterList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView tvFilterName;
        public ViewHolder(View itemView){
            super(itemView);

            tvFilterName = (TextView)itemView.findViewById(R.id.filterText);
        }

    }
}
