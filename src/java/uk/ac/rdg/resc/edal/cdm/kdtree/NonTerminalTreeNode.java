package uk.ac.rdg.resc.edal.cdm.kdtree;

public class NonTerminalTreeNode extends TreeNode {
	double discriminator;
	boolean is_latitude;
	
	public NonTerminalTreeNode(double discriminator, boolean isLatitude) {
		super();
		this.discriminator = discriminator;
		is_latitude = isLatitude;
	}
	
}
