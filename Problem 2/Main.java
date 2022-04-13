import java.util.Random;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class Main {
    // Parameters/Constants
    static final int NUM_THREADS = 8;
    static final int MIN_TEMP = -100;
    static final int MAX_TEMP = 70;
    static final int HOURS = 2;
    static final int NUM_INTERVALS = HOURS * 60;
    static final int TEMP_RANGE = MAX_TEMP - MIN_TEMP;
    static final List<Integer> MIN_LIST = Arrays.asList(MIN_TEMP, MIN_TEMP, MIN_TEMP, MIN_TEMP, MIN_TEMP);
    static final List<Integer> MAX_LIST = Arrays.asList(MAX_TEMP, MAX_TEMP, MAX_TEMP, MAX_TEMP, MAX_TEMP);

    // Globals
    static boolean takeReading = true;
    static int count[] = new int[NUM_THREADS];
    static ConcurrentLinkedDeque<Integer> allTemps = new ConcurrentLinkedDeque<>();
    static CountDownLatch latch = new CountDownLatch(NUM_THREADS + 1);

    @SuppressWarnings("unchecked")
    static ArrayList<Integer> temps[] = new ArrayList[NUM_THREADS];
    @SuppressWarnings("unchecked")
    static ArrayList<Integer> minMax[][] = new ArrayList[NUM_THREADS][2];

    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();

        // Initialize thread array
        ArrayList<Thread> threads = new ArrayList<Thread>(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            temps[i] = new ArrayList<Integer>();
        }

        // Initialize minMax array
        for (int i = 0; i < NUM_THREADS; i++) {
            minMax[i][0] = new ArrayList<>(MAX_LIST);
            minMax[i][1] = new ArrayList<>(MIN_LIST);
        }

        // Sensors/Threads
        for (int i = 0; i < NUM_THREADS; i++) {
            final int id = i;
            threads.add(new Thread(new Runnable() {
                private final int ID = id;
                private Random rand = new Random();

                public void run() {
                    CountDownLatch currentLatch = latch;
                    while (takeReading) {
                        currentLatch = latch;
                        count[ID]++;
                        currentLatch.countDown();

                        // Random temp within range
                        int newTemp = rand.nextInt(TEMP_RANGE + 1) - Math.abs(MIN_TEMP);

                        // Add temp to this sensor's record
                        temps[ID].add(newTemp);
                        allTemps.add(newTemp);

                        // Wait until main releases the latch (next time interval)
                        while (currentLatch.getCount() > 0) {
                            try {
                                currentLatch.await();
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                }
            }));
        }

        // Start threads
        for (Thread t : threads) {
            t.start();
        }

        // Using latches to simulate a time intervals of 1 minute
        CountDownLatch oldLatch;
        for (int i = 1; i < NUM_INTERVALS + 1; i++) {
            while (latch.getCount() > 1) {
            }
            oldLatch = latch;
            latch = new CountDownLatch(NUM_THREADS + 1);
            if (i == NUM_INTERVALS) {
                takeReading = false;
            }

            if (i % 60 == 0) {
                System.out.println("____Hour " + i / 60 + " Report____");
                ArrayList<Integer> sortedTemps = new ArrayList<>(allTemps);
                sortedTemps.sort(Comparator.naturalOrder());
                System.out.println("Lowest 5 temps: " + sortedTemps.subList(0, 5));
                System.out.println("Highest 5 temps: " + sortedTemps.subList(sortedTemps.size() - 5, sortedTemps.size()));
                System.out.println("---------------------");

                // Reset arrays
                allTemps.clear();
                for (int j = 0; j < NUM_THREADS; j++) {
                    temps[j].clear();
                }
            }
            oldLatch.countDown();
        }

        // Wait for threads to stop
        for (Thread t : threads) {
            t.join();
        }

        // Count total readings
        int finalCount = 0;
        for (int i = 0; i < NUM_THREADS; i++) {
            if (count[i] != NUM_INTERVALS) {
                System.out.println(
                        "Error: Thread " + i + " : " + count[i] + " readings " + " in " + NUM_INTERVALS + " intervals");
            }
            finalCount += count[i];
        }

        System.out.println("Total Readings: " + finalCount);

        double execTime = (double) (System.nanoTime() - start) / Math.pow(10, 9);
        System.out.println("Execution time: " + execTime + " s");
    }
}