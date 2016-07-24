package lee.kyuhae.john.compphoto.algorithm.maxflow;

import lombok.Data;

/**
 * This is translation of the work by Vladimir Kolmogorov.
 * The original work is written in c++ and you can find download the original source code here:
 * {@see http://pub.ist.ac.at/~vnk/software.html#MATCH}
 * The file name is match-v3.4.src.tar.gz.
 *
 * Corresponding implementation is found in graph.h -- look for struct called node_pointer_st
 *
 * Created by john.lee on 7/20/16.
 */
@Data
public class NodePointer {
    private Node pointer;
    private NodePointer next;
}
