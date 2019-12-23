package com.nestandlove.sms;


import android.content.res.AssetManager;
import org.tensorflow.Operation;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DeepMoji implements AutoCloseable {

//    static {
//        System.loadLibrary("tensorflow_inference");
//    }

    private static final String VOCAB_PATH = "vocabulary.json";
    private static final String MODEL_PATH = "file:///android_asset/deepmoji_graph.pb";

    private static final String INPUT_NAME = "input_tensor";
    private static final String OUTPUT_NAME = "softmax/Softmax";

    private static final int MAX_SENTENCE_LENGTH = 30;

    private final SentenceTokenizer tokenizer;

    private static final int[][] SENTENCES_TOKENIZED =

            {
                    {18, 87, 3497, 1864, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0}, // I love mom's cooking
                    {18, 87, 79, 13, 122, 935, 85, 34, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0}, // I love how you never reply back.
                    {18, 87, 11824, 31, 41, 5113, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0}, // I love cruising with my homies
                    {18, 87, 3745, 31, 2324, 440, 50, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0}, // I love messing with yo mind!!
                    {18, 87, 13, 12, 111, 102, 42, 642, 34,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0}, // I love you and now you're just gone
                    {22, 28, 172, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0}, // This is shit
                    {22, 28, 10, 172, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0}  // This is the shit
            };

    private TensorFlowInferenceInterface tf;

    public DeepMoji(AssetManager assetManager) throws IOException {
        // load the model
        tf = new TensorFlowInferenceInterface(assetManager, MODEL_PATH);

        Iterator<Operation> i = tf.graph().operations();
        while(i.hasNext()) {
            Operation op = i.next();
            System.out.println(op.name());
        }

        // load the vocabulary
        try(InputStream vs = assetManager.open(VOCAB_PATH)) {
            tokenizer = SentenceTokenizer.fromVocabulary(vs, MAX_SENTENCE_LENGTH);
        }

        // self-test
        if(!Arrays.equals(tokenizer.tokenize("I love mom's cooking"), SENTENCES_TOKENIZED[0])) {
            throw new IllegalStateException("boo");
        }
    }

    public int[][] tokenize(String... sentence) {
        int[][] result = new int[sentence.length][MAX_SENTENCE_LENGTH];
        for (int i = 0; i < sentence.length; i++) {
            result[i] = tokenizer.tokenize(sentence[i]);
        }
        return result;
    }

    public List<Scored> score(String... sentences) {

        // tokenize
        int[][] tokens = tokenize(sentences);

        // score
        Tensor<Integer> input = Tensors.create(tokens);
        List<Scored> scored;
        try(Session s = new Session(tf.graph())) {
            List<Tensor<?>> outputs = s.runner().feed(INPUT_NAME, input).fetch(OUTPUT_NAME).run();
            Tensor<Float> scoresT = (Tensor<Float>) outputs.get(0);
            FloatBuffer buffer = FloatBuffer.allocate(scoresT.numElements());
            scoresT.writeTo(buffer);
            buffer.flip();

            scored = Arrays.stream(sentences).map(sentence -> {
                float[] scores = new float[(int) scoresT.shape()[1]];
                buffer.get(scores);
                return new Scored(sentence, scores);
            }).collect(Collectors.toList());
        }

        for (Scored s : scored) {
            System.out.println(s.getSentence() + ": " + s.getPredictions());
        }

        return scored;
    }

    @Override
    public void close() {
        tf.close();
    }

    public static class Scored {
        private final String sentence;
        private final float[] scores;
        private List<Prediction> predictions;

        public Scored(String sentence, float[] scores) {
            this.sentence = sentence;
            this.scores = scores;
            this.predictions = IntStream.range(0, scores.length)
                    .mapToObj(i -> new Prediction(i, scores[i]))
                    .sorted()
                    .limit(5)
                    .collect(Collectors.toList());
        }

        public String getSentence() {
            return sentence;
        }

        public List<Prediction> getPredictions() {
            return predictions;
        }
    }

    public static class Prediction implements Comparable<Prediction> {
        public int emoji;
        public float score;

        public Prediction(int emoji, float score) {
            this.emoji = emoji;
            this.score = score;
        }

        @Override
        public String toString() {
            return String.format("%d (%.2f)", emoji, score);
        }

        @Override
        public int compareTo(Prediction o) {
            if (this.score == o.score) return 0;
            return this.score > o.score ? -1 : 1;
        }

    }
}
