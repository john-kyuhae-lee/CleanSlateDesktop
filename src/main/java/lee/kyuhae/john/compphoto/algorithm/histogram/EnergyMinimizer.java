package lee.kyuhae.john.compphoto.algorithm.histogram;


import lee.kyuhae.john.compphoto.algorithm.Coordinate;
import lee.kyuhae.john.compphoto.algorithm.maxflow.Graph;
import lee.kyuhae.john.compphoto.algorithm.maxflow.MaxFlowFinder;
import lee.kyuhae.john.compphoto.algorithm.maxflow.Node;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;

/**
 * EnergyMinimizer.
 *
 * Takes images and compute labels that minimize the energy (data and interaction penalties).
 *
 * Acknowledge:
 * - Interactive Digital Photomontage by
 *      Aseem Agarwala, Mira Dontcheva, Maneesh Agrawala, Steven Drucker, Alex Colburn,
 *      Brian Curless, David Salesin, and Michael Cohen
 *
 * - Fast Approximate Energy Minimization via Graph Cuts by
 *      Yuri Boykov, Olga Veksler, and Ramin Zabih
 *
 * Class contains extracted and translated method from tmpfuse/graphcut/portraitcut.cpp and portraitcut.h
 * Mainly the energy minimization algorithm method prepended with BVZ.
 *
 * Created by john.lee on 7/22/16.
 */
@Slf4j
abstract class EnergyMinimizer {
    private static final long ACTIVE_NODE_INDEX = -1;
    private static final long NON_PRESENT_NODE_INDEX = -2;
    private static final Node ACTIVE_NODE = new Node(ACTIVE_NODE_INDEX);
    private static final Node NON_PRESENT_NODE = new Node(NON_PRESENT_NODE_INDEX);

    static final double INFINITE_CAPACITY = 1000000;

    /**
     * Constants for Interaction Penalty calculation.
     */
    private static final float INTERACTION_PENALTY_COEFFICIENT = 6.0f;
    private static final int NUM_CHANNEL = 3;
    private static final double INTERACTION_PENALTY_EXPANSION_THRESHOLD = 0.0001d;

    /**
     * Constants for BVZ algorithm -- general ones that are used in a multiple BVZ functions.
     */
    static final Coordinate ZERO_COORDINATE = new Coordinate(0, 0);
    private static final Coordinate[] NEIGHBORS = {new Coordinate(1, 0), new Coordinate(0, -1)};

    /**
     * BVZ termA expansion sink-source configuration variables.
     */
    static final boolean ALPHA_EXPANSION_FLAG_DEFAULT = false;
    private boolean alphaSink;
    private int termA;
    private int termB;

    final Mat[] images;
    final int height;
    final int width;
    final short[] labels;
    final Coordinate coordinateLimit;

    EnergyMinimizer(Mat[] images, short[] labels) {
        this(images, labels, ALPHA_EXPANSION_FLAG_DEFAULT);
    }

    EnergyMinimizer(Mat[] images, short[] labels, boolean expansionFlag) {
        this.images = images;
        this.labels = labels;
        this.height = images[0].height();
        this.width = images[0].width();
        this.coordinateLimit = new Coordinate(width, height);
        log.info("Energy Minimizer initialized with {} images, {} height, {} width", images.length, height, width);

        setAlphaSink(expansionFlag);
    }

    private void setAlphaSink(boolean flag) {
        alphaSink = flag;
        if (alphaSink) {
            termA = Graph.SOURCE;
            termB = Graph.SINK;
        } else {
            termA = Graph.SINK;
            termB = Graph.SOURCE;
        }
    }

    abstract double BVZDataPenalty(Coordinate point, short d);

