import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;

class Main {
    // Parameters/Constants
    static final int NUM_THREADS = 8;
    static final int MIN_TEMP = -100;
    static final int MAX_TEMP = 70;
    static final int HOURS = 24;
    static final int NUM_INTERVALS = HOURS * 60;
    static final int TEMP_RANGE = MAX_TEMP - MIN_TEMP;

    // Globals
    static boolean takeReading = true;
    static int count[] = new int[NUM_THREADS];
    static ConcurrentLinkedDeque<Double> allTemps = new ConcurrentLinkedDeque<>();
    static CountDownLatch latch = new CountDownLatch(NUM_THREADS + 1);

    @SuppressWarnings("unchecked")
    static ArrayList<Double> temps[] = new ArrayList[NUM_THREADS];

    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();

        // Initialize thread array
        ArrayList<Thread> threads = new ArrayList<Thread>(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            temps[i] = new ArrayList<Double>();
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
                        double newTemp = rand.nextInt(TEMP_RANGE + 1) - Math.abs(MIN_TEMP);

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
                int hour = (i / 60) - 1;
                System.out.println("____Hour " + hour + " Report____");

                // Calculate 10 min interval of time containing the largest temp difference
                double tempDiff = 0;
                int startIndex = 0;
                int endIndex = 0;
                int sensor0 = 0;
                int sensor1 = 0;
                /**
                 * 50 10-min intervals (0-9) -> (50->59)
                 * For each interval,
                 * Check all readings from each sensor from the start of the interval to the end
                 * of the interval
                 */
                for (int j = 0; j < 50; j++) {
                    for (int k = 0; k < NUM_THREADS; k++) {
                        for (int l = 0; l < NUM_THREADS; l++) {
                            for (int m = j + 1; m < j + 10; m++) {
                                double newDiff = Math.abs(temps[k].get(j) - temps[l].get(m));
                                if (newDiff > tempDiff) {
                                    tempDiff = newDiff;
                                    sensor0 = k;
                                    sensor1 = l;
                                    startIndex = j;
                                    endIndex = m;
                                }
                            }
                        }
                    }
                }
                String timeIntervalStr = String.format("%2d:%02d to %2d:%02d", hour, startIndex, hour,
                        startIndex + 10);
                String sensorStr0 = String.format("Sensor %d (%d:%02d): %fF", sensor0, hour, startIndex,
                        temps[sensor0].get(startIndex));
                String sensorStr1 = String.format("Sensor %d (%d:%02d): %fF", sensor1, hour, endIndex,
                        temps[sensor1].get(endIndex));
                System.out.println("Largest temperature difference (10 min interval): " +
                        timeIntervalStr + " (" + tempDiff + "F)" + "\n\t" + sensorStr0 + "\n\t" + sensorStr1);

                // Sort every temperature reading and print the first and last five readings
                ArrayList<Double> sortedTemps = new ArrayList<>(allTemps);
                sortedTemps.sort(Comparator.naturalOrder());
                System.out.println("Lowest 5 temperature (F): " + sortedTemps.subList(0, 5));
                System.out.println("Highest 5 temperature (F): " +
                        sortedTemps.subList(sortedTemps.size() - 5, sortedTemps.size()));

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