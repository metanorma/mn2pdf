package org.metanorma.fop.utils;

// From https://github.com/joumorisu/SuuKotoba/blob/master/src/SuuKotoba.java

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class containing static utility functions that allow for the conversion of (Arabic) numerals to Japanese kanji form
 * Note that {@link BigInteger} is used to model the number - as the function handles numbers (0, 9.999 * 10^15].
 *
 * @author Joseph Morris
 * @version 1.0
 */

public class JapaneseToNumbers {

    final private static String[] NUMERALS_KANJI = new String[] {"","一","二","三","四","五","六","七","八","九","十"};

    private static Map<BigInteger, String> placeValues;
    static {
        // LinkedHashMap used to preserve order
        placeValues = new LinkedHashMap<BigInteger, String>();
        placeValues.put(new BigInteger("1000000000000"), "兆");
        placeValues.put(new BigInteger("100000000"), "億");
        placeValues.put(new BigInteger("10000"), "万");
        placeValues.put(new BigInteger("1000"), "千");
        placeValues.put(new BigInteger("100"), "百");
        placeValues.put(new BigInteger("10"), "十");
    };

    /**
     * Constructor.
     *
     * @param	num		String form of the numeral to be converted
     */
    public static String numToWord(String num) {
        return numToWord(num, false);
    }

    /**
     * Overloaded Constructor.
     *
     * @param	num		Integer form of the numeral to be converted
     */
    public static String numToWord(Integer num) {
        String numStr = num.toString();
        return numToWord(numStr, false);
    }

    /**
     * Overloaded Constructor.
     *
     * @param	num		BigInteger form of the numeral to be converted
     */
    public static String numToWord(BigInteger num) {
        String numStr = num.toString();
        return numToWord(numStr, false);
    }

    /**
     * Performs the conversion of a numeral (from String) to Japanese kanji form
     * Keeping track of recursive calls is necessary to allow the function to properly
     * determine the unit value (1-9999) of the place-value before moving to the next place-value.
     *
     * I.E.	486900000000 --> 4869 * 10^8 (oku) --> We can just find the written form of 4869 then add oku after it
     * 		4869 --> our recursive call --> 四千八百六十九
     * 		10^8 --> 億
     * 		四千八百六十九  * 億   === 四千八百六十九億
     *
     * @param 	num				the BigInteger number to be converted
     * @param 	isRecursive		whether or not this call is recursive
     * @return	numStr			the final written representation in Japanese kanji
     */
    public static String numToWord(String num, Boolean isRecursive) {
        String numStr = "";

        // Counter will be used to keep track of the remainder as each larger unit is subtracted
        // E.G., 2486954371891 --> minus 2 chou (10^12) --> 486954371891 --> . . .

        BigInteger counter = new BigInteger(num);

        // Walk through the place values from largest to smallest
        for (Map.Entry<BigInteger, String> entry : placeValues.entrySet()) {
            String pvKanji = entry.getValue();
            Object[] results = getUnitStr(entry.getKey(), pvKanji, counter);

            // There are some irregularities with the 10's, 100's, and 1000's place in terms of written representation
            // Namely, "一十"　and "一百" are invalid. In addition, "sen" is used with numbers 1000-1999,
            // and issen with all higher numbers containing 1 in the 1000's place of the unit for that place-value.
            // E.G., 10000000 --> 1000 * 10000 (man) --> 一千万 (issenman)

            String strVal = (String) results[0];

            if (pvKanji.equals("十") || pvKanji.equals("百")) {
                if (strVal.startsWith("一")) {
                    strVal = pvKanji;
                }
            }

            if (pvKanji.equals("千") && !isRecursive) {
                if (strVal.startsWith("一千")) {

                    // If numStr is empty at this point, it means that num >= 9999
                    // and therefore 一 shouldn't be placed before 千
                    if (numStr.isEmpty()) {
                        strVal = strVal.substring(1);
                    }

                }
            }

            numStr += strVal;
            counter = (BigInteger) results[1];
        }

        // Tack on the one's place value
        numStr += NUMERALS_KANJI[counter.intValue()];

        return numStr;
    }

    /**
     * This recursive function is the core of the library. It determines the number of units within a place-value.
     * It uses recursive calls to break numbers down into chunks of 4 digits (the place-value system of Japanese
     * changes in groups of 4 (units), not 3 like the US) in order to determine the written form for that place-value.
     * The counter must be updated (subtracting the current place-value) to allow for smaller place values
     * to be determined.
     *
     * @param 	placeValue	The place-value that we are determining
     * @param 	pvKanji		The kanji representation of this place-value
     * @param 	counter		The current number that we are converting
     * @return	results		Array with two elements: [0] = String result of conversion, [1] = updated counter
     */
    private static Object[] getUnitStr(BigInteger placeValue, String pvKanji, BigInteger counter) {
        String unitStr;
        BigInteger numUnit = counter.divide(placeValue);
        Object[] results;

        if  (numUnit.intValue() > 9) {
            // If numUnit > 10 then that means we can use recursion to get the written form for this unit
            unitStr = numToWord(numUnit.toString(), true) + pvKanji;
        } else {
            // Otherwise, we can simply use the 1-9 kanji representations before the place-value
            unitStr = NUMERALS_KANJI[numUnit.intValue()] + pvKanji;
        }

        counter = counter.subtract(numUnit.multiply(placeValue));

        // Don't return anything if there are no units of that place-value present
        results = new Object[] {(numUnit.intValue() == 0) ? "" : unitStr, counter};

        return results;
    }
}
