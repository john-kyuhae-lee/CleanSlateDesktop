package lee.kyuhae.john.compphoto.algorithm.histogram;

import lee.kyuhae.john.compphoto.algorithm.Coordinate;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;

/**
 * Maximum Likelihood Object(MLO) Energy Minimizer
 * Enabled by max histogram cut algorithm
 *
 * Created by john.lee on 7/19/16.
 */
@Slf4j
public class MLOEnergyMinimizer extends EnergyMinimizer {
    /**
     * Max Histogram Cut related Constants.
     */
    private static final int MAX_ITERATION = 100;
    private static final float MAX_PROBABILITY = 1.0f;

    /**
     * Interaction Penalty related constants.
     */
    private static final float POTTS_INTERACTION_ENERGY_CONSTANT = 0.000001f;
    private static final float REGULAR_INTERACTION_ENERGY_CONSTANT = 0.1f;

    private final Histogram histogram;

    public MLOEnergyMinimizer(Mat[] images, short[] labels) {
        this(images, labels, ALPHA_EXPANSION_FLAG_DEFAULT);
    }

    public MLOEnergyMinimizer(Mat[] images, short[] labels, boolean expansionFlag) {
        super(images, labels, expansionFlag);

        this.histogram = new Histogram(images);
        log.debug("Starting to compute histogram for the given images.");
        histogram.compute();
        log.debug("Completed histogram computation.");
    }

    private double getDataCost(Coordinate point, short d) {
        if (histogram == null) {
            log.debug("Calling getDataCost when histogram has not set.");
            throw new IllegalStateException("Histogram has not set!");
        }

        Histogram.Pixel histogramPixel = histogram.getPixel(point.getCol(), point.getRow());
        double[] rgbValues = images[d].get(point.getRow(), point.getCol());
        double probability = histogramPixel.getProbability(rgbValues);

        if (probability < 0 || probability > MAX_PROBABILITY) {
            log.debug("Probability is " + probability + ", Your codes seem to be broken. " +
                    "Have fun debugging :)");
            throw new IllegalStateException("Probability is not within the limit.");
        }

        // Info: If you want to do a min histogram, return the probability as is.
        return MAX_PROBABILITY - probability;
    }

    double BVZDataPenalty(Coordinate point, short d) {
        // Warn: Make sure 'transform' doesn't apply to my case.
        // If it does apply, needs to implement _displace() function.
        if (point.greaterThanOrEqualTo(ZERO_COORDINATE) && point.smallerThan(coordinateLimit)) {
            return getDataCost(point, d);
        } else {
            return INFINITE_CAPACITY;
        }
    }

    @Override
    double BVZInteractionPenalty(
            Coordinate cPoint, Coordinate nPoint, short cLabel, short nLabel) {
        double M = super.BVZInteractionPenalty(cPoint, nPoint, cLabel, nLabel);

        if (M == 0) {
            return M;
        }

        return POTTS_INTERACTION_ENERGY_CONSTANT + (REGULAR_INTERACTION_ENERGY_CONSTANT * M);
    }

    public void compute() {
        double energy, energyOld;
        int stepCounter = 0;

        energy = BVZComputeEnergy();
        log.debug("Starting energy: " + energy);
        for (int i = 0; i < MAX_ITERATION; i++) {
            for (short step = 0; step < images.length && stepCounter < images.length; step++) {
                energyOld = energy;
                energy = BVZExpand(step, energyOld);

                if (energyOld == energy) {
                    stepCounter++;
                } else {
                    stepCounter = 0;
                }

                log.debug("i: " + i + ", step: " + step + ", stepCounter: " + stepCounter
                        + ", energy: " + energy + ", energyOld: " + energyOld);

                // TODO: Put an event-driven stop functionality.
                // This could take awhile, and it seems this is what the original authors did.
            }
        }
    }

    public double getCurrentDataPenalty(Coordinate cPoint) {
        if ( cPoint.greaterThanOrEqualTo(ZERO_COORDINATE) &&
                cPoint.smallerThan(coordinateLimit) ) {
            return BVZDataPenalty(cPoint, labels[cPoint.getOneDimensionalIndex(width)]);
        } else {
            String message = "Received coordinate outside the range: " + cPoint.toString();
            log.debug(message);
            throw new IllegalArgumentException(message);
        }
    }

    public double getCurrentMaxInteractionPenalty(Coordinate cPoint) {
        if ( cPoint.greaterThanOrEqualTo(ZERO_COORDINATE) &&
                cPoint.smallerThan(coordinateLimit) ) {
            double maxPenalty = Double.MIN_VALUE;
            int col = cPoint.getCol();
            int row = cPoint.getRow();
            Coordinate nPoint;
            if (col > 0) {
                nPoint = new Coordinate(col - 1, row);
                maxPenalty = Math.max(maxPenalty, BVZInteractionPenalty(cPoint, nPoint,
                        labels[cPoint.getOneDimensionalIndex(width)],
                        labels[nPoint.getOneDimensionalIndex(width)]));
            }

            if (col < width - 1) {
                nPoint = new Coordinate(col + 1, row);
                maxPenalty = Math.max(maxPenalty, BVZInteractionPenalty(cPoint, nPoint,
                        labels[cPoint.getOneDimensionalIndex(width)],
                        labels[nPoint.getOneDimensionalIndex(width)]));
            }

            if (row > 0) {
                nPoint = new Coordinate(col, row - 1);
                maxPenalty = Math.max(maxPenalty, BVZInteractionPenalty(cPoint, nPoint,
                        labels[cPoint.getOneDimensionalIndex(width)],
                        labels[nPoint.getOneDimensionalIndex(width)]));
            }

            if (row < height - 1) {
                nPoint = new Coordinate(col, row + 1);
                maxPenalty = Math.max(maxPenalty, BVZInteractionPenalty(cPoint, nPoint,
                        labels[cPoint.getOneDimensionalIndex(width)],
                        labels[nPoint.getOneDimensionalIndex(width)]));
            }

            return maxPenalty;
        } else {
            String message = "Received coordinate outside the range: " + cPoint.toString();
            log.debug(message);
            throw new IllegalArgumentException(message);
        }
    }
}
