import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicMarkableReference;

class Main {
    // Presents have a tag with unique number
    // Presents were thrown into a large bag with no particular order
    // Minotaur want to create chain of presents (Linked list)
    // Presents in chain are ordered by increasing tag number
    // 4 (threads) servants to help Minotaur to create chain of presents

    /**
     * Actions:
     * 1. Take present from bag, add to to linked list
     * 2. Write thank you card and remove present from linked list
     * 3. Check whether a gift is present in linked list
     */

    /**
     * What could have gone wrong?
     * When linking and unlinking presents, there is a possibility that a
     * present is removed linking the predecessor and successor of the present
     * while a present was just being added, which would cause the present to
     * be lost.
     */

    // 500,000 presents

    static final int NUM_THREADS = 4;
    static final int NUM_PRESENTS = 500000;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // Shuffled array list of presents, represented by integers
        ArrayList<Integer> presentsList = new ArrayList<>();
        for (int i = 0; i < NUM_PRESENTS; i++) {
            presentsList.add(i);
        }
        Collections.shuffle(presentsList);
        ConcurrentLinkedQueue<Integer> bag = new ConcurrentLinkedQueue<>(presentsList);

        ConcurrentLinkedList<Integer> chain = new ConcurrentLinkedList<>();
        // ConcurrentSkipListSet<Integer> set = new ConcurrentSkipListSet<>();

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CompletionService<Integer> completionService = new ExecutorCompletionService<>(executor);
        ArrayList<Future<Integer>> futures = new ArrayList<>();
        // Threads (Servants)
        for (int i = 0; i < NUM_THREADS; i++) {
            final int id = i;
            futures.add(completionService.submit(new Callable<Integer>() {
                private final int ID = id;
                private final Random rand = new Random();

                public Integer call() {
                    int thankYouCards = 0;
                    while (!bag.isEmpty() || !chain.isEmpty()) {
                        if (rand.nextInt(2) == 1) {
                            Integer present = bag.poll();
                            if (present != null) {
                                chain.add(present);
                            }
                        } else {
                            if (chain.removeHead()) {
                                thankYouCards++;
                            }
                        }
                    }
                    return thankYouCards;
                }

            }));
        }

        while(!bag.isEmpty() || !chain.isEmpty()) {

        }

        // Wait for all threads to finish
        for (int i = 0; i < NUM_THREADS; i++) {
            completionService.take();
        }

        int totalThankYouCards = 0;

        for (Future<Integer> future : futures) {
            System.out.println(future.get());
            totalThankYouCards += future.get();
        }

        executor.shutdown();

        // for (int i = 0; i < NUM_PRESENTS; i++) {
        // System.out.println(chain.poll());
        // }

        System.out.println("Total thank you cards: " + totalThankYouCards);

        // chain.print();

        // for(int i = 0; i < NUM_PRESENTS; i++) {
        // if(!chain.contains(i)) {
        // System.out.println("Missing present: " + i);
        // }
        // }

    }

}

// Wait Free List as described in the textbook
// Modified to use Integer value as key instead of hash to ensure order
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
                    if(curr == tail)
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

        if (n != tail && remove(n.item)){
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