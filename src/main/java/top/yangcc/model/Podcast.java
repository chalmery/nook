package top.yangcc.model;

import java.util.ArrayList;
import java.util.List;

public class Podcast {

    private String title;
    private String rssUrl;
    private String imageUrl;
    private String author;
    private String description;
    private String link;
    private List<Episode> episodes = new ArrayList<>();

    public Podcast() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRssUrl() { return rssUrl; }
    public void setRssUrl(String rssUrl) { this.rssUrl = rssUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public List<Episode> getEpisodes() { return episodes; }
    public void setEpisodes(List<Episode> episodes) { this.episodes = episodes; }

    @Override
    public String toString() {
        return title != null ? title : "未知播客";
    }
}
