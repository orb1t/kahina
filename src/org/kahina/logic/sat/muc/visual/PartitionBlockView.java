package org.kahina.logic.sat.muc.visual;

import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.ListModel;

import org.kahina.core.control.KahinaEvent;
import org.kahina.core.gui.event.KahinaRedrawEvent;
import org.kahina.core.gui.event.KahinaSelectionEvent;
import org.kahina.core.visual.KahinaView;
import org.kahina.logic.sat.muc.MUCInstance;
import org.kahina.logic.sat.muc.MUCStep;
import org.kahina.logic.sat.muc.data.PartitionBlockHandler;

public class PartitionBlockView extends KahinaView<PartitionBlockHandler>
{
    protected DefaultListModel listModel;
    
    // mapping from status values to display properties
    HashMap<Integer, Color> statusColorEncoding;
    
    MUCInstance kahina;
    MUCStep currentStep;
    
    //cash state of current block here during recalculation
    List<Integer> lineStatus;
    
    BlockContentSummarizer summarizer;
    
    public PartitionBlockView(MUCInstance kahina)
    {
        super(kahina);
        this.kahina = kahina;
        this.currentStep = null;
        this.listModel = new DefaultListModel();
        this.statusColorEncoding = new HashMap<Integer, Color>();
        this.lineStatus = new LinkedList<Integer>();
    }
    
    public void doDisplay()
    {
        summarizer = new CfgBlockContentSummarizer(kahina.getSatInstance());
        recalculate();
    }
    
    public void setBlockContentSummarizer(BlockContentSummarizer summarizer)
    {
        this.summarizer = summarizer;
        recalculate();
    }
    
    public void processEvent(KahinaEvent e)
    {
        if (e instanceof KahinaSelectionEvent)
        {
            processEvent((KahinaSelectionEvent) e);
        }
        else
        {
            super.processEvent(e);
        }
    }
    
    public void processEvent(KahinaSelectionEvent e)
    {
        if (model != null) 
        {
            recalculate();
            kahina.dispatchEvent(new KahinaRedrawEvent());
        }
    }
    
    public void setStatusColorEncoding(int status, Color color)
    {
        statusColorEncoding.put(status, color);
    }
    
    public int getLineStatus(int lineID)
    {
        if (currentStep != null)
        {
            if (model.getBlocks().size() > 0 && lineStatus.size() > lineID)
            {
                return lineStatus.get(lineID);
            }
        }
        return 0;
    }
    
    public Color getLineColor(int lineID)
    {
        int status = getLineStatus(lineID);
        Color col = statusColorEncoding.get(status);
        //System.err.println("line: " + lineID + " status: " + status + " color: " + col);
        if (col == null)
        {
            return Color.BLACK;
        } 
        else
        {
            return col;
        }
    }
    
    public ListModel getListModel()
    {
        return listModel;
    }

    @Override
    public JComponent makePanel()
    {
        PartitionBlockViewPanel panel = new PartitionBlockViewPanel(kahina);
        kahina.registerInstanceListener("redraw", panel);
        panel.setView(this);
        return panel;
    }
    
    public void displayText(String string)
    {
        listModel.clear();
        listModel.addElement(string);
    }
    
    public void recalculate()
    {
        kahina.getLogger().startMeasuring();
        int stepID = kahina.getState().getSelectedStepID();
        listModel.clear();
        lineStatus.clear();
        if (stepID == -1)
        {
            displayText("No reduction state selected!");
        }
        else if (model.getBlocks().size() == 0)
        {
            displayText("No reduction blocks found so far!");
            lineStatus.add(0);
        }
        else
        {
            currentStep = kahina.getState().retrieve(MUCStep.class, stepID);
            for (TreeSet<Integer> block : model.retrieveBlocks())
            {
                listModel.addElement(summarizer.buildBlockSummary(block));
                lineStatus.add(currentStep.relationToBlock(block));
            }
        }
        kahina.getLogger().endMeasuring("for recalculating the PartitionBlockView");
    }

}