    double BVZInteractionPenalty(
           Coordinate cPoint, Coordinate nPoint, short cLabel, short nLabel) {
        // Warn: For max histogram, I think C_NORMAL type is used - this could be source of trouble.
        if (cLabel >= images.length || nLabel >= images.length) {
            String message = "Received curPointLabel: " + cLabel + ", neighborPointLabel: " + nLabel
                    + ", images.length: " + images.length + "\nLabelValue should be less than image length.";
            log.debug(message);
            throw new IllegalStateException(message);
        }

        // No penalty case.
        if (cLabel == nLabel) {
            return 0.0d;
        }

        // C_NORMAL type interaction penalty calculation.
        // 1. Calculate the difference at point Coordinate.
        double a = 0, M;
        double[] cPointRGB = images[cLabel].get(cPoint.getRow(), cPoint.getCol());
        double[] nPointRGB = images[nLabel].get(cPoint.getRow(), cPoint.getCol());
        for (int c = 0; c < NUM_CHANNEL; c++) {
            int k = (int) cPointRGB[c] - (int) nPointRGB[c];
            // log.debug("Difference at cPoint: {}", k);
            a += (k * k);
        }
        M = Math.sqrt(a);

        // 2. Calculate the difference at neighborPoint Coordinate.
        a = 0;
        cPointRGB = images[cLabel].get(nPoint.getRow(), nPoint.getCol());
        nPointRGB = images[nLabel].get(nPoint.getRow(), nPoint.getCol());
        for (int c = 0; c < NUM_CHANNEL; c++) {
            int k = (int) cPointRGB[c] - (int) nPointRGB[c];
            // log.debug("Difference at nPoint: {}", k);
            a += (k * k);
        }
        M += Math.sqrt(a);
        M /= INTERACTION_PENALTY_COEFFICIENT;
        if (M > INFINITE_CAPACITY) {
            M = INFINITE_CAPACITY;
        }

        return M;
    }

    private boolean isNode(Node n) {
        return n != null && n != ACTIVE_NODE && n != NON_PRESENT_NODE && n.getIndex() >= 0;
    }

    double BVZComputeEnergy() {
        double energy = 0.0;
        for (int row = 0; row < images[0].height(); row++) {
            for (int col = 0; col < images[0].width(); col++) {
                Coordinate cPoint = new Coordinate(col, row);

                // Instead of increment index each iterations using index calculation from the Coordinate.
                // TODO: If something's off, this is changed from the original code.
                short cLabel = labels[cPoint.getOneDimensionalIndex(width)];
                energy += BVZDataPenalty(cPoint, cLabel);

                for (Coordinate adjPoint : NEIGHBORS) {
                    Coordinate nPoint =
                            new Coordinate(cPoint.getCol() + adjPoint.getCol(), cPoint.getRow() + adjPoint.getRow());

                    if (nPoint.greaterThanOrEqualTo(ZERO_COORDINATE) && nPoint.smallerThan(coordinateLimit)) {
                        short nLabel = labels[nPoint.getOneDimensionalIndex(width)];
                        energy += BVZInteractionPenalty(cPoint, nPoint, cLabel, nLabel);
                    }
                }
            }
        }

        return energy;
    }

