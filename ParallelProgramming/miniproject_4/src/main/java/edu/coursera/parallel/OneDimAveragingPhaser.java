package edu.coursera.parallel;

import java.util.concurrent.Phaser;

/**
 * Wrapper class for implementing one-dimensional iterative averaging using
 * phasers.
 */
public final class OneDimAveragingPhaser {
    /**
     * Default constructor.
     */
    private OneDimAveragingPhaser() {
    }

    /**
     * Sequential implementation of one-dimensional iterative averaging.
     *
     * @param iterations The number of iterations to run
     * @param myNew A double array that starts as the output array
     * @param myVal A double array that contains the initial input to the
     *        iterative averaging problem
     * @param n The size of this problem
     */
    public static void runSequential(final int iterations, final double[] myNew,
            final double[] myVal, final int n) {
        double[] next = myNew;
        double[] curr = myVal;

        for (int iter = 0; iter < iterations; iter++) {
            for (int j = 1; j <= n; j++) {
                next[j] = (curr[j - 1] + curr[j + 1]) / 2.0;
            }
            double[] tmp = curr;
            curr = next;
            next = tmp;
        }
    }

    /**
     * An example parallel implementation of one-dimensional iterative averaging
     * that uses phasers as a simple barrier (arriveAndAwaitAdvance).
     *
     * @param iterations The number of iterations to run
     * @param myNew A double array that starts as the output array
     * @param myVal A double array that contains the initial input to the
     *        iterative averaging problem
     * @param n The size of this problem
     * @param tasks The number of threads/tasks to use to compute the solution
     */
    public static void runParallelBarrier(final int iterations,
            final double[] myNew, final double[] myVal, final int n,
            final int tasks) {
        Phaser ph = new Phaser(0);
        ph.bulkRegister(tasks);

        Thread[] threads = new Thread[tasks];

        for (int ii = 0; ii < tasks; ii++) {
            final int i = ii;

            threads[ii] = new Thread(() -> {
                double[] threadPrivateMyVal = myVal;
                double[] threadPrivateMyNew = myNew;

                final int chunkSize = (n + tasks - 1) / tasks;
                final int left = (i * chunkSize) + 1;
                int right = (left + chunkSize) - 1;
                if (right > n) right = n;

                for (int iter = 0; iter < iterations; iter++) {
                    for (int j = left; j <= right; j++) {
                        threadPrivateMyNew[j] = (threadPrivateMyVal[j - 1]
                            + threadPrivateMyVal[j + 1]) / 2.0;
                    }
                    ph.arriveAndAwaitAdvance();

                    double[] temp = threadPrivateMyNew;
                    threadPrivateMyNew = threadPrivateMyVal;
                    threadPrivateMyVal = temp;
                }
            });
            threads[ii].start();
        }

        for (int ii = 0; ii < tasks; ii++) {
            try {
                threads[ii].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A parallel implementation of one-dimensional iterative averaging that
     * uses the Phaser.arrive and Phaser.awaitAdvance APIs to overlap
     * computation with barrier completion.
     *
     * TODO Complete this method based on the provided runSequential and
     * runParallelBarrier methods.
     *
     * @param iterations The number of iterations to run
     * @param myNew A double array that starts as the output array
     * @param myVal A double array that contains the initial input to the
     *              iterative averaging problem
     * @param n The size of this problem
     * @param tasks The number of threads/tasks to use to compute the solution
     */
    public static void runParallelFuzzyBarrier(final int iterations,
            final double[] myNew, final double[] myVal, final int n,
            final int tasks) {

        Phaser ph = new Phaser(0);
        ph.bulkRegister(tasks);

        Thread[] threads = new Thread[tasks];
        for (int ii = 0; ii < tasks; ii++) {
            int i_thread = ii; // WHY WE NEED THIS LINE?
            threads[i_thread] = new Thread(() -> {

                // if we don't have the following two lines
                // all thread can access myVal and myNew
                // there will be a data race
                // where all threads try to change these two pointers
                double[] threadPrivateMyVal = myVal;
                double[] threadPrivateMyNew = myNew;

                for (int k = 0; k < iterations; k++) {  

                    final int chunk_size = (n-1)/tasks + 1;
                    final int left = i_thread * chunk_size + 1;
                    int right = left + chunk_size - 1;
                    // right can't be final because we might change its value later
                    if (right > n) right = n;
    
                    threadPrivateMyNew[left] = (threadPrivateMyVal[left-1] 
                                                + threadPrivateMyVal[left+1])/2.0;
                    threadPrivateMyNew[right] = (threadPrivateMyVal[right-1] 
                                                + threadPrivateMyVal[right+1])/2.0;
    
                    ph.arrive();
                    for (int i = left+1; i < right; i++) 
                        threadPrivateMyNew[i] = (threadPrivateMyVal[i-1] 
                                                + threadPrivateMyVal[i+1])/2.0;
                    ph.awaitAdvance(i_thread);
    
                    double temp[] = threadPrivateMyVal;
                    threadPrivateMyVal = threadPrivateMyNew;
                    threadPrivateMyNew = threadPrivateMyVal;
                }
            });

            threads[ii].start();
        }

        for (int i_thread = 0; i_thread < tasks; i_thread++) {
            try {
                threads[i_thread].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
