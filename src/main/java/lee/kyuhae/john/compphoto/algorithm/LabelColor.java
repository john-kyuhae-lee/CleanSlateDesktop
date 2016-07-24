package lee.kyuhae.john.compphoto.algorithm;

import java.util.ArrayList;

import lombok.Data;

/**
 * Colors to be used for marking labels.
 *
 * Acknowledge: Interactive Digital Photomontage by
 *      Aseem Agarwala, Mira Dontcheva, Maneesh Agrawala, Steven Drucker, Alex Colburn,
 *      Brian Curless, David Salesin, and Michael Cohen
 *
 * This class contains logic that are extacted from tmpfuse/compViewer.cpp.
 * Created by john.lee on 7/22/16.
 */
@Data
public class LabelColor {
    public static final ArrayList<RGBVector> list = new ArrayList<>();

    static {
        list.add(new RGBVector(255, 0 ,0));
        list.add(new RGBVector(0, 255, 0));
        list.add(new RGBVector(0, 0, 255));
        list.add(new RGBVector(255, 255, 0));
        list.add(new RGBVector(255, 0, 255));
        list.add(new RGBVector(0, 255, 255));
        list.add(new RGBVector(255, 128, 0));
        list.add(new RGBVector(255, 0, 128));
        list.add(new RGBVector(0, 255, 128));
        list.add(new RGBVector(128, 255, 0));
        list.add(new RGBVector(0, 128, 255));
        list.add(new RGBVector(255, 255, 128));
        list.add(new RGBVector(255, 128, 255));
        list.add(new RGBVector(128, 255, 255));
        list.add(new RGBVector(255, 128, 128));
        list.add(new RGBVector(128, 255, 255));
        list.add(new RGBVector(255, 128, 128));
        list.add(new RGBVector(128, 255, 128));
        list.add(new RGBVector(128, 128, 255));
        list.add(new RGBVector(128, 64, 128));
        list.add(new RGBVector(128, 128, 64));
    }
}
