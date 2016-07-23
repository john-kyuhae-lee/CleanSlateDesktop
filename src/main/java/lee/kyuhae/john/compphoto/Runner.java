package lee.kyuhae.john.compphoto;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import lee.kyuhae.john.compphoto.algorithm.ImageProcessor;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.apache.commons.io.FileUtils;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by john.lee on 7/23/16.
 */
@Slf4j
public class Runner {
    private static final String TEST_IMAGE_FOLDER = "cathedral";
    private static final String LOGBACK_APPENDER_PATH = "log/logback.xml";

    static {
        OpenCV.loadLibrary();
    }

    public static void main(String[] args) {
        // Configure logback.
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();
        JoranConfigurator configurator = new JoranConfigurator();

        try {
            InputStream configStream =
                    FileUtils.openInputStream(new File(LOGBACK_APPENDER_PATH));
            configurator.setContext(loggerContext);
            configurator.doConfigure(configStream);
            configStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Read all .jpg files from the resource folder
        File testImageFolder = new File(TEST_IMAGE_FOLDER);
        File[] imageFiles = testImageFolder.listFiles();

        assert imageFiles != null;

        ArrayList<Mat> imageList = new ArrayList<>();

        for (File imageFile : imageFiles) {
            if (imageFile.isFile()) {
                String fileName = imageFile.getName();
                if (fileName.endsWith(".jpg")) {
                    log.info("Found {}. Reading it into a Mat.", imageFile.getName());
                    Mat image = Highgui.imread(imageFile.getPath());
                    imageList.add(image);
                }
            }
        }

        log.info("Successfully loaded {} images", imageList.size());

        ImageProcessor imageProcessor = new ImageProcessor(imageList.toArray(new Mat[imageList.size()]));

        log.info("Start imageProcessor computing.");
        imageProcessor.compute();
        log.info("Completed the computation.");

        Highgui.imwrite("composite.jpg", imageProcessor.getCompositeImage());
        Highgui.imwrite("label.jpg", imageProcessor.getLabelImage());
    }
}
