package top.yangcc.rss;

import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.jdom2.Element;
import top.yangcc.model.Episode;
import top.yangcc.model.Podcast;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RssParser {

    private static final Logger LOG = System.getLogger("top.yangcc.rss");

    public static Podcast parse(String rssUrl) throws Exception {
        LOG.log(Level.INFO, "Parsing RSS feed: {0}", rssUrl);

        URL url = URI.create(rssUrl).toURL();
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(url));

        Podcast podcast = new Podcast();
        podcast.setRssUrl(rssUrl);
        podcast.setTitle(feed.getTitle());
        podcast.setDescription(feed.getDescription());
        podcast.setAuthor(feed.getAuthor());
        podcast.setLink(feed.getLink());
        podcast.setLanguage(feed.getLanguage());
        podcast.setCopyright(feed.getCopyright());

        LOG.log(Level.INFO, "Feed title: {0}, author: {1}, entries: {2}",
                feed.getTitle(), feed.getAuthor(), feed.getEntries().size());

        if (feed.getImage() != null) {
            podcast.setImageUrl(feed.getImage().getUrl());
            LOG.log(Level.INFO, "Feed image (standard): {0}", feed.getImage().getUrl());
        }

        // try itunes:image from foreign markup
        if (podcast.getImageUrl() == null || podcast.getImageUrl().isBlank()) {
            for (Element el : feed.getForeignMarkup()) {
                if ("image".equals(el.getName())
                        && el.getAttributeValue("href") != null
                        && el.getNamespace() != null
                        && el.getNamespace().getURI() != null
                        && el.getNamespace().getURI().contains("itunes")) {
                    podcast.setImageUrl(el.getAttributeValue("href"));
                    LOG.log(Level.INFO, "Feed image (itunes): {0}", podcast.getImageUrl());
                    break;
                }
            }
        }

        // itunes:author
        for (Element el : feed.getForeignMarkup()) {
            if ("author".equals(el.getName())
                    && el.getNamespace() != null
                    && el.getNamespace().getURI() != null
                    && el.getNamespace().getURI().contains("itunes")) {
                podcast.setItunesAuthor(el.getTextTrim());
                break;
            }
        }

        List<Episode> episodes = new ArrayList<>();
        int withAudio = 0;
        for (SyndEntry entry : feed.getEntries()) {
            Episode episode = new Episode();
            episode.setTitle(entry.getTitle());
            episode.setGuid(entry.getUri());
            episode.setPubDate(entry.getPublishedDate());
            episode.setLink(entry.getLink());

            if (entry.getDescription() != null) {
                episode.setDescription(entry.getDescription().getValue());
            }

            for (SyndEnclosure enclosure : entry.getEnclosures()) {
                String type = enclosure.getType();
                if (type != null && type.startsWith("audio/")) {
                    episode.setAudioUrl(enclosure.getUrl());
                    break;
                }
            }

            if (episode.getAudioUrl() == null && !entry.getEnclosures().isEmpty()) {
                episode.setAudioUrl(entry.getEnclosures().get(0).getUrl());
            }

            if (episode.getAudioUrl() != null) withAudio++;

            episode.setDuration(extractDuration(entry));

            // itunes:season and itunes:episode
            for (Element el : entry.getForeignMarkup()) {
                if (el.getNamespace() == null || el.getNamespace().getURI() == null
                        || !el.getNamespace().getURI().contains("itunes")) continue;
                String name = el.getName();
                if ("season".equals(name)) {
                    try { episode.setSeason(Integer.parseInt(el.getTextTrim())); } catch (NumberFormatException ignored) {}
                } else if ("episode".equals(name)) {
                    try { episode.setEpisodeNumber(Integer.parseInt(el.getTextTrim())); } catch (NumberFormatException ignored) {}
                }
            }

            episodes.add(episode);
        }

        LOG.log(Level.INFO, "Parsed {0} episodes, {1} with audio URLs", episodes.size(), withAudio);
        podcast.setEpisodes(episodes);
        return podcast;
    }

    private static String extractDuration(SyndEntry entry) {
        for (org.jdom2.Element element : entry.getForeignMarkup()) {
            if ("duration".equals(element.getName())
                    && element.getNamespace() != null
                    && element.getNamespace().getURI() != null
                    && element.getNamespace().getURI().contains("itunes")) {
                String dur = element.getTextTrim();
                return formatDuration(dur);
            }
        }
        return "";
    }

    private static String formatDuration(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        if (raw.contains(":")) return raw;
        try {
            int seconds = Integer.parseInt(raw);
            int h = seconds / 3600;
            int m = (seconds % 3600) / 60;
            int s = seconds % 60;
            if (h > 0) {
                return String.format("%d:%02d:%02d", h, m, s);
            }
            return String.format("%d:%02d", m, s);
        } catch (NumberFormatException e) {
            return raw;
        }
    }
}
