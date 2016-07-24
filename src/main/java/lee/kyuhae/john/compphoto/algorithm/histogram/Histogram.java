package lee.kyuhae.john.compphoto.algorithm.histogram;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import java.util.Arrays;

import lombok.Getter;

/**
 * Histogram -- This class is translated from the following corresponding original work.
 * (tmpfuse/histogram.h, tmpfuse/histogram.cpp).
 *
 * Created by john.lee on 7/19/16.
 */
@Slf4j
class Histogram {
    private class Channel {
        private static final int MIN = 0;
        private static final int MAX = 256;
        private static final int NUM_BINS = 20;
        private static final float BIN_SIZE = ( MAX - MIN ) / (float) NUM_BINS;

        private int totalNumDataPoint = 0;
        private int peakBinIdx = -1;
        private int[] histogram;
        @Getter private double variance;

        Channel() {
            this.histogram = new int[NUM_BINS];
            Arrays.fill(histogram, 0);
        }

        void addValue(int val) {
            if (val < MIN || val > MAX) {
                log.error("A given data with value " + val + ".");
                throw new IllegalArgumentException("Data should be between " + MIN + " and " + MAX
                        + ". Given " + val + ".");
            }

            int binIdx = (int) ( val / BIN_SIZE );
            histogram[binIdx]++;
            totalNumDataPoint++;

            if (peakBinIdx < 0 || histogram[binIdx] > histogram[peakBinIdx]) {
                peakBinIdx = binIdx;
            }
        }

        void computeVariance() {
            double mean = 0.0;
            for (int i = 0; i < NUM_BINS; i++) {
                mean += histogram[i] * ((i + 1) * BIN_SIZE);
            }
            mean /= totalNumDataPoint;

            variance = 0.0;
            for (int i = 0; i < NUM_BINS; i++) {
                variance += histogram[i] * (((i + 1) * BIN_SIZE - mean) * (i * BIN_SIZE - mean));
            }
        }

        double getProbability(int val) {
            int binIdx = (int) (val / BIN_SIZE);
            return histogram[binIdx] / (double) totalNumDataPoint;
        }
    }

    class Pixel {
        private final Channel rChannel = new Channel();
        private final Channel gChannel = new Channel();
        private final Channel bChannel = new Channel();

        void addValues(double[] values) {
            addValues((int) values[0], (int) values[1], (int) values[2]);
        }

        void addValues(int r, int g, int b) {
            this.rChannel.addValue(r);
            this.gChannel.addValue(g);
            this.bChannel.addValue(b);
        }

        void computeVariance() {
            this.rChannel.computeVariance();
            this.gChannel.computeVariance();
            this.bChannel.computeVariance();
        }

        double getProbability(double[] rgbValues) {
            return getProbability((int) rgbValues[0], (int) rgbValues[1], (int) rgbValues[2]);
        }

        double getProbability(int r, int g, int b) {
            return rChannel.getProbability(r)
                    * gChannel.getProbability(g)
                    * bChannel.getProbability(b);
        }
    }

    private static final int MIN_REQUIRED_NUM_IMAGES = 2;
    private final Pixel[] pixels;
    private final int width, height;
    private final Mat[] images;

    Histogram(final Mat[] images) {
        // Check that at least MIN_REQUIRED_NUM_IMAGES are given.
        if (images.length < MIN_REQUIRED_NUM_IMAGES) {
            throw new IllegalArgumentException("Mininum of " + MIN_REQUIRED_NUM_IMAGES +
                    " images required. Given " + images.length + " images.");
        }

        // Assuming that all images are of the same height and same weights.
        width = images[0].width();
        height = images[0].height();
        this.pixels = new Pixel[width * height];
        this.images = images;

    }

    void compute() {
        for (int col = 0; col < width; col++) {
            for (int row = 0; row < height; row++) {
                int pixelLocation = row * width + col;
                pixels[pixelLocation] = new Pixel();
                for (Mat image : images) {
                    // openCV expects row first.
                    double[] pixelValues = image.get(row, col);
                    pixels[pixelLocation].addValues(pixelValues);
                }
                pixels[pixelLocation].computeVariance();
            }
        }
    }

    Pixel getPixel(int col, int row) {
        int pixelLocation = row * width + col;
        return pixels[pixelLocation];
    }
}
