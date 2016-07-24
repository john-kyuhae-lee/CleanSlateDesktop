package lee.kyuhae.john.compphoto.algorithm;

import lee.kyuhae.john.compphoto.algorithm.histogram.MLOEnergyMinimizer;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.Arrays;

import lombok.Getter;


/**
 * ImageProcessor class that takes set of images and does a computation.
 * The only available mode is MLO (Maximum Likelihood Objective).
 *
 * Flow of the computation:
 * 1. Receive a set of images.
 * 2. Create MLEEnergyMinimizer.
 *  -2a. Creates a histogram per pixel in order to identify the most likely pixel that is background.
 *  -2b. Process and compare energy of each image using maxflow algorithm
 *      so that the final results contains only pixels that generates lowest value for the energy.
 *      This information is stored in the labels.
 * 3. Create the final composite.
 *
 * Acknowledge: Interactive Digital Photomontage by
 *      Aseem Agarwala, Mira Dontcheva, Maneesh Agrawala, Steven Drucker, Alex Colburn,
 *      Brian Curless, David Salesin, and Michael Cohen
 *
 * This class contains logic that are extacted from tmpfuse/compViewer.cpp.
 * Main flow of the work is modeled after the works cited above
 * -- Use of labels and creating the final composite using the results of computations on the labels.
 *
 * Created by john.lee on 7/22/16.
 */
@Slf4j
public class ImageProcessor {
    private static final String TAG = "ImageProcessor";
    private static final double PENALTY_VALUE_CONVERSION_COEFFICIENT = 100.0d;


    private final int width, height;
    private final short[] labels;
    private final MLOEnergyMinimizer energyMinimizer;
    private final Mat[] images;

    /**
     * All products from this class.
     */
    @Getter private Mat compositeImage = null;
    @Getter private Mat labelImage = null;
    @Getter private Mat dataPenaltyImage = null;
    @Getter private Mat interactionPenaltyImage = null;

    public ImageProcessor(Mat[] images) {
        this.width = images[0].width();
        this.height = images[0].height();
        this.images = images;

        // Info: CV_8U may not be the right type... We'll see.
        // CV_8U is for unsigned int 0-255.
        this.labelImage = Mat.ones(height, width, CvType.CV_32S);

        this.labels = new short[width * height];
        Arrays.fill(labels, (short) 0);
        colorLabelImage();

        this.energyMinimizer = new MLOEnergyMinimizer(images, labels);
    }

    public void compute() {
        // This alters labels array.
        log.debug("Starting MLE Minimizer computation.");
        this.energyMinimizer.compute();
        log.debug("Completed MLE Minimizer computation.");

        // Info: This is optional
        // re-coloring of the label images after minimizer computation has ran.
        colorLabelImage();

        // Info: This is optional
        // This creates intermediate penalty data visualization.
        // createPenaltyVisualization();

        // Now, build a composite based on updated labels.
        this.compositeImage = createComposite();
    }

    private void colorLabelImage() {
        int index = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++, index++) {
                RGBVector rgbVector = LabelColor.list.get(labels[index]);
                int[] color = {rgbVector.getR(), rgbVector.getG(), rgbVector.getB()};
                labelImage.put(row, col, color);
            }
        }
    }

    private void createPenaltyVisualization() {
        this.dataPenaltyImage = Mat.ones(height, width, CvType.CV_32S);
        this.interactionPenaltyImage = Mat.ones(height, width, CvType.CV_32S);

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Coordinate cPoint = new Coordinate(col, row);
                int dataPenalty = (int) (PENALTY_VALUE_CONVERSION_COEFFICIENT *
                        energyMinimizer.getCurrentDataPenalty(cPoint));
                int[] dpVals = {dataPenalty, dataPenalty, dataPenalty};
                dataPenaltyImage.put(row, col, dpVals);

                int interactionPenalty = (int) (PENALTY_VALUE_CONVERSION_COEFFICIENT
                        * energyMinimizer.getCurrentMaxInteractionPenalty(cPoint));
                int[] ipVals = {interactionPenalty, interactionPenalty, interactionPenalty};
                interactionPenaltyImage.put(row, col, ipVals);
            }
        }
    }

    private Mat createComposite() {
        Mat composite = Mat.ones(height, width, CvType.CV_32S);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Coordinate cPoint = new Coordinate(col, row);
                int imageIdx = labels[cPoint.getOneDimensionalIndex(width)];
                double[] rgbVals = images[imageIdx].get(row, col);
                composite.put(row, col, rgbVals);
            }
        }

        return composite;
    }
}
