package lee.kyuhae.john.compphoto.algorithm.maxflow;

import lombok.Data;

/**
 * Created by john.lee on 7/20/16.
 */
@Data
public class NodePointer {
    private Node pointer;
    private NodePointer next;
}
