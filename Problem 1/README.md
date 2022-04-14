# <ins>Problem 1: The Birthday Presents Party (50 points)</ins>

The Minotaur’s birthday party was a success. The Minotaur received a lot of presents from his guests. The next day he decided to sort all of his presents and start writing “Thank you” cards. Every present had a tag with a unique number that was associated with the guest who gave it. Initially all of the presents were thrown into a large bag with no particular order. The Minotaur wanted to take the presents from this unordered bag and create a chain of presents hooked to each other with special links (similar to storing elements in a linked-list). In this chain (linked-list) all of the presents had to be ordered according to their tag numbers in increasing order. The Minotaur asked 4 of his servants to help him with creating the chain of presents and writing the cards to his guests. Each servant would do one of three actions in no particular order:

1. Take a present from the unordered bag and add it to the chain in the correct location by hooking it to the predecessor’s link. The servant also had to make sure that the newly added present is also linked with the next present in the chain.
2. Write a “Thank you” card to a guest and remove the present from the chain. To do so, a servant had to unlink the gift from its predecessor and make sure to connect the predecessor’s link with the next gift in the chain.
3. Per the Minotaur’s request, check whether a gift with a particular tag was present in the chain or not; without adding or removing a new gift, a servant would scan through the chain and check whether a gift with a particular tag is already added to the ordered chain of gifts or not.

As the Minotaur was impatient to get this task done quickly, he instructed his servants not to wait until all of the presents from the unordered bag are placed in the chain of linked and ordered presents. Instead, every servant was asked to alternate adding gifts to the ordered chain and writing “Thank you” cards. The servants were asked not to stop or even take a break until the task of writing cards to all of the Minotaur’s guests was complete.

After spending an entire day on this task the bag of unordered presents and the chain of ordered presents were both finally empty!

Unfortunately, the servants realized at the end of the day that they had more presents than “Thank you” notes. What could have gone wrong?

Can we help the Minotaur and his servants improve their strategy for writing “Thank you” notes?

Design and implement a concurrent linked-list that can help the Minotaur’s 4 servants with this task. In your test, simulate this concurrent “Thank you” card writing scenario by dedicating 1 thread per servant and assuming that the Minotaur received 500,000 presents from his guests.

## What Could Have Gone Wrong?

When linking and unlinking presents, there is a possibility that a present is removed, linking the predecessor and successor of the present, while a present was just being added in the same position which could cause the servant removing a present to unlink the new gift when linking the predecessor and successor.

## Implemetation

My implementation simulates the scenario by creating 4 threads representing the servants and the main thread representing the minotaur. The bag is represented by java's concurrent linked queue. To simulate the effect of being shuffled, an array list is created first and populated with integers from 0 to 500,000. The list is then shuffled using Java's collections shuffle function and used to initialize the queue. The chain is represented by the wait free list as described in the textbook. The thank you cards are represented by local variables to each servant which are summed together when the threads are completed.

Each servant thread contains a loop that continues until both the bag and chain are empty. Within the loop, the servant randomly chooses to either remove a present from the bag and add it to the chain or remove a present from the chain and increment the thank you cards counter. The servant then checks if it must check if a present is contained in the chain. The last action is simulated by the Minotaur (main thread) occasionally setting a variable using compare and set with the ID of the present to check for and flagging the reference. The servant uses the contains method and resets the present ID and flag.

## Efficiency

By letting the servants alternate randomly, this implementation allows for the chain to grow to any size, and the larger the size the less efficient the wait free linked list becomes as traversals become more costly. However, due to the randomness, the size of the list should not grow too large on average as the servants will removing presents from the chain at about the same rate as they are adding to it.

Efficiency could be improved as each thread is only removing presents from the head of the queue and linked list causing high contention.

## Experimental Evaluation

Runtime of the program averages around `0.93` seconds using `500,000` presents and `4` threads.
