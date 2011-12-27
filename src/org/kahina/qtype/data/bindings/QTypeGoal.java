package org.kahina.qtype.data.bindings;

import gralej.parsers.ParseException;

import org.kahina.core.data.KahinaObject;
import org.kahina.tralesld.data.fs.TraleSLDFS;
import org.kahina.tralesld.visual.fs.VisualizationUtility;

public class QTypeGoal extends KahinaObject
{
	private static final long serialVersionUID = 1626803422982352323L;
	
	private static final boolean TEST_MODE = false;

	private TraleSLDFS in;
	
	private TraleSLDFS out;
	
	private static final VisualizationUtility util = new VisualizationUtility();
	
	public QTypeGoal()
	{
	}
	
	public QTypeGoal(QTypeGoal original)
	{
		this();
		in = original.in;
		out = original.out;
	}
	
	public void setIn(TraleSLDFS grisu)
	{
		test(grisu);
		in = grisu;
	}
	
	public void setOut(TraleSLDFS grisu)
	{
		test(grisu);
		out = grisu;
	}
	
	public TraleSLDFS getIn()
	{
		return in;
	}
	
	public TraleSLDFS getOut()
	{
		return out;
	}
	
	private void test(TraleSLDFS grisuMessage)
	{
		if (TEST_MODE)
		{
			String string = grisuMessage.toString();
			System.err.print(string);
			
			try
			{
				util.parseGrisu(string);
			} catch (ParseException e)
			{
				e.printStackTrace();
			}
		}
	}
}
