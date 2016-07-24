package lee.kyuhae.john.compphoto.algorithm;

import lombok.Data;

/**
 * Something to represent Vec3i in java in the context of this app.
 * Created by john.lee on 7/22/16.
 */
@Data
public class RGBVector {
    private final int r;
    private final int g;
    private final int b;

    public RGBVector(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }
}
