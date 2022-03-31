package synthesiserplugin.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class MethodDeclarationVisitor {
	
	private ArrayList<MethodDeclaration> allMethodDeclarations = new ArrayList<MethodDeclaration>();
	private ArrayList<MethodDeclaration> utilityMethods = new ArrayList<MethodDeclaration>();
	private ArrayList<MethodDeclaration> documentationMethods = new ArrayList<MethodDeclaration>();
	CompilationUnit cu ;
	
	public MethodDeclarationVisitor(CompilationUnit cu) {
		this.cu = cu;
		this.setUp(cu);
	}

	public ArrayList<MethodDeclaration> getAllMethodDeclarations() {
		return allMethodDeclarations;
	}

	public void setAllMethodDeclarations(ArrayList<MethodDeclaration> allMethodDeclarations) {
		this.allMethodDeclarations = allMethodDeclarations;
	}

	public ArrayList<MethodDeclaration> getUtilityMethods() {
		return utilityMethods;
	}

	public void setUtilityMethods(ArrayList<MethodDeclaration> utilityMethods) {
		this.utilityMethods = utilityMethods;
	}

	public ArrayList<MethodDeclaration> getDocumentationMethods() {
		return documentationMethods;
	}

	public void setDocumentationMethods(ArrayList<MethodDeclaration> documentationMethods) {
		this.documentationMethods = documentationMethods;
	}
	
	/**
	 * locates all documentation and utility methods in each @Param cu
	 * @param cu of each .java file
	 */
	public void setUp(CompilationUnit cu) {
		
		cu.accept(new ASTVisitor() {

			public boolean visit(MethodDeclaration node) {

				
				@SuppressWarnings("unchecked")
				List<ASTNode> modifiers = (List<ASTNode>) node.getStructuralProperty(node.getModifiersProperty());
				 modifiers.stream().forEach(modifier -> {
					 if(modifier instanceof Annotation) {
						 Annotation annotation = (Annotation) modifier;
						 String typeName = annotation.getTypeName().toString();
						 if(typeName.equals("Documentation")) {
							 documentationMethods.add(node);
							 allMethodDeclarations.add(node);
						 }
						 
						 else if (typeName.equals("Utility")) {
							 utilityMethods.add(node);
							 allMethodDeclarations.add(node);
						 }
						 
						 else {
							 allMethodDeclarations.add(node);
						 }
					
					 }
				 });
				
				return true;
			}
			
			
		});
	
	}
	
	/**
	 * 
	 * @return MethodInvocation nodes inside @param documentationMethod
	 */
	public Map<Integer, MethodInvocation> locateUtilityCalls(MethodDeclaration documentationMethod) {
		
		Map<Integer, MethodInvocation> utilityCalls = new HashMap<Integer, MethodInvocation>();
		CompilationUnit cUnit = this.cu;
		
		documentationMethod.accept(new ASTVisitor() {
			public boolean visit(MethodInvocation node) {
				utilityMethods.stream().forEach(method -> {
					if(method.getName().toString().equals(node.getName().toString())) {
						utilityCalls.put(cUnit.getLineNumber(node.getStartPosition()), node);
					}
				});
				
				
				return true;
			}
		});
		
		
		return utilityCalls;
	}
	
	/**
	 * 
	 * @return MethodDeclaration nodes of Utility methods called inside @param documentationMethod
	 */
	public ArrayList <MethodDeclaration> locateUtilityDeclarations (MethodDeclaration documentationMethod) {
		
		ArrayList <MethodDeclaration> utilityDeclarations = new ArrayList <MethodDeclaration> ();
		
		// loop through calls and utilityMethods filed to get them
		
		return utilityDeclarations;
	}

}
