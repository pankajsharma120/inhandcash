package com.inhandcash.www.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.inhandcash.www.R;
import com.inhandcash.www.models.Transition;

import java.util.ArrayList;


public class TranisitionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int ITEM = 0;
    private static final int LOADING = 1;
    private boolean isLoadingAdded = false;

    private ArrayList<Transition> transitions;
    private Context mContext;

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case ITEM:
                viewHolder = getViewHolder(parent, inflater);
                break;
            case LOADING:
                View v2 = inflater.inflate(R.layout.item_progress, parent, false);
                viewHolder = new LoadingVH(v2);
                break;
        }
        return viewHolder;
    }

    @NonNull
    private RecyclerView.ViewHolder getViewHolder(ViewGroup parent, LayoutInflater inflater) {
        RecyclerView.ViewHolder viewHolder;
        View v1 = inflater.inflate(R.layout.single_transition, parent, false);
        viewHolder = new SINViewHolder(v1);
        return viewHolder;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == transitions.size() - 1 && isLoadingAdded) ? LOADING : ITEM;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case ITEM:{
                SINViewHolder siHolder = ((SINViewHolder)holder);
                TextView amount = siHolder.amount;
                TextView sender = siHolder.sender;
                TextView receiver = siHolder.receiver;
                TextView date_time = siHolder.date_time;
                TextView status = siHolder.status;
                amount.setText(String.valueOf(transitions.get(position).getAmount()));
                status.setText(transitions.get(position).getStatus_name());
                sender.setText(transitions.get(position).getSender());
                receiver.setText(transitions.get(position).getReceiver());
                date_time.setText(transitions.get(position).getDate());
                if(transitions.get(position).getStatus()!=null && transitions.get(position).getStatus().compareTo("success")!=0){
                    holder.itemView.setBackgroundColor(mContext.getColor(R.color.danger_light));
                }
                break;
            }
            case LOADING:
//                Do nothing
                break;
        }
    }

    @Override
    public int getItemCount() {
        return transitions == null ? 0 : transitions.size();
    }

    public TranisitionAdapter(ArrayList<Transition> transitions, Context mContext){
        this.transitions = transitions;
        this.mContext = mContext;
    }

    public class SINViewHolder extends RecyclerView.ViewHolder {
        TextView amount;
        TextView sender;
        TextView date_time;
        TextView status;
        TextView receiver;
        public SINViewHolder(@NonNull View itemView) {
            super(itemView);
            this.amount = (itemView).findViewById(R.id.amount_tv);
            this.sender = (itemView).findViewById(R.id.sender_tv);
            this.receiver = (itemView).findViewById(R.id.receiver_tv);
            this.date_time = (itemView).findViewById(R.id.dateTime_tv);
            this.status = (itemView).findViewById(R.id.status);
        }
    }

    protected class LoadingVH extends RecyclerView.ViewHolder {

        public LoadingVH(View itemView) {
            super(itemView);
        }
    }


    public void add(Transition mc) {
        transitions.add(mc);
        notifyItemInserted(transitions.size()-1);
    }

    public void addAtPosition(Transition ts, int postion){
        transitions.add(postion, ts);
        notifyItemInserted(postion);
    }

    public void addAll(ArrayList<Transition> mcList) {
        for (Transition mc : mcList) {
            add(mc);
        }
    }

    public void remove(Transition city) {
        int position = transitions.indexOf(city);
        if (position > -1) {
            transitions.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clear() {
        isLoadingAdded = false;
        while (getItemCount() > 0) {
            remove(getItem(0));
        }
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }


    public void addLoadingFooter() {
        isLoadingAdded = true;
        add(new Transition());
    }

    public void removeLoadingFooter() {
        if(transitions.size()==0){
            return;
        }
        isLoadingAdded = false;
        int position = transitions.size() - 1;
        Transition item = getItem(position);

        if (item != null) {
            transitions.remove(position);
            notifyItemRemoved(position);
        }
    }

    public Transition getItem(int position) {
        return transitions.get(position);
    }

}
