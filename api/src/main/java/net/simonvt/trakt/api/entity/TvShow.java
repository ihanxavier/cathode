package net.simonvt.trakt.api.entity;

import net.simonvt.trakt.api.enumeration.DayOfWeek;
import net.simonvt.trakt.api.enumeration.ShowStatus;

import java.util.List;

public class TvShow {

    private String title;

    private Integer year;

    private String imdbId;

    private Integer tvdbId;

    private Integer tvrageId;

    private String url;

    private Images images;

    private List<String> genres;

    private Long firstAired;

    private String country;

    private String overview;

    private Integer runtime;

    private String network;

    private DayOfWeek airDay;

    private String airTime;

    private String certification;

    private ShowStatus status;

    //TODO rating;

    private Ratings ratings;

    private Stats stats;

    private Long lastUpdated;

    private List<Season> seasons;

    private List<Episode> episodes;

    public String getTitle() {
        return title;
    }

    public Integer getYear() {
        return year;
    }

    public String getImdbId() {
        return imdbId;
    }

    public Integer getTvdbId() {
        return tvdbId;
    }

    public Integer getTvrageId() {
        return tvrageId;
    }

    public String getUrl() {
        return url;
    }

    public Images getImages() {
        return images;
    }

    public List<String> getGenres() {
        return genres;
    }

    public Long getFirstAired() {
        return firstAired;
    }

    public String getCountry() {
        return country;
    }

    public String getOverview() {
        return overview;
    }

    public Integer getRuntime() {
        return runtime;
    }

    public String getNetwork() {
        return network;
    }

    public DayOfWeek getAirDay() {
        return airDay;
    }

    public String getAirTime() {
        return airTime;
    }

    public String getCertification() {
        return certification;
    }

    public ShowStatus getStatus() {
        return status;
    }

    public Ratings getRatings() {
        return ratings;
    }

    public Stats getStats() {
        return stats;
    }

    public Long getLastUpdated() {
        return lastUpdated;
    }

    public List<Season> getSeasons() {
        return seasons;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }
}