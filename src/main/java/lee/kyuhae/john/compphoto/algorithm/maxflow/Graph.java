package lee.kyuhae.john.compphoto.algorithm.maxflow;

/**
 * This is translation of the work by Vladimir Kolmogorov.
 * The original work is written in c++ and you can find download the original source code here:
 * {@see http://pub.ist.ac.at/~vnk/software.html#MATCH}
 * The file name is match-v3.4.src.tar.gz.
 *
 * Corresponding files are graph.h and graph.cpp.
 * Note:
 * - All structs (node_st, arc_st, node_pointer_st) are made into separate classes.
 * - There is no corresponding block.h translation since
 * they are for memory optimization specific to c++. Mentioning here because structures from block.h
 * are used in the original work for graph structure.
 *
 * Created by john.lee on 7/20/16.
 */
public class Graph {
    public static final int SOURCE = 0;
    public static final int SINK = 1;

    public static void addEdge(Node from, Node to, double capacity, double reverseCapacity) {
        Arc a = new Arc();
        Arc aRev = new Arc();

        a.setSister(aRev);
        aRev.setSister(a);
        a.setNext(from.getFirst());
        from.setFirst(a);
        aRev.setNext(to.getFirst());
        to.setFirst(aRev);
        a.setHead(to);
        aRev.setHead(from);
        a.setResidualCapacity(capacity);
        aRev.setResidualCapacity(reverseCapacity);
    }

    public static int whatSegment(Node i) {
        if (i.getParent() != null && !i.isSink()) {
            return SOURCE;
        }
        return SINK;
    }
}
