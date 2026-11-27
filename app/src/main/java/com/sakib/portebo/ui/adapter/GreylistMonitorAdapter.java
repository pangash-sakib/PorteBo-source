package com.sakib.portebo.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.sakib.portebo.R;
import com.sakib.portebo.model.AppItem;
import com.sakib.portebo.util.AnimUtils;
import java.util.List;

public class GreylistMonitorAdapter extends RecyclerView.Adapter<GreylistMonitorAdapter.ViewHolder> {
    private final Context context;
    private List<AppItem> greylistApps;
    private final PackageManager pm;

    public GreylistMonitorAdapter(Context context, List<AppItem> greylistApps) {
        this.context = context;
        this.greylistApps = greylistApps;
        this.pm = context.getPackageManager();
    }

    public void updateData(List<AppItem> newApps) {
        this.greylistApps = newApps;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_greylist_tracker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppItem item = greylistApps.get(position);
        holder.tvName.setText(item.getName());

        int rem = Math.max(0, item.getQuotaSeconds() - item.getUsedSeconds());
        holder.tvRemaining.setText(String.format("%02d:%02d", rem / 60, rem % 60));

        if (rem <= 15) {
            holder.tvRemaining.setTextColor(context.getResources().getColor(R.color.tier_blacklist));
        } else if (rem <= 60) {
            holder.tvRemaining.setTextColor(context.getResources().getColor(R.color.tier_greylist));
        } else {
            holder.tvRemaining.setTextColor(context.getResources().getColor(R.color.tier_whitelist));
        }

        holder.btnLaunch.setOnClickListener(v -> {
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
    }

    @Override
    public int getItemCount() {
        return greylistApps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRemaining;
        MaterialButton btnLaunch;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTrackerAppName);
            tvRemaining = itemView.findViewById(R.id.tvTrackerRemaining);
            btnLaunch = itemView.findViewById(R.id.btnLaunchGreylistApp);
        }
    }
}