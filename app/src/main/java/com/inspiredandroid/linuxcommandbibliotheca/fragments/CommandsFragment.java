package com.inspiredandroid.linuxcommandbibliotheca.fragments;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.inspiredandroid.linuxcommandbibliotheca.AboutActivity;
import com.inspiredandroid.linuxcommandbibliotheca.R;
import com.inspiredandroid.linuxcommandbibliotheca.adapter.CommandsAdapter;
import com.inspiredandroid.linuxcommandbibliotheca.fragments.dialogs.RateDialogFragment;
import com.inspiredandroid.linuxcommandbibliotheca.misc.AppManager;
import com.inspiredandroid.linuxcommandbibliotheca.misc.FragmentCoordinator;
import com.inspiredandroid.linuxcommandbibliotheca.models.Command;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Simon Schubert.
 */
public class CommandsFragment extends Fragment implements AdapterView.OnItemClickListener {

    @BindView(R.id.fragment_commands_lv)
    ListView mList;
    @BindView(R.id.fragment_commands_ll_nothingfound)
    LinearLayout mLLNothingFound;

    private CommandsAdapter mAdapter;
    private Realm mRealm;
    private String mQuery = "";
    // private FirebaseAnalytics mFirebaseAnalytics;
    // private Handler mHandler;
    private String lastTrackedQuery = "";
    private final Runnable mSearchQueryCheck = new Runnable() {
        @Override
        public void run() {
            if (!lastTrackedQuery.equals(mQuery)) {
                lastTrackedQuery = mQuery;
            } else {
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM, mQuery);
                // mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle);
            }

            // mHandler.postDelayed(this, 1000);
        }
    };

    public CommandsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        mRealm = Realm.getDefaultInstance();
        mAdapter = new CommandsAdapter(getContext(), getAllCommands(), false);

        if (AppManager.shouldShowRateDialog(getContext())) {
            RateDialogFragment rateDialogFragment = RateDialogFragment.getInstance();
            rateDialogFragment.show(getChildFragmentManager(), RateDialogFragment.class.getName());
        }

        // mFirebaseAnalytics = FirebaseAnalytics.getInstance(getContext());

        // mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_commands, container, false);

        ButterKnife.bind(this, view);

        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();

        // mHandler.removeCallbacks(mSearchQueryCheck);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FragmentCoordinator.startCommandManActivity(getActivity(), id);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);

        MenuItem item = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if (!isAdded()) {
                    return true;
                }
                mQuery = query;
                if (query.length() > 0) {
                    String normalizedText = Normalizer.normalize(query, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
                    search(normalizedText);
                } else {
                    resetSearchResults();
                }

                return true;
            }
        });
        MenuItemCompat.setOnActionExpandListener(item, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                resetSearchResults();
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.about) {
            startAboutActivity();
            return true;
        }
        return false;
    }

    private void startAboutActivity() {
        Intent intent = new Intent(getContext(), AboutActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mRealm.close();
    }

    @Override
    public void onResume() {
        super.onResume();

        // mHandler.postDelayed(mSearchQueryCheck, 1000);

        if (AppManager.hasBookmarkChanged(getContext())) {
            resetSearchResults();
        }
    }

    @OnClick(R.id.fragment_commands_btn_send_request)
    public void sendCommandRequestEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + "sschubert89@gmail.com"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Command request");
        intent.putExtra(Intent.EXTRA_TEXT, "Command: " + mQuery);
        try {
            startActivity(Intent.createChooser(intent, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ignored) {
        }
    }

    /**
     * reset mAdapter entries
     */
    private void resetSearchResults() {
        mAdapter.updateRealmResults(getAllCommands());
        mAdapter.setSearchQuery("");
        mAdapter.updateBookmarkIds();
        updateViews();
    }

    /**
     * Get list of all commands sorted by name
     *
     * @return
     */
    private List<RealmResults<Command>> getAllCommands() {
        List<RealmResults<Command>> results = new ArrayList<>();
        List<Long> ids = AppManager.getBookmarkIds(getContext());
        for (long id : ids) {
            results.add(mRealm.where(Command.class).equalTo(Command.ID, id).findAll());
        }
        results.add(mRealm.where(Command.class).findAll().sort("name"));
        return results;
    }

    /**
     * search for query in all command names and short descriptions and update adapter
     *
     * @param query search query
     */
    private void search(String query) {
        List<RealmResults<Command>> results = new ArrayList<>();
        results.add(mRealm.where(Command.class).equalTo(Command.NAME, query).findAll());
        results.add(mRealm.where(Command.class).beginsWith(Command.NAME, query).notEqualTo(Command.NAME, query).findAll());
        results.add(mRealm.where(Command.class).contains(Command.NAME, query).not().beginsWith(Command.NAME, query).notEqualTo(Command.NAME, query).findAll());
        results.add(mRealm.where(Command.class).contains(Command.DESCRIPTION, query).not().contains(Command.NAME, query).findAll());

        mAdapter.updateRealmResults(results);
        mAdapter.setSearchQuery(query);

        updateViews();
    }

    private void updateViews() {
        if (mAdapter.getCount() == 0) {
            mLLNothingFound.setVisibility(View.VISIBLE);
            mList.setVisibility(View.GONE);
        } else {
            mLLNothingFound.setVisibility(View.GONE);
            mList.setVisibility(View.VISIBLE);
        }
    }
}
