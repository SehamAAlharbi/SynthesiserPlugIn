package synthesiser.models;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.MethodDeclaration;

public class DocumentationMethod extends Method {

	public DocumentationMethod(MethodDeclaration node) {
		super(node);
		
	}
	
	public ArrayList<MethodDeclaration> getUtilityCalls() {
		
		ArrayList<MethodDeclaration> utilityCalls = null;
		return utilityCalls;
	}

}
