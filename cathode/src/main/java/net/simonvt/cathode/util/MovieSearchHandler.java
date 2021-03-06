/*
 * Copyright (C) 2013 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonvt.cathode.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.service.SearchService;
import net.simonvt.cathode.event.MovieSearchResult;
import net.simonvt.cathode.event.SearchFailureEvent;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.sync.SyncMovieTask;
import retrofit.RetrofitError;

public class MovieSearchHandler {

  private static final String TAG = "MovieSearchHandler";

  private Context context;

  private Bus bus;

  public static List<Long> movieIds;

  private SearchThread thread;

  public MovieSearchHandler(Context context, Bus bus) {
    this.context = context;
    this.bus = bus;
    bus.register(this);
  }

  @Produce public MovieSearchResult produceSearchResult() {
    if (movieIds != null) {
      return new MovieSearchResult(movieIds);
    }

    return null;
  }

  public boolean isSearching() {
    return thread != null;
  }

  public void deliverResult(List<Long> movieIds) {
    MovieSearchHandler.movieIds = movieIds;
    bus.post(new MovieSearchResult(movieIds));
  }

  public void deliverFailure() {
    bus.post(new SearchFailureEvent(SearchFailureEvent.Type.MOVIE));
  }

  public void search(final String query) {
    movieIds = null;

    if (thread != null) {
      thread.unregister();
    }
    thread = new SearchThread(context, query);
    thread.start();
  }

  public static final class SearchThread extends Thread {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    @Inject MovieSearchHandler handler;

    @Inject SearchService searchService;

    @Inject TraktTaskQueue queue;

    private Context context;

    private String query;

    private SearchThread(Context context, String query) {
      this.context = context;
      this.query = query;

      CathodeApp.inject(context, this);
    }

    public void unregister() {
      handler = null;
    }

    @Override public void run() {
      try {
        List<Movie> movies = searchService.movies(query);

        final List<Long> movieIds = new ArrayList<Long>(movies.size());

        for (Movie movie : movies) {
          if (!TextUtils.isEmpty(movie.getTitle())) {
            final boolean exists =
                MovieWrapper.exists(context.getContentResolver(), movie.getTmdbId());

            final long movieId =
                MovieWrapper.updateOrInsertMovie(context.getContentResolver(), movie);
            movieIds.add(movieId);

            if (!exists) queue.add(new SyncMovieTask(movie.getTmdbId()));
          }
        }

        MAIN_HANDLER.post(new Runnable() {
          @Override public void run() {
            if (handler != null) handler.deliverResult(movieIds);
          }
        });
      } catch (RetrofitError e) {
        e.printStackTrace();
        MAIN_HANDLER.post(new Runnable() {
          @Override public void run() {
            if (handler != null) handler.deliverFailure();
          }
        });
      }
    }
  }
}
