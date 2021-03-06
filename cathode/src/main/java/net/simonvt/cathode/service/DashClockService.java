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
package net.simonvt.cathode.service;

import android.database.Cursor;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.util.DateUtils;

public class DashClockService extends DashClockExtension {

  @Override protected void onUpdateData(int reason) {
    Cursor c = getContentResolver().query(Episodes.EPISODES, null,
        EpisodeColumns.FIRST_AIRED + ">? AND "
        + EpisodeColumns.WATCHED + "=0"
        + " AND ((SELECT " + DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.WATCHED_COUNT
        + " FROM " + DatabaseSchematic.Tables.SHOWS + " WHERE "
        + DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.ID + "="
        + DatabaseSchematic.Tables.EPISODES + "." + EpisodeColumns.SHOW_ID
        + ")>0 OR " + EpisodeColumns.IN_WATCHLIST + "=1 OR "
        + "(SELECT " + DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.IN_WATCHLIST
        + " FROM " + DatabaseSchematic.Tables.SHOWS + " WHERE "
        + DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.ID + "="
        + DatabaseSchematic.Tables.EPISODES + "." + EpisodeColumns.SHOW_ID
        + ")=1)",
        new String[] {
            String.valueOf(System.currentTimeMillis()),
        }, EpisodeColumns.FIRST_AIRED + " ASC LIMIT 1");

    if (c.moveToFirst()) {
      final int episode = c.getInt(c.getColumnIndex(EpisodeColumns.EPISODE));
      final int season = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON));
      final String title = c.getString(c.getColumnIndex(EpisodeColumns.TITLE));
      final long showId = c.getLong(c.getColumnIndex(EpisodeColumns.SHOW_ID));
      final long firstAired = c.getLong(c.getColumnIndex(EpisodeColumns.FIRST_AIRED));

      final String date = DateUtils.millisToString(this, firstAired, false);

      Cursor show =
          getContentResolver().query(Shows.withId(showId), null, null, null,
              null);
      if (!show.moveToFirst()) {
        show.close();
        return; // Wat
      }
      final String showTitle = show.getString(show.getColumnIndex(ShowColumns.TITLE));
      show.close();

      ExtensionData data = new ExtensionData().visible(true)
          .status(date)
          .expandedTitle(showTitle + " - " + date)
          .expandedBody(season + "x" + episode + " - " + title);

      publishUpdate(data);
    } else {
      publishUpdate(new ExtensionData().visible(false));
    }
    c.close();
  }
}
