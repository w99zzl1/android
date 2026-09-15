package com.example.calculator;

import android.text.Html;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeSearchTool {

    private static final String UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36";

    public String searchText(String query) throws Exception {
        if (query == null || query.trim().isEmpty()) return "Search query is empty";

        List<Video> videos = searchVideos(query);
        if (videos.isEmpty()) {
            return "No videos found for: " + query;
        }

        StringBuilder sb = new StringBuilder("Top YouTube videos for \"").append(query.trim()).append("\":\n");
        int n = Math.min(videos.size(), 6);
        for (int i = 0; i < n; i++) {
            Video v = videos.get(i);
            sb.append(i + 1).append(". \"").append(v.title).append('"');
            if (!v.duration.isEmpty()) sb.append(" [").append(v.duration).append(']');
            sb.append(" (").append(v.author).append(")").append(" videoId=").append(v.videoId).append('\n');
        }
        sb.append("\nChoose the most suitable video and call OPEN_YOUTUBE_VIDEO with its exact videoId. " +
                  "Never call a search page.");
        return sb.toString();
    }

    private List<Video> searchVideos(String query) throws Exception {
        String base = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name());

        try {
            List<Video> fromDesktop = parseVideos(fetch("https://www.youtube.com/results?search_query=" + base));
            if (!fromDesktop.isEmpty()) return fromDesktop;
        } catch (Exception ignored) {
        }

        try {
            List<Video> fromMobile = parseVideos(fetch("https://m.youtube.com/results?search_query=" + base));
            if (!fromMobile.isEmpty()) return fromMobile;
        } catch (Exception ignored) {
        }

        return parseDdg(fetch("https://duckduckgo.com/html/?q="
            + URLEncoder.encode("site:youtube.com " + query.trim(), StandardCharsets.UTF_8.name())));
    }

    private String fetch(String urlString) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new Exception("YouTube search returned HTTP " + code);
            }
            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            reader.close();
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private List<Video> parseVideos(String html) {
        List<Video> out = new ArrayList<>();

        Pattern block = Pattern.compile(
            "\"videoRenderer\":\\{.*?\"videoId\":\"([A-Za-z0-9_-]{11})\".*?" +
            "\"title\":\\{\"runs\":\\[\\{\"text\":\"(.*?)\".*?" +
            "\"lengthText\":\\{\"simpleText\":\"([0-9:]+)\".*?" +
            "\"ownerText\":\\{.*?\"text\":\"(.*?)\"",
            Pattern.DOTALL);
        Matcher m = block.matcher(html);
        while (m.find() && out.size() < 6) {
            String id = m.group(1);
            String title = clean(m.group(2));
            String duration = m.group(3);
            String author = clean(m.group(4));
            out.add(new Video(id, title, duration, author));
        }
        if (!out.isEmpty()) return out;

        List<String> ids = new ArrayList<>();
        Matcher idm = Pattern.compile("\"videoId\":\"([A-Za-z0-9_-]{11})\"").matcher(html);
        while (idm.find()) {
            String id = idm.group(1);
            if (!ids.contains(id)) ids.add(id);
        }
        List<String> titles = new ArrayList<>();
        Matcher tm = Pattern.compile("\"title\":\\{\"runs\":\\[\\{\"text\":\"(.*?)\"").matcher(html);
        while (tm.find() && titles.size() < ids.size()) titles.add(clean(tm.group(1)));

        int n = Math.min(Math.min(6, ids.size()), titles.size());
        for (int i = 0; i < n; i++) {
            out.add(new Video(ids.get(i), titles.get(i), "", ""));
        }
        return out;
    }

    private List<Video> parseDdg(String html) {
        List<Video> out = new ArrayList<>();
        List<String> seen = new ArrayList<>();

        Pattern linkPattern = Pattern.compile(
            "<a rel=\"nofollow\" class=\"result__a\" href=\"([^\"]+)\"[^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = linkPattern.matcher(html);
        while (m.find() && out.size() < 6) {
            String title = clean(m.group(2));
            String url = m.group(1);
            String id = extractVideoId(url);
            if (id == null || seen.contains(id)) continue;
            seen.add(id);
            out.add(new Video(id, title, "", ""));
        }
        return out;
    }

    private String extractVideoId(String url) {
        try {
            if (url == null) return null;
            if (url.startsWith("//")) url = "https:" + url;
            Matcher watch = Pattern.compile("[?&]v=([A-Za-z0-9_-]{11})").matcher(url);
            if (watch.find()) return watch.group(1);
            Matcher shared = Pattern.compile("youtu\\.be/([A-Za-z0-9_-]{11})").matcher(url);
            if (shared.find()) return shared.group(1);
            Matcher shorts = Pattern.compile("/shorts/([A-Za-z0-9_-]{11})").matcher(url);
            if (shorts.find()) return shorts.group(1);
        } catch (Exception ignored) {
        }
        return null;
    }

    private String clean(String s) {
        if (s == null) return "";
        s = s.replace("\\u0026", "&").replace("\\/", "/").replace("\\\"", "\"");
        String t = Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY).toString();
        return t.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static class Video {
        final String videoId;
        final String title;
        final String duration;
        final String author;

        Video(String videoId, String title, String duration, String author) {
            this.videoId = videoId;
            this.title = title;
            this.duration = duration;
            this.author = author;
        }
    }
}