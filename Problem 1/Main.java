import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicMarkableReference;

class Main {
    static final int NUM_THREADS = 4;
    static final int NUM_PRESENTS = 500000;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        long start = System.nanoTime();
        // Shuffled array list of presents, represented by integers
        ArrayList<Integer> presentsList = new ArrayList<>();
        for (int i = 0; i < NUM_PRESENTS; i++) {
            presentsList.add(i);
        }
        Collections.shuffle(presentsList);

        // ConcurrentLinkedQueue to represent bag of presents
        ConcurrentLinkedQueue<Integer> bag = new ConcurrentLinkedQueue<>(presentsList);
        ConcurrentLinkedList<Integer> chain = new ConcurrentLinkedList<>();

        // Markable references for each thread for communicating with Minotaur
        @SuppressWarnings (value="unchecked")
        AtomicMarkableReference<Integer> checkPresent[] = new AtomicMarkableReference[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            checkPresent[i] = new AtomicMarkableReference<>(-1, false);
        }

        // ExecutorService to run threads
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CompletionService<Integer> completionService = new ExecutorCompletionService<>(executor);

        /**
         * Servants (Threads)
         * Actions:
         * 1. Take present from bag, add it to linked list
         * 2. Remove present from linked list and write thank you card
         * 3. Check whether a gift is present in linked list
         *
         * One of the first two actions is performed randomly, the third action
         * is performed at the Minotaur's (Main thread) request.
         *
         * These actions are repeated until the bag and linked list are empty.
         */
        for (int i = 0; i < NUM_THREADS; i++) {
            final int id = i;
            completionService.submit(new Callable<Integer>() {
                private final int ID = id;
                private final Random rand = new Random();

                public Integer call() {
                    int thankYouCards = 0;
                    while (!bag.isEmpty() || !chain.isEmpty()) {
                        // Randomly choose action 1 or 2
                        if (rand.nextBoolean()) {
                            Integer present = bag.poll();
                            if (present != null) {
                                chain.add(present);
                            }
                        } else if (chain.removeHead()) {
                            thankYouCards++;
                        }

                        // Action 3
                        if (checkPresent[ID].isMarked()) {
                            chain.contains(checkPresent[ID].getReference());
                            // Print out for action 3, unneeded
                            // if(list.contains(checkPresent[ID].getReference()))
                            //     System.out.println("Present " + checkPresent[ID].getReference() + " is present in the chain.");
                            // else
                            //     System.out.println("Present " + checkPresent[ID].getReference() + " is not present in the chain.");
                            checkPresent[ID].set(-1, false);
                        }
                    }

                    return thankYouCards;
                }
            });
        }

        // Minotaur picks random servant to check if a random present is in the list
        Random rand = new Random();
        while (!bag.isEmpty() || !chain.isEmpty()) {
            // Sleep for a random amount of time between 0 and 10 milliseconds
            Thread.sleep(rand.nextInt(10));

            checkPresent[rand.nextInt(NUM_THREADS)].compareAndSet(-1, rand.nextInt(NUM_PRESENTS), false, true);
        }

        // Wait for all threads to finish and sum thank you cards written
        int totalThankYouCards = 0;
        for (int i = 0; i < NUM_THREADS; i++) {
            totalThankYouCards += completionService.take().get();
        }
        System.out.println("Total thank you cards: " + totalThankYouCards);

        executor.shutdown();
        double execTime = (double) (System.nanoTime() - start) / Math.pow(10, 9);
        System.out.println("Execution time: " + execTime + " s");
    }

}

// Wait Free List as described in the textbook
// Uses Integer value as key instead of hash (when applicable) to ensure natural ordering
class ConcurrentLinkedList<T> {
    private Node head = new Node(null, Integer.MIN_VALUE);
    private Node tail = new Node(null, Integer.MAX_VALUE);

    public ConcurrentLinkedList() {
        head.next = new AtomicMarkableReference<Node>(tail, false);
    }

    private class Node {
        T item;
        int key;
        AtomicMarkableReference<Node> next;

        public Node(T item, int key) {
            this.item = item;
            this.key = key;
        }
    }

    class Window {
        public Node pred, curr;

        Window(Node myPred, Node myCurr) {
            pred = myPred;
            curr = myCurr;
        }
    }

    Window find(Node head, int key) {
        Node pred = null, curr = null, succ = null;
        boolean[] marked = { false };
        boolean snip;
        retry: while (true) {
            pred = head;
            curr = pred.next.getReference();

            while (true) {
                if (curr == tail) {
                    return new Window(pred, curr);
                }
                succ = curr.next.get(marked);
                while (marked[0]) {
                    snip = pred.next.compareAndSet(curr, succ, false, false);
                    if (!snip)
                        continue retry;
                    curr = succ;
                    if (curr == tail)
                        continue retry;
                    succ = curr.next.get(marked);
                }
                if (curr.key >= key)
                    return new Window(pred, curr);
                pred = curr;
                curr = succ;
            }
        }
    }

    public boolean add(T item) {
        int key = item.getClass() == Integer.class ? (Integer) item : item.hashCode();
        // int key = item.hashCode();
        while (true) {
            Window window = find(head, key);
            Node pred = window.pred, curr = window.curr;
            if (curr.key == key) {
                return false;
            } else {
                Node node = new Node(item, key);
                node.next = new AtomicMarkableReference<>(curr, false);
                if (pred.next.compareAndSet(curr, node, false, false)) {
                    return true;
                }
            }
        }
    }

    public boolean remove(T item) {
        int key = item.getClass() == Integer.class ? (Integer) item : item.hashCode();
        // int key = item.hashCode();
        boolean snip;
        while (true) {
            Window window = find(head, key);
            Node pred = window.pred, curr = window.curr;
            if (curr.key != key) {
                return false;
            } else {
                Node succ = curr.next.getReference();
                snip = curr.next.compareAndSet(succ, succ, false, true);
                if (!snip)
                    continue;
                pred.next.compareAndSet(curr, succ, false, false);
                return true;
            }
        }
    }

    public boolean contains(T item) {
        int key = item.getClass() == Integer.class ? (Integer) item : item.hashCode();
        // int key = item.hashCode();
        Node curr = head;
        while (curr.key < key) {
            curr = curr.next.getReference();
        }
        return (curr.key == key && !curr.next.isMarked());
    }

    public boolean removeHead() {
        Node n = head.next.getReference();

        if (n != tail && remove(n.item)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isEmpty() {
        return head.next.getReference() == tail;
    }

    public void print() {
        Node curr = head.next.getReference();
        while (curr.next != null) {
            System.out.println(curr.item);
            if (curr.next != null) {
                curr = curr.next.getReference();
            } else {
                break;
            }
        }
    }
}