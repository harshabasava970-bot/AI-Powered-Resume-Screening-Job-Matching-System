package com.resumescreening.service;

import com.resumescreening.entity.Skill;
import com.resumescreening.repository.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dedicated service for extracting skills from resume or job description text.
 *
 * Algorithm:
 * 1. Load all known skills from the database (case-insensitive normalized names).
 * 2. Tokenize the input text into words and phrases (1-3 grams).
 * 3. Match tokens against the skill dictionary using case-insensitive comparison.
 * 4. Return the list of matched Skill entities.
 *
 * This is an explainable keyword-matching approach — no black-box model required.
 * The skill dictionary is configurable via the database (DataInitializer seeds it).
 */
@Service
public class SkillExtractionService {

    private static final Logger log = LoggerFactory.getLogger(SkillExtractionService.class);

    private final SkillRepository skillRepository;

    public SkillExtractionService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    /**
     * Extract skills from free-form text.
     *
     * @param text Raw text from a resume or job description
     * @return List of matched Skill entities (deduplicated)
     */
    @Transactional(readOnly = true)
    public List<Skill> extractSkills(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        // Load all skills and build a normalized -> Skill map
        List<Skill> allSkills = skillRepository.findAll();
        Map<String, Skill> skillMap = allSkills.stream()
                .collect(Collectors.toMap(
                        s -> s.getNormalizedName(),
                        s -> s,
                        (existing, replacement) -> existing  // keep first on duplicate key
                ));

        // Normalize the input text for matching
        String normalizedText = normalizeText(text);

        // Generate n-grams (1 to 3 words) from the text
        Set<String> extractedNormalized = new LinkedHashSet<>();
        String[] tokens = normalizedText.split("\\s+");

        for (int i = 0; i < tokens.length; i++) {
            // 1-gram
            String unigram = tokens[i];
            if (skillMap.containsKey(unigram)) {
                extractedNormalized.add(unigram);
            }
            // 2-gram
            if (i + 1 < tokens.length) {
                String bigram = tokens[i] + " " + tokens[i + 1];
                if (skillMap.containsKey(bigram)) {
                    extractedNormalized.add(bigram);
                }
            }
            // 3-gram
            if (i + 2 < tokens.length) {
                String trigram = tokens[i] + " " + tokens[i + 1] + " " + tokens[i + 2];
                if (skillMap.containsKey(trigram)) {
                    extractedNormalized.add(trigram);
                }
            }
        }

        List<Skill> matched = extractedNormalized.stream()
                .map(skillMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.debug("Skill extraction: found {} skills from text of length {}", matched.size(), text.length());
        return matched;
    }

    /**
     * Extract skill names (strings) from text — convenience wrapper.
     */
    @Transactional(readOnly = true)
    public List<String> extractSkillNames(String text) {
        return extractSkills(text).stream()
                .map(Skill::getName)
                .collect(Collectors.toList());
    }

    /**
     * Given a list of skill names (from a form), resolve them to Skill entities.
     * Creates new Skill records for unrecognized skills.
     */
    @Transactional
    public List<Skill> resolveSkillNames(List<String> skillNames) {
        if (skillNames == null || skillNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<Skill> resolved = new ArrayList<>();
        for (String name : skillNames) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) continue;

            String normalized = trimmed.toLowerCase();
            Skill skill = skillRepository.findByNormalizedName(normalized)
                    .orElseGet(() -> {
                        // Auto-create unknown skills so recruiters can define custom skills
                        Skill newSkill = new Skill(trimmed, "Other");
                        return skillRepository.save(newSkill);
                    });
            resolved.add(skill);
        }
        return resolved;
    }

    /**
     * Normalize text: lowercase, remove punctuation except hyphens (for C++, .NET etc.),
     * collapse whitespace.
     */
    private String normalizeText(String text) {
        // Keep alphanumerics, spaces, +, #, . (for C++, C#, Node.js)
        String cleaned = text
                .replaceAll("[^a-zA-Z0-9+#.\\s]", " ")
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned;
    }
}
