package org.metanorma.fop;

import org.metanorma.utils.LoggerHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.metanorma.Constants.DEBUG;

public class Profiler {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    static ArrayList<String> arraysMethodCalls = new ArrayList<>();

    static Stack<String> stackMethods = new Stack<>();

    public static void addMethodCall(String methodName) {
        debugShowMemoryInfo(false);
        stackMethods.push(methodName);
        String msg = String.join("", Collections.nCopies(stackMethods.size() * 2, " ")) + methodName;
        arraysMethodCalls.add(msg);
    }

    public static void removeMethodCall() {
        debugShowMemoryInfo(true);
        try {
            stackMethods.pop();
        } catch (Exception ex) {};
    }

    public static void printProcessingTime(String methodName, long startTime, String ... params) {
        if (DEBUG) {
            long endTime = System.currentTimeMillis();
            String addon = Arrays.toString(params);
            if (addon.isEmpty()) {
                addon = "(" + addon + ")";
            }
            String msg = String.format(methodName + addon + " processing time: %d milliseconds", endTime - startTime);
            logger.log(Level.INFO, msg);
            arraysMethodCalls.add(String.join("", Collections.nCopies(stackMethods.size() * 2, " ")) + msg);
        }
    }

    public static void printFullProcessingTime() {
        if (DEBUG) {
            logger.log(Level.INFO, "============================");
            logger.log(Level.INFO, "============================");
            logger.log(Level.INFO, "============================");
            for (String msg: arraysMethodCalls) {
                logger.log(Level.INFO, msg);
            }
            logger.log(Level.INFO, "============================");
            logger.log(Level.INFO, "============================");
            logger.log(Level.INFO, "============================");
        }
    }

    public static void debugShowMemoryInfo(boolean after) {
        if (DEBUG) {
            long heapSize = Runtime.getRuntime().totalMemory();
            long heapFreeSize = Runtime.getRuntime().freeMemory();
            long maxSize = Runtime.getRuntime().maxMemory();
            String strAfter = after ? " after" : " before";
            long usedSize = heapSize - heapFreeSize;
            logger.log(Level.INFO, "Java used memory size" + strAfter + ": " + usedSize);
            logger.log(Level.INFO, "Java available memory size" + strAfter + ": " + (maxSize - usedSize));
        }
    }

}
