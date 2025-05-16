package com.reveny.virtualinject;

import com.vcore.BlackBoxCore;
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

public class ClonedAppsFragment extends Fragment {
    private RecyclerView recyclerView;
    private EditText searchBar;
    private AppsGridAdapter adapter;
    private List<AppInfo> allClonedApps = new ArrayList<>();
    private List<AppInfo> filteredClonedApps = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cloned_apps, container, false);
        recyclerView = view.findViewById(R.id.apps_grid);
        searchBar = view.findViewById(R.id.search_bar);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        adapter = new AppsGridAdapter(filteredClonedApps, getContext(), false);
        recyclerView.setAdapter(adapter);

        adapter.setOnAppClickListener(appInfo -> {
            // Parse userId from appInfo.appName, e.g., "Telegram (3)"
            int userId = 0;
            String name = appInfo.appName;
            int idx1 = name.lastIndexOf('(');
            int idx2 = name.lastIndexOf(')');
            if (idx1 != -1 && idx2 != -1 && idx2 > idx1) {
                try {
                    userId = Integer.parseInt(name.substring(idx1 + 1, idx2));
                } catch (Exception ignored) {}
            }
            Toast.makeText(getContext(), "Clicked: " + appInfo.packageName + " userId: " + userId, Toast.LENGTH_SHORT).show();
            try {
                boolean success = BlackBoxCore.get().launchApk(appInfo.packageName, userId);
                updateStatusAndLog("Launched: " + appInfo.packageName + " (User " + userId + ")", success);
            } catch (Exception e) {
                updateStatusAndLog("Error launching: " + appInfo.packageName + " (User " + userId + "): " + e.getMessage(), false);
            }
        });

        adapter.setOnAppLongClickListener(appInfo -> {
            // Parse userId from appInfo.appName, e.g., "Telegram (3)"
            int userId = 0;
            String name = appInfo.appName;
            int idx1 = name.lastIndexOf('(');
            int idx2 = name.lastIndexOf(')');
            if (idx1 != -1 && idx2 != -1 && idx2 > idx1) {
                try {
                    userId = Integer.parseInt(name.substring(idx1 + 1, idx2));
                } catch (Exception ignored) {}
            }
            try {
                BlackBoxCore.get().uninstallPackageAsUser(appInfo.packageName, userId);
                updateStatusAndLog("Uninstalled: " + appInfo.packageName + " (User " + userId + ")", true);
                loadClonedApps(); // Refresh the list
            } catch (Exception e) {
                updateStatusAndLog("Error uninstalling: " + appInfo.packageName + " (User " + userId + "): " + e.getMessage(), false);
            }
        });

        loadClonedApps();

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterClonedApps(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadClonedApps();
    }

    private void loadClonedApps() {
        allClonedApps.clear();
        PackageManager pm = getContext().getPackageManager();
        int flags = 0; // Use 0 for flags for broader results
        int totalFound = 0;
        StringBuilder foundUsers = new StringBuilder();
        for (int userId = 0; userId < 50; userId++) {
            List<ApplicationInfo> apps = BlackBoxCore.get().getInstalledApplications(flags, userId);
            if (apps != null && !apps.isEmpty()) {
                for (ApplicationInfo appInfo : apps) {
                    try {
                        AppInfo cloned = new AppInfo();
                        cloned.packageName = appInfo.packageName;
                        String appName = pm.getApplicationLabel(appInfo).toString();
                        cloned.appName = appName + " (" + userId + ")";
                        cloned.icon = pm.getApplicationIcon(appInfo);
                        allClonedApps.add(cloned);
                        totalFound++;
                        foundUsers.append("[" + appInfo.packageName + " @ userId " + userId + "] ");
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error loading app: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        if (totalFound == 0) {
            Toast.makeText(getContext(), "No cloned apps found!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Cloned apps found: " + totalFound + "\n" + foundUsers, Toast.LENGTH_LONG).show();
        }
        filteredClonedApps.clear();
        filteredClonedApps.addAll(allClonedApps);
        adapter.notifyDataSetChanged();
    }

    private void filterClonedApps(String query) {
        filteredClonedApps.clear();
        for (AppInfo app : allClonedApps) {
            if (app.appName.toLowerCase().contains(query.toLowerCase()) ||
                app.packageName.toLowerCase().contains(query.toLowerCase())) {
                filteredClonedApps.add(app);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void updateStatusAndLog(String message, boolean success) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        // You can also add logging here if needed
    }
} 