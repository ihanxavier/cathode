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
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.action.CancelShowCheckinTask;
import net.simonvt.cathode.remote.action.DismissShowRecommendation;
import net.simonvt.cathode.remote.action.EpisodeCollectionTask;
import net.simonvt.cathode.remote.action.EpisodeWatchedTask;
import net.simonvt.cathode.remote.action.ShowCollectionTask;
import net.simonvt.cathode.remote.action.ShowRateTask;
import net.simonvt.cathode.remote.action.ShowWatchedTask;
import net.simonvt.cathode.remote.action.ShowWatchlistTask;
import net.simonvt.cathode.remote.sync.SyncShowTask;

public class ShowTaskScheduler extends BaseTaskScheduler {

  @Inject EpisodeTaskScheduler episodeScheduler;

  public ShowTaskScheduler(Context context) {
    super(context);
    CathodeApp.inject(context, this);
  }

  /**
   * Sync data for show with Trakt.
   *
   * @param showId The database id of the show.
   */
  public void sync(final long showId) {
    execute(new Runnable() {
      @Override public void run() {
        ContentValues cv = new ContentValues();
        cv.put(ShowColumns.FULL_SYNC_REQUESTED, System.currentTimeMillis());
        context.getContentResolver().update(Shows.withId(showId), cv, null, null);
        final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
        queueTask(new SyncShowTask(tvdbId));
      }
    });
  }

  public void watchedNext(final long showId) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver().query(Episodes.fromShow(showId), new String[] {
                EpisodeColumns.ID, EpisodeColumns.SEASON, EpisodeColumns.EPISODE,
            }, "watched=0 AND season<>0", null,
            EpisodeColumns.SEASON + " ASC, " + EpisodeColumns.EPISODE + " ASC LIMIT 1"
        );

        if (c.moveToNext()) {
          final long episodeId = c.getLong(c.getColumnIndexOrThrow(EpisodeColumns.ID));
          episodeScheduler.setWatched(episodeId, true);

          final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
          final int season = c.getInt(c.getColumnIndexOrThrow(EpisodeColumns.SEASON));
          final int number = c.getInt(c.getColumnIndexOrThrow(EpisodeColumns.EPISODE));
          queuePriorityTask(new EpisodeWatchedTask(tvdbId, season, number, true));
        }

        c.close();
      }
    });
  }

  //public void checkinNext(final long showId) {
  //  execute(new Runnable() {
  //    @Override public void run() {
  //      Cursor c = context.getContentResolver()
  //          .query(CathodeContract.EpisodeColumns.buildFromShowId(showId), new String[] {
  //              CathodeContract.EpisodeColumns.ID, CathodeContract.EpisodeColumns.SEASON,
  //              CathodeContract.EpisodeColumns.EPISODE,
  //          }, "watched=0 AND season<>0", null, CathodeContract.EpisodeColumns.SEASON
  //              + " ASC, "
  //              + CathodeContract.EpisodeColumns.EPISODE
  //              + " ASC LIMIT 1");
  //
  //      if (c.moveToNext()) {
  //        final long episodeId = c.getLong(c.getColumnIndexOrThrow(CathodeContract.EpisodeColumns.ID));
  //        episodeScheduler.checkin(episodeId);
  //      }
  //
  //      c.close();
  //    }
  //  });
  //}

  public void cancelCheckin() {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver()
            .query(Episodes.EPISODES, null, EpisodeColumns.CHECKED_IN + "=1", null, null);
        if (c.moveToNext()) {
          final long showId = c.getLong(c.getColumnIndex(EpisodeColumns.SHOW_ID));
          final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);

          ContentValues cv = new ContentValues();
          cv.put(EpisodeColumns.CHECKED_IN, false);
          context.getContentResolver().update(Episodes.EPISODE_WATCHING, cv, null, null);

          queuePriorityTask(new CancelShowCheckinTask());
          queueTask(new SyncShowTask(tvdbId));
        }
        c.close();
      }
    });
  }

  public void collectedNext(final long showId) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver().query(Episodes.fromShow(showId), new String[] {
                EpisodeColumns.ID, EpisodeColumns.SEASON, EpisodeColumns.EPISODE,
            }, "inCollection=0 AND season<>0", null,
            EpisodeColumns.SEASON + " ASC, " + EpisodeColumns.EPISODE + " ASC LIMIT 1"
        );

        if (c.moveToNext()) {
          final long episodeId = c.getLong(c.getColumnIndexOrThrow(EpisodeColumns.ID));
          episodeScheduler.setIsInCollection(episodeId, true);

          final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
          final int season = c.getInt(c.getColumnIndexOrThrow(EpisodeColumns.SEASON));
          final int number = c.getInt(c.getColumnIndexOrThrow(EpisodeColumns.EPISODE));
          queuePriorityTask(new EpisodeCollectionTask(tvdbId, season, number, true));
        }

        c.close();
      }
    });
  }

  public void setWatched(final long showId, final boolean watched) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver().query(Shows.withId(showId), new String[] {
            ShowColumns.TVDB_ID,
        }, null, null, null);

        if (c.moveToFirst()) {
          final int tvdbId = c.getInt(c.getColumnIndex(ShowColumns.TVDB_ID));
          ShowWrapper.setWatched(context.getContentResolver(), showId, watched);
          queue.add(new ShowWatchedTask(tvdbId, watched));
        }

        c.close();
      }
    });
  }

  public void setIsInWatchlist(final long showId, final boolean inWatchlist) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver().query(Shows.withId(showId), new String[] {
            ShowColumns.TVDB_ID, ShowColumns.EPISODE_COUNT,
        }, null, null, null);

        if (c.moveToFirst()) {
          final int tvdbId = c.getInt(c.getColumnIndex(ShowColumns.TVDB_ID));
          ShowWrapper.setIsInWatchlist(context.getContentResolver(), showId, inWatchlist);
          queue.add(new ShowWatchlistTask(tvdbId, inWatchlist));

          final int episodeCount = c.getInt(c.getColumnIndex(ShowColumns.EPISODE_COUNT));
          if (episodeCount == 0) {
            queueTask(new SyncShowTask(tvdbId));
          }
        }

        c.close();
      }
    });
  }

  public void setIsInCollection(final long showId, final boolean inCollection) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver().query(Shows.withId(showId), new String[] {
            ShowColumns.TVDB_ID,
        }, null, null, null);

        if (c.moveToFirst()) {
          final int tvdbId = c.getInt(c.getColumnIndex(ShowColumns.TVDB_ID));
          ShowWrapper.setIsInCollection(context.getContentResolver(), showId, inCollection);
          queue.add(new ShowCollectionTask(tvdbId, inCollection));
        }

        c.close();
      }
    });
  }

  public void setIsHidden(final long showId, final boolean isHidden) {
    execute(new Runnable() {
      @Override public void run() {
        ShowWrapper.setIsHidden(context.getContentResolver(), showId, isHidden);
      }
    });
  }

  public void dismissRecommendation(final long showId) {
    execute(new Runnable() {
      @Override public void run() {
        final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);

        ContentValues cv = new ContentValues();
        cv.put(ShowColumns.RECOMMENDATION_INDEX, -1);
        context.getContentResolver().update(Shows.withId(showId), cv, null, null);

        queue.add(new DismissShowRecommendation(tvdbId));
      }
    });
  }

  /**
   * Rate a show on trakt. Depending on the user settings, this will also send out social updates
   * to facebook,
   * twitter, and tumblr.
   *
   * @param showId The database id of the show.
   * @param rating A rating betweeo 1 and 10. Use 0 to undo rating.
   */
  public void rate(final long showId, final int rating) {
    execute(new Runnable() {
      @Override public void run() {
        final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);

        ContentValues cv = new ContentValues();
        cv.put(ShowColumns.RATING, rating);
        context.getContentResolver().update(Shows.withId(showId), cv, null, null);

        queue.add(new ShowRateTask(tvdbId, rating));
      }
    });
    // TODO:
  }
}
