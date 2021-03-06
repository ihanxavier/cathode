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
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.service.RecommendationsService;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import timber.log.Timber;

public class SyncShowRecommendations extends TraktTask {

  @Inject transient RecommendationsService recommendationsService;

  @Override protected void doTask() {
    try {
      ContentResolver resolver = getContentResolver();

      List<TvShow> shows = recommendationsService.shows();
      List<Long> showIds = new ArrayList<Long>();
      List<Integer> showSummaries = new ArrayList<Integer>();

      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
      Cursor c = resolver.query(Shows.SHOWS_RECOMMENDED, null, null, null, null);
      while (c.moveToNext()) {
        showIds.add(c.getLong(c.getColumnIndex(ShowColumns.ID)));
      }
      c.close();

      for (int index = 0, count = Math.min(shows.size(), 25); index < count; index++) {
        TvShow show = shows.get(index);
        if (show.getTvdbId() == null) {
          continue;
        }
        long showId = ShowWrapper.getShowId(resolver, show);
        if (showId == -1L) {
          showId = ShowWrapper.insertShow(resolver, show);
        }

        showIds.remove(showId);

        ContentProviderOperation op = ContentProviderOperation.newUpdate(Shows.withId(showId))
            .withValue(ShowColumns.RECOMMENDATION_INDEX, index)
            .build();
        ops.add(op);
      }

      for (Long id : showIds) {
        ContentProviderOperation op = ContentProviderOperation.newUpdate(Shows.withId(id))
            .withValue(ShowColumns.RECOMMENDATION_INDEX, -1)
            .build();
        ops.add(op);
      }

      resolver.applyBatch(BuildConfig.PROVIDER_AUTHORITY, ops);
      postOnSuccess();
    } catch (RemoteException e) {
      Timber.e(e, "SyncShowRecommendationsTask failed");
      postOnFailure();
    } catch (OperationApplicationException e) {
      Timber.e(e, "SyncShowRecommendationsTask failed");
      postOnFailure();
    }
  }
}
