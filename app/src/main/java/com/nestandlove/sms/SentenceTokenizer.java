package com.nestandlove.sms;

import android.text.TextUtils;
import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SentenceTokenizer {

    private static final int MASKING_VALUE = 0;
    private static final int UNKNOWN_VALUE = 1;

    private final int maxSentenceLength;
    private final Map<String, Integer> tokenMap = new HashMap<>(50000);
    private final WordGenerator wordGenerator = new WordGenerator();

    public SentenceTokenizer(int maxSentenceLength) {
        this.maxSentenceLength = maxSentenceLength;
    }

    public static SentenceTokenizer fromVocabulary(InputStream is, int maxSentenceLength) throws IOException {
        SentenceTokenizer t = new SentenceTokenizer(maxSentenceLength);
        try(JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                int token = reader.nextInt();
                t.tokenMap.put(key, token);
            }
            reader.endObject();
        }
        return t;
    }

    /**
     * Converts a given sentence into an array according to its vocabulary.
     */
    public int[] tokenize(String sentence) {
        int[] tokens = new int[maxSentenceLength];
        IntBuffer buffer = IntBuffer.wrap(tokens);
        Arrays.stream(wordGenerator.getWords(sentence)).limit(maxSentenceLength).forEachOrdered(w -> {
            int token = tokenMap.getOrDefault(w, UNKNOWN_VALUE);
            buffer.put(token);
        });
        return tokens;
    }

    static class WordGenerator {

        private static final String[] SPECIAL_TOKENS = {"CUSTOM_MASK",
                "CUSTOM_UNKNOWN",
                "CUSTOM_AT",
                "CUSTOM_URL",
                "CUSTOM_NUMBER",
                "CUSTOM_BREAK"};

        /**
         * Tokenizes a sentence into individual words.
         *             Converts Unicode punctuation into ASCII if that option is set.
         *             Ignores sentences with Unicode if that option is set.
         *             Returns an empty list of words if the sentence has Unicode and
         *             that is not allowed.
         */
        public String[] getWords(String sentence) {
            sentence = sentence.trim().toLowerCase();
            sentence = convertLinebreaks(sentence);
            return TextUtils.split(sentence, " ");
        }

        private static String convertLinebreaks(String text) {
            return text.replaceAll("[\n\r]", " " + SPECIAL_TOKENS[5] + " ");
        }
    }
}
