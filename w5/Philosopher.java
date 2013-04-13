import java.util.concurrent.*;

public class Philosopher implements Runnable {
    /**
     * Main thread creates the monitors (chopsticks) and philosopher
     * threads and kicks off the meal.
     * */
    public static void main(String[] args) {
        final int NUM_PLATES = 5;
        final int NUM_BITES = 5;

        // set the table
        Chopstick[] chopsticks = new Chopstick[NUM_PLATES];
        for(int plate=0; plate < NUM_PLATES; plate++) {
            chopsticks[plate] = new Chopstick(plate);
        }

        // bring in guests
        Philosopher[] phils = new Philosopher[NUM_PLATES];
        for(int plate=0; plate < NUM_PLATES; plate++) {
            int chop1 = plate;
            int chop2 = (plate+1) % NUM_PLATES;
            int id = plate+1;

            phils[plate] = new Philosopher(id, chopsticks[chop1], chopsticks[chop2], NUM_BITES);
        }

        // seat the guests
        ExecutorService executor = Executors.newFixedThreadPool(NUM_PLATES);
        for(int guest=0; guest < NUM_PLATES; guest++) {
            executor.execute(phils[guest]);
        }

        // serve dinner
        try {
            System.out.println("Dinner is starting!\n");
            for(int plate=0; plate < NUM_PLATES; plate++) {
                synchronized(chopsticks[plate]) {
                    chopsticks[plate].makeAvailable();
                    chopsticks[plate].notifyAll();
                }
            }

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            System.out.println("Dinner is over!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Chopstick is the monitor object. Anyone who wants to use
     * a chopstick must wait until it is available and then declare
     * that this chopstick is being used. When they are done with
     * the chopstick they let everyone know that it is now available.
     * */
    static class Chopstick implements Comparable<Chopstick> {
        private boolean available;
        private int id;
        public Chopstick(int id) {
            this.id = id;
            this.available = false;
        }

        public void makeAvailable() {
            this.available = true;
        }

        public synchronized void pickup() throws InterruptedException {
            while(!available)
                wait();

            available = false;
        }

        public synchronized void putdown() {
            available = true;
            notifyAll();
        }

        public int compareTo(Chopstick other) {
            return this.id - other.id;
        }
    }

    private int id;
    private Chopstick left;
    private Chopstick right;
    private int numBites;

    /**
     * A Philosopher has an ID and references to a left and right chopstick
     * as well as a pre-defined number of bites he or she will take.
     *
     * Since the left chopstick is always picked up first, make sure the
     * left chopstick is always the one with the lower ID in order to
     * prevent deadlock (via resource ordering).
     * */
    public Philosopher(int id, Chopstick chop1, Chopstick chop2, int numBites) {
        this.id = id;
        this.numBites = numBites;

        if(chop1.compareTo(chop2) > 0) {
            this.left = chop1;
            this.right = chop2;
        } else {
            this.left = chop2;
            this.right = chop1;
        }
    }

    /**
     * For each bite, a philosopher will eat and then spend some
     * time thinking.
     * */
    public void run() {
        try {
            for(int biteNum = 0; biteNum < numBites; biteNum++) {
                eating();
                thinking();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * When a philosopher wants to eat, he or she will pick up
     * the left chopstick first and then the right chopstick.
     * */
    private void eating() throws InterruptedException {
        left.pickup();
        System.out.println("Philosopher " + id + " picks up left chopstick.");
        right.pickup();
        System.out.println("Philosopher " + id + " picks up right chopstick.");
        System.out.println("Philosopher " + id + " eats.");
    }

    /**
     * When a philosopher wants to spend some time thinking, he
     * or she will put down the left chopstick and then the
     * right chopstick.
     * */
    private void thinking() {
        left.putdown();
        System.out.println("Philosopher " + id + " puts down left chopstick.");
        right.putdown();
        System.out.println("Philosopher " + id + " puts down right chopstick.");
    }

}
