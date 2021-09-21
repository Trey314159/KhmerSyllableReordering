package trey314159.khmerSyllableReordering;

/*
 * A simple command-line driver for line-by-line KhmerSyllableReordering of a
 * text file.
 *
 * MIT License
 *
 * Copyright (c) 2021 Trey Jones
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KhmerSyllableReorder {

    // useful characters and character classes
    private static final String consonant = "[\u1780-\u17A2]";
    private static final String ro = "\u179A";
    private static final String indepVowel = "[\u17A3-\u17B3]";
    private static final String depVowel = "[\u17B6-\u17C5]";
    private static final String coeng = "\u17D2";
    private static final String diacritic = "[\u17C6-\u17D1\u17DD]";
    private static final String regShifter = "[\u17C9\u17CA]";
    private static final String robat = "\u17CC";
    private static final String nonSpacing = "[\u17C6\u17CB\u17CD-\u17D1\u17DD]";
    private static final String spacing = "[\u17C7\u17C8]";
    private static final String zeroWidth = "[\u200B-\u200D\u00AD\u2063]";
    private static final String coengRo = coeng + ro;

    // A syllable is a consonant or independent vowel, followed by zero or more of (i) a
    // subscript cons or indep vowel (coeng + cons or indep vowel), (ii) a sequence of
    // dependent vowels, diacritics, or zero-width characters. We also allow multiple
    // coengs in (i) because they are usually invisible and typos happen.
    private static final String syllDef =
        "(?:" + consonant + "|" + indepVowel + ")" +
        "(?:" + coeng + "+(?:" + consonant + "|" + indepVowel + ")" +
              "|(?:" + depVowel + "|" + diacritic + "|" + zeroWidth + ")+" +
        ")*";

    // match a syllable to reorder
    private static final Pattern syllPat = Pattern.compile(syllDef);

    // a "coeng chunk" is a coeng (or several) plus a cons or indep vowel, and an optional
    // register shifter.
    private static final String chunkDef =
        "(?:" + coeng + "+" +
        "(?:" + consonant + "|" + indepVowel + ")" +
        regShifter + "?)";

    // match a coeng chunk or an individual character.
    private static final Pattern chunkOrCharPat = Pattern.compile(chunkDef + "|.");

    // map of deprecated and obsolete characters that need to be replaced or deleted.
    private static final Map<String, String> regularizeMap = initRegularizeMap();

    // map of characters that need to be merged
    private static final Map<String, String> mergeVowelsMap = initMergeVowelsMap();

    // use the keys of regularizeMap to build regularizePat; they are individual
    // characters, so build a character class [abc]
    private static final Pattern regularizePat =
        Pattern.compile("[" + String.join("", regularizeMap.keySet()) + "]");

    // use the keys of mergeVowelsMap to build mergeVowelsPat; they are sequences of
    // characters, so build a group (ab|cd|ef)
    private static final Pattern mergeVowelsPat =
        Pattern.compile("(" + String.join("|", mergeVowelsMap.keySet()) + ")");

    private static Map<String, String> initRegularizeMap() {
        Map<String, String> map = new HashMap<>();
        map.put("\u17A3", "\u17A2");             // deprecated indep vowel ឣ → អ
        map.put("\u17A4", "\u17A2\u17B6");       // deprecated indep vowel digraph ឤ → អា
        map.put("\u17A8", "\u17A7\u1780");       // obsolete ligature ឨ → ឧក
        map.put("\u17B2", "\u17B1");             // replace ឲ as a variant of ឱ
        map.put("\u17B4", "");                   // delete non-visible inherent vowel (឴)
        map.put("\u17B5", "");                   // delete non-visible inherent vowel (឵)
        map.put("\u17D3", "\u17C6");             // deprecated BATHAMASAT ៓ → NIKAHIT ំ
        map.put("\u17D8", "\u17D4\u179B\u17D4"); // deprecated trigraph ៘ → ។ល។
        map.put("\u17DD", "\u17D1");             // obsolete ATTHACAN ៝ → VIRIAM ៑
        return map;
    }

    private static Map<String, String> initMergeVowelsMap() {
        Map<String, String> map = new HashMap<>();
        map.put("\u17C1\u17B8", "\u17BE");       // replace េ + ី with ើ
        map.put("\u17B8\u17C1", "\u17BE");       // replace ី + េ with ើ
        map.put("\u17C1\u17B6", "\u17C4");       // replace េ + ា  with ោ
        return map;
    }

    private static String replacePatWithMap(String s, Pattern pat, Map<String, String> map) {
        Matcher m = pat.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String charToReplace = m.group();
            m.appendReplacement(sb, map.get(charToReplace));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String reorder(String s) {
        String osyll = s;

        ArrayList<String> coengChunks = new ArrayList<String>();
        ArrayList<String> depVowelChunks = new ArrayList<String>();
        ArrayList<String> regShifterChunks = new ArrayList<String>();
        ArrayList<String> robatChunks = new ArrayList<String>();
        ArrayList<String> nonSpacingChunks = new ArrayList<String>();
        ArrayList<String> spacingChunks = new ArrayList<String>();
        ArrayList<String> allChunks = new ArrayList<String>();

        // pop off the first character as the base char
        String base = Character.toString(s.charAt(0));

        // match chunks for everything after the base char
        Matcher m = chunkOrCharPat.matcher(s.substring(1));

        while (m.find()) {
            String chunk = m.group();
            if (chunk.matches(depVowel)) {
                depVowelChunks.add(chunk);
            } else if (chunk.startsWith(coeng)) {
                // remove duplicate coengs, if any
                chunk = chunk.replaceAll(coeng + "+", coeng);
                coengChunks.add(chunk);
            } else if (chunk.matches(nonSpacing)) {
                nonSpacingChunks.add(chunk);
            } else if (chunk.matches(spacing)) {
                spacingChunks.add(chunk);
            } else if (chunk.matches(regShifter)) {
                regShifterChunks.add(chunk);
            } else if (chunk.equals(robat)) {
                robatChunks.add(chunk);
            }
        }

        // reorder subscript consonants (ro is always last)
        int numCoeng = coengChunks.size() - 1;
        for (int i = 0; i < numCoeng; i++) {
            if (coengChunks.get(i).startsWith(coengRo)) {
                coengChunks.add(coengChunks.get(i));
                coengChunks.set(i, "");
            }
        }

        // merge various chunk types in the right order
        allChunks.add(base);
        allChunks.addAll(regShifterChunks);
        allChunks.addAll(robatChunks);
        allChunks.addAll(coengChunks);
        allChunks.addAll(depVowelChunks);
        allChunks.addAll(nonSpacingChunks);
        allChunks.addAll(spacingChunks);

        // dedupe all the chunks and join in the new order, then merge split vowel
        return replacePatWithMap(
            String.join("", dedupeArrayList(allChunks)),
            mergeVowelsPat, mergeVowelsMap
        );
    }

    // replace duplicate elements with empty string
    private static ArrayList<String> dedupeArrayList(ArrayList<String> myList) {
        for (int i = 1; i < myList.size(); i++) {
            if (myList.get(i).equals(myList.get(i - 1))) {
                myList.set(i - 1, "");
            }
        }
        return myList;
    }

    public static String KhmerSyllableReorder(String s) {

        s = replacePatWithMap(s, regularizePat, regularizeMap);

        Matcher m = syllPat.matcher(s);
        StringBuffer sb = new StringBuffer();
        String syll;
        while (m.find()) {
            syll = m.group();
            if (syll.length() > 1) {
                syll = reorder(syll);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(syll));
        }
        m.appendTail(sb);

        return sb.toString();
    }

}
