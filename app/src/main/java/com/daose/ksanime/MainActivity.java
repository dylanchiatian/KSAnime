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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.applovin.nativeAds.AppLovinNativeAd;
import com.daose.ksanime.adapter.SearchAdapter;
import com.daose.ksanime.fragment.AnimeListFragment;
import com.daose.ksanime.fragment.SearchFragment;
import com.daose.ksanime.fragment.SettingsFragment;
import com.daose.ksanime.model.Anime;
import com.lapism.searchview.SearchView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        AnimeListFragment.OnFragmentInteractionListener,
        SearchFragment.OnFragmentInteractionListener,
        DrawerLayout.DrawerListener,
        SearchView.OnQueryTextListener,
        SearchView.OnOpenCloseListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private boolean isNewMenuItem = false;
    private DrawerLayout drawer;
    private SearchView searchView;
    private ArrayList<String> searchList;
    private ImageView headerImage;
    public static List<AppLovinNativeAd> nativeAds;

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
        headerImage = (ImageView) navigationView.getHeaderView(0);
        Picasso.with(this).load(R.drawable.icon_landscape).fit().centerInside().into(headerImage);
        navigationView.getMenu().getItem(0).setChecked(true);
        setTitle(navigationView.getMenu().getItem(0).getTitle());
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupSearchView() {
        searchList = new ArrayList<String>();
        searchView = (SearchView) findViewById(R.id.search_view);
        if (searchView != null) {
            searchView.setVersion(SearchView.VERSION_MENU_ITEM);
            searchView.setVersionMargins(SearchView.VERSION_MARGINS_MENU_ITEM);
            searchView.setTextSize(16);
            searchView.setHint("Search");
            searchView.setDivider(false);
            searchView.setVoice(false);
            searchView.setAdapter(new SearchAdapter(this, searchList));
            searchView.setTheme(SearchView.THEME_DARK);
            searchView.setAnimationDuration(SearchView.ANIMATION_DURATION);
            searchView.setShadowColor(ContextCompat.getColor(this, R.color.search_shadow_layout));
            searchView.setOnQueryTextListener(this);
            searchView.setOnOpenCloseListener(this);
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
        isNewMenuItem = false;
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        if (isNewMenuItem) {
            Fragment fragment;
            if (getTitle().equals("Settings")) {
                fragment = SettingsFragment.newInstance();
            } else {
                fragment = AnimeListFragment.newInstance(getTitle().toString());
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_placeholder, fragment).commit();
        }
        isNewMenuItem = false;
    }

    @Override
    public void onDrawerStateChanged(int newState) {
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText.length() < 1) {
            int items = searchList.size();
            searchList.clear();
            searchView.getAdapter().notifyItemRangeRemoved(0, items);
            return false;
        }
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Anime> queryResult = realm.where(Anime.class).beginsWith("title", newText, Case.INSENSITIVE).findAll();
        int searchSize = searchList.size();
        int querySize = queryResult.size();
        int minSize = (searchSize > querySize) ? querySize : searchSize;
        for (int i = 0; i < minSize; i++) {
            if (!searchList.get(i).equals(queryResult.get(i).title)) {
                searchList.set(i, queryResult.get(i).title);
                searchView.getAdapter().notifyItemChanged(i);
            }
        }
        if (querySize > searchSize) {
            for (int i = minSize; i < querySize; i++) {
                searchList.add(queryResult.get(i).title);
            }
            searchView.getAdapter().notifyItemRangeInserted(minSize, querySize - minSize);
        } else {
            for (int i = minSize; i < searchSize; i++) {
                searchList.remove(minSize);
            }
            searchView.getAdapter().notifyItemRangeRemoved(minSize, searchSize - minSize);
        }
        realm.close();
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchView.close(true);
        isNewMenuItem = true;
        setTitle("Search Results");
        Fragment fragment = SearchFragment.newInstance(query);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_placeholder, fragment).commit();
        return false;
    }

    @Override
    public void onClose() {
        searchList.clear();
    }

    @Override
    public void onOpen() {
        searchList.clear();
    }
}
