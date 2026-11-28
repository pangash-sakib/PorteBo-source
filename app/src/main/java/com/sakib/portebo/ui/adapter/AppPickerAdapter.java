package com.sakib.portebo.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sakib.portebo.R;
import com.sakib.portebo.data.AppTierManager;
import com.sakib.portebo.model.AppItem;
import com.sakib.portebo.util.AnimUtils;
import com.sakib.portebo.util.NotificationMuteHelper;

import java.util.ArrayList;
import java.util.List;

public class AppPickerAdapter extends RecyclerView.Adapter<AppPickerAdapter.ViewHolder> {
    public interface OnAppSelectedListener {
        void onAppSelected(String packageName, String name);
    }

    private final Context context;
    private final PackageManager pm;
    private final List<ResolveInfo> allLauncherApps = new ArrayList<>();
    private final OnAppSelectedListener listener;

    public AppPickerAdapter(Context context, OnAppSelectedListener listener) {
        this.context = context;
        this.pm = context.getPackageManager();
        this.listener = listener;

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(mainIntent, 0);
        if (list != null) {
            allLauncherApps.addAll(list);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_picker_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ResolveInfo ri = allLauncherApps.get(position);
        String pkg = ri.activityInfo.packageName;
        String name = ri.loadLabel(pm).toString();

        holder.tvName.setText(name);
        holder.tvPackage.setText(pkg);

        com.sakib.portebo.util.IconCache.load(context, pkg, holder.ivIcon);

        AppItem item = AppTierManager.getApp(pkg);
        boolean alreadyAdded = item != null;
        holder.ivCheck.setVisibility(alreadyAdded ? View.VISIBLE : View.GONE);

        // --- NEW: Mute Toggle Logic ---
        if (item != null) {
            holder.btnMute.setVisibility(View.VISIBLE);
            int muteColor = item.isMuted() 
                    ? ContextCompat.getColor(context, R.color.tier_blacklist) 
                    : ContextCompat.getColor(context, R.color.text_muted);
            holder.btnMute.setColorFilter(muteColor);

            holder.btnMute.setOnClickListener(v -> {
                AnimUtils.pressFeedback(v, () -> {
                    boolean newState = !item.isMuted();
                    item.setMuted(newState);
                    AppTierManager.saveTiersDebounced(context);
                    
                    // Update UI immediately
                    int newColor = newState 
                            ? ContextCompat.getColor(context, R.color.tier_blacklist) 
                            : ContextCompat.getColor(context, R.color.text_muted);
                    holder.btnMute.setColorFilter(newColor);

                    // Apply actual Android mute
                    NotificationMuteHelper.setMuted(context, pkg, newState);
                });
            });
        } else {
            holder.btnMute.setVisibility(View.GONE);
        }
        // --- END Mute Toggle Logic ---

        holder.itemView.setOnClickListener(v -> {
            AnimUtils.pressFeedback(v, () -> {
                if (listener != null) {
                    listener.onAppSelected(pkg, name);
                    holder.ivCheck.setVisibility(View.VISIBLE);
                    // Ensure mute button is visible after adding
                    holder.btnMute.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return allLauncherApps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvPackage;
        ImageView ivCheck;
        ImageView btnMute; // NEW

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivPickerAppIcon);
            tvName = itemView.findViewById(R.id.tvPickerAppName);
            tvPackage = itemView.findViewById(R.id.tvPickerAppPackage);
            ivCheck = itemView.findViewById(R.id.ivPickerAddedCheck);
            btnMute = itemView.findViewById(R.id.btnMuteApp); // NEW
        }
    }
}