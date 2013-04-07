import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;

/**
 * You are to design a simple Java program where you create two threads, Ping and Pong, to alternately
 * display “Ping” and “Pong” respectively on the console.  The program should create output that looks like this:
 *
 * Ready… Set… Go!
 *
 * Ping!
 * Pong!
 * Ping!
 * Pong!
 * Ping!
 * Pong!
 * Done!
 *
 * It is up to you to determine how to ensure that the two threads alternate printing on the console, and how to
 * ensure that the main thread waits until they are finished to print: “Done!”  The order does not matter (it could
 * start with "Ping!" or "Pong!").
 *
 *  Consider using any of the following concepts discussed in the videos:
 *
 * ·      wait() and notify()
 *
 * ·      Semaphores
 *
 * ·      Mutexes
 *
 * ·      Locks
 *
 * Please design this program in Java without using extra frameworks or libraries (you may use java.util.concurrent)
 * and contain code in a single file.  Someone should be able to run something like “javac Program.java” and “java Program”
 * and your program should successfully run!
 * */
abstract class Pingpong implements Runnable {
    public static void main(String[] args) {
        AtomicBoolean gameLock = new AtomicBoolean();
        AtomicBoolean pongTurn = new AtomicBoolean();
        ExecutorService game = Executors.newFixedThreadPool(2);

        // prep the two players for 3 turns
        Pingpong ping = newPing(2, pongTurn, gameLock);
        Pingpong pong = newPong(2, pongTurn, gameLock);
        game.execute(ping);
        game.execute(pong);

        // start game with Ping going first and wait up to three seconds for it to finish.
        System.out.println("Ready... Set... Go!");
        synchronized(pongTurn) {
            pongTurn.set(false);
            pongTurn.notifyAll(); // TODO: <-- race condition :( Need to sort out who gets to go first.
        }
        synchronized(gameLock) {
            gameLock.set(false);
            gameLock.notifyAll();
        }

        try {
            game.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        System.out.println("Done!");
    }

    public static Pingpong newPing(int turns, AtomicBoolean pongTurn, AtomicBoolean game) {
        return new Pingpong(turns, pongTurn, game) {
            protected void hitBall() throws InterruptedException {
                synchronized(pongTurn) {
                    while(pongTurn.get())
                        pongTurn.wait();
                }

                synchronized(game) {
                    while(!game.get())
                        game.wait();
                }

                System.out.println("Ping!");
                synchronized(pongTurn) {
                    pongTurn.compareAndSet(false, true);
                    pongTurn.notify();
                }
            }
        };
    }

    public static Pingpong newPong(int turns, AtomicBoolean pongTurn, AtomicBoolean game) {
        return new Pingpong(turns, pongTurn, game) {
            protected void hitBall() throws InterruptedException {
                synchronized(pongTurn) {
                    while(!pongTurn.get())
                        pongTurn.wait();
                    pongTurn.compareAndSet(true, false);
                    System.out.println("Pong!");
                    pongTurn.notify();
                }
            }
        };
    }

    protected int turns;
    protected AtomicBoolean pongTurn;
    protected AtomicBoolean game;

    /**
     * A 'Pingpong' instance represents one player in the pingpong game.
     * Here is the scenario:
     *
     * The player is prepared to play a certain number of turns and will
     * only hit the pongTurn when it is his/her turn. Only one player may hit
     * the pongTurn at a time.
     *
     * When the player hits a pongTurn, they yell out their name.
     * */
    public Pingpong(int turns, AtomicBoolean pongTurn, AtomicBoolean game) {
        this.turns = turns;
        this.pongTurn = pongTurn;
        this.game = game;
    }

    /**
     * Attempt to play a certain number of turns. Wait for the
     * ball to be hit back to us before attempting to hit it.
     * */
    public void run() {
        for(int i=0; i < turns; i++) {
            try {
                hitBall();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Hit the ball back to the opponent.
     * */
    protected abstract void hitBall() throws InterruptedException;
}
