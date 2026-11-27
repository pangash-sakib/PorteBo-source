package com.sakib.portebo.ui;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.sakib.portebo.R;
import com.sakib.portebo.data.PrefsManager;
import com.sakib.portebo.lockdown.LockdownManager;
import com.sakib.portebo.model.DailyRoutine;
import com.sakib.portebo.model.ExamItem;
import com.sakib.portebo.security.ParentControlManager;
import com.sakib.portebo.service.AlarmScheduler;
import com.sakib.portebo.ui.adapter.ExamAdapter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

public class RoutineFragment extends Fragment {
    private RecyclerView rv;
    private ExamAdapter adapter;
    private final List<ExamItem> exams = new ArrayList<>();
    private EditText start, end;
    private android.content.SharedPreferences prefs;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_routine, container, false);
        prefs = requireContext().getSharedPreferences("routine_data", Context.MODE_PRIVATE);
        rv = rootView.findViewById(R.id.rvExams);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        loadExams();
        adapter = new ExamAdapter(requireContext(), exams);
        rv.setAdapter(adapter);

        start = rootView.findViewById(R.id.etRoutineStart);
        end = rootView.findViewById(R.id.etRoutineEnd);
        DailyRoutine routine = PrefsManager.getDailyRoutine(requireContext());
        start.setText(routine.getStartTime());
        end.setText(routine.getEndTime());

        start.setOnClickListener(v -> { if (canEdit()) pickTime(start); });
        end.setOnClickListener(v -> { if (canEdit()) pickTime(end); });
        rootView.findViewById(R.id.btnLock1Hour).setOnClickListener(v -> startLock(60));
        rootView.findViewById(R.id.btnLock2Hour).setOnClickListener(v -> startLock(120));
        rootView.findViewById(R.id.btnLock4Hour).setOnClickListener(v -> startLock(240));
        rootView.findViewById(R.id.btnSaveRoutine).setOnClickListener(v -> {
            if (!canEdit()) return;
            String from = start.getText().toString();
            String to = end.getText().toString();
            PrefsManager.saveDailyRoutine(requireContext(), new DailyRoutine(true, from, to));
            AlarmScheduler.scheduleDaily(requireContext(), from, to);
            Toast.makeText(requireContext(), "দৈনিক রুটিন সেভ হয়েছে।", Toast.LENGTH_SHORT).show();
        });
        rootView.findViewById(R.id.btnAddExam).setOnClickListener(v -> showAdd());
        applyAccess();
        return rootView;
    }

    private boolean canEdit() {
        return ParentControlManager.requireEditAccess(requireContext());
    }

    private void pickTime(EditText target) {
        if (!canEdit()) return;
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(requireContext(), (view, hour, minute) -> {
            if (canEdit()) target.setText(String.format(Locale.US, "%02d:%02d", hour, minute));
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void startLock(int minutes) {
        if (!canEdit()) return;
        LockdownManager.startLockdown(requireContext(), minutes, LockdownManager.LockdownSource.QUICK);
        Toast.makeText(requireContext(), "লকডাউন শুরু হয়েছে।", Toast.LENGTH_SHORT).show();
    }

    private void loadExams() {
        exams.clear();
        try {
            JSONArray data = new JSONArray(prefs.getString("exams", "[]"));
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.getJSONObject(i);
                exams.add(new ExamItem(item.getString("id"), item.getString("subject"), item.getString("date")));
            }
        } catch (Exception ignored) { }
    }

    private void saveExams() {
        if (!ParentControlManager.canEdit(requireContext())) return;
        try {
            JSONArray data = new JSONArray();
            for (ExamItem item : exams) {
                JSONObject json = new JSONObject();
                json.put("id", item.getId());
                json.put("subject", item.getSubject());
                json.put("date", item.getDate());
                data.put(json);
            }
            prefs.edit().putString("exams", data.toString()).apply();
            AlarmScheduler.scheduleExams(requireContext(), exams);
        } catch (Exception ignored) { }
    }

    private void showAdd() {
        if (!canEdit()) return;
        View form = getLayoutInflater().inflate(R.layout.dialog_edit_exam, null);
        EditText subject = form.findViewById(R.id.etExamSubject);
        EditText date = form.findViewById(R.id.etExamDate);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(form).create();
        form.findViewById(R.id.btnSaveExam).setOnClickListener(view -> {
            if (!canEdit()) return;
            String name = subject.getText().toString().trim();
            String examDate = date.getText().toString().trim();
            if (name.isEmpty() || examDate.isEmpty()) {
                Toast.makeText(requireContext(), "বিষয় আর তারিখ দাও।", Toast.LENGTH_SHORT).show();
                return;
            }
            exams.add(new ExamItem(UUID.randomUUID().toString(), name, examDate));
            saveExams();
            adapter.notifyItemInserted(exams.size() - 1);
            dialog.dismiss();
        });
        dialog.show();
    }

    public void refreshAccess() {
        if (!isAdded() || rootView == null) return;
        applyAccess();
    }

    private void applyAccess() {
        if (!isAdded() || rootView == null) return;
        boolean edit = ParentControlManager.canEdit(requireContext());
        setEditable(start, edit);
        setEditable(end, edit);
        setEditable(rootView.findViewById(R.id.btnSaveRoutine), edit);
        setEditable(rootView.findViewById(R.id.btnAddExam), edit);
        setEditable(rootView.findViewById(R.id.btnLock1Hour), edit);
        setEditable(rootView.findViewById(R.id.btnLock2Hour), edit);
        setEditable(rootView.findViewById(R.id.btnLock4Hour), edit);
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void setEditable(View view, boolean enabled) {
        if (view == null) return;
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.45f);
    }

    @Override public void onResume() {
        super.onResume();
        applyAccess();
    }

    @Override public void onPause() {
        saveExams();
        super.onPause();
    }
}
