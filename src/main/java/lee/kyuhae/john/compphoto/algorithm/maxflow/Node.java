package lee.kyuhae.john.compphoto.algorithm.maxflow;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created by john.lee on 7/20/16.
 */
@Data
@Accessors(chain = true)
public class Node {
    private final long index;
    private Arc first = null;
    private Arc parent = null;
    private Node next = null;

    private int timestamp = 0;
    private int distance = 0;
    private boolean sink = false;

    private double residualCapacity = 0.0;
}
