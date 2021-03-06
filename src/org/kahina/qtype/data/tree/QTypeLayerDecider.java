package org.kahina.qtype.data.tree;

import java.util.HashMap;
import java.util.Map;

import org.kahina.core.data.tree.KahinaTree;
import org.kahina.core.data.tree.LayerDecider;

/**
 * Do not use an instance of this class for more than one tree, as it caches
 * layer IDs based on node IDs.
 * 
 * @author ke
 * 
 */
public class QTypeLayerDecider extends LayerDecider
{

	private static final boolean VERBOSE = false;

	/**
	 * 
	 */
	private static final long serialVersionUID = -3668167056515761575L;

	/**
	 * Cache. Never gets invalidated because decisions are never revised as long
	 * as no nodes are removed (and no nodes are ever removed).
	 */
	private final Map<Integer, Integer> layerByNode = new HashMap<Integer, Integer>();

	@Override
	public int decideOnLayer(int nodeID, KahinaTree tree)
	{
		Integer layer = layerByNode.get(nodeID);
		if (layer == null)
		{
			layer = doDecideOnLayer(nodeID, tree);
			layerByNode.put(nodeID, layer);
		}
		return layer;
	}

	private int doDecideOnLayer(int nodeID, KahinaTree tree)
	{
		if (VERBOSE)
		{
			System.err.println(this + ".doDecideOnLayer(" + nodeID + ", [tree]");
		}
		
		int parentID = tree.getParent(nodeID);
		
		if (parentID == -1)
		{
			return 0;
		}
		
		int parentLayer = decideOnLayer(parentID, tree);
		
		if (parentLayer == 1)
		{
			return 1;
		}
		
		String caption = tree.getNodeCaption(nodeID);
		
		if (caption.contains("compile_grammar(") || caption.contains(" lc(") || caption.contains(" lc_complete(") || caption.contains(" lc_list(") || caption.contains(" lexentry_existence(") || caption.contains(" tokenize_and_parse_the_external_syntax("))
		{
			return 0;
		}
		
		if (caption.contains("(") || caption.endsWith(" copy_dbfs2"))
		{
			return 1;
		}
		
		return 0;
	}

}
