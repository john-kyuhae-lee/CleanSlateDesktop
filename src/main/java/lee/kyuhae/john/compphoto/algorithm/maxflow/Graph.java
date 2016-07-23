package lee.kyuhae.john.compphoto.algorithm.maxflow;

/**
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
