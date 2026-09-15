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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSearchTool {

    private static final String USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36";

    public String search(String query) throws Exception {
        if (query == null || query.trim().isEmpty()) return "Search query is empty";

        String url = "https://duckduckgo.com/html/?q="
            + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name());
        String html = fetch(url);

        List<Result> results = parseResults(html);
        if (results.isEmpty()) {
            return "No results found for: " + query;
        }

        StringBuilder sb = new StringBuilder();
        int n = Math.min(results.size(), 6);
        for (int i = 0; i < n; i++) {
            Result r = results.get(i);
            sb.append(i + 1).append(". ").append(r.title).append('\n')
              .append("   URL: ").append(r.url).append('\n');
            if (!r.snippet.isEmpty()) {
                sb.append("   Summary: ").append(r.snippet).append('\n');
            }
        }
        return sb.toString();
    }

    private String fetch(String urlString) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new Exception("Search service returned HTTP " + code);
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

    private List<Result> parseResults(String html) {
        List<Result> out = new ArrayList<>();

        Pattern linkPattern = Pattern.compile(
            "<a rel=\"nofollow\" class=\"result__a\" href=\"([^\"]+)\"[^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher linkMatcher = linkPattern.matcher(html);

        Pattern snippetPattern = Pattern.compile(
            "<a class=\"result__snippet\"[^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        List<String> snippets = new ArrayList<>();
        Matcher snippetMatcher = snippetPattern.matcher(html);
        while (snippetMatcher.find() && snippets.size() < 6) {
            snippets.add(clean(snippetMatcher.group(1)));
        }

        while (linkMatcher.find() && out.size() < 6) {
            String rawUrl = linkMatcher.group(1);
            String title = clean(linkMatcher.group(2));
            String url = extractUrl(rawUrl);
            if (url == null || url.isEmpty()) continue;
            String snippet = snippets.size() > out.size() ? snippets.get(out.size()) : "";
            out.add(new Result(title, url, snippet));
        }

        return out;
    }

    private String extractUrl(String raw) {
        try {
            if (raw.startsWith("//")) raw = "https:" + raw;
            if (raw.startsWith("/")) return null;
            URL url = new URL(raw);
            return url.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String clean(String s) {
        if (s == null) return "";
        String text = Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY).toString();
        return text.replace('\n', ' ').trim();
    }

    private static class Result {
        final String title;
        final String url;
        final String snippet;

        Result(String title, String url, String snippet) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
        }
    }
}