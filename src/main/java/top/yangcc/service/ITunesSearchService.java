package top.yangcc.service;

import com.google.gson.*;
import top.yangcc.model.Podcast;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ITunesSearchService {

    private static final Logger LOG = System.getLogger("top.yangcc.itunes");

    private static final String SEARCH_URL = "https://itunes.apple.com/search";
    private static final String LOOKUP_URL = "https://itunes.apple.com/lookup";
    private static final String TRENDING_URL = "https://rss.marketingtools.apple.com/api/v2/cn/podcasts/top/%d/podcasts.json";

    private static final String COUNTRY = "cn";
    private static final String MEDIA = "podcast";
    private static final String ENTITY = "podcast";
    private static final int DEFAULT_LIMIT = 20;

    private final HttpClient httpClient;
    private final Gson gson;

    public ITunesSearchService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
    }

    public List<Podcast> search(String query, int limit) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = SEARCH_URL + "?term=" + encoded
                + "&country=" + COUNTRY
                + "&media=" + MEDIA
                + "&entity=" + ENTITY
                + "&limit=" + limit;
        return fetchAndParse(url);
    }

    public List<Podcast> search(String query) {
        return search(query, DEFAULT_LIMIT);
    }

    /**
     * Searches by genre name. The iTunes Search API does not support genreId filtering
     * directly, so we use the genre name as the search term for reliable results.
     */
    public List<Podcast> searchByGenreName(String genreName, int limit) {
        String encoded = URLEncoder.encode(genreName, StandardCharsets.UTF_8);
        String url = SEARCH_URL + "?term=" + encoded
                + "&country=" + COUNTRY
                + "&media=" + MEDIA
                + "&entity=" + ENTITY
                + "&limit=" + limit;
        return fetchAndParse(url);
    }

    public List<Podcast> searchByGenreName(String genreName) {
        return searchByGenreName(genreName, DEFAULT_LIMIT);
    }

    public List<Podcast> getTrending(int limit) {
        String url = String.format(TRENDING_URL, limit);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.log(Level.WARNING, "Trending API returned status {0}", response.statusCode());
                return List.of();
            }
            return parseTrendingResponse(response.body());
        } catch (Exception e) {
            LOG.log(Level.ERROR, "Failed to fetch trending podcasts: " + e.getMessage(), e);
            return List.of();
        }
    }

    public List<Podcast> getTrending() {
        return getTrending(10);
    }

    /**
     * Looks up a podcast by iTunes collection ID to get its feedUrl.
     */
    public String lookupFeedUrl(String collectionId) {
        Podcast p = lookupPodcast(collectionId);
        return p != null ? p.getRssUrl() : null;
    }

    /**
     * Full podcast lookup by iTunes collection ID. Returns all available metadata
     * including description, releaseDate, trackCount, and more.
     */
    public Podcast lookupPodcast(String collectionId) {
        String url = LOOKUP_URL + "?id=" + collectionId
                + "&country=" + COUNTRY
                + "&media=" + MEDIA
                + "&entity=" + ENTITY;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            String json = response.body();
            List<Podcast> results = parseSearchResponse(json);
            if (!results.isEmpty()) {
                Podcast p = results.get(0);
                // Save the collection ID for later use
                if (p.getLink() == null || p.getLink().isBlank()) {
                    p.setLink(collectionId);
                }
                return p;
            }
        } catch (Exception e) {
            LOG.log(Level.ERROR, "Failed to lookup podcast " + collectionId + ": " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Enriches an existing Podcast with data from the iTunes Lookup API.
     * Thread-safe: returns a new Podcast if lookup succeeds, or the original if it fails.
     */
    public Podcast enrichPodcast(Podcast podcast) {
        String collectionId = podcast.getLink();
        if (collectionId == null || collectionId.isBlank()) return podcast;
        Podcast lookedUp = lookupPodcast(collectionId);
        if (lookedUp == null) return podcast;

        // Merge lookup data into existing podcast, preferring non-null lookup values
        if (podcast.getRssUrl() == null || podcast.getRssUrl().isBlank()) {
            podcast.setRssUrl(lookedUp.getRssUrl());
        }
        if (podcast.getImageUrl() == null || podcast.getImageUrl().isBlank()) {
            podcast.setImageUrl(lookedUp.getImageUrl());
        }
        if (podcast.getPrimaryGenre() == null || podcast.getPrimaryGenre().isBlank()) {
            podcast.setPrimaryGenre(lookedUp.getPrimaryGenre());
        }
        if (podcast.getReleaseDate() == null || podcast.getReleaseDate().isBlank()) {
            podcast.setReleaseDate(lookedUp.getReleaseDate());
        }
        if (podcast.getTrackCount() == 0) {
            podcast.setTrackCount(lookedUp.getTrackCount());
        }
        if ((podcast.getGenres() == null || podcast.getGenres().isEmpty())
                && lookedUp.getGenres() != null && !lookedUp.getGenres().isEmpty()) {
            podcast.setGenres(lookedUp.getGenres());
        }
        if (podcast.getDescription() == null || podcast.getDescription().isBlank()) {
            podcast.setDescription(lookedUp.getDescription());
        }
        if (podcast.getCopyright() == null || podcast.getCopyright().isBlank()) {
            podcast.setCopyright(lookedUp.getCopyright());
        }
        return podcast;
    }

    private List<Podcast> fetchAndParse(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.log(Level.WARNING, "Search API returned status {0}", response.statusCode());
                return List.of();
            }
            return parseSearchResponse(response.body());
        } catch (Exception e) {
            LOG.log(Level.ERROR, "Failed to search iTunes: " + e.getMessage(), e);
            return List.of();
        }
    }

    private List<Podcast> parseSearchResponse(String json) {
        List<Podcast> results = new ArrayList<>();
        JsonObject root = gson.fromJson(json, JsonObject.class);
        JsonArray items = root.getAsJsonArray("results");
        if (items == null) return results;

        for (JsonElement el : items) {
            JsonObject obj = el.getAsJsonObject();
            String kind = getString(obj, "kind");
            if (!"podcast".equals(kind)) continue;

            Podcast p = new Podcast();
            p.setTitle(getString(obj, "collectionName"));
            p.setAuthor(getString(obj, "artistName"));
            p.setRssUrl(getString(obj, "feedUrl"));
            p.setImageUrl(getString(obj, "artworkUrl600"));
            if (p.getImageUrl() == null || p.getImageUrl().isBlank()) {
                p.setImageUrl(upscaleArtworkUrl(getString(obj, "artworkUrl100")));
            }
            p.setPrimaryGenre(getString(obj, "primaryGenreName"));
            p.setReleaseDate(getString(obj, "releaseDate"));
            p.setTrackCount(getInt(obj, "trackCount"));
            p.setDescription(getString(obj, "description"));
            p.setCopyright(getString(obj, "copyright"));
            p.setLink(getCollectionId(obj));

            JsonArray genreArray = obj.getAsJsonArray("genres");
            if (genreArray != null) {
                List<String> genreList = new ArrayList<>();
                for (JsonElement g : genreArray) {
                    String genreName = g.getAsString();
                    if (genreName != null && !genreName.equals("Podcasts")) {
                        genreList.add(genreName);
                    }
                }
                p.setGenres(genreList);
            }
            results.add(p);
        }
        return results;
    }

    private List<Podcast> parseTrendingResponse(String json) {
        List<Podcast> results = new ArrayList<>();
        JsonObject root = gson.fromJson(json, JsonObject.class);
        JsonObject feed = root.getAsJsonObject("feed");
        if (feed == null) return results;

        JsonArray items = feed.getAsJsonArray("results");
        if (items == null) return results;

        for (JsonElement el : items) {
            JsonObject obj = el.getAsJsonObject();
            Podcast p = new Podcast();
            p.setTitle(getString(obj, "name"));
            p.setAuthor(getString(obj, "artistName"));
            p.setImageUrl(upscaleArtworkUrl(getString(obj, "artworkUrl100")));
            // Trending API doesn't provide feedUrl — store the collection ID for later lookup
            String id = getString(obj, "id");
            if (id != null && !id.isBlank()) {
                p.setLink(id);
            }
            // parse genres array
            JsonArray genreArray = obj.getAsJsonArray("genres");
            if (genreArray != null) {
                List<String> genreList = new ArrayList<>();
                for (JsonElement g : genreArray) {
                    JsonObject genreObj = g.getAsJsonObject();
                    String genreName = getString(genreObj, "name");
                    if (genreName != null && !genreName.isBlank()) {
                        genreList.add(genreName);
                    }
                }
                p.setGenres(genreList);
            }
            results.add(p);
        }
        return results;
    }

    /** Replace 100x100bb with 600x600bb in Apple artwork URLs for higher resolution. */
    private static String upscaleArtworkUrl(String url) {
        if (url == null || url.isBlank()) return url;
        return url.replace("100x100bb", "600x600bb");
    }

    private static String getString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }

    /** collectionId is an integer in iTunes API. Extract and convert to string. */
    private static String getCollectionId(JsonObject obj) {
        if (obj.has("collectionId") && !obj.get("collectionId").isJsonNull()) {
            JsonElement el = obj.get("collectionId");
            if (el.isJsonPrimitive()) {
                return el.getAsJsonPrimitive().getAsNumber().toString();
            }
        }
        return null;
    }

    private static int getInt(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsInt();
        }
        return 0;
    }
}
