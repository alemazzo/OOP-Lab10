package it.unibo.oop.lab.reactivegui03;


import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * This is a first example on how to realize a reactive GUI.
 */
public final class AnotherConcurrentGUI extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final double WIDTH_PERC = 0.2;
    private static final double HEIGHT_PERC = 0.1;
    private final JLabel display = new JLabel();
    private final JButton up = new JButton("up");
    private final JButton down = new JButton("down");
    private final JButton stop = new JButton("stop");

    /**
     * Builds a new CGUI.
     */
    public AnotherConcurrentGUI() {
        super();
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setSize((int) (screenSize.getWidth() * WIDTH_PERC), (int) (screenSize.getHeight() * HEIGHT_PERC));
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        final JPanel panel = new JPanel();
        panel.add(display);
        panel.add(up);
        panel.add(down);
        panel.add(stop);
        this.getContentPane().add(panel);
        this.setVisible(true);
        
        /*
         * Create the counter agent and start it. This is actually not so good:
         * thread management should be left to
         * java.util.concurrent.ExecutorService
         */
        final Agent agent = new Agent();
        new Thread(agent).start();
        
        /*
         * Create the timer agent and start it.
         */
        final TimerAgent timer = new TimerAgent(5_000, () -> {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    AnotherConcurrentGUI.this.stop.doClick();
                });
            } catch (InvocationTargetException | InterruptedException e1) {
                e1.printStackTrace();
            }           
        });
        new Thread(timer).start();
        
        
        /*
         * Register a listener that set the increment positive
         */
        up.addActionListener( e -> {
            agent.setStep(1);
        });
        
        /*
         * Register a listener that set the increment negative
         */
        down.addActionListener( e -> {
            agent.setStep(-1);
        });
        
        /*
         * Register a listener that stops it
         */
        stop.addActionListener( e -> {
            agent.stopCounting();
            up.setEnabled(false);
            down.setEnabled(false);
            stop.setEnabled(false);
        });
    }

    /*
     * The counter agent is implemented as a nested class. This makes it
     * invisible outside and encapsulated.
     */
    private class TimerAgent implements Runnable {

        private static final int DEFAULT_SLEEP_TIME = 10_000;
        
        private int sleepTime;
        private Runnable function;
        
        public TimerAgent(final int sleepTime, final Runnable function){
            this.sleepTime = sleepTime;
            this.function = function;
        }
        public TimerAgent(final Runnable function) {
            this(DEFAULT_SLEEP_TIME, function);
        }
        
        @Override
        public void run() {
            try {
                Thread.sleep(this.sleepTime);
                this.function.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        }

    }
    
    /*
     * The counter agent is implemented as a nested class. This makes it
     * invisible outside and encapsulated.
     */
    private class Agent implements Runnable {
        /*
         * Stop is volatile to ensure visibility. Look at:
         * 
         * http://archive.is/9PU5N - Sections 17.3 and 17.4
         * 
         * For more details on how to use volatile:
         * 
         * http://archive.is/4lsKW
         * 
         */
        private volatile boolean stop;
        private int counter;
        private volatile int step = 1;

        @Override
        public void run() {
            while (!this.stop) {
                try {
                    /*
                     * All the operations on the GUI must be performed by the
                     * Event-Dispatch Thread (EDT)!
                     */
                    SwingUtilities.invokeAndWait(() -> AnotherConcurrentGUI.this.display.setText(Integer.toString(Agent.this.counter)));
                    this.counter += step;
                    Thread.sleep(100);
                } catch (InvocationTargetException | InterruptedException ex) {
                    /*
                     * This is just a stack trace print, in a real program there
                     * should be some logging and decent error reporting
                     */
                    ex.printStackTrace();
                }
            }
        }

        /**
         * External command to stop counting.
         */
        public void stopCounting() {
            this.stop = true;
        }
        
        
        /**
         * External command to set step value.
         */
        public void setStep(final int step) {
            this.step = step;
        }
    }
    
}
