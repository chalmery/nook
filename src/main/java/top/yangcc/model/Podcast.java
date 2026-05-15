package top.yangcc.model;

import java.util.ArrayList;
import java.util.List;

public class Podcast {

    private String title;
    private String rssUrl;
    private String imageUrl;
    private String author;
    private String itunesAuthor;
    private String description;
    private String link;
    private String language;
    private String copyright;
    private String primaryGenre;
    private List<String> genres = new ArrayList<>();
    private String releaseDate;
    private int trackCount;
    private List<Episode> episodes = new ArrayList<>();
    private String source; // "discover" or null for manual

    public Podcast() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRssUrl() { return rssUrl; }
    public void setRssUrl(String rssUrl) { this.rssUrl = rssUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getItunesAuthor() { return itunesAuthor; }
    public void setItunesAuthor(String itunesAuthor) { this.itunesAuthor = itunesAuthor; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCopyright() { return copyright; }
    public void setCopyright(String copyright) { this.copyright = copyright; }

    public String getPrimaryGenre() { return primaryGenre; }
    public void setPrimaryGenre(String primaryGenre) { this.primaryGenre = primaryGenre; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

    public int getTrackCount() { return trackCount; }
    public void setTrackCount(int trackCount) { this.trackCount = trackCount; }

    public List<Episode> getEpisodes() { return episodes; }
    public void setEpisodes(List<Episode> episodes) { this.episodes = episodes; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    @Override
    public String toString() {
        return title != null ? title : "未知播客";
    }
}
