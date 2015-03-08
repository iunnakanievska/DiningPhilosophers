package com.iunna.concurrent.synchronization.diningphilosophers;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Solution of the Dining Philosopher Synchronization Problem.
 */
public class DiningPhilosopherProblemSolution {

    // How many to test with.
    private static final int NUMBER_OF_PHILOSOPHERS = 5;
    // Meeting time - 25 seconds
    private static final int MEETING_TIME_IN_MILLIS = 25 * 1000;

    public static void main(String args[]) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_PHILOSOPHERS);

        Philosopher[] philosophers = new Philosopher[NUMBER_OF_PHILOSOPHERS];
        // One chopstick between each pair of the Philosophers
        Chopstick[] chopsticks = new Chopstick[NUMBER_OF_PHILOSOPHERS];

        try {
            for (int i = 0; i < NUMBER_OF_PHILOSOPHERS; i++) {
                chopsticks[i] = new Chopstick(i);
            }

            for (int i = 0; i < NUMBER_OF_PHILOSOPHERS; i++) {
                philosophers[i] = new Philosopher(i, chopsticks[i], chopsticks[(i + 1) % NUMBER_OF_PHILOSOPHERS]);
                executorService.execute(philosophers[i]);
            }

            // Main thread shall sleep till the meeting's end
            Thread.sleep(MEETING_TIME_IN_MILLIS);

            // Stop all philosophers, as they are tired
            for (Philosopher philosopher : philosophers) {
                philosopher.isTired = true;
            }

        } finally {
            executorService.shutdown();

            // Executor Service need some time for termination
            while (!executorService.isTerminated()) {
                Thread.sleep(1000);
            }

            // Some statistics
            for (Philosopher philosopher : philosophers) {
                System.out.println(philosopher.curriculumVitae());
            }
        }
    }

    /**
     * Chopstick
     */
    public static class Chopstick {
        private static final int DEFAULT_LOCK_ATTEMPT_TIME = 10;
        // Chopstick ID.
        private final int id;
        // Lock to ensure that only one Philosopher can grab Chopstick at any moment.
        private Lock grab = new ReentrantLock();

        public Chopstick(int id) {
            this.id = id;
        }

        /**
         * The ability for an @{Philosopher} to grab the @{Chopstick}.
         *
         * @param philosopher @{Philosopher} which grab the @{Chopstick}
         * @param position    the @{Chopstick}'s position relative to the @{Philosopher}
         * @return {true} if grab attempt is successful, otherwise - {false}
         * @throws InterruptedException if the current thread is interrupted
         *                              while acquiring the lock (and interruption of lock
         *                              acquisition is supported)
         */
        public boolean grab(Philosopher philosopher, String position) throws InterruptedException {
            if (grab.tryLock(DEFAULT_LOCK_ATTEMPT_TIME, TimeUnit.MILLISECONDS)) {
                System.out.println(String.format("%s has grabbed %s %s", philosopher, position, this));
                return true;
            }
            return false;
        }

        /**
         * The ability for an @{Philosopher} to put the @{Chopstick}.
         *
         * @param philosopher @{Philosopher} which grab the @{Chopstick}
         * @param position    the @{Chopstick}'s position relative to the @{Philosopher}
         */
        public void put(Philosopher philosopher, String position) {
            grab.unlock();
            System.out.println(String.format("%s has put %s %s", philosopher, position, this));
        }

        @Override
        public String toString() {
            return "Chopstick #" + id;
        }
    }

    /**
     * Philosopher
     */
    public static class Philosopher implements Runnable {
        // The bound of the random values
        public static final int BOUND = 1000;

        private static final String LEFT = "left";
        private static final String RIGHT = "right";

        // Philosopher's ID.
        private final int id;
        // Philosopher's name.
        private final String name;
        // The chopsticks on either side of me.
        private final Chopstick leftChopstick;
        private final Chopstick rightChopstick;
        // Indicate if Philosopher is tired
        volatile boolean isTired = false;

        // Counter of the successful attempts to eat
        private int eatingCounter;
        // Counter of the Pondering times
        private int ponderingCounter;

        // The generator of the pseudo-random values for the eating and pondering periods
        private Random randomizer = new Random();


        /**
         * @param id             philosopher's ID
         * @param name           philosopher's name
         * @param leftChopstick  philosopher's left chopstick
         * @param rightChopstick philosopher's right chopstick
         */
        public Philosopher(int id, String name, Chopstick leftChopstick, Chopstick rightChopstick) {
            this.id = id;
            this.name = name;
            this.leftChopstick = leftChopstick;
            this.rightChopstick = rightChopstick;
        }

        /**
         * @param id             philosopher's ID
         * @param leftChopstick  philosopher's left chopstick
         * @param rightChopstick philosopher's right chopstick
         */
        public Philosopher(int id, Chopstick leftChopstick, Chopstick rightChopstick) {
            this(id, "Philosopher #" + id, leftChopstick, rightChopstick);
        }

        @Override
        public void run() {

            try {
                while (!isTired) {
                    // Time to ponder
                    ponder();
                    // Pondering makes philosopher hungered
                    if (leftChopstick.grab(this, LEFT)) {
                        if (rightChopstick.grab(this, RIGHT)) {
                            // Time to eat
                            eat();
                            // Clean up...
                            rightChopstick.put(this, RIGHT);
                        }
                        // ...
                        leftChopstick.put(this, LEFT);
                    }
                    // ... and back to the pondering
                }
            } catch (Exception e) {
                // Prevent crashes caused by exceptions that occurred inside the loop
                System.out.println("Something terrible occurred with " + this);
                e.printStackTrace();
            }
        }

        /**
         * Ability to ponder
         *
         * @throws InterruptedException
         */
        private void ponder() throws InterruptedException {
            System.out.println(this + " is pondering");
            ++ponderingCounter;
            Thread.sleep(randomizer.nextInt(BOUND));
        }

        /**
         * Ability to eat
         *
         * @throws InterruptedException
         */
        private void eat() throws InterruptedException {
            System.out.println(this + " is eating");
            eatingCounter++;
            Thread.sleep(randomizer.nextInt(BOUND));
        }

        /**
         * Ability to obtain statistics
         *
         * @return statistics string
         */
        public String curriculumVitae() {
            return String.format("%s has pondered %d times and ate %d times", name, ponderingCounter, eatingCounter);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}