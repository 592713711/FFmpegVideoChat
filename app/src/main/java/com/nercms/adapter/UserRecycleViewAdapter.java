package com.nercms.adapter;

import android.content.Context;
import android.support.v4.view.LayoutInflaterFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nercms.Config;
import com.nercms.R;
import com.nercms.model.User;
import com.nercms.util.Util;

import java.util.ArrayList;

/**
 * Created by zsg on 2016/11/15.
 */
public class UserRecycleViewAdapter extends RecyclerView.Adapter{
    private ArrayList<User> datas;
    LayoutInflater inflater;
    RecycleOnClickListener lis;

    public UserRecycleViewAdapter(Context context,RecycleOnClickListener listener){
        this.inflater=LayoutInflater.from(context);
        datas=new ArrayList<>();
        this.lis=listener;
    }

    public void updateData(ArrayList<User> list) {
        datas.clear();
        datas.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MyViewHolder holder;
        View v = inflater.inflate(R.layout.userlist_item, null);
        holder = new MyViewHolder(v);
        return holder;

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        User user=datas.get(position);
        MyViewHolder myViewHolder= (MyViewHolder) holder;

        myViewHolder.user_name.setText(user.username);
        myViewHolder.head_icon.setImageResource(Util.getHeadId(user.icon));
    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    private class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView head_icon;
        TextView user_name;


        public MyViewHolder(View itemView) {
            super(itemView);
            head_icon= (ImageView) itemView.findViewById(R.id.icon_image);
            user_name= (TextView) itemView.findViewById(R.id.username_text);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            lis.onClickItem(getAdapterPosition());
        }
    }
}
