import java.util.Random;
import java.util.concurrent.*;
import java.util.ArrayList;

class Main {
    static final int NUM_THREADS = 8;
    static final int MIN_TEMP = -100;
    static final int MAX_TEMP = 70;
    public static boolean takeReading = true;

    public static void main(String[] args) throws Exception {
        ArrayList<Thread> threads = new ArrayList<Thread>(NUM_THREADS);

        for(int i = 0; i < NUM_THREADS; i++) {
            final int id = i;
        threads.add(new Thread(new Runnable() {
            private final int ID = id;
            private int x = 0;

            public void run() {
                while (takeReading) {
                    // System.out.println("T" + ID + ": " + x++);
                    try {
                        Thread.sleep(Long.MAX_VALUE);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }));
    }

        long start = System.nanoTime();
        for (Thread t : threads) {
            t.start();
        }

        for (int i = 0; i < 100; i++) {
            Thread.sleep(100);
            for (Thread t : threads) {
                t.interrupt();
            }
        }

        // Stop all threads
        takeReading = false;
        for (Thread t : threads) {
            t.interrupt();
            t.join();
        }

        double execTime = (double) (System.nanoTime() - start) / Math.pow(10, 9);
        System.out.println("Execution time: " + execTime + " ms");

        // // ExecutorService to run threads
        // ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        // CompletionService<Void> completionService = new
        // ExecutorCompletionService<>(executor);

        // for (int i = 0; i < NUM_THREADS; i++) {
        // final int id = i;
        // completionService.submit(new Callable<Void>() {
        // private final int ID = id;
        // private final Random rand = new Random();

        // public Void call() {
        // while (takeReading) {

        // try {
        // Thread.sleep(1000000);
        // } catch (InterruptedException e) {
        // System.out.println("interrupted " + ID);
        // break;
        // // continue;
        // }
        // }

        // return null;
        // }
        // });
        // }

        // for (int i = 0; i < NUM_THREADS; i++) {
        // completionService.take();
        // }

    }
}