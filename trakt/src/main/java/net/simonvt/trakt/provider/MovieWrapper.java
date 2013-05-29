package net.simonvt.trakt.provider;

import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.entity.Person;
import net.simonvt.trakt.provider.TraktContract.Movies;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.List;

public final class MovieWrapper {

    private static final String TAG = "MovieWrapper";

    private MovieWrapper() {
    }

    public static int getTmdbId(ContentResolver resolver, long movieId) {
        Cursor c = resolver.query(Movies.buildMovieUri(movieId), new String[] {
                TraktContract.MovieColumns.TMDB_ID,
        }, null, null, null);

        int tmdbId = -1;
        if (c.moveToFirst()) {
            tmdbId = c.getInt(c.getColumnIndex(TraktContract.MovieColumns.TMDB_ID));
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

            cv.put(TraktContract.MovieGenres.MOVIE_ID, movieId);
            cv.put(TraktContract.MovieGenres.GENRE, genre);

            resolver.insert(TraktContract.MovieGenres.buildFromMovieId(movieId), cv);
        }
    }

    private static void insertPeople(ContentResolver resolver, long movieId, Movie.People people) {
        resolver.delete(TraktContract.MovieDirectors.buildFromMovieId(movieId), null, null);
        List<Person> directors = people.getDirectors();
        for (Person person : directors) {
            ContentValues cv = new ContentValues();
            cv.put(TraktContract.MovieDirectors.MOVIE_ID, movieId);
            cv.put(TraktContract.MovieDirectors.NAME, person.getName());
            cv.put(TraktContract.MovieDirectors.HEADSHOT, person.getImages().getHeadshot());

            resolver.insert(TraktContract.MovieDirectors.buildFromMovieId(movieId), cv);
        }

        resolver.delete(TraktContract.MovieWriters.buildFromMovieId(movieId), null, null);
        List<Person> writers = people.getWriters();
        for (Person person : writers) {
            ContentValues cv = new ContentValues();
            cv.put(TraktContract.MovieWriters.MOVIE_ID, movieId);
            cv.put(TraktContract.MovieWriters.NAME, person.getName());
            cv.put(TraktContract.MovieWriters.HEADSHOT, person.getImages().getHeadshot());
            cv.put(TraktContract.MovieWriters.JOB, person.getJob());

            resolver.insert(TraktContract.MovieWriters.buildFromMovieId(movieId), cv);
        }

        resolver.delete(TraktContract.MovieProducers.buildFromMovieId(movieId), null, null);
        List<Person> producers = people.getProducers();
        for (Person person : producers) {
            ContentValues cv = new ContentValues();
            cv.put(TraktContract.MovieProducers.MOVIE_ID, movieId);
            cv.put(TraktContract.MovieProducers.NAME, person.getName());
            cv.put(TraktContract.MovieProducers.HEADSHOT, person.getImages().getHeadshot());
            cv.put(TraktContract.MovieProducers.EXECUTIVE, person.isExecutive());

            resolver.insert(TraktContract.MovieProducers.buildFromMovieId(movieId), cv);
        }

        resolver.delete(TraktContract.MovieActors.buildFromMovieId(movieId), null, null);
        List<Person> actors = people.getActors();
        for (Person person : actors) {
            ContentValues cv = new ContentValues();
            cv.put(TraktContract.MovieActors.MOVIE_ID, movieId);
            cv.put(TraktContract.MovieActors.NAME, person.getName());
            cv.put(TraktContract.MovieActors.HEADSHOT, person.getImages().getHeadshot());
            cv.put(TraktContract.MovieActors.CHARACTER, person.getCharacter());

            resolver.insert(TraktContract.MovieActors.buildFromMovieId(movieId), cv);
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
            cv.put(Movies.POSTER, movie.getImages().getPoster());
            cv.put(Movies.FANART, movie.getImages().getFanart());
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

        if (movie.getWatched() != null) cv.put(Movies.WATCHED, movie.getWatched());

        cv.put(Movies.PLAYS, movie.getPlays());

        // TODO: rating
        // TODO: ratingAdvanced
        cv.put(Movies.IN_WATCHLIST, movie.getInWatchlist());
        cv.put(Movies.IN_COLLECTION, movie.getInCollection());

        return cv;
    }
}
