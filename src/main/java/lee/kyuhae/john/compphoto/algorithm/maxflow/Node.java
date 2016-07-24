package lee.kyuhae.john.compphoto.algorithm.maxflow;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * This is translation of the work by Vladimir Kolmogorov.
 * The original work is written in c++ and you can find download the original source code here:
 * {@see http://pub.ist.ac.at/~vnk/software.html#MATCH}
 * The file name is match-v3.4.src.tar.gz.
 *
 * Corresponding implementation is found in graph.h -- look for struct called node_st
 *
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
