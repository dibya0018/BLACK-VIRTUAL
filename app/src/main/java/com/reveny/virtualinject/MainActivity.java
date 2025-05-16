package com.reveny.virtualinject;

import android.os.Bundle;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.view.*;
import android.widget.*;
import android.util.Log;
import java.util.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.vcore.BlackBoxCore;
import com.vcore.entity.pm.InstallResult;

public class MainActivity extends AppCompatActivity {
	
	private Toolbar toolbar;
	private Button cloneButton;
	private ListView appsListView;
	private RecyclerView clonedAppsRecyclerView;
	private List<AppInfo> installedApps;
	private List<ClonedAppInfo> clonedApps;
	private View rootView;
	private int selectedCloneCount = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
		bottomNav.setOnNavigationItemSelectedListener(item -> {
			Fragment selectedFragment;
			if (item.getItemId() == R.id.nav_all_apps) {
				selectedFragment = new AllAppsFragment();
			} else {
				selectedFragment = new ClonedAppsFragment();
			}
			getSupportFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, selectedFragment)
				.commit();
			return true;
		});

		// Default fragment
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, new AllAppsFragment())
				.commit();
		}
	}

	private void initialize(Bundle _savedInstanceState) {

		toolbar = findViewById(R.id.toolbar);
		//cloneButton = findViewById(R.id.cloneButton);
		//appsListView = findViewById(R.id.appsListView);
		clonedAppsRecyclerView = findViewById(R.id.clonedAppsRecyclerView);

		rootView = findViewById(android.R.id.content);

		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		
		installedApps = new ArrayList<>();
		clonedApps = new ArrayList<>();
		
		cloneButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				showCloneCountDialog();
			}
		});
		
		appsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				AppInfo selectedApp = installedApps.get(position);
				cloneAndLaunchApp(selectedApp.packageName);
			}
		});

		// Setup RecyclerView for cloned apps
		clonedAppsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		ClonedAppsAdapter clonedAdapter = new ClonedAppsAdapter(clonedApps);
		clonedAppsRecyclerView.setAdapter(clonedAdapter);
	}
	
	private void showCloneCountDialog() {
		String[] items = new String[50];
		for (int i = 0; i < 50; i++) {
			items[i] = String.valueOf(i + 1);
		}
		
		new MaterialAlertDialogBuilder(this)
			.setTitle("Select Number of Clones")
			.setSingleChoiceItems(items, selectedCloneCount - 1, (dialog, which) -> {
				selectedCloneCount = which + 1;
				dialog.dismiss();
				loadInstalledApps();
			})
			.show();
	}
	
	private void loadInstalledApps() {
		installedApps.clear();
		PackageManager pm = getPackageManager();
		List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
		
		for (ApplicationInfo packageInfo : packages) {
			if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
				AppInfo appInfo = new AppInfo();
				appInfo.packageName = packageInfo.packageName;
				appInfo.appName = pm.getApplicationLabel(packageInfo).toString();
				appInfo.icon = pm.getApplicationIcon(packageInfo);
				installedApps.add(appInfo);
			}
		}
		
		AppListAdapter adapter = new AppListAdapter(this, installedApps);
		appsListView.setAdapter(adapter);
		
		// Update cloned apps list
		updateClonedAppsList();
	}

	private void updateClonedAppsList() {
		clonedApps.clear();
		PackageManager pm = getPackageManager();
		
		// Get all installed packages for each user ID
		for (int userId = 0; userId < 50; userId++) {
			List<PackageInfo> packages = BlackBoxCore.get().getInstalledPackages(PackageManager.GET_META_DATA, userId);
			if (packages != null) {
				for (PackageInfo packageInfo : packages) {
					try {
						ClonedAppInfo clonedApp = new ClonedAppInfo();
						clonedApp.packageName = packageInfo.packageName;
						clonedApp.appName = pm.getApplicationLabel(pm.getApplicationInfo(packageInfo.packageName, 0)).toString();
						clonedApp.icon = pm.getApplicationIcon(packageInfo.packageName);
						clonedApp.userId = userId;
						clonedApps.add(clonedApp);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		// Update RecyclerView
		clonedAppsRecyclerView.getAdapter().notifyDataSetChanged();
	}
	
	private void cloneAndLaunchApp(String packageName) {
		showProgressDialog();
		
		new Thread(() -> {
			int successCount = 0;
			for (int i = 0; i < selectedCloneCount; i++) {
				if (BlackBoxCore.get().isInstalled(packageName, i)) {
					boolean success = BlackBoxCore.get().launchApk(packageName, i);
					if (success) successCount++;
				} else {
					InstallResult result = BlackBoxCore.get().installPackageAsUser(packageName, i);
					if (result.success) {
						boolean launchSuccess = BlackBoxCore.get().launchApk(packageName, i);
						if (launchSuccess) successCount++;
					}
				}
			}
			
			final int finalSuccessCount = successCount;
			runOnUiThread(() -> {
				dismissProgressDialog();
				showSnackbar("Successfully cloned " + finalSuccessCount + " out of " + selectedCloneCount + " instances");
				updateClonedAppsList();
			});
		}).start();
	}
	
	private ProgressDialog progressDialog;
	
	private void showProgressDialog() {
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Cloning app...");
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setCancelable(false);
		progressDialog.show();
	}
	
	private void dismissProgressDialog() {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	}
	
	private void showSnackbar(String message) {
		Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
	}
	
	private void initializeLogic() {
		loadInstalledApps();
	}
	
	private static class AppInfo {
		String packageName;
		String appName;
		android.graphics.drawable.Drawable icon;
		
		@Override
		public String toString() {
			return appName;
		}
	}

	private static class ClonedAppInfo {
		String packageName;
		String appName;
		android.graphics.drawable.Drawable icon;
		int userId;
	}
	
	private class AppListAdapter extends ArrayAdapter<AppInfo> {
		private Context context;
		private List<AppInfo> apps;
		
		public AppListAdapter(Context context, List<AppInfo> apps) {
			super(context, R.layout.app_list_item, apps);
			this.context = context;
			this.apps = apps;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.app_list_item, parent, false);
			}
			
			AppInfo app = apps.get(position);
			
			ImageView iconView = convertView.findViewById(R.id.app_icon);
			TextView nameView = convertView.findViewById(R.id.app_name);
			TextView packageView = convertView.findViewById(R.id.app_package);
			
			iconView.setImageDrawable(app.icon);
			nameView.setText(app.appName);
			packageView.setText(app.packageName);
			
			return convertView;
		}
	}

	private class ClonedAppsAdapter extends RecyclerView.Adapter<ClonedAppsAdapter.ViewHolder> {
		private List<ClonedAppInfo> clonedApps;
		
		public ClonedAppsAdapter(List<ClonedAppInfo> clonedApps) {
			this.clonedApps = clonedApps;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = getLayoutInflater().inflate(R.layout.cloned_app_item, parent, false);
			return new ViewHolder(view);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			ClonedAppInfo app = clonedApps.get(position);
			holder.instanceNumber.setText(app.appName + " (User " + app.userId + ")");
			holder.instanceStatus.setText("Cloned Instance");
			
			holder.launchButton.setOnClickListener(v -> {
				boolean success = BlackBoxCore.get().launchApk(app.packageName, app.userId);
				showSnackbar("Launching " + app.appName + " (User " + app.userId + "): " + 
					(success ? "Success" : "Failed"));
			});
		}
		
		@Override
		public int getItemCount() {
			return clonedApps.size();
		}
		
		class ViewHolder extends RecyclerView.ViewHolder {
			TextView instanceNumber;
			TextView instanceStatus;
			com.google.android.material.button.MaterialButton launchButton;
			
			ViewHolder(View view) {
				super(view);
				instanceNumber = view.findViewById(R.id.instance_number);
				instanceStatus = view.findViewById(R.id.instance_status);
				launchButton = view.findViewById(R.id.launch_button);
			}
		}
	}
}
