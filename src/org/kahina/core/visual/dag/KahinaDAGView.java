package org.kahina.core.visual.dag;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.kahina.core.KahinaInstance;
import org.kahina.core.data.dag.KahinaDAG;
import org.kahina.core.data.dag.KahinaMemDAG;
import org.kahina.core.gui.KahinaProgressBar;
import org.kahina.core.gui.event.KahinaUpdateEvent;
import org.kahina.core.visual.KahinaView;

public class KahinaDAGView extends KahinaView<KahinaDAG>
{
    public static final boolean verbose = false;
    
    protected KahinaDAGViewConfiguration config;
    protected KahinaDAGLayouter layout;
    
    //  possible values for antialiasing policy
    public static final int ANTIALIASING = 0;
    public static final int NO_ANTIALIASING = 1;
    
    //display coordinates for nodes
    HashMap<Integer, Integer> nodeX;
    private HashMap<Integer, Integer> nodeY;
    
    private HashMap<Integer, Integer> nodeWidths;

    // special display properties for certain nodes
    HashMap<Integer, Color> nodeBorderColor;

    // mapping from status values to display properties
    HashMap<Integer, Color> statusNodeColorEncoding;
    HashMap<Integer, Color> statusEdgeColorEncoding;
    HashMap<Integer, Color> statusBorderColorEncoding;
    HashMap<Integer, Stroke> statusStrokeEncoding;
    HashMap<Integer, Font> statusFontEncoding;
    
    //allow marking of a single node in the graph
    private int markedNode;
    
    public KahinaDAGView(KahinaInstance<?, ?, ?, ?> kahina, KahinaDAGLayouter layout)
    {
    	super(kahina);
        this.model = new KahinaMemDAG();
        
        this.config = new KahinaDAGViewConfiguration();
        this.layout = layout;
        
        this.nodeX = new HashMap<Integer, Integer>();
        this.nodeY = new HashMap<Integer, Integer>();
        
        this.nodeWidths = new HashMap<Integer, Integer>();
        
        this.nodeBorderColor = new HashMap<Integer, Color>();
        this.statusNodeColorEncoding = new HashMap<Integer, Color>();
        this.statusEdgeColorEncoding = new HashMap<Integer, Color>();
        this.statusBorderColorEncoding = new HashMap<Integer, Color>();
        this.statusStrokeEncoding = new HashMap<Integer, Stroke>();
        this.statusFontEncoding = new HashMap<Integer, Font>();

        this.markedNode = -1;
        
        kahina.registerInstanceListener("update", this);
    }

	public void display(KahinaDAG dagModel)
    {
        model = dagModel;
        nodeBorderColor = new HashMap<Integer, Color>();
        layout.newDAG(this);
        recalculate();
    }
    
    public KahinaDAGViewConfiguration getConfig()
    {
        return config;
    }
    
    public void setConfig(KahinaDAGViewConfiguration config)
    {
        this.config = config;
        layout.newDAG(this);
    }
    
    public void setLayouter(KahinaDAGLayouter layouter)
    {
        layout = layouter;
        layout.newDAG(this);
    }
    
    public KahinaDAGLayouter getLayouter()
    {
        return layout;
    }

    public Font getNodeFont(int nodeID)
    {
        int status = model.getNodeStatus(nodeID);
        Font fnt = statusFontEncoding.get(status);
        if (fnt == null)
        {
            if (model.isCollapsed(nodeID))
            {
                return new Font(Font.SANS_SERIF, Font.BOLD, config.getNodeSize());
            }
            return new Font(Font.SANS_SERIF, Font.PLAIN, config.getNodeSize());
        }
        else
        {
            return new Font(fnt.getFamily(), fnt.getStyle(), config.getNodeSize());
        }
    }
    
    public Color getNodeColor(int nodeID)
    {
        int status = model.getNodeStatus(nodeID);
        Color col = statusNodeColorEncoding.get(status);
        // System.err.println("Node " + nodeID + ": status " + status +
        // " ->  color " + col);
        if (col == null)
        {
            return Color.WHITE;
        } else
        {
            return col;
        }
    }

    public void setNodeBorderColor(int nodeID, Color color)
    {
        if (color == null)
        {
            nodeBorderColor.remove(nodeID);
        } 
        else
        {
            nodeBorderColor.put(nodeID, color);
        }
    }

    public Color getNodeBorderColor(int nodeID)
    {
        return nodeBorderColor.get(nodeID);
    }
    
    public Map<Integer,Integer> getXCoordinates()
    {
        return nodeX;
    }
    
    public Map<Integer,Integer> getYCoordinates()
    {
        return nodeY;
    }
    
    public Map<Integer,Integer> getNodeWidths()
    {
        return nodeWidths;
    }
    
    public int getNodeX(int nodeID)
    {
        Integer x = nodeX.get(nodeID);
        if (x == null) return 0;
        return x;
    }

    public int getNodeY(int nodeID)
    {
        Integer y = nodeY.get(nodeID);
        if (y == null) return 0;
        return y;
    }
    
    public int getNodeWidth(int nodeID)
    {
        Integer width = nodeWidths.get(nodeID);
        if (width == null) return 0;
        return width;
    }
    
    public int getDisplayHeight()
    {
        return layout.getDisplayHeight();
    }
    
    public int getDisplayWidth()
    {
        return layout.getDisplayWidth();
    }
    
    public void setStatusColorEncoding(int status, Color color)
    {
        statusNodeColorEncoding.put(status, color);
    }

    public void setStatusFontEncoding(int status, Font font)
    {
        statusFontEncoding.put(status, font);
    }

    public int getMarkedNode()
    {
        return markedNode;
    }

    public void setMarkedNode(int markedNode)
    {
        this.markedNode = markedNode;
    }
    
    public boolean displaysNode(int nodeID)
    {
        return (nodeX.get(nodeID) != null);
    }

    public KahinaDAG getDAGModel()
    {
        return model;
    }

    @Override
    public JComponent makePanel()
    {
        KahinaProgressBar progressBar = new KahinaProgressBar();
        KahinaDAGViewPanel panel = new KahinaDAGViewPanel(kahina);
        panel.setPreferredSize(new Dimension(200,300));
        kahina.registerInstanceListener("redraw", panel);
        panel.setView(this);
        JPanel scrollPaneAndProgressBar = new JPanel();
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.getViewport().setBackground(config.getBackgroundColor());
        scrollPane.setViewportView(panel);
        scrollPaneAndProgressBar.setLayout(new BoxLayout(scrollPaneAndProgressBar, BoxLayout.Y_AXIS));
        scrollPaneAndProgressBar.add(scrollPane);
        scrollPaneAndProgressBar.add(progressBar);
        panel.setProgressBar(progressBar);
        return scrollPaneAndProgressBar;
    }
    
	protected void processEvent(KahinaUpdateEvent e)
    {
        // recalculation is implicitly part of this (via marker)
        markedNode = e.getSelectedStep();
        this.recalculate();
    }
	
	public void recalculate()
	{
        layout.computeLayout();
	}

    public boolean isNodeVisible(int node)
    {
        // TODO Auto-generated method stub
        return true;
    }
}
