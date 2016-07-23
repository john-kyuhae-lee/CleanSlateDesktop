package lee.kyuhae.john.compphoto.algorithm.maxflow;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created by john.lee on 7/20/16.
 */

@Data
@Accessors(chain = true)
public class Arc {
    private Node head;
    private Arc next;
    private Arc sister;

    private double residualCapacity;
}
