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

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.service.UserService;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.remote.TraktTask;

public class SyncMoviesWatchlistTask extends TraktTask {

  @Inject transient UserService userService;

  @Override protected void doTask() {
    Cursor c = getContentResolver().query(Movies.MOVIES, new String[] {
        MovieColumns.ID,
    }, MovieColumns.IN_WATCHLIST, null, null);

    List<Long> movieIds = new ArrayList<Long>();

    while (c.moveToNext()) {
      movieIds.add(c.getLong(c.getColumnIndex(MovieColumns.ID)));
    }
    c.close();

    List<Movie> movies = userService.watchlistMovies();

    for (Movie movie : movies) {
      if (movie.getTmdbId() == null) {
        continue;
      }
      final long tmdbId = movie.getTmdbId();
      final long movieId = MovieWrapper.getMovieId(getContentResolver(), tmdbId);

      if (movieId == -1) {
        queueTask(new SyncMovieTask(tmdbId));
      } else {
        if (!movieIds.remove(movieId)) {
          MovieWrapper.setIsInWatchlist(getContentResolver(), movieId, true);
        }
      }
    }

    for (Long movieId : movieIds) {
      MovieWrapper.setIsInWatchlist(getContentResolver(), movieId, false);
    }

    postOnSuccess();
  }
}
