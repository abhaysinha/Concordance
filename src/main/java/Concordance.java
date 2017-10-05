import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.BreakIterator;
import java.util.*;
import java.util.stream.Collectors;

/**
 * For given text in English, generates concordance as
 * an alphabetical list of all word occurrences, labeled with word frequencies.
 * Each word is also labelled with the sentence numbers in which each occurrence appeared.
 *
 * <p></p>
 * Example:
 * <p></p>
 * <pre>
 *     String filePath = "doc.txt";
 *     Concordance c = new Concordance();
 *     c.generate(filePath, System.out);
 * </pre>
 */
public class Concordance {

    // Max word padding to use in formatting output word
    private static final int MAX_WORD_PADDING = 30;

    // Max list marker characters
    private static final int MAX_LIST_MARKER_CHARS = 3;

    // Out of bound list character
    private static final String OUT_OF_BOUND_LIST_MARKER = "zzz";

    /**
     * Class to hold count and sentence reference for each word.
     */
    private static class WordStat {
        private List<String> sentenceNums = new ArrayList<>();
        private int count = 0;

        WordStat(int sentenceNum) {
            addNextSentenceNumber(sentenceNum) ;
        }

        /**
         * Increment word count and add sentence number to the list
         * @param sentenceNum int
         */
        void addNextSentenceNumber(int sentenceNum){
            count++;
            sentenceNums.add(Integer.toString(sentenceNum));
        }

        /**
         * Helper class to print the sentence numbers separated by comma.
         * @return Formatted String
         */
        String getSentenceNumString() {
            return sentenceNums.stream().collect(Collectors.joining(","));
        }

        /**
         * Word Count
         * @return int
         */
        int getCount() {
            return count;
        }
    }

    /**
     * Scan English text to parse sentences and then populate word stats from the given text in English
     * @param text Given text
     * @param map Map to store word stats
     */
    private void scanText(String text, Map<String, WordStat> map) {
        BreakIterator breakIterator = BreakIterator.getSentenceInstance(Locale.ENGLISH);
        breakIterator.setText(text);
        int start = breakIterator.first();
        int end = breakIterator.next();
        int sentenceNum = 1;

        while (end != BreakIterator.DONE) {
            String sentence = text.substring(start,end);
            scanSentence(sentence, sentenceNum++, map);
            start = end;
            end = breakIterator.next();
        }

    }

    /**
     * Scan English sentence to populate word stats with word count and sentence number.
     * @param sentence Given sentence
     * @param sentenceNum Sentence number
     * @param map Map to store word stats
     */
    private void scanSentence(String sentence, int sentenceNum, Map<String, WordStat> map){

        BreakIterator breakIterator = BreakIterator.getWordInstance(Locale.ENGLISH);
        breakIterator.setText(sentence);
        int start = breakIterator.first();
        int end = breakIterator.next();

        while (end != BreakIterator.DONE) {
            String word = sentence.substring(start,end);
            if (Character.isLetter(word.charAt(0))) {
                //Use words that start with a letter
                String lcWord = word.toLowerCase();
                WordStat wordStat = map.get(lcWord);
                if (wordStat == null) {
                    map.put(lcWord, new WordStat(sentenceNum));
                }
                else {
                    wordStat.addNextSentenceNumber(sentenceNum);
                }
            }
            start = end;
            end = breakIterator.next();
        }

    }

    /**
     * Converts number to alphabet representation for list display
     * For example - "1" -> "a.", "27" -> ".aa"
     * Numbers above 78 are returned as ".zzz"
     * @param i int
     * @return formatted string
     */
    private String getAlphabeticalCountLabel(int i) {
        StringBuilder sb = new StringBuilder();
        computeAlphabeticalCountLabel(i, sb);
        sb.append(".");
        return sb.toString();
    }

    private void computeAlphabeticalCountLabel(int i, StringBuilder sb) {
        if ( i > 26 * MAX_LIST_MARKER_CHARS) {
            sb.append(OUT_OF_BOUND_LIST_MARKER);
        }
        else {
            if ( i > 26 ) {
                computeAlphabeticalCountLabel(i -26, sb);
            }
            char c = sb.length() == 0 ? (char) (64 + i) : sb.charAt(0);
            sb.append(Character.toString(c).toLowerCase());
        }
    }


    /**
     * For given file with text in English, generates concordance as
     * an alphabetical list of all word occurrences, labeled with word frequencies.
     * Each word is also labelled with the sentence numbers in which each occurrence appeared.
     *
     * @param filePath Path of the text document
     * @param targetOutput Target Output
     * @exception RuntimeException if file
     */
    public void generate(String filePath, PrintStream targetOutput) {

        // Read text from the file
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to open file - %s", filePath));
        }

        // Nothing to generate for empty file.
        if (bytes.length == 0) {
            return;
        }

        // Add word as key to the map. Update WordStat during scan.
        Map<String, WordStat> map = new HashMap<>();

        // Scan the text to populate words with stats
        scanText(new String(bytes), map);

        // Sort words
        List<String> words = map.keySet().stream().sorted().collect(Collectors.toList());

        // Evaluate max padding for words
        Optional<String> maxLenString = words.stream().max(Comparator.comparingInt(String::length));
        int wordPadding = maxLenString.map(String::length).orElse(MAX_WORD_PADDING);

        int i = 0;
        for (String word : words) {
            i++;
            WordStat e = map.get(word);

            // Format list item
            String listEntry = String.format("%-4s %-" + wordPadding + "s {%d:%s}",
                    getAlphabeticalCountLabel(i), word, e.getCount(), e.getSentenceNumString());

            targetOutput.println(listEntry);
        }

    }

    /**
     * Prints the concordance of a document in English
     * @param args Expects args[0] as file path.
     */
    public static void main(String[] args) {

        String filePath = null;
        if (args.length > 0) {
            filePath = args[0];
        }

        if (filePath == null) {
            System.out.println("Please pass file path as argument.");
            System.exit(0);
        }

        try {
            Concordance c = new Concordance();
            c.generate(filePath, System.out);
        } catch (Exception e) {
            System.err.printf("Error during execution - %s%n", e.getMessage());
            e.printStackTrace();
        }
    }
}

