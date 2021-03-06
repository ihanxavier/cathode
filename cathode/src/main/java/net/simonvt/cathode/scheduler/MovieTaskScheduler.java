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
package net.simonvt.cathode.scheduler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.remote.action.CancelMovieCheckinTask;
import net.simonvt.cathode.remote.action.CheckInMovieTask;
import net.simonvt.cathode.remote.action.DismissMovieRecommendation;
import net.simonvt.cathode.remote.action.MovieCollectionTask;
import net.simonvt.cathode.remote.action.MovieRateTask;
import net.simonvt.cathode.remote.action.MovieWatchedTask;
import net.simonvt.cathode.remote.action.MovieWatchlistTask;
import net.simonvt.cathode.remote.sync.SyncMovieTask;

public class MovieTaskScheduler extends BaseTaskScheduler {

  public MovieTaskScheduler(Context context) {
    super(context);
  }

  /**
   * Add episodes watched outside of trakt to user library.
   *
   * @param movieId The database id of the episode.
   * @param watched Whether the episode has been watched.
   */
  public void setWatched(final long movieId, final boolean watched) {
    execute(new Runnable() {
      @Override public void run() {
        final long tmdbId = MovieWrapper.getTmdbId(context.getContentResolver(), movieId);

        MovieWrapper.setWatched(context.getContentResolver(), movieId, watched);
        if (watched) MovieWrapper.setIsInWatchlist(context.getContentResolver(), movieId, false);

        queuePriorityTask(new MovieWatchedTask(tmdbId, watched));
      }
    });
  }

  public void setIsInWatchlist(final long movieId, final boolean inWatchlist) {
    execute(new Runnable() {
      @Override public void run() {
        final long tmdbId = MovieWrapper.getTmdbId(context.getContentResolver(), movieId);

        MovieWrapper.setIsInWatchlist(context.getContentResolver(), movieId, inWatchlist);

        queuePriorityTask(new MovieWatchlistTask(tmdbId, inWatchlist));
      }
    });
  }

  public void setIsInCollection(final long movieId, final boolean inCollection) {
    execute(new Runnable() {
      @Override public void run() {
        final long tmdbId = MovieWrapper.getTmdbId(context.getContentResolver(), movieId);

        MovieWrapper.setIsInCollection(context.getContentResolver(), movieId, inCollection);

        queuePriorityTask(new MovieCollectionTask(tmdbId, inCollection));
      }
    });
  }

  /**
   * Check into a movie on trakt. Think of this method as in between a seen and a scrobble.
   * After checking in, the trakt will automatically display it as watching then switch over to
   * watched status once
   * the duration has elapsed.
   *
   * @param movieId The database id of the movie.
   */
  public void checkin(final long movieId, final String message, final boolean facebook,
      final boolean twitter, final boolean tumblr, final boolean path, final boolean prowl) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver().query(Movies.WATCHING, null, null, null, null);

        if (c.getCount() == 0) {
          ContentValues cv = new ContentValues();
          cv.put(MovieColumns.CHECKED_IN, true);
          context.getContentResolver().update(Movies.withId(movieId), cv, null, null);

          final long tmdbId = MovieWrapper.getTmdbId(context.getContentResolver(), movieId);
          queuePriorityTask(
              new CheckInMovieTask(tmdbId, message, facebook, twitter, tumblr, path, prowl));
        }
      }
    });
  }

  /**
   * Notify trakt that user wants to cancel their current check in.
   */
  public void cancelCheckin() {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver().query(Movies.WATCHING, new String[] {
            MovieColumns.TMDB_ID,
        }, null, null, null);

        if (c.moveToFirst()) {
          final long tmdbId = c.getLong(c.getColumnIndex(MovieColumns.TMDB_ID));

          ContentValues cv = new ContentValues();
          cv.put(MovieColumns.CHECKED_IN, false);
          context.getContentResolver().update(Movies.WATCHING, cv, null, null);

          queuePriorityTask(new CancelMovieCheckinTask());
          queueTask(new SyncMovieTask(tmdbId));
        }

        c.close();
      }
    });
  }

  public void dismissRecommendation(final long movieId) {
    execute(new Runnable() {
      @Override public void run() {
        final long tmdbId = MovieWrapper.getTmdbId(context.getContentResolver(), movieId);
        ContentValues cv = new ContentValues();
        cv.put(MovieColumns.RECOMMENDATION_INDEX, -1);
        context.getContentResolver().update(Movies.withId(movieId), cv, null, null);
        queue.add(new DismissMovieRecommendation(tmdbId));
      }
    });
  }

  /**
   * Rate a movie on trakt. Depending on the user settings, this will also send out social updates
   * to facebook,
   * twitter, and tumblr.
   *
   * @param movieId The database id of the movie.
   * @param rating A rating betweeo 1 and 10. Use 0 to undo rating.
   */
  public void rate(final long movieId, final int rating) {
    execute(new Runnable() {
      @Override public void run() {
        final long tmdbId = MovieWrapper.getTmdbId(context.getContentResolver(), movieId);

        ContentValues cv = new ContentValues();
        cv.put(MovieColumns.RATING, rating);
        context.getContentResolver().update(Movies.withId(movieId), cv, null, null);

        queue.add(new MovieRateTask(tmdbId, rating));
      }
    });
    // TODO:
  }
}
