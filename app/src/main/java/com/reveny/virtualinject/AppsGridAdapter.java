package com.reveny.virtualinject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AppsGridAdapter extends RecyclerView.Adapter<AppsGridAdapter.ViewHolder> {
    private List<AppInfo> apps;
    private Context context;
    private boolean isCloneable;
    private OnAppClickListener onAppClickListener;
    private OnAppLongClickListener onAppLongClickListener;

    public interface OnAppClickListener {
        void onAppClick(AppInfo appInfo);
    }

    public interface OnAppLongClickListener {
        void onAppLongClick(AppInfo appInfo);
    }

    public AppsGridAdapter(List<AppInfo> apps, Context context, boolean isCloneable) {
        this.apps = apps;
        this.context = context;
        this.isCloneable = isCloneable;
    }

    public void setOnAppClickListener(OnAppClickListener listener) {
        this.onAppClickListener = listener;
    }

    public void setOnAppLongClickListener(OnAppLongClickListener listener) {
        this.onAppLongClickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.grid_app_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AppInfo app = apps.get(position);
        holder.icon.setImageDrawable(app.icon);
        holder.name.setText(app.appName);

        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(context, "Clicked: " + app.appName, Toast.LENGTH_SHORT).show();
            if (onAppClickListener != null) onAppClickListener.onAppClick(app);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (onAppLongClickListener != null) {
                onAppLongClickListener.onAppLongClick(app);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            name = itemView.findViewById(R.id.app_name);
        }
    }
} 