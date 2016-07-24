package lee.kyuhae.john.compphoto.algorithm.maxflow;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * This is translation of the work by Vladimir Kolmogorov.
 * The original work is written in c++ and you can find download the original source code here:
 * {@see http://pub.ist.ac.at/~vnk/software.html#MATCH}
 * The file name is match-v3.4.src.tar.gz.
 *
 * Corresponding implementation is found in graph.h -- look for struct called arc_st
 *
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
