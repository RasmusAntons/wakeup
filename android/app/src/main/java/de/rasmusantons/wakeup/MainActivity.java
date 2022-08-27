package de.rasmusantons.wakeup;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());
                executorService.execute(() -> {
                    String fbToken = getSharedPreferences("firebase", MODE_PRIVATE)
                            .getString("fb_token", null);
                    Log.i(TAG, String.format("using fbToken: %s", fbToken));
                    String ownDeviceId = WakeupApi.getOwnDeviceUrl(MainActivity.this, fbToken);
                    handler.post(() -> {
                        Snackbar.make(view, String.format("ownDeviceId=%s", ownDeviceId), Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    });
                });
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
