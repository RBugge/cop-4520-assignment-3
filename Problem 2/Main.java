import java.util.Random;
import java.util.concurrent.*;

class Main {
    static final int NUM_THREADS = 8;
    static final int MIN_TEMP = -100;
    static final int MAX_TEMP = 70;
    public static boolean takeReading = true;

    public static void main(String[] args) throws Exception {
        // Thread threads[] = new Thread[NUM_THREADS];

        // Thread t1 = new Thread(new Runnable() {
        //     int i = 0;
        //     public void run() {
        //         while (takeReading) {
        //             System.out.println("T1: " + i++);
        //             try {
        //                 Thread.sleep(Long.MAX_VALUE);
        //             } catch (InterruptedException e) {
        //                 System.out.println("interrupted");
        //                 break;
        //                 // TODO Auto-generated catch block
        //                 // e.printStackTrace();
        //             }
        //         }
        //     }
        // });

        // t1.start();
        // // takeReading = false;
        // t1.interrupt();
        // t1.join();

        // ExecutorService to run threads
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);

        for (int i = 0; i < NUM_THREADS; i++) {
            final int id = i;
            completionService.submit(new Callable<Void>() {
                private final int ID = id;
                private final Random rand = new Random();

                public Void call() {
                    while (takeReading) {

                        try {
                            Thread.sleep(1000000);
                        } catch (InterruptedException e) {
                            System.out.println("interrupted");
                            break;
                            // continue;
                        }
                    }

                    return null;
                }
            });
        }

        // for(int i = 0; i < 10; i++) {
        //     // completionService.notifyAll();
        // }
        // takeReading = false;
        // // completionService.notifyAll();




        for(int i = 0; i < NUM_THREADS; i++) {
            completionService.take();
        }

    }
}