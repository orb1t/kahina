package org.kahina.core.visual.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseListener;

import javax.swing.JList;
import javax.swing.JScrollPane;

import org.kahina.core.KahinaInstance;
import org.kahina.core.visual.KahinaViewPanel;

public class KahinaTextViewPanel extends KahinaViewPanel<KahinaTextView<?>>
{
	private static final long serialVersionUID = -615641085387993443L;
	
	protected JList list;
    JScrollPane listScrollPane;
    //determines how many lines are automatically displayed before and after the lead selection line
    //also determines the minimum height of the component
    private int displayContext = 2;
    private final KahinaInstance<?, ?, ?, ?> kahina;
    
    public KahinaTextViewPanel(KahinaInstance<?, ?, ?, ?> kahina)
    {
        this.setLayout(new GridLayout());
        this.kahina = kahina;
        view = null;
        list = new JList();
        list.setSelectionBackground(Color.YELLOW);
        list.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        list.setFixedCellHeight(16);
        
        listScrollPane = new JScrollPane(list);
        this.add(listScrollPane);          
    }
    
    @Override
    public void setView(KahinaTextView<?> view)
    {
        this.view = view;
        list.setModel(view.getListModel());
        list.setSelectionModel(view.getSelectionModel());
        for (MouseListener mouseListener : list.getMouseListeners())
        {
            list.removeMouseListener(mouseListener);
        }
        list.addMouseListener(new KahinaTextViewListener(this, kahina));
        this.updateDisplayAndRepaintFromEventDispatchThread();
    }
    
    @Override
    public void updateDisplay()
    {
    	// FIXME The view's model may have changed, e.g. when loading a stored session.
        Integer leadIndex = view.getSelectionModel().getLeadSelectionIndex();  
        if (leadIndex != null)
        {
            int startIndex = leadIndex - displayContext;
            int endIndex = leadIndex + displayContext;
            if (startIndex < 0)
            {
                startIndex = 0;
                endIndex -= startIndex;
            }
            
            if (endIndex >= list.getModel().getSize())
            {
                endIndex = list.getModel().getSize() - 1;
            }
            try
            {
                list.ensureIndexIsVisible(startIndex);
                list.ensureIndexIsVisible(endIndex);
            }
            //be extremely careful because of nasty swing errors
            catch (NullPointerException e)
            {
                System.err.println("Jumping within a KahinaTextViewLabel would have caused an error - evaded.");
            }
        }
        repaint();      
    }   
}
