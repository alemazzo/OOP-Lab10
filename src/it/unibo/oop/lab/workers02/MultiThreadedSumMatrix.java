package it.unibo.oop.lab.workers02;

import java.util.ArrayList;
import java.util.List;


public class MultiThreadedSumMatrix implements SumMatrix {
    
    private final int nthread;

    /**
     * 
     * @param nthread
     *            no. of thread performing the sum.
     */
    public MultiThreadedSumMatrix(final int nthread) {
        this.nthread = nthread;
    }

    private static class Worker extends Thread {
        private final double[][] matrix;
        private final int startpos;
        private final int nelem;
        private double res;

        /**
         * Build a new worker.
         * 
         * @param matrix
         *            the matrix to sum
         * @param startpos
         *            the initial position for this worker
         * @param nelem
         *            the no. of elems to sum up for this worker
         */
        Worker(final double[][] matrix, final int startpos, final int nelem) {
            super();
            this.matrix = matrix;
            this.startpos = startpos;
            this.nelem = nelem;
        }

        @Override
        public void run() {
            
            final int totalLength = matrix.length * matrix[0].length;
            final int rows = this.matrix.length;
            final int columns = this.matrix[0].length;
            
            System.out.println("Working from position " + startpos + " to position " + (startpos + nelem - 1));
            for (int i = startpos; i < totalLength && i < startpos + nelem; i++) {
                this.res += this.matrix[i / columns][i % rows];
            }
        }

        /**
         * Returns the result of summing up the integers within the matrix.
         * 
         * @return the sum of every element in the matrix
         */
        public double getResult() {
            return this.res;
        }

    }

    @Override
    public double sum(double[][] matrix) {
        final int totalLength = matrix.length * matrix[0].length;
        final int size = totalLength % nthread + totalLength / nthread;
        /*
         * Build a list of wor kers
         */
        final List<Worker> workers = new ArrayList<>(nthread);
        for (int start = 0; start < totalLength; start += size) {
            workers.add(new Worker(matrix, start, size));
        }
        /*
         * Start them
         */
        for (final Worker w: workers) {
            w.start();
        }
        /*
         * Wait for every one of them to finish. This operation is _way_ better done by
         * using barriers and latches, and the whole operation would be better done with
         * futures.
         */
        long sum = 0;
        for (final Worker w: workers) {
            try {
                w.join();
                sum += w.getResult();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
        /*
         * Return the sum
         */
        return sum;
    }

}
