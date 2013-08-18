package net.simonvt.cathode.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import java.util.List;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.Person;
import net.simonvt.cathode.provider.CathodeContract.Movies;
import net.simonvt.cathode.util.ApiUtils;

public final class MovieWrapper {

  private static final String TAG = "MovieWrapper";

  private MovieWrapper() {
  }

  public static long getTmdbId(ContentResolver resolver, long movieId) {
    Cursor c = resolver.query(Movies.buildMovieUri(movieId), new String[] {
        CathodeContract.MovieColumns.TMDB_ID,
    }, null, null, null);

    int tmdbId = -1;
    if (c.moveToFirst()) {
      tmdbId = c.getInt(c.getColumnIndex(CathodeContract.MovieColumns.TMDB_ID));
    }

    c.close();

    return tmdbId;
  }

  public static long getMovieId(ContentResolver resolver, long tmdbId) {
    Cursor c = resolver.query(Movies.CONTENT_URI, new String[] {
        BaseColumns._ID,
    }, Movies.TMDB_ID + "=?", new String[] {
        String.valueOf(tmdbId),
    }, null);

    long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(BaseColumns._ID));

    c.close();

    return id;
  }

  public static boolean exists(ContentResolver resolver, long tmdbId) {
    Cursor c = null;
    try {
      c = resolver.query(Movies.CONTENT_URI, new String[] {
          Movies._ID,
      }, Movies.TMDB_ID + "=?", new String[] {
          String.valueOf(tmdbId),
      }, null);

      return c.moveToFirst();
    } finally {
      if (c != null) c.close();
    }
  }

  public static boolean needsUpdate(ContentResolver resolver, long tmdbId, long lastUpdated) {
    if (lastUpdated == 0) return true;

    Cursor c = null;
    try {
      c = resolver.query(Movies.CONTENT_URI, new String[] {
          Movies.LAST_UPDATED,
      }, Movies.TMDB_ID + "=?", new String[] {
          String.valueOf(tmdbId),
      }, null);

      boolean exists = c.moveToFirst();
      if (exists) {
        return lastUpdated > c.getLong(c.getColumnIndex(Movies.LAST_UPDATED));
      }

      return false;
    } finally {
      if (c != null) c.close();
    }
  }

  public static long updateOrInsertMovie(ContentResolver resolver, Movie movie) {
    long movieId = getMovieId(resolver, movie.getTmdbId());

    if (movieId == -1) {
      movieId = insertMovie(resolver, movie);
    } else {
      updateMovie(resolver, movie);
    }

    return movieId;
  }

  public static void updateMovie(ContentResolver resolver, Movie movie) {
    final long movieId = getMovieId(resolver, movie.getTmdbId());
    ContentValues cv = getContentValues(movie);
    resolver.update(Movies.buildMovieUri(movieId), cv, null, null);

    if (movie.getGenres() != null) insertGenres(resolver, movieId, movie.getGenres());
    if (movie.getPeople() != null) insertPeople(resolver, movieId, movie.getPeople());
  }

  public static long insertMovie(ContentResolver resolver, Movie movie) {
    ContentValues cv = getContentValues(movie);

    Uri uri = resolver.insert(Movies.CONTENT_URI, cv);
    final long movieId = Long.valueOf(Movies.getMovieId(uri));

    if (movie.getGenres() != null) insertGenres(resolver, movieId, movie.getGenres());
    if (movie.getPeople() != null) insertPeople(resolver, movieId, movie.getPeople());

    return movieId;
  }

  public static void insertGenres(ContentResolver resolver, long movieId, List<String> genres) {
    for (String genre : genres) {
      ContentValues cv = new ContentValues();

      cv.put(CathodeContract.MovieGenres.MOVIE_ID, movieId);
      cv.put(CathodeContract.MovieGenres.GENRE, genre);

      resolver.insert(CathodeContract.MovieGenres.buildFromMovieId(movieId), cv);
    }
  }

  private static void insertPeople(ContentResolver resolver, long movieId, Movie.People people) {
    resolver.delete(CathodeContract.MovieDirectors.buildFromMovieId(movieId), null, null);
    List<Person> directors = people.getDirectors();
    for (Person person : directors) {
      ContentValues cv = new ContentValues();
      cv.put(CathodeContract.MovieDirectors.MOVIE_ID, movieId);
      cv.put(CathodeContract.MovieDirectors.NAME, person.getName());
      if (!ApiUtils.isPlaceholder(person.getImages().getHeadshot())) {
        cv.put(CathodeContract.MovieDirectors.HEADSHOT, person.getImages().getHeadshot());
      }

      resolver.insert(CathodeContract.MovieDirectors.buildFromMovieId(movieId), cv);
    }

    resolver.delete(CathodeContract.MovieWriters.buildFromMovieId(movieId), null, null);
    List<Person> writers = people.getWriters();
    for (Person person : writers) {
      ContentValues cv = new ContentValues();
      cv.put(CathodeContract.MovieWriters.MOVIE_ID, movieId);
      cv.put(CathodeContract.MovieWriters.NAME, person.getName());
      if (!ApiUtils.isPlaceholder(person.getImages().getHeadshot())) {
        cv.put(CathodeContract.MovieWriters.HEADSHOT, person.getImages().getHeadshot());
      }
      cv.put(CathodeContract.MovieWriters.JOB, person.getJob());

      resolver.insert(CathodeContract.MovieWriters.buildFromMovieId(movieId), cv);
    }

    resolver.delete(CathodeContract.MovieProducers.buildFromMovieId(movieId), null, null);
    List<Person> producers = people.getProducers();
    for (Person person : producers) {
      ContentValues cv = new ContentValues();
      cv.put(CathodeContract.MovieProducers.MOVIE_ID, movieId);
      cv.put(CathodeContract.MovieProducers.NAME, person.getName());
      if (!ApiUtils.isPlaceholder(person.getImages().getHeadshot())) {
        cv.put(CathodeContract.MovieProducers.HEADSHOT, person.getImages().getHeadshot());
      }
      cv.put(CathodeContract.MovieProducers.EXECUTIVE, person.isExecutive());

      resolver.insert(CathodeContract.MovieProducers.buildFromMovieId(movieId), cv);
    }

    resolver.delete(CathodeContract.MovieActors.buildFromMovieId(movieId), null, null);
    List<Person> actors = people.getActors();
    for (Person person : actors) {
      ContentValues cv = new ContentValues();
      cv.put(CathodeContract.MovieActors.MOVIE_ID, movieId);
      cv.put(CathodeContract.MovieActors.NAME, person.getName());
      if (!ApiUtils.isPlaceholder(person.getImages().getHeadshot())) {
        cv.put(CathodeContract.MovieActors.HEADSHOT, person.getImages().getHeadshot());
      }
      cv.put(CathodeContract.MovieActors.CHARACTER, person.getCharacter());

      resolver.insert(CathodeContract.MovieActors.buildFromMovieId(movieId), cv);
    }
  }

  private static ContentValues getContentValues(Movie movie) {
    ContentValues cv = new ContentValues();

    cv.put(Movies.TITLE, movie.getTitle());
    cv.put(Movies.YEAR, movie.getYear());
    cv.put(Movies.RELEASED, movie.getReleased());
    cv.put(Movies.URL, movie.getUrl());
    cv.put(Movies.TRAILER, movie.getTrailer());
    cv.put(Movies.RUNTIME, movie.getRuntime());
    cv.put(Movies.TAGLINE, movie.getTagline());
    cv.put(Movies.OVERVIEW, movie.getOverview());
    cv.put(Movies.CERTIFICATION, movie.getCertification());
    cv.put(Movies.IMDB_ID, movie.getImdbId());
    cv.put(Movies.TMDB_ID, movie.getTmdbId());
    cv.put(Movies.RT_ID, movie.getRtId());
    cv.put(Movies.LAST_UPDATED, movie.getLastUpdated());
    if (movie.getImages() != null) {
      Images images = movie.getImages();
      if (!ApiUtils.isPlaceholder(images.getPoster())) cv.put(Movies.POSTER, images.getPoster());
      if (!ApiUtils.isPlaceholder(images.getFanart())) cv.put(Movies.FANART, images.getFanart());
    }
    if (movie.getRatings() != null) {
      cv.put(Movies.RATING_PERCENTAGE, movie.getRatings().getPercentage());
      cv.put(Movies.RATING_VOTES, movie.getRatings().getVotes());
      cv.put(Movies.RATING_LOVED, movie.getRatings().getLoved());
      cv.put(Movies.RATING_HATED, movie.getRatings().getHated());
    }
    if (movie.getStats() != null) {
      cv.put(Movies.WATCHERS, movie.getStats().getWatchers());
      cv.put(Movies.PLAYS, movie.getStats().getPlays());
      cv.put(Movies.SCROBBLES, movie.getStats().getScrobbles());
      cv.put(Movies.CHECKINS, movie.getStats().getCheckins());
    }

    // TODO: Top watchers

    if (movie.isWatched() != null) cv.put(Movies.WATCHED, movie.isWatched());

    cv.put(Movies.PLAYS, movie.getPlays());

    // TODO: rating
    // TODO: ratingAdvanced
    cv.put(Movies.IN_WATCHLIST, movie.isInWatchlist());
    cv.put(Movies.IN_COLLECTION, movie.isInCollection());

    return cv;
  }

  public static void setWatched(ContentResolver resolver, long movieId, boolean isWatched) {
    ContentValues cv = new ContentValues();
    cv.put(Movies.WATCHED, isWatched);
    resolver.update(Movies.buildMovieUri(movieId), cv, null, null);
  }

  public static void setIsInCollection(ContentResolver resolver, long movieId,
      boolean inCollection) {
    ContentValues cv = new ContentValues();
    cv.put(Movies.IN_COLLECTION, inCollection);
    resolver.update(Movies.buildMovieUri(movieId), cv, null, null);
  }

  public static void setIsInWatchlist(ContentResolver resolver, long movieId, boolean inWatchlist) {
    ContentValues cv = new ContentValues();
    cv.put(Movies.IN_WATCHLIST, inWatchlist);
    resolver.update(Movies.buildMovieUri(movieId), cv, null, null);
  }
}