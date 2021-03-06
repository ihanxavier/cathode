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
package net.simonvt.cathode.remote.action;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.MoviesBody;
import net.simonvt.cathode.api.entity.Response;
import net.simonvt.cathode.api.service.MovieService;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.remote.TraktTask;

public class MovieWatchedTask extends TraktTask {

  @Inject transient MovieService movieService;

  private long tmdbId;

  private boolean watched;

  public MovieWatchedTask(long tmdbId, boolean watched) {
    this.tmdbId = tmdbId;
    this.watched = watched;
  }

  @Override protected void doTask() {
    if (watched) {
      Response response = movieService.seen(new MoviesBody(tmdbId));
    } else {
      Response response = movieService.unseen(new MoviesBody(tmdbId));
    }

    MovieWrapper.setWatched(getContentResolver(), tmdbId, watched);
    postOnSuccess();
  }
}
