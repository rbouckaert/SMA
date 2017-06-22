package sma;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.tree.Node;
import beast.evolution.tree.TreeDistribution;
import beast.evolution.tree.TreeInterface;
import beast.math.distributions.ParametricDistribution;

@Description("Exponential branch length distribution, specially for SMA analyses")
public class SMATreePrior extends TreeDistribution {
    final public Input<ParametricDistribution> distInput = new Input<>("distr", "distribution used to calculate prior, e.g. normal, beta, gamma.", Validate.REQUIRED);
 
    TreeInterface tree;
    protected ParametricDistribution dist;
    
    @Override
    public void initAndValidate() {
    	tree = treeInput.get();
    	
    	super.initAndValidate();
    }
    
    
    @Override
    public double calculateLogP() {
    	logP = 0;
    	for (Node node : tree.getNodesAsArray()) {
    		if (!node.isRoot()) {
    	        logP += dist.logDensity(node.getLength());
    		}
    	}
    	return logP;
    }
}