package lee.kyuhae.john.compphoto.algorithm.maxflow;

import lombok.extern.slf4j.Slf4j;

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
 * The corresponding class is maxflow.cpp
 **/
@Slf4j
public class MaxFlowFinder {
    private static final Arc TERMINAL = new Arc();
    private static final Arc ORPHAN = new Arc();
    private static final int INFINITE_DISTANCE = 1000000000;

    private final Node[] nodes;

    private Node[] queueFirst = new Node[2];
    private Node[] queueLast = new Node[2];
    private NodePointer orphanFirst = null;
    private NodePointer orphanLast = null;
    private int timestamp = 0;
    private double flow = 0.0;

    public MaxFlowFinder(Node[] nodes) {
        this.nodes = nodes;
    }

    public void setTweights(Node i, double sourceCapacity, double sinkCapacity) {
        flow += sourceCapacity < sinkCapacity ? sourceCapacity : sinkCapacity;
        i.setResidualCapacity(sourceCapacity - sinkCapacity);
    }

    public void addTweights(Node i, double sourceCapacity, double sinkCapacity) {
        double delta = i.getResidualCapacity();
        if (delta > 0) {
            sourceCapacity += delta;
        } else {
            sinkCapacity -= delta;
        }

        setTweights(i, sourceCapacity, sinkCapacity);
    }

    private void setActive(Node i) {
        if (i.getNext() == null) {
            if (queueLast[1] != null) {
                queueLast[1].setNext(i);
            } else {
                queueFirst[1] = i;
                queueLast[1] = i;
                i.setNext(i);
            }
        }
    }

    private Node nextActive() {
        Node i;

        while (true) {
            if ((i = queueFirst[0]) == null) {
                queueFirst[0] = i = queueFirst[1];
                queueLast[0] = queueLast[1];
                queueFirst[1] = null;
                queueLast[1] = null;

                if (i == null) {
                    return null;
                }
            }

            // Remove from the active list.
            if (i.getNext() == i) {
                queueFirst[0] = queueLast[0] = null;
            } else {
                queueFirst[0] = i.getNext();
                i.setNext(null);
            }

            // A node in the list is active iff it has a parent.
            if (i.getParent() != null) {
                return i;
            }
        }
    }

    private void init() {
        queueFirst[0] = null;
        queueFirst[1] = null;
        queueLast[0] = null;
        queueLast[0] = null;
        orphanFirst = null;

        for (Node node : nodes) {
            node.setNext(null);
            node.setTimestamp(0);
            if (node.getResidualCapacity() > 0) {
                // this node is connected to the source.
                node.setSink(false);
                node.setParent(TERMINAL);
                setActive(node);
                node.setDistance(1);
            } else if (node.getResidualCapacity() < 0) {
                // this node is connected to the sink
                node.setSink(true);
                node.setParent(TERMINAL);
                setActive(node);
                node.setDistance(1);
            } else {
                node.setParent(null);
            }
        }

        timestamp = 0;
    }

    private void augment(Arc middleArc) {
        Node i;
        Arc a;
        double bottleneck;
        NodePointer np;

	    /* 1. Finding bottleneck capacity */
        /* 1a - the source tree */
        bottleneck = middleArc.getResidualCapacity();
        for (i = middleArc.getSister().getHead(); ; i = a.getHead()) {
            a = i.getParent();
            if (a == TERMINAL) {
                break;
            }

            if (bottleneck > a.getSister().getResidualCapacity()) {
                bottleneck = a.getSister().getResidualCapacity();
            }
        }

        if (bottleneck > i.getResidualCapacity()) {
            bottleneck = i.getResidualCapacity();
        }

	    /* 1b - the sink tree */
        for (i = middleArc.getHead(); ; i = a.getHead()) {
            a = i.getParent();
            if (a == TERMINAL) {
                break;
            }

            if (bottleneck > a.getResidualCapacity()) {
                bottleneck = a.getResidualCapacity();
            }
        }

        if (bottleneck > -i.getResidualCapacity()) {
            bottleneck = -i.getResidualCapacity();
        }

	    /* 2. Augmenting */
        /* 2a - the source tree */
        double sisterRC = middleArc.getSister().getResidualCapacity() + bottleneck;
        middleArc.getSister().setResidualCapacity(sisterRC);
        double middleArcRC = middleArc.getResidualCapacity() - bottleneck;
        middleArc.setResidualCapacity(middleArcRC);

        for (i = middleArc.getSister().getHead(); ; i = a.getHead()) {
            a = i.getParent();
            if (a == TERMINAL) {
                break;
            }

            middleArcRC = a.getResidualCapacity() + bottleneck;
            a.setResidualCapacity(middleArcRC);

            sisterRC = a.getSister().getResidualCapacity() - bottleneck;
            a.getSister().setResidualCapacity(sisterRC);

            if (a.getSister().getResidualCapacity() <= 0) {
                /* add i to the adoption list */
                i.setParent(ORPHAN);
                np = new NodePointer();
                np.setPointer(i);
                np.setNext(orphanFirst);
                orphanFirst = np;
            }
        }

        double updatedRC = i.getResidualCapacity() - bottleneck;
        i.setResidualCapacity(updatedRC);

        if (i.getResidualCapacity() <= 0) {
             /* add i to the adoption list */
            i.setParent(ORPHAN);
            np = new NodePointer();
            np.setPointer(i);
            np.setNext(orphanFirst);
            orphanFirst = np;
        }

	    /* 2b - the sink tree */
        for (i = middleArc.getHead(); ; i = a.getHead()) {
            a = i.getParent();
            if (a == TERMINAL ) {
                break;
            }

            sisterRC = a.getSister().getResidualCapacity() + bottleneck;
            a.getSister().setResidualCapacity(sisterRC);
            updatedRC = a.getResidualCapacity() - bottleneck;
            a.setResidualCapacity(updatedRC);

            if (a.getResidualCapacity() <= 0) {
                /* add i to the adoption list */
                i.setParent(ORPHAN);
                np = new NodePointer();
                np.setPointer(i);
                np.setNext(orphanFirst);
                orphanFirst = np;
            }
        }

        updatedRC = i.getResidualCapacity() + bottleneck;
        i.setResidualCapacity(updatedRC);

        if (i.getResidualCapacity() <= 0) {
            /* add i to the adoption list */
            i.setParent(ORPHAN);
            np = new NodePointer();
            np.setPointer(i);
            np.setNext(orphanFirst);
            orphanFirst = np;
        }

        flow += bottleneck;
    }

    private void processSourceOrphan(Node i) {
        Node j;
        Arc a0, a0Min = null, a;
        NodePointer np;
        int d, dMin = INFINITE_DISTANCE;

        /* trying to find a new parent */
        for (a0 = i.getFirst(); a0 != null; a0 = a0.getNext()) {
            if (a0.getSister().getResidualCapacity() > 0) {
                j = a0.getHead();
                if (!j.isSink() && j.getParent() != null) {
                    /* checking the origin of j */
                    d = 0;
                    while (true) {
                        if (j.getTimestamp() == timestamp) {
                            d += j.getDistance();
                            break;
                        }
                        a = j.getParent();
                        d++;

                        if (a == TERMINAL) {
                            j.setTimestamp(timestamp);
                            j.setDistance(1);
                            break;
                        }

                        if (a == ORPHAN) {
                            d = INFINITE_DISTANCE;
                            break;
                        }
                        j = a.getHead();
                    }

                    /* j originates from the source - done */
                    if (d < INFINITE_DISTANCE) {
                        if (d < dMin) {
                            a0Min = a0;
                            dMin = d;
                        }

                        /* set marks along the path */
                        for (j = a0.getHead(); j.getTimestamp() != timestamp; j = j.getParent().getHead()) {
                            j.setTimestamp(timestamp);
                            j.setDistance(d--);
                        }
                    }
                }
            }
        }

        i.setParent(a0Min);
        if (i.getParent() != null) {
            i.setTimestamp(timestamp);
            i.setDistance(dMin + 1);
        } else {
            /* no parent is found */
            i.setTimestamp(0);

            /* process neighbors */
            for (a0 = i.getFirst(); a0 != null; a0 = a0.getNext()) {
                j = a0.getHead();
                if (!j.isSink() && (a = j.getParent()) != null) {
                    if (a0.getSister().getResidualCapacity() > 0) {
                        setActive(j);
                    }

                    if (a != TERMINAL && a != ORPHAN && a.getHead() == i) {
                        /* add j to the adoption list */
                        j.setParent(ORPHAN);
                        np = new NodePointer();
                        np.setPointer(j);
                        if (orphanLast != null) {
                            orphanLast.setNext(np);
                        } else {
                            orphanFirst = np;
                        }
                        orphanLast = np;
                        np.setNext(null);
                    }
                }
            }
        }
    }

    private void processSinkOrphan(Node i) {
        Node j;
        Arc a0, a0Min = null, a;
        NodePointer np;
        int d, dMin = INFINITE_DISTANCE;

        /* trying to find a new parent */
        for (a0 = i.getFirst(); a0 != null; a0 = a0.getNext()) {
            if (a0.getResidualCapacity() > 0) {
                j = a0.getHead();
                if (j.isSink() && j.getParent() != null) {
                    /* checking the origin of j */
                    d = 0;
                    while (true) {
                        if (j.getTimestamp() == timestamp) {
                            d += j.getDistance();
                            break;
                        }

                        a = j.getParent();
                        d++;
                        if (a == TERMINAL) {
                            j.setTimestamp(timestamp);
                            j.setDistance(1);
                            break;
                        }

                        if (a == ORPHAN) {
                            d = INFINITE_DISTANCE;
                            break;
                        }

                        j = a.getHead();
                    }

                    /* j originates from the sink - done */
                    if (d < INFINITE_DISTANCE) {
                        if (d < dMin) {
                            a0Min = a0;
                            dMin = d;
                        }

                        /* set marks along the path */
                        for (j = a0.getHead(); j.getTimestamp() != timestamp; j = j.getParent().getHead()) {
                            j.setTimestamp(timestamp);
                            j.setDistance(d--);
                        }
                    }
                }
            }
        }

        i.setParent(a0Min);
        if (i.getParent() != null) {
            i.setTimestamp(timestamp);
            i.setDistance(dMin + 1);
        } else {
            /* no parent is found */
            i.setTimestamp(0);

            /* process neighbors */
            for (a0 = i.getFirst(); a0 != null; a0 = a0.getNext()) {
                j = a0.getHead();
                if (j.isSink() && (a = j.getParent()) != null) {
                    if (a0.getResidualCapacity() > 0) {
                        setActive(j);
                    }

                    if (a != TERMINAL && a != ORPHAN && a.getHead() == i) {
                        /* add j to the adoption list */
                        j.setParent(ORPHAN);
                        np = new NodePointer();
                        np.setPointer(j);
                        if (orphanLast != null) {
                            orphanLast.setNext(np);
                        } else {
                            orphanFirst = np;
                        }
                        orphanLast = np;
                        np.setNext(null);
                    }
                }
            }
        }
    }

    public double findMaxFlow() {
        Node i, j, cur = null;
        Arc a;
        NodePointer np, npNext;

        init();
        while (true) {
            i = cur;
            if (i != null) {
                /* remove active flag */
                i.setNext(null);
                if (i.getParent() == null) {
                    i = null;
                }
            }

            if (i == null) {
                i = nextActive();
                if (i == null) {
                    break;
                }
            }

            /* growth */
            if (!i.isSink()) {
                /* grow source tree */
                for (a = i.getFirst(); a != null; a = a.getNext()) {
                    if (a.getResidualCapacity() > 0) {
                        j = a.getHead();
                        if (j.getParent() == null) {
                            j.setSink(false);
                            j.setParent(a.getSister());
                            j.setTimestamp(i.getTimestamp());
                            j.setDistance(i.getDistance() + 1);
                            setActive(j);
                        } else if (j.isSink()) {
                            break;
                        } else if (j.getTimestamp() <= i.getTimestamp()
                                && j.getDistance() > i.getDistance()) {
                            /* heuristic - trying to make the distance from j to the source shorter */
                            j.setParent(a.getSister());
                            j.setTimestamp(i.getTimestamp());
                            j.setDistance(i.getDistance() + 1);
                        }
                    }
                }
            } else {
                /* grow sink tree */
                for (a = i.getFirst(); a != null; a = a.getNext()) {
                    if (a.getSister().getResidualCapacity() > 0) {
                        j = a.getHead();
                        if (j.getParent() == null) {
                            j.setSink(true);
                            j.setParent(a.getSister());
                            j.setTimestamp(i.getTimestamp());
                            j.setDistance(i.getDistance() + 1);
                            setActive(j);
                        } else if (!j.isSink()) {
                            a = a.getSister();
                            break;
                        } else if (j.getTimestamp() <= i.getTimestamp()
                                && j.getDistance() > i.getDistance()) {
                            /* heuristic - trying to make the distance from the j to the sink shorter */
                            j.setParent(a.getSister());
                            j.setTimestamp(i.getTimestamp());
                            j.setDistance(i.getDistance() + 1);
                        }
                    }
                }
            }

            timestamp++;

            if (a != null) {
                /* set active flag */
                i.setNext(i);
                cur = i;

                /* augmentation */
                augment(a);
                /* augmentation end */

                /* adoption */
                while ((np = orphanFirst) != null) {
                    npNext = np.getNext();
                    np.setNext(null);

                    while ((np = orphanFirst) != null) {
                        orphanFirst = np.getNext();
                        i = np.getPointer();
                        if (orphanFirst == null) {
                            orphanLast = null;
                        }

                        if (i.isSink()) {
                            processSinkOrphan(i);
                        } else {
                            processSourceOrphan(i);
                        }
                    }
                    orphanFirst = npNext;
                }
                /* adoption end */
            } else {
                cur = null;
            }
        }

        log.debug("Max-flow computation completed. Returning flow {}.", flow);
        return flow;
    }
}