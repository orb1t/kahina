package org.kahina.core.data.dag;

import org.kahina.core.control.KahinaEvent;

public class KahinaDAGEvent extends KahinaEvent
{
 //TODO: include info on which DAG the event concerns
    
    private int dagEventType;
    private int firstID;
    private int secondID;
    
    public KahinaDAGEvent(int dagEventType, int firstID, int secondID)
    {
        super("dag");
        this.dagEventType = dagEventType;
        this.firstID = firstID;
        this.secondID = secondID;
    }
    
    public int getDAGEventType()
    {
        return dagEventType;
    }
    
    public int getFirstID()
    {
        return firstID;
    }
    
    public int getSecondID()
    {
        return secondID;
    }
    
    @Override
	public String toString()
    {
        String s = "tree - ";
        switch (dagEventType)
        {
            case KahinaDAGEventType.NEW_NODE:
            {
                s += "new node: firstID = " + firstID + ", secondID = " + secondID;
                break;
            }
        }
        return s;
    }
}
