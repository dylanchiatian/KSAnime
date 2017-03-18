package com.daose.ksanime;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.daose.ksanime.adapter.SearchAdapter;
import com.daose.ksanime.fragment.AnimeListFragment;
import com.daose.ksanime.fragment.DownloadFragment;
import com.daose.ksanime.fragment.HomeFragment;
import com.daose.ksanime.fragment.NewsFragment;
import com.daose.ksanime.fragment.SearchFragment;
import com.daose.ksanime.fragment.SettingsFragment;
import com.daose.ksanime.model.Anime;
import com.daose.ksanime.util.Utils;
import com.daose.ksanime.web.Browser;
import com.daose.ksanime.web.HtmlListener;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.lapism.searchview.SearchView;

import java.util.ArrayList;
import java.util.List;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        AnimeListFragment.OnFragmentInteractionListener,
        SearchFragment.OnFragmentInteractionListener,
        DownloadFragment.OnFragmentInteractionListener,
        HomeFragment.OnFragmentInteractionListener,
        NewsFragment.OnFragmentInteractionListener,
        DrawerLayout.DrawerListener,
        SearchView.OnQueryTextListener,
        SearchView.OnOpenCloseListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean isNewMenuItem = false;
    private ArrayList<String> searchList;

    // UI
    private DrawerLayout drawer;
    private SearchView searchView;

    // Chromecast
    private CastSession mCastSession;
    private SessionManagerListener<CastSession> mSessionManagerListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupUI();
        displayAlert();
        mountView();
        setupChromecast();
    }

    /** SETUP UI **/
    private void setupUI() {
        setupToolbar();
        setupDrawer();
        setupNavigationView();
        setupSearchView();
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationContentDescription(getResources().getString(R.string.app_name));
        toolbar.setNavigationIcon(R.drawable.ic_menu_black_24dp);
        toolbar.setTranslationY(toolbar.getHeight());
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

    /**
     * Alerts that need to display on start can be shown here
     */
    private void displayAlert() {}

    private void mountView() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if(getIntent().getAction().equals(AnimeListFragment.Type.Starred.name())) {
            ft.replace(R.id.fragment_placeholder, AnimeListFragment.newInstance(getIntent().getAction()));
            setTitle(getIntent().getAction());
        } else {
            ft.replace(R.id.fragment_placeholder, HomeFragment.newInstance());
        }
        ft.commit();
    }

    /** TOOLBAR AND NAVBAR **/
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
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        isNewMenuItem = true;
        setTitle(item.getTitle());
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
            String title = getTitle().toString();
            switch (title) {
                case "Settings":
                    fragment = SettingsFragment.newInstance();
                    break;
                case "Downloaded":
                    fragment = DownloadFragment.newInstance();
                    break;
                case "Home":
                    fragment = HomeFragment.newInstance();
                    break;
                case "News":
                    fragment = NewsFragment.newInstance();
                    break;
                default:
                    fragment = AnimeListFragment.newInstance(title);
                    break;
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_placeholder, fragment).commit();
        }
        isNewMenuItem = false;
    }

    @Override
    public void onDrawerStateChanged(int newState) {
    }

    /** SEARCH **/
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
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_placeholder, fragment).addToBackStack(null).commit();
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

    /** CALLBACKS **/
    @Override
    public void onAnimeClick(String anime) {
        Intent intent = new Intent(this, AnimeActivity.class);
        intent.putExtra("anime", anime);
        startActivity(intent);
    }

    @Override
    public void onNewsItemClick(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    @Override
    public void onShowMore(String title) {
        setTitle(title);
        Fragment fragment = AnimeListFragment.newInstance(title);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_placeholder, fragment).addToBackStack(null).commit();
    }

    @Override
    public void onVideoClick(String path) {
        Uri uri = Uri.parse("file://" + path);
        Intent extIntent = new Intent(Intent.ACTION_VIEW, uri);
        extIntent.setDataAndType(uri, "video/mp4");

        if(extIntent.resolveActivity(getPackageManager()) != null &&
                !getSharedPreferences(getString(R.string.shared_preferences_key), Context.MODE_PRIVATE).getBoolean(getString(R.string.use_internal_player), false)) {
            startActivity(extIntent);
        } else {
            Intent intent = new Intent(this, FullScreenVideoPlayerActivity.class);
            intent.putExtra("url", path);
            startActivity(intent);
        }
    }

    /** CHROMECAST **/
    private void setupCastListener() {
        mSessionManagerListener = new SessionManagerListener<CastSession>() {

            @Override
            public void onSessionEnded(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionResumed(CastSession session, boolean wasSuspended) {
                onApplicationConnected(session);
            }

            @Override
            public void onSessionResumeFailed(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarted(CastSession session, String sessionId) {
                onApplicationConnected(session);
            }

            @Override
            public void onSessionStartFailed(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarting(CastSession session) {
            }

            @Override
            public void onSessionEnding(CastSession session) {
            }

            @Override
            public void onSessionResuming(CastSession session, String sessionId) {
            }

            @Override
            public void onSessionSuspended(CastSession session, int reason) {
            }

            private void onApplicationConnected(CastSession castSession) {
                mCastSession = castSession;
                invalidateOptionsMenu();
            }

            private void onApplicationDisconnected() {
                invalidateOptionsMenu();
            }
        };
    }

    private void setupChromecast() {
        CastContext castContext = CastContext.getSharedInstance(this);
        setupCastListener();
        castContext.getSessionManager().addSessionManagerListener(mSessionManagerListener, CastSession.class);
        mCastSession = castContext.getSessionManager().getCurrentCastSession();
    }

}
