package net.simonvt.trakt.ui.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import javax.inject.Inject;
import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.remote.TraktTaskQueue;
import net.simonvt.trakt.remote.sync.SyncTask;
import net.simonvt.trakt.scheduler.EpisodeTaskScheduler;
import net.simonvt.trakt.ui.LibraryType;
import net.simonvt.trakt.ui.ShowsNavigationListener;
import net.simonvt.trakt.ui.adapter.ShowsAdapter;
import net.simonvt.trakt.util.LogWrapper;

public abstract class ShowsFragment extends AbsAdapterFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

  private static final String TAG = "ShowsFragment";

  @Inject EpisodeTaskScheduler episodeScheduler;

  @Inject TraktTaskQueue queue;

  private ShowsAdapter showsAdapter;

  private ShowsNavigationListener navigationListener;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      navigationListener = (ShowsNavigationListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + " must implement ShowsNavigationListener");
    }
  }

  @Override
  public void onCreate(Bundle inState) {
    super.onCreate(inState);
    TraktApp.inject(getActivity(), this);

    setHasOptionsMenu(true);

    getLoaderManager().initLoader(getLoaderId(), null, this);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
  }

  @Override
  public void onDestroy() {
    if (getActivity().isFinishing() || isRemoving()) {
      getLoaderManager().destroyLoader(getLoaderId());
    }
    super.onDestroy();
  }

  @Override
  public void onDetach() {
    super.onDetach();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.fragment_shows, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_refresh:
        queue.add(new SyncTask());
        return true;

      case R.id.menu_search:
        navigationListener.onStartShowSearch();
        return true;

      default:
        return false;
    }
  }

  @Override
  protected void onItemClick(AdapterView l, View v, int position, long id) {
    Cursor c = (Cursor) getAdapter().getItem(position);
    navigationListener.onDisplayShow(id, c.getString(c.getColumnIndex(TraktContract.Shows.TITLE)),
        getLibraryType());
  }

  private void setCursor(Cursor cursor) {
    if (showsAdapter == null) {
      showsAdapter = new ShowsAdapter(getActivity(), cursor, getLibraryType());
      setAdapter(showsAdapter);
      return;
    }

    showsAdapter.changeCursor(cursor);
  }

  protected abstract LibraryType getLibraryType();

  protected abstract int getLoaderId();

  @Override
  public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
    LogWrapper.d(TAG, "[onLoadFinished]");
    setCursor(cursor);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> cursorLoader) {
    showsAdapter.changeCursor(null);
  }
}
