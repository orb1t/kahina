package org.kahina.core;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.kahina.core.control.KahinaController;
import org.kahina.core.control.KahinaEvent;
import org.kahina.core.control.KahinaEventTypes;
import org.kahina.core.control.KahinaListener;
import org.kahina.core.data.KahinaObject;
import org.kahina.core.data.agent.KahinaBreakpoint;
import org.kahina.core.data.project.KahinaProject;
import org.kahina.core.data.text.KahinaLineReference;
import org.kahina.core.data.text.KahinaTextModel;
import org.kahina.core.gui.event.KahinaMessageEvent;
import org.kahina.core.gui.event.KahinaSelectionEvent;
import org.kahina.core.io.magazine.ObjectMagazine;

/**
 * The current state of a Kahina instance.
 * 
 * Implicitly contains all the information on the current run of Kahina.
 * 
 * Can be serialized and deserialized, allowing to interrupt and continue runs.
 * 
 *  @author jdellert
 */

public class KahinaState implements Serializable, KahinaListener
{   
    /**
	 * 
	 */
	private static final long serialVersionUID = -1884751676781509811L;
	
	private static final boolean VERBOSE = false;
	
	//the messages that will be stored in the console
    protected KahinaTextModel consoleMessages;
    //map from stepIDs to lines in console
    protected Map<Integer,Set<KahinaLineReference>> consoleLines;
    
    protected Map<KahinaBreakpoint, Integer> warnThresholdByBreakpoint; 
    protected Map<KahinaBreakpoint, Integer> matchCountByBreakpoint;
    
    private int selectedStepID = -1;
    
    private int nextStepID = 1;
    
    protected KahinaInstance<?,?,?,?> kahina;
    
    private static ObjectMagazine<KahinaStep> steps;
    
    public KahinaState(KahinaInstance<?,?,?,?> kahina)
    {
		steps = ObjectMagazine.create();
        initialize();
        this.kahina = kahina;
        kahina.registerInstanceListener(KahinaEventTypes.SELECTION, this);
    }
    
	public void initialize() 
	{
		if (steps != null)
		{
			steps.close();
		}
		steps = ObjectMagazine.create();
		selectedStepID = -1;
		nextStepID = 1;
		//console is refilled for each new process
		//TODO: think about an additional console for global events (warnings etc.)
        consoleMessages = new KahinaTextModel();
        consoleLines = new HashMap<Integer,Set<KahinaLineReference>>();
        warnThresholdByBreakpoint = new HashMap<KahinaBreakpoint, Integer>();
        matchCountByBreakpoint = new HashMap<KahinaBreakpoint, Integer>();
	}
    
    public synchronized int nextStepID()
    {
    	if (VERBOSE)
    	{
    		System.err.println(this + ".nextStepID() = " + nextStepID);
    	}
    	return nextStepID++;
    }
    
    public int getStepCount()
    {
    	return nextStepID - 1;
    }
    
    public ObjectMagazine<KahinaStep> getSteps()
    {
    	return steps;
    }
    
    public synchronized void store(int id, KahinaObject object)
	{
		if (VERBOSE)
		{
			System.err.println("KahinaRunner.store(" + id + "," + object + ")");
			System.err.println("steps == " + steps);
		}
		steps.store(id, (KahinaStep) object);
	}

	@SuppressWarnings("unchecked")
	public synchronized <T extends KahinaObject> T retrieve(Class<T> type, int stepID)
	{
		// TODO we want to do this differently
		return (T) steps.retrieve(stepID);
	}
	
	public void loadSteps(File directory)
	{
		steps = ObjectMagazine.load(directory, KahinaStep.class);
	}
    
    public void processEvent(KahinaEvent event)
    {
    	if (event instanceof KahinaSelectionEvent)
    	{
    		processSelectionEvent((KahinaSelectionEvent) event);
    	}
    }
    
    private void processSelectionEvent(KahinaSelectionEvent event)
	{
    	if (VERBOSE)
    	{
    		System.err.println(this + ".processSelectionEvent(" + event + ")");
    	}
    	int newSelectedStep = event.getSelectedStep();
        if (newSelectedStep != -1 && selectedStepID != newSelectedStep)
        {
            selectedStepID = event.getSelectedStep();
            processSelection();  
        }
	}
    
    protected void processSelection()
    {
        
    }
    
    public int getSelectedStepID()
    {
    	return selectedStepID;
    }

	public void consoleMessage(int stepID, String message)
    {
        int lineID = consoleMessages.text.addLine(message);
        KahinaLineReference ref = new KahinaLineReference(consoleMessages,lineID,stepID);
        Set<KahinaLineReference> refs = consoleLines.get(stepID);
        if (refs == null)
        {
            refs = new HashSet<KahinaLineReference>();
            consoleLines.put(stepID, refs);
        }
        refs.add(ref);
        kahina.dispatchInstanceEvent(new KahinaMessageEvent(ref));
    }
    
    public KahinaTextModel getConsoleMessages()
    {
        return consoleMessages;
    }
    
    public Set<KahinaLineReference> getLineReferencesForStep(int stepID)
    {
    	return consoleLines.get(stepID);
    }
    
    public Map<KahinaBreakpoint, Integer> getMatchCountByBreakpoint()
    {
    	return matchCountByBreakpoint;
    }
    
    public Map<KahinaBreakpoint, Integer> getWarnThresholdByBreakpoint()
    {
    	return warnThresholdByBreakpoint;
    }
    
    /**
     * Shortcut for quick access to step data; should be overriden in the obvios way for custom step types.
     * @param stepID
     * @return
     */
    public KahinaStep get(int stepID)
    {
        return retrieve(KahinaStep.class, stepID);
    }
}
