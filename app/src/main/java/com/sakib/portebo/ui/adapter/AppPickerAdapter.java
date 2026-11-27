package com.sakib.portebo.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import com.sakib.portebo.util.IconCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sakib.portebo.R;
import com.sakib.portebo.data.AppTierManager;
import com.sakib.portebo.util.AnimUtils;
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

        IconCache.load(context, pkg, holder.ivIcon);

        boolean alreadyAdded = AppTierManager.getApp(pkg) != null;
        holder.ivCheck.setVisibility(alreadyAdded ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            AnimUtils.pressFeedback(v, () -> {
                if (listener != null) {
                    listener.onAppSelected(pkg, name);
                    holder.ivCheck.setVisibility(View.VISIBLE);
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

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivPickerAppIcon);
            tvName = itemView.findViewById(R.id.tvPickerAppName);
            tvPackage = itemView.findViewById(R.id.tvPickerAppPackage);
            ivCheck = itemView.findViewById(R.id.ivPickerAddedCheck);
        }
    }
}