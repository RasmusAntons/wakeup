package de.rasmusantons.wakeup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import org.json.JSONException;

import de.rasmusantons.wakeup.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private static final String TAG = "MainActivity";

    protected void login() {
        LoginActivity.deleteLogin(this);
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_website, R.id.nav_settings)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navigationView.setNavigationItemSelectedListener(item -> {
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (!handled) {
                if (item.getItemId() == R.id.nav_login) {
                    login();
                } else if (item.getItemId() == R.id.nav_website) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    String wakeupUrl = prefs.getString(getString(R.string.wakeup_server_url), "https://wakeup.3po.ch");
                    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                    CustomTabsIntent customTabsIntent = builder.build();
                    customTabsIntent.launchUrl(this, Uri.parse(wakeupUrl));
                }
            }
            drawer.closeDrawer(GravityCompat.START);
            return handled;
        });

        Log.i(TAG, "creating MainActivity, trying to restore login");
        if (LoginActivity.restoreLogin(this)) {
            Log.i(TAG, "restored login");
            setUsername();
        } else {
            Log.i(TAG, "did not restore login");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setUsername();
    }

    private void setUsername() {
        TextView navHeaderSubtitle = findViewById(R.id.nav_header_subtitle);
        if (navHeaderSubtitle == null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(this::setUsername, 100);
            return;
        }
        if (LoginActivity.mAuthState != null && LoginActivity.mUserInfoJson != null) {
            try {
                String username = LoginActivity.mUserInfoJson.getString("username");
                navHeaderSubtitle.setText(username);
                return;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            navHeaderSubtitle.setText(R.string.nav_header_subtitle);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
