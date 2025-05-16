package com.reveny.virtualinject;
import com.vcore.BlackBoxCore;
import com.vcore.entity.pm.InstallResult;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;

public class AllAppsFragment extends Fragment {
    private RecyclerView recyclerView;
    private EditText searchBar;
    private AppsGridAdapter adapter;
    private List<AppInfo> allApps = new ArrayList<>();
    private List<AppInfo> filteredApps = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_apps, container, false);
        recyclerView = view.findViewById(R.id.apps_grid);
        searchBar = view.findViewById(R.id.search_bar);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        adapter = new AppsGridAdapter(filteredApps, getContext(), true);
        recyclerView.setAdapter(adapter);

        adapter.setOnAppClickListener(appInfo -> showCloneDialog(appInfo));

        loadInstalledApps();

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void loadInstalledApps() {
        allApps.clear();
        PackageManager pm = getContext().getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                AppInfo appInfo = new AppInfo();
                appInfo.packageName = packageInfo.packageName;
                appInfo.appName = pm.getApplicationLabel(packageInfo).toString();
                appInfo.icon = pm.getApplicationIcon(packageInfo);
                allApps.add(appInfo);
            }
        }
        filteredApps.clear();
        filteredApps.addAll(allApps);
        adapter.notifyDataSetChanged();
    }

    private void filterApps(String query) {
        filteredApps.clear();
        for (AppInfo app : allApps) {
            if (app.appName.toLowerCase().contains(query.toLowerCase()) ||
                app.packageName.toLowerCase().contains(query.toLowerCase())) {
                filteredApps.add(app);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showCloneDialog(AppInfo appInfo) {
        final String[] cloneCounts = new String[10];
        for (int i = 0; i < 10; i++) cloneCounts[i] = String.valueOf(i + 1);
        new AlertDialog.Builder(getContext())
            .setTitle("Select Number of Clones")
            .setItems(cloneCounts, (dialog, which) -> {
                int count = which + 1;
                new Thread(() -> {
                    for (int i = 0; i < count; i++) {
                        try {
                            InstallResult result = BlackBoxCore.get().installPackageAsUser(appInfo.packageName, i);
                            boolean success = result.success;
                            final int userId = i;
                            final boolean cloneSuccess = success;
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(),
                                    (cloneSuccess ? "Cloned: " : "Failed to clone: ") + appInfo.appName + " (User " + userId + ")",
                                    Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            final int userId = i;
                            final String errorMsg = e.getMessage();
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(),
                                    "Error cloning: " + appInfo.appName + " (User " + userId + "): " + errorMsg,
                                    Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                }).start();
            })
            .show();
    }
} 