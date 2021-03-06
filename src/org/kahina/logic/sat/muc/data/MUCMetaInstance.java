package org.kahina.logic.sat.muc.data;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.kahina.logic.sat.data.cnf.CnfSatInstance;

public class MUCMetaInstance extends CnfSatInstance
{
    static final boolean VERBOSE = false;
    
    LiteralBlockHandler blockHandler;
    
    public MUCMetaInstance(int numOrigClauses)
    {
        super();
        maxVarID = numOrigClauses;
        //default: a partition block handler
        this.blockHandler = new PartitionBlockHandler(this);
    }
    
    public void setBlockHandler(LiteralBlockHandler blockHandler)
    {
        this.blockHandler = blockHandler;
    }
    
    public void learnNewBlock(TreeSet<Integer> block)
    {
        blockHandler.ensureRepresentability(block);
    }
    
    public void learnNewClause(TreeSet<Integer> clause)
    {
        addClause(blockHandler.buildRepresentation(clause));
        needsUpdate = true;
    }
    
    public Collection<TreeSet<Integer>> getBlocks()
    {
        return blockHandler.getBlocks();
    }
    
}
