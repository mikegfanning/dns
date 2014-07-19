package org.code_revue.dns.util;

/**
 * Utilities for managing {@link java.lang.Thread}s.
 *
 * @author Mike Fanning
 */
public class ThreadUtils {

    /**
     * Prints information about all of the threads in the current group to standard out.
     */
    public static void printThreadInfo() {
        ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
        Thread[] threads = new Thread[currentGroup.activeCount() + 10];
        int numThreads = currentGroup.enumerate(threads);
        String format = "%1$4s %2$-20s %3$8s %4$-10s";
        System.out.println(String.format(format, "Id", "Name", "Priority", "State"));
        System.out.println("---------------------------------------------");
        for (int i = 0; i < numThreads; i++) {
            Thread thread = threads[i];
            System.out.println(String.format(format, thread.getId(), thread.getName(),
                    thread.getPriority(), thread.getState()));
        }
    }

}
