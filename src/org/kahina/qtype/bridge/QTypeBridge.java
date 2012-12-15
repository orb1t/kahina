package org.kahina.qtype.bridge;

import gralej.om.IEntity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kahina.core.control.KahinaControlEvent;
import org.kahina.core.control.KahinaStepUpdateEvent;
import org.kahina.core.data.chart.KahinaChart;
import org.kahina.core.gui.event.KahinaRedrawEvent;
import org.kahina.prolog.util.PrologUtil;
import org.kahina.qtype.QTypeDebuggerInstance;
import org.kahina.qtype.QTypeState;
import org.kahina.qtype.QTypeStep;
import org.kahina.qtype.control.QTypeControlEventCommands;
import org.kahina.qtype.data.bindings.QTypeGoal;
import org.kahina.sicstus.bridge.SICStusPrologBridge;
import org.kahina.tralesld.data.fs.TraleSLDFS;
import org.kahina.tralesld.data.fs.TraleSLDFSPacker;
import org.kahina.tralesld.visual.fs.GraleJUtility;

public class QTypeBridge extends SICStusPrologBridge
{
	private static final boolean VERBOSE = false;

	private static final Pattern SENTENCE_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");
	
	QTypeState state;

	private final TraleSLDFSPacker packer;
	
	//temporary variables for tracking the state of the chart
	int currentPosition;

	public QTypeBridge(final QTypeDebuggerInstance kahina)
	{
		super(kahina);
	    this.state = kahina.getState();
		packer = new TraleSLDFSPacker();
	}
	
    private void initializeChart(List<String> wordList)
    {
        KahinaChart chart = state.getChart();
        for (int i = 0; i < wordList.size(); i++)
        {
            chart.setSegmentCaption(i, wordList.get(i));
        }
        currentPosition = 0;
    }

	@Override
	public void step(int extID, String type, String description, String consoleMessage)
	{
	    super.step(extID, type, description, consoleMessage);
	       
		// For compile_grammar steps, unregister any previously registered grammar:
		if (description.startsWith("compile_grammar(") && description.endsWith(")"))
		{
			kahina.dispatchEvent(new KahinaControlEvent(QTypeControlEventCommands.REGISTER_GRAMMAR, new Object[] { null }));
		}
		
		
		if (description.startsWith("lc(") && description.endsWith(")"))
		{
		    //for lc/1 steps, register the sentence
			Matcher matcher = SENTENCE_PATTERN.matcher(description.substring(3, description.length() - 1));
			if (matcher.matches())
			{
			    List<String> wordList = PrologUtil.parsePrologStringList(matcher.group(0));
			    initializeChart(wordList);
				kahina.dispatchEvent(new KahinaControlEvent(QTypeControlEventCommands.REGISTER_SENTENCE, new Object[] { wordList }));
			}
			//TODO: otherwise, detect the prediction (???)
		}
		
	    if (description.startsWith("db_word("))
	    {
	        //create chart edge with label "lex:word" from the current position to the next
	        String word = description.substring(8,description.indexOf(','));
	        int edgeID = state.getChart().addEdge(currentPosition, currentPosition + 1, "lex:" + word, 2);
	        state.linkEdgeToNode(edgeID, currentID);
	    }
	    
        maybeUpdateStepCount(true);
        kahina.dispatchEvent(new KahinaRedrawEvent());
	}
	
	@Override
	public void exit(int extID, boolean deterministic, String newDescription)
	{		
		super.exit(extID, deterministic, newDescription);
		int stepID = convertStepID(extID);
		
		// At exit of compile_grammar steps, register the grammar:
		if (newDescription.startsWith("compile_grammar(") && newDescription.endsWith(")"))
		{
			String path = PrologUtil.atomLiteralToString(newDescription.substring(16, newDescription.length() - 1));
			kahina.dispatchEvent(new KahinaControlEvent(QTypeControlEventCommands.REGISTER_GRAMMAR, new Object[] { path }));
		}
		
		//if we have an associated edge, set it to successful
		int associatedEdge = state.getEdgeForNode(stepID);
		if (associatedEdge != -1)
		{
		    state.getChart().setEdgeStatus(associatedEdge, 0);
		}
		
        kahina.dispatchEvent(new KahinaRedrawEvent());
	}
	
    public void fail(int extID)
    {
        super.fail(extID);
        int stepID = convertStepID(extID);
        
        //if we have an associated edge, set it to successful
        int associatedEdge = state.getEdgeForNode(stepID);
        if (associatedEdge != -1)
        {
            state.getChart().setEdgeStatus(associatedEdge, 1);
        }
        
        kahina.dispatchEvent(new KahinaRedrawEvent());
    }

	public void registerExample(int number, String expectation, String sentence)
	{
		if (VERBOSE)
		{
			System.err.println(this + ".registerExample(" + number + ", " + expectation + ", " + sentence + ")");
		}
		try
		{
			kahina.dispatchEvent(new KahinaControlEvent(QTypeControlEventCommands.REGISTER_EXAMPLE, new Object[] { number, expectation, PrologUtil.parsePrologStringList(sentence) }));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	protected QTypeStep generateStep()
	{
		return new QTypeStep();
	}

	public void registerGoal(int extID, String key, String grisu)
	{
	    int stepID = stepIDConv.get(extID);
		try
		{
			QTypeGoal goal = state.retrieve(QTypeStep.class, stepID).getGoal();

			if ("fs".equals(key))
			{
				goal.setFS(packer.pack(grisu));
			} 
			else if ("tree".equals(key))
			{
				goal.setTree(packer.pack(grisu));
			} 
			else if ("in".equals(key))
			{
				goal.setIn(packer.pack(grisu));
			} 
			else if ("out".equals(key))
			{	    
				goal.setOut(packer.pack(grisu));
				
				IEntity graleFS = GraleJUtility.grisuToGralej(grisu);
				//read out useful info for edge labels from feature structures
				//if we have an associated edge...
		        int associatedEdge = state.getEdgeForNode(stepID);
		        if (associatedEdge != -1)
		        {
		            //...analyse the preliminary label to decide what to extract
		            String oldCaption = state.getChart().getEdgeCaption(associatedEdge);
		            if (oldCaption.startsWith("lex:"))
		            {
		                String type = GraleJUtility.getType(graleFS);
		                state.getChart().setEdgeCaption(associatedEdge, type + oldCaption.substring(3));
		            }
		        }
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