    double BVZExpand(short a, double energyOld) {
        log.debug("BVZExpand starting with a {}, energyOld {}", a, energyOld);
        double energy = 0.0d;

        Node[] nodeArray = new Node[width * height];
        double[] penaltyArray = new double[width * height];

        // Initializing -- Start of the graph building.
        log.debug("Starting dataPenalty computation.");
        Coordinate cPoint = new Coordinate(0, 0);
        for (cPoint.setRow(0); cPoint.getRow() < height; cPoint.incrementRow()) {
            // Warn: Original code uses index and increment it by 1 each loop.
            for (cPoint.setCol(0); cPoint.getCol() < width; cPoint.incrementColumn()) {
                int index = cPoint.getOneDimensionalIndex(width);
                short cLabel = labels[index];

                if (a == cLabel) {
                    nodeArray[index] = ACTIVE_NODE;
                    energy += BVZDataPenalty(cPoint, cLabel);
                    log.trace("index {}: cLabel {} and a {} are equal. " +
                            "Energy is updated to {}. Continuing.", index, cLabel, a, energy);
                    continue;
                }

                nodeArray[index] = new Node(index);
                double delta = BVZDataPenalty(cPoint, cLabel);
                penaltyArray[index] = BVZDataPenalty(cPoint, a) - delta;
                energy += delta;
                log.trace("index {}: cLabel {}, a {}, energy {}.", index, cLabel, a, energy);
            }
        }
        log.debug("Completed dataPenalty computation.");

        log.debug("Starting InteractionPenalty calculation.");
        for (cPoint.setRow(0); cPoint.getRow() < height; cPoint.incrementRow()) {
            // Warn: Original code uses index and increment it by 1 each loop.
            for (cPoint.setCol(0); cPoint.getCol() < width; cPoint.incrementColumn()) {
                int cIndex = cPoint.getOneDimensionalIndex(width);
                short cLabel = labels[cIndex];
                Node cNode = nodeArray[cIndex];

                // Adding interactionug
                for (Coordinate adjPoint : NEIGHBORS) {
                    Coordinate nPoint =
                            new Coordinate(cPoint.getCol() + adjPoint.getCol(), cPoint.getRow() + adjPoint.getRow());

                    boolean skip = !(nPoint.greaterThanOrEqualTo(ZERO_COORDINATE)
                            && nPoint.smallerThan(coordinateLimit));

                    if (skip) {
                        continue;
                    }

                    int nIndex = nPoint.getOneDimensionalIndex(width);
                    short nLabel = labels[nIndex];
                    Node nNode = nodeArray[nIndex];

                    if (isNode(cNode) && isNode(nNode)) {
                        double penalty00 = BVZInteractionPenalty(cPoint, nPoint, cLabel, nLabel);
                        double penalty0A = BVZInteractionPenalty(cPoint, nPoint, cLabel, a);
                        double penaltyA0 = BVZInteractionPenalty(cPoint, nPoint, a, nLabel);

                        double delta = penalty00 < penalty0A ? penalty00 : penalty0A;
                        if (delta > 0) {
                            penaltyArray[cPoint.getOneDimensionalIndex(width)] -= delta;
                            energy += delta;
                            penalty00 -= delta;
                            penalty0A -= delta;
                        }

                        delta = penalty00 < penaltyA0 ? penalty00 : penaltyA0;
                        if (delta > 0) {
                            penaltyArray[nPoint.getOneDimensionalIndex(width)] -= delta;
                            energy += delta;
                            penalty00 -= delta;
                            penaltyA0 -= delta;
                        }

                        if (penalty00 > INTERACTION_PENALTY_EXPANSION_THRESHOLD) {
                           log.error("penalty00 is over the threshold. It is non-metric: " + penalty00);
                        }

                        if (alphaSink) {
                            Graph.addEdge(cNode, nNode, penalty0A, penaltyA0);
                        } else {
                            Graph.addEdge(cNode, nNode, penaltyA0, penalty0A);
                        }
                    } else if (isNode(cNode) && !isNode(nNode)) {
                        // Case where nNode does not exist.
                        double delta = BVZInteractionPenalty(cPoint, nPoint, cLabel, a);
                        penaltyArray[cPoint.getOneDimensionalIndex(width)] -= delta;
                        energy += delta;
                    } else if (!isNode(cNode) && isNode(nNode)) {
                        // Case where nNode does not exist.
                        double delta = BVZInteractionPenalty(cPoint, nPoint, a, nLabel);
                        penaltyArray[nPoint.getOneDimensionalIndex(width)] -= delta;
                        energy += delta;
                    }
                }
            }
        }
        log.debug("Completed Interaction Penalty calculation.");
        /* -- end of the graph building. ready to call MaxFlowFinder */

        MaxFlowFinder maxFlowFinder = new MaxFlowFinder(nodeArray);
        log.debug("Updating source and sink edges.");
        /* Adding source and sink edges */
        for (cPoint.setRow(0); cPoint.getRow() < height; cPoint.incrementRow()) {
            for (cPoint.setCol(0); cPoint.getCol() < width; cPoint.incrementColumn()) {
                Node cNode = nodeArray[cPoint.getOneDimensionalIndex(width)];
                if (isNode(cNode)) {
                    double delta = penaltyArray[cPoint.getOneDimensionalIndex(width)];
                    if (alphaSink) {
                        if (delta > 0) {
                            maxFlowFinder.setTweights(cNode, delta, 0);
                        } else {
                            maxFlowFinder.setTweights(cNode, 0, -delta);
                            energy += delta;
                        }
                    } else {
                        if (delta > 0) {
                            maxFlowFinder.setTweights(cNode, 0, delta);
                        } else {
                            maxFlowFinder.setTweights(cNode, -delta, 0);
                            energy += delta;
                        }
                    }
                }
            }
        }

        log.debug("Finding a maxflow now..");
        energy += maxFlowFinder.findMaxFlow();

        log.debug("After addting maxflow, energy is {}", energy);
        if (energy < energyOld) {
            for (cPoint.setRow(0); cPoint.getRow() < height; cPoint.incrementRow()) {
                for (cPoint.setCol(0); cPoint.getCol() < width; cPoint.incrementColumn()) {
                    Node cNode = nodeArray[cPoint.getOneDimensionalIndex(width)];

                    if (isNode(cNode) && Graph.whatSegment(cNode) == termB) {
                        labels[cPoint.getOneDimensionalIndex(width)] = a;
                    }
                }
            }
            return energy;
        }
        return energyOld;
    }
}
