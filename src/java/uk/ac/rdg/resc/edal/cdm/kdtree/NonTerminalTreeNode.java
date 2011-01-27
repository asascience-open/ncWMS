package uk.ac.rdg.resc.edal.cdm.kdtree;

public class NonTerminalTreeNode extends TreeNode {
	float discriminator;
	boolean is_latitude;
	
	public NonTerminalTreeNode(float discriminator, boolean isLatitude) {
		super();
		this.discriminator = discriminator;
		is_latitude = isLatitude;
	}
	
}
