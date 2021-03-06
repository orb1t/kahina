package org.kahina.core.test;

import org.kahina.core.data.tree.KahinaTree;
import org.kahina.core.data.tree.LayerDecider;

public class TestLayerDecider extends LayerDecider
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2227151963480031811L;

	@Override
	public int decideOnLayer(int nodeID, KahinaTree tree)
	{
		if (nodeID == tree.getRootID(0)) return 0;
        if (nodeID == -1) return -1;
        KahinaTree primaryModel = tree.getPrimaryModel();
        if (primaryModel.getNodeCaption(nodeID).indexOf("rule") != -1 || primaryModel.getNodeCaption(nodeID).indexOf("\"") != -1) return 0;
        else if (primaryModel.getNodeCaption(nodeID).indexOf("goal") != -1) return 1;
        return 2;
	}
}
