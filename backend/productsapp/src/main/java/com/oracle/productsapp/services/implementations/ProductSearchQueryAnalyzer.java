package com.oracle.productsapp.services.implementations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/** Applies conservative query-routing rules before lexical or dense retrieval. */
@Component
public class ProductSearchQueryAnalyzer {

    /* Split compact pack sizes such as 500g and 2L into searchable parts. */
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\p{L}+|\\p{N}+");
    private static final Pattern LETTERS_ONLY = Pattern.compile("\\p{L}+");
    private static final Pattern REPEATED_CHARACTER = Pattern.compile("(.)\\1{2,}");
    private static final Set<String> ORACLE_TEXT_OPERATORS = Set.of(
            "and", "or", "not", "about", "accum", "minus", "near",
            "fuzzy", "within", "sentence", "paragraph", "section",
            "zone", "path", "inpath", "haspath", "equiv", "stem", "thes"
    );
    /* Connector words add no grocery intent and would otherwise disable semantic search. */
    private static final Set<String> QUERY_STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "by", "for", "from", "in",
            "into", "is", "it", "make", "my", "of", "on", "or", "the", "to",
            "with", "add", "create", "get", "need", "some", "want"
    );

    public ProductSearchQuery analyze(String query) {
        String normalized = query.trim().replaceAll("\\s+", " ");
        Matcher matcher = TOKEN_PATTERN.matcher(normalized.toLowerCase(Locale.ROOT));
        List<String> tokens = new ArrayList<>();

        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() >= 2
                    && !ORACLE_TEXT_OPERATORS.contains(token)
                    && !QUERY_STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }

        tokens = tokens.stream().distinct().limit(12).toList();
        boolean onePlausibleWord = tokens.size() == 1 && isPlausibleWord(tokens.get(0));
        boolean naturalLanguagePhrase = tokens.size() >= 2
                && tokens.stream().allMatch(this::isPlausibleWord)
                && tokens.stream().mapToInt(String::length).sum() >= 8;

        /*
         * Fuzzy matching is deliberately limited to a plausible single word.
         * It keeps useful corrections such as "milkk" while excluding short
         * inputs ("dd", "aaa") and strings with repeated-key noise.
         */
        boolean allowFuzzy = onePlausibleWord && tokens.get(0).length() >= 4;

        /*
         * A one-word query is still served by exact/prefix/alias search.  It
         * must not activate dense fallback because a vector index has a
         * neighbour even when the word is meaningless.
         */
        boolean allowSemantic = naturalLanguagePhrase;

        return new ProductSearchQuery(normalized, tokens, allowFuzzy, allowSemantic);
    }

    private boolean isPlausibleWord(String token) {
        if (!LETTERS_ONLY.matcher(token).matches() || token.length() < 3
                || REPEATED_CHARACTER.matcher(token).find()) {
            return false;
        }

        long distinctCharacters = token.chars().distinct().count();
        long vowels = token.chars()
                .filter(character -> "aeiou".indexOf(character) >= 0)
                .count();
        return distinctCharacters >= 3 && vowels > 0;
    }
}
