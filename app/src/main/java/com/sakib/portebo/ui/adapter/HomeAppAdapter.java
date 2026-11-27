package com.sakib.portebo.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import com.sakib.portebo.util.IconCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sakib.portebo.R;
import com.sakib.portebo.model.AppItem;
import com.sakib.portebo.util.AnimUtils;
import java.util.List;

public class HomeAppAdapter extends RecyclerView.Adapter<HomeAppAdapter.ViewHolder> {
    private final Context context;
    private List<AppItem> apps;
    private final PackageManager pm;
    private boolean firstLoad = true;

    public HomeAppAdapter(Context context, List<AppItem> apps) {
        this.context = context;
        this.apps = apps;
        this.pm = context.getPackageManager();
    }

    public void updateData(List<AppItem> newApps) {
        this.apps = newApps;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_home_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppItem item = apps.get(position);
        holder.tvName.setText(item.getName());

        IconCache.load(context, item.getPackageName(), holder.ivIcon);

        switch (item.getTier()) {
            case WHITELIST:
                holder.tvBadge.setText("খোলা");
                holder.tvBadge.setTextColor(context.getResources().getColor(R.color.tier_whitelist));
                break;
            case GREYLIST:
                int rem = Math.max(0, item.getQuotaSeconds() - item.getUsedSeconds());
                holder.tvBadge.setText(String.format("%02d:%02d", rem / 60, rem % 60));
                holder.tvBadge.setTextColor(context.getResources().getColor(R.color.tier_greylist));
                break;
            case BLACKLIST:
                holder.tvBadge.setText("ব্লক");
                holder.tvBadge.setTextColor(context.getResources().getColor(R.color.tier_blacklist));
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            AnimUtils.pressFeedback(v, () -> {
                try {
                    Intent launchIntent = pm.getLaunchIntentForPackage(item.getPackageName());
                    if (launchIntent != null) {
                        context.startActivity(launchIntent);
                    }
                } catch (Exception ignored) {
                }
            });
        });

        if (firstLoad) {
            AnimUtils.fadeInSlideUp(holder.itemView, position * 25);
            if (position == apps.size() - 1) firstLoad = false;
        }
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvBadge;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivHomeAppIcon);
            tvName = itemView.findViewById(R.id.tvHomeAppName);
            tvBadge = itemView.findViewById(R.id.tvHomeAppTierBadge);
        }
    }
}