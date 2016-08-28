package com.daose.ksanime;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.applovin.nativeAds.AppLovinNativeAd;
import com.daose.ksanime.fragment.AnimeListFragment;
import com.daose.ksanime.fragment.SearchFragment;
import com.lapism.searchview.SearchAdapter;
import com.lapism.searchview.SearchItem;
import com.lapism.searchview.SearchView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        AnimeListFragment.OnFragmentInteractionListener,
        SearchFragment.OnFragmentInteractionListener,
        DrawerLayout.DrawerListener,
        SearchView.OnQueryTextListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private boolean isNewMenuItem = false;
    private DrawerLayout drawer;
    private SearchView searchView;

    //TODO:: request native ads from somewhere global and inflate with old native ad then replace so there's no unnecessary shifting

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        setupDrawer();
        setupNavigationView();
        setupSearchView();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_placeholder, AnimeListFragment.newInstance("Popular"));
        ft.commit();
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationContentDescription(getResources().getString(R.string.app_name));
        toolbar.setNavigationIcon(R.drawable.ic_menu_black_24dp);
        setSupportActionBar(toolbar);
    }

    private void setupDrawer() {
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.addDrawerListener(this);
    }

    private void setupNavigationView() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(0).setChecked(true);
        setTitle(navigationView.getMenu().getItem(0).getTitle());
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupSearchView() {
        searchView = (SearchView) findViewById(R.id.search_view);
        if (searchView != null) {
            searchView.setVersion(SearchView.VERSION_MENU_ITEM);
            searchView.setVersionMargins(SearchView.VERSION_MARGINS_MENU_ITEM);
            searchView.setTextSize(16);
            searchView.setHint("Search");
            searchView.setDivider(false);
            searchView.setVoice(false);
            searchView.setTheme(SearchView.THEME_DARK);
            searchView.setAnimationDuration(SearchView.ANIMATION_DURATION);
            searchView.setShadowColor(ContextCompat.getColor(this, R.color.search_shadow_layout));
            searchView.setOnQueryTextListener(this);
            searchView.setOnOpenCloseListener(new SearchView.OnOpenCloseListener() {
                @Override
                public void onOpen() {
                    Log.d(TAG, "onOpen");
                }

                @Override
                public void onClose() {
                    Log.d(TAG, "onClose");
                }
            });

            if (searchView.getAdapter() == null) {
                List<SearchItem> suggestionsList = new ArrayList<>();
                suggestionsList.add(new SearchItem("search1"));
                suggestionsList.add(new SearchItem("search2"));
                suggestionsList.add(new SearchItem("search3"));

                SearchAdapter searchAdapter = new SearchAdapter(this, suggestionsList);
                searchAdapter.addOnItemClickListener(new SearchAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        TextView textView = (TextView) view.findViewById(R.id.textView_item_text);
                        String query = textView.getText().toString();
                        Log.d(TAG, "query: " + query);
                        // mSearchView.close(false);
                    }
                });
                searchView.setAdapter(searchAdapter);
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                searchView.open(true, item);
                return true;
            case android.R.id.home:
                drawer.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        isNewMenuItem = true;
        setTitle(item.getTitle());
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onNativeAdClick(AppLovinNativeAd ad) {
        ad.launchClickTarget(this);
    }

    @Override
    public void onAnimeClick(String anime) {
        Intent intent = new Intent(this, AnimeActivity.class);
        intent.putExtra("anime", anime);
        startActivity(intent);
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {
    }

    @Override
    public void onDrawerOpened(View drawerView) {
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        if (isNewMenuItem) {
            Fragment fragment = AnimeListFragment.newInstance(getTitle().toString());
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_placeholder, fragment).commit();
        }
        isNewMenuItem = false;
    }

    @Override
    public void onDrawerStateChanged(int newState) {
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.d(TAG, "onQueryTextChange: " + newText);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchView.close(true);
        Log.d(TAG, "onQueryTextSubmit: " + query);
        isNewMenuItem = true;
        setTitle("Search Results");
        Fragment fragment = SearchFragment.newInstance(query);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_placeholder, fragment).commit();
        return false;
    }
}
