package lee.kyuhae.john.compphoto.algorithm.maxflow;

import lombok.Data;

/**
 * This is translation of the work by.
 *
 * Acknowledgement:
 * - An Experimental Comparison of Min-Cut/Max-Flow Algorithms for Energy Minimization
 *      by Yuri Boykov and Vladimir Kolmogorov
 *
 * The original implementation is written by Vladimir and in c++.
 * You can find download the original source code here:
 * {@see http://pub.ist.ac.at/~vnk/software.html#MATCH}
 * The source file archive is match-v3.4.src.tar.gz.
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
