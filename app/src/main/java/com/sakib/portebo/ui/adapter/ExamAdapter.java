package com.sakib.portebo.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sakib.portebo.R;
import com.sakib.portebo.model.ExamItem;
import com.sakib.portebo.security.ParentControlManager;
import com.sakib.portebo.util.AnimUtils;
import java.util.List;

public class ExamAdapter extends RecyclerView.Adapter<ExamAdapter.ViewHolder> {
    private final Context context;
    private final List<ExamItem> examList;

    public ExamAdapter(Context context, List<ExamItem> examList) {
        this.context = context;
        this.examList = examList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_exam, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExamItem item = examList.get(position);
        holder.tvSubject.setText(item.getSubject());
        holder.tvDate.setText("তারিখ: " + item.getDate());
        boolean canEdit = ParentControlManager.canEdit(context);
        holder.btnDelete.setEnabled(canEdit);
        holder.btnDelete.setAlpha(canEdit ? 1f : 0.45f);

        holder.btnDelete.setOnClickListener(v -> {
            if (!ParentControlManager.requireEditAccess(context)) return;
            AnimUtils.pressFeedback(v, () -> {
                if (!ParentControlManager.requireEditAccess(context)) return;
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    examList.remove(pos);
                    notifyItemRemoved(pos);
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return examList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvDate;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvExamSubject);
            tvDate = itemView.findViewById(R.id.tvExamDate);
            btnDelete = itemView.findViewById(R.id.btnDeleteExam);
        }
    }
}