package net.simonvt.trakt.api;

import retrofit.RetrofitError;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import net.simonvt.trakt.api.entity.TraktResponse;

import java.io.InputStream;
import java.io.InputStreamReader;

import javax.inject.Inject;

public class ResponseParser {

    private static final String TAG = "ResponseParser";

    @Inject @Trakt Gson mGson;

    /**
     * Attempts to parse a RetrofitError into a {@link TraktResponse}.
     *
     * @param e The error
     * @return Return a {@link TraktResponse} if parsing was successful, null if not
     */
    public TraktResponse tryParse(RetrofitError e) {
        try {
            InputStream is = e.getResponse().getBody().in();
            return mGson.fromJson(new JsonReader(new InputStreamReader(is)), TraktResponse.class);
        } catch (Throwable t) {
            // Ignore
        }

        return null;
    }
}
