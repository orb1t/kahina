package org.kahina.core.data.dag;

import java.util.List;
import java.util.Set;

import org.kahina.core.data.KahinaObject;

public abstract class KahinaDAG extends KahinaObject
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 4477263548864006864L;
    
    public KahinaDAG()
    {
        // need no-arg constructor for lightweight subclasses
    }
    
    public abstract void addNode(int id, String caption, int nodeStatus);

    public abstract int addNode(String caption, int nodeStatus);
    
    public abstract void addEdge(int edgeID, int start, int end, String label);
    
    public abstract int addEdge(int start, int end, String label);
    
    public void addEdgeNoDuplicates(int edgeID, int start, int end, String label)
    {
        if (getEdgeBetween(start,end) == -1)
        {
            addEdge(edgeID,start,end,label);
        }
        else
        {
            //overwrite the label of a previously existent edge
            setEdgeLabel(edgeID, label);
        }
    }
    
    public int addEdgeNoDuplicates(int start, int end, String label)
    {
        int edgeID = getEdgeBetween(start,end);
        if (edgeID == -1)
        {
            edgeID = addEdge(start,end,label);
        }
        else
        {
            //overwrite the label of a previously existent edge
            setEdgeLabel(edgeID, label);
        }
        return edgeID;
    }

    public abstract void decollapseAll();

    public abstract void decollapse(int nodeID);

    public abstract void collapse(int nodeID);

    public abstract boolean isCollapsed(int nodeID);
    
    public abstract List<Integer> getOutgoingEdges(int nodeID);
    
    public abstract List<Integer> getVisibleParents(int nodeID);
    
    public abstract List<Integer> getIncomingEdges(int nodeID);
    
    public int getEdgeBetween(int startID, int endID)
    {
        for (int edgeID : getOutgoingEdges(startID))
        {
            if (getEndNode(edgeID) == endID)
            {
                return edgeID;
            }
        }
        return -1;
    }
    
    public abstract List<Integer> getVisibleChildren(int nodeID);

    public abstract int getNodeStatus(int nodeID);

    public abstract void setNodeStatus(int nodeID, int status);

    public abstract String getEdgeLabel(int edgeID);

    public abstract void setEdgeLabel(int edgeID, String label);
    
    public abstract int getStartNode(int edgeID);

    public abstract void setStartNode(int edgeID, int startNode);
    
    public abstract int getEndNode(int edgeID);

    public abstract void setEndNode(int edgeID, int endNode);


    public abstract String getNodeCaption(int nodeID);

    public abstract void setNodeCaption(int nodeID, String caption);

    public abstract int getSize();
    
    public abstract Iterable<Integer> getNodeIDIterator();
    
    public abstract Iterable<Integer> getEdgeIDIterator();
    
    public abstract Set<Integer> getRoots();
    
    public abstract List<Integer> findShortestPathFromRoot(int nodeID);
    
    public void toggleCollapse(int nodeID)
    {
        if (!isCollapsed(nodeID))
        {
            collapse(nodeID);
        } else
        {
            decollapse(nodeID);
        }
    }
    
    public String exportXML()
    {
        StringBuilder b = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        b.append("<kahinaDAG>\n");
        for (Integer nodeID : getNodeIDIterator())
        {
            b.append("  <node id=\"" + nodeID + "\" caption=\"" + getNodeCaption(nodeID) + "\" status=\"" + getNodeStatus(nodeID) + "\"/>\n");
        }
        for (Integer edgeID : getEdgeIDIterator())
        {
            b.append("  <edge id=\"" + edgeID + "\" label=\"" + getEdgeLabel(edgeID) + "\" start=\"" + getStartNode(edgeID) + "\" end=\"" + getEndNode(edgeID) + "\"/>\n");
        }
        b.append("</kahinaDAG>\n");
        return b.toString();
    }
}
