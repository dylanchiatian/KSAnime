package com.daose.anime;

import android.content.Intent;
import android.graphics.Color;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.daose.anime.Adapter.AnimeAdapter;
import com.daose.anime.Anime.Anime;
import com.daose.anime.Anime.AnimeList;
import com.daose.anime.web.Browser;
import com.daose.anime.web.HtmlListener;
import com.daose.anime.widgets.AutofitRecyclerView;
import com.gigamole.navigationtabbar.ntb.NavigationTabBar;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmResults;

public class HomeActivity extends AppCompatActivity implements TextView.OnEditorActionListener {

    private static final String TAG = HomeActivity.class.getSimpleName();
    private Realm realm;

    //TODO:: get rid of splash activity and just load here
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setupDatabase();
        initUI();
        //hotList = (ArrayList<Anime>) getIntent().getSerializableExtra("hotList");
        //popularList = (ArrayList<Anime>) getIntent().getSerializableExtra("popularList");
    }

    private void setupDatabase() {
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            search(v.getText().toString());
        }
        return false;
    }

    private void search(String query) {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("query", query);
        startActivity(intent);
    }

    private void initUI() {
        EditText search = (EditText) findViewById(R.id.search);
        search.setOnEditorActionListener(this);
        final ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        if (viewPager == null) return;
        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return 3;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view.equals(object);
            }

            @Override
            public void destroyItem(final View container, final int position, final Object object) {
                ((ViewPager) container).removeView((View) object);
            }

            @Override
            public Object instantiateItem(final ViewGroup container, final int position) {
                final View view = LayoutInflater.from(getBaseContext()).inflate(R.layout.anime_list, null, false);
                final AutofitRecyclerView rv = (AutofitRecyclerView) view.findViewById(R.id.recycler_view);
                rv.setHasFixedSize(true);

                switch (position) {
                    case 0:
                        //TODO:: replace with favourite list
                        rv.setAdapter(new AnimeAdapter(getBaseContext(), new RealmList<Anime>()));
                        break;
                    case 1:
                        rv.setAdapter(new AnimeAdapter(getBaseContext(), realm.where(AnimeList.class).equalTo("key", "hotList").findFirst().animeList));
                        break;
                    case 2:
                        rv.setAdapter(new AnimeAdapter(getBaseContext(), realm.where(AnimeList.class).equalTo("key", "popularList").findFirst().animeList));
                        break;
                    default:
                        rv.setAdapter(new AnimeAdapter(getBaseContext(), new RealmList<Anime>()));
                        break;
                }
                container.addView(view);
                return view;
            }
        });

        final String[] colors = getResources().getStringArray(R.array.nav_colors);

        final NavigationTabBar ntb = (NavigationTabBar) findViewById(R.id.ntb);
        assert ntb != null;
        final ArrayList<NavigationTabBar.Model> models = new ArrayList<NavigationTabBar.Model>();
        models.add(
                new NavigationTabBar.Model.Builder(
                        getResources().getDrawable(R.drawable.ic_star_border_black_24dp),
                        Color.parseColor(colors[0]))
                        .title("Starred")
                        .build()
        );
        models.add(
                new NavigationTabBar.Model.Builder(
                        getResources().getDrawable(R.drawable.ic_whatshot_black_24dp),
                        Color.parseColor(colors[1]))
                        .title("Hot")
                        .build()
        );
        models.add(
                new NavigationTabBar.Model.Builder(
                        getResources().getDrawable(R.drawable.ic_trending_up_black_24dp),
                        Color.parseColor(colors[2]))
                        .title("Popular")
                        .build()
        );

        ntb.setModels(models);
        ntb.setViewPager(viewPager, 1);
    }
}
