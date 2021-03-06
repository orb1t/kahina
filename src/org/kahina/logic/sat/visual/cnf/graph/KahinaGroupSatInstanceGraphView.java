package org.kahina.logic.sat.visual.cnf.graph;

import java.awt.Color;
import java.util.HashMap;

import org.kahina.core.KahinaInstance;
import org.kahina.core.visual.graph.KahinaGraphLayouter;
import org.kahina.logic.sat.data.cnf.GroupCnfSatInstance;

public class KahinaGroupSatInstanceGraphView extends KahinaSatInstanceGraphView
{
    GroupCnfSatInstance sat;
    
    public KahinaGroupSatInstanceGraphView(KahinaInstance<?, ?, ?, ?> kahina, KahinaGraphLayouter layout)
    {
        super(kahina, layout);
    }

    public void display(GroupCnfSatInstance sat)
    {
        //do not recalculate if the sat instance is already displayed
        if (this.sat == null || this.sat != sat)
        {
            this.sat = sat;
            super.sat = sat;
            displayGroupsByVariables();
        }
    }
    
    public void displayGroupsByVariables()
    {
        model = sat.generateClaGroupByVarGraph();
        if (!clauseGraph)
        {
            vertexBorderColor = new HashMap<Integer, Color>();
            resetLayoutStructures();
            layout.newGraph(this);
        }
        clauseGraph = true;
    }
    
    public void displayGroupsByLiterals()
    {
        model = sat.generateClaGroupByLitGraph();
        if (!clauseGraph)
        {
            vertexBorderColor = new HashMap<Integer, Color>();
            resetLayoutStructures();
            layout.newGraph(this);
        }
        clauseGraph = true;
    }
    
    public void displayGroupsByComplementaryLiterals()
    {
        model = sat.generateClaGroupByCompLitGraph();
        if (!clauseGraph)
        {
            vertexBorderColor = new HashMap<Integer, Color>();
            resetLayoutStructures();
            layout.newGraph(this);
        }
        clauseGraph = true;
    }
    
    public void displayVariablesByGroups()
    {
        model = sat.generateVarByClaGroupGraph();
        vertexBorderColor = new HashMap<Integer, Color>();
        resetLayoutStructures();
        layout.newGraph(this);
        clauseGraph = false;
    }
    
    public void displayLiteralsByGroups()
    {
        model = sat.generateLitByClaGroupGraph();
        vertexBorderColor = new HashMap<Integer, Color>();
        resetLayoutStructures();
        layout.newGraph(this);
        clauseGraph = false;
    }
}
