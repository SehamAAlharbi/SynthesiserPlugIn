package synthesiser.models;

import org.eclipse.jdt.core.dom.MethodDeclaration;

public class Method {
	
	MethodDeclaration node;

	public Method(MethodDeclaration node) {
		
		this.node=node;
	
	}

	public MethodDeclaration getNode() {
		return node;
	}

	public void setNode(MethodDeclaration node) {
		this.node = node;
	}
	
	
}
