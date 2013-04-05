import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
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
class Pingpong implements Runnable {
    public static void main(String[] args) {
        ReentrantLock ball = new ReentrantLock();
        Condition pingTurn = ball.newCondition();
        Condition pongTurn = ball.newCondition();
        ExecutorService game = Executors.newFixedThreadPool(2);

        // prep the two players for 3 turns
        ball.lock();
        game.execute(new Pingpong(1, ball, pongTurn, pingTurn, "Ping!"));
        game.execute(new Pingpong(1, ball, pingTurn, pongTurn, "Pong!"));

        // start game with Ping going first and wait up to three seconds for it to finish.
        System.out.println("Ready... Set... Go!");
        pingTurn.signal();
        ball.unlock();
        try {
            game.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        System.out.println("Done!");
    }

    private int turns;
    private Lock ball;
    private Condition myTurn;
    private Condition theirTurn;
    private String name;

    /**
     * A 'Pingpong' instance represents one player in the pingpong game.
     * Here is the scenario:
     *
     * The player is prepared to play a certain number of turns and will
     * only hit the ball when it is his/her turn. Only one player may hit
     * the ball at a time.
     *
     * When the player hits a ball, they yell out their name.
     * */
    public Pingpong(int turns, Lock ball, Condition theirTurn, Condition myTurn, String name) {
        this.turns = turns;
        this.name = name;
        this.ball = ball;
        this.myTurn = myTurn;
        this.theirTurn = theirTurn;
    }

    /**
     * Attempt to play a certain number of turns. Wait for the
     * ball to be hit back to us before attempting to hit it.
     * */
    public void run() {
        for(int i=0; i < turns; i++) {
            try {
                System.err.println(name + " waiting to hit teh ball");
                hitBall();
                System.err.println(name + " should be done w the ball");
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Hit the ball back to the opponent.
     * */
    private void hitBall() throws InterruptedException {
        ball.lock();
        System.err.println(name + " has the ball");
        try {
        System.err.println(name + " waiting for their turn");
            myTurn.await();
            System.out.println(name);
            theirTurn.signal();
        System.err.println(name + " signaled the other player");
        } finally {
            ball.unlock();
        }
    }
}
