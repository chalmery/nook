package top.yangcc.model;

import java.util.Date;

public class Episode {

    private String title;
    private String description;
    private String audioUrl;
    private String duration;
    private Date pubDate;
    private String guid;

    public Episode() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public Date getPubDate() { return pubDate; }
    public void setPubDate(Date pubDate) { this.pubDate = pubDate; }

    public String getGuid() { return guid; }
    public void setGuid(String guid) { this.guid = guid; }

    @Override
    public String toString() {
        return title != null ? title : "未知单集";
    }
}
