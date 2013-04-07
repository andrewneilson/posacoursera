import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

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
class Pingpong extends Thread {
    public enum Player {
        PING("Ping!"), PONG("Pong!");
        private String name;

        Player(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private static Object ball = new Object();
    private Player name;
    private AtomicReference<Player> turn;
    private int turns;

    public Pingpong(Player name, int turns, AtomicReference<Player> turn) {
        this.name = name;
        this.turn = turn;
        this.turns = turns;
    }

    public void hitBall() throws InterruptedException {
        synchronized(ball) {
            while(turn.get() != name) {
                ball.wait();
            }

            System.out.println(name);
            if(name == Player.PING)
                turn.set(Player.PONG);
            else
                turn.set(Player.PING);

            ball.notify();
        }
    }

    public void run() {
        try {
            for(int i=0; i < turns; i++) {
                hitBall();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        AtomicReference<Player> turn = new AtomicReference<Player>(Player.PING);
        Pingpong ping = new Pingpong(Player.PING, 3, turn);
        Pingpong pong = new Pingpong(Player.PONG, 3, turn);

        System.out.println("Ready... Set... Go!");
        ping.start();
        pong.start();

        try {
            ping.join();
            pong.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Done!");
        }
    }
}
