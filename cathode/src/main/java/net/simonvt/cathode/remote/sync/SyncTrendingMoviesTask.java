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
package net.simonvt.cathode.remote.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.service.MoviesService;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.remote.TraktTask;
import timber.log.Timber;

public class SyncTrendingMoviesTask extends TraktTask {

  @Inject transient MoviesService moviesService;

  @Override protected void doTask() {
    try {
      ContentResolver resolver = getContentResolver();

      List<Movie> movies = moviesService.trending();

      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
      Cursor c = resolver.query(Movies.TRENDING, null, null, null, null);
      while (c.moveToNext()) {
        final long movieId = c.getLong(c.getColumnIndex(MovieColumns.ID));
        ContentValues cv = new ContentValues();
        cv.put(MovieColumns.TRENDING_INDEX, -1);
        ContentProviderOperation op =
            ContentProviderOperation.newUpdate(Movies.withId(movieId)).withValues(cv).build();
        ops.add(op);
      }
      c.close();

      for (int i = 0, count = Math.min(movies.size(), 25); i < count; i++) {
        Movie movie = movies.get(i);
        if (movie.getTmdbId() == null) {
          continue;
        }
        long movieId = MovieWrapper.getMovieId(resolver, movie.getTmdbId());
        if (movieId == -1L) {
          queueTask(new SyncMovieTask(movie.getTmdbId()));
          movieId = MovieWrapper.insertMovie(resolver, movie);
        }

        ContentValues cv = new ContentValues();
        cv.put(MovieColumns.TRENDING_INDEX, i);
        ContentProviderOperation op =
            ContentProviderOperation.newUpdate(Movies.withId(movieId)).withValues(cv).build();
        ops.add(op);
      }

      resolver.applyBatch(BuildConfig.PROVIDER_AUTHORITY, ops);
      postOnSuccess();
    } catch (RemoteException e) {
      Timber.e(e, "SyncTrendingMoviesTask failed");
      postOnFailure();
    } catch (OperationApplicationException e) {
      Timber.e(e, "SyncTrendingMoviesTask failed");
      postOnFailure();
    }
  }
}
