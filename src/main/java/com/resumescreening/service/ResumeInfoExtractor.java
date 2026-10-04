package com.resumescreening.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts structured information (name, email, phone, education, experience, projects)
 * from raw resume text using regex patterns and heuristics.
 *
 * Note: Resume parsing is inherently imprecise due to the diverse formats candidates use.
 * Fields that cannot be detected are returned as null and displayed as "Not detected".
 */
@Service
public class ResumeInfoExtractor {

    private static final Logger log = LoggerFactory.getLogger(ResumeInfoExtractor.class);

    // Email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}",
            Pattern.CASE_INSENSITIVE);

    // Phone pattern: handles various formats like +1-555-0100, (555)0100, 555.0100.0000
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(\\+?\\d{1,3}[\\s\\-.]?)?(\\(?\\d{3}\\)?[\\s\\-.]?)\\d{3}[\\s\\-.]?\\d{4}");

    // Section headers for education
    private static final Pattern EDUCATION_HEADER = Pattern.compile(
            "(?i)^(education|academic|qualification|degree|university|college).*$",
            Pattern.MULTILINE);

    // Section headers for experience
    private static final Pattern EXPERIENCE_HEADER = Pattern.compile(
            "(?i)^(experience|work experience|employment|career|professional experience|internship).*$",
            Pattern.MULTILINE);

    // Section headers for projects
    private static final Pattern PROJECTS_HEADER = Pattern.compile(
            "(?i)^(project|projects|personal project|academic project|side project).*$",
            Pattern.MULTILINE);

    // Name heuristic: first non-blank line that looks like a name (2-4 words, each capitalized, no digits)
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^([A-Z][a-z]+(\\s[A-Z][a-z]+){1,3})$",
            Pattern.MULTILINE);

    /**
     * Extracts all available information from resume text.
     * Returns a map with keys: name, email, phone, education, experience, projects
     */
    public Map<String, String> extractAll(String resumeText) {
        Map<String, String> result = new HashMap<>();

        if (resumeText == null || resumeText.isBlank()) {
            log.warn("Empty resume text provided for extraction");
            return result;
        }

        result.put("email",      extractEmail(resumeText));
        result.put("phone",      extractPhone(resumeText));
        result.put("name",       extractName(resumeText));
        result.put("education",  extractSection(resumeText, EDUCATION_HEADER));
        result.put("experience", extractSection(resumeText, EXPERIENCE_HEADER));
        result.put("projects",   extractSection(resumeText, PROJECTS_HEADER));

        return result;
    }

    public String extractEmail(String text) {
        Matcher m = EMAIL_PATTERN.matcher(text);
        return m.find() ? m.group().trim() : null;
    }

    public String extractPhone(String text) {
        Matcher m = PHONE_PATTERN.matcher(text);
        return m.find() ? m.group().trim() : null;
    }

    /**
     * Attempt to identify the candidate's name from the resume.
     * Strategy: look for the first short capitalized line before any section header.
     */
    public String extractName(String text) {
        // Try first-line heuristic
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Skip lines that look like headers, emails, phones, or addresses
            if (trimmed.contains("@")) continue;
            if (trimmed.matches(".*\\d{5,}.*")) continue;  // long numbers (phone/zip)
            if (trimmed.length() > 60) continue;            // too long to be a name

            Matcher m = NAME_PATTERN.matcher(trimmed);
            if (m.matches()) {
                return trimmed;
            }
            // Even if pattern doesn't match perfectly, first short line is often the name
            if (trimmed.split("\\s+").length <= 4 && trimmed.matches("[A-Za-z .'-]+")) {
                return trimmed;
            }
        }
        return null;
    }

    /**
     * Extract a section of the resume following a section header.
     * Returns up to 800 characters of content after the detected header.
     */
    public String extractSection(String text, Pattern headerPattern) {
        Matcher headerMatcher = headerPattern.matcher(text);
        if (!headerMatcher.find()) {
            return null;
        }

        int start = headerMatcher.end();
        // Find where the next major section begins
        int end = findNextSectionStart(text, start);
        if (end < 0 || end <= start) {
            end = Math.min(start + 800, text.length());
        }

        String section = text.substring(start, end).trim();
        if (section.length() > 800) {
            section = section.substring(0, 800) + "...";
        }
        return section.isBlank() ? null : section;
    }

    /**
     * Find the index of the next section header after 'from' position.
     * Uses common resume section keywords.
     */
    private int findNextSectionStart(String text, int from) {
        Pattern nextSection = Pattern.compile(
                "(?i)^(education|experience|work|skills|projects|certifications|" +
                "awards|publications|languages|references|summary|objective|" +
                "technical|professional|academic|interests|activities).*$",
                Pattern.MULTILINE);
        Matcher m = nextSection.matcher(text);
        while (m.find()) {
            if (m.start() > from) {
                return m.start();
            }
        }
        return -1;
    }
}
