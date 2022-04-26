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

public class MethodDeclarationVisitor  {

	private CompilationUnit cu;
	private ArrayList<MethodDeclaration> allMethodDeclarations;
	private ArrayList<MethodDeclaration> utilityMethods;
	private ArrayList<MethodDeclaration> documentationMethods;

	public MethodDeclarationVisitor(CompilationUnit cu) {
		this.cu = cu;
		this.allMethodDeclarations = new ArrayList<MethodDeclaration>();
		this.utilityMethods = new ArrayList<MethodDeclaration>();
		this.documentationMethods = new ArrayList<MethodDeclaration>();

		visitCU();
	}
	
	public CompilationUnit getCu() {
		return cu;
	}

	public void setCu(CompilationUnit cu) {
		this.cu = cu;
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
	 * finds all MethodDeclarations in this.cu and specify all documentation and
	 * utility methods
	 */
	public void visitCU() {
		
		this.cu.accept(new ASTVisitor() {

			public boolean visit(MethodDeclaration node) {
				@SuppressWarnings("unchecked")
				List<ASTNode> modifiers = (List<ASTNode>) node.getStructuralProperty(node.getModifiersProperty());
				modifiers.stream().forEach(modifier -> {
					if (modifier instanceof Annotation) {
						Annotation annotation = (Annotation) modifier;
						String typeName = annotation.getTypeName().toString();
						if (typeName.equals("Documentation")) {
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
	 * @return MethodInvocation nodes of Utility methods called inside @param
	 *         documentationMethod
	 */
	public Map<Integer, MethodInvocation> locateUtilityCalls(MethodDeclaration documentationMethod) {

		Map<Integer, MethodInvocation> utilityCalls = new HashMap<Integer, MethodInvocation>();
		CompilationUnit cUnit = this.cu;

		documentationMethod.accept(new ASTVisitor() {
			public boolean visit(MethodInvocation node) {
				utilityMethods.stream().forEach(method -> {
					if (method.getName().toString().equals(node.getName().toString())) {
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
	 * @return MethodDeclaration nodes of Utility methods called inside @param
	 *         documentationMethod
	 * 
	 */
	public ArrayList<MethodDeclaration> getUtilityDeclarations(MethodDeclaration documentationMethod) {

		ArrayList<MethodDeclaration> utilityDeclarations = new ArrayList<MethodDeclaration>();

		// 1. get the calls in this MethodDeclaration
		Map<Integer, MethodInvocation> utilityCalls = locateUtilityCalls(documentationMethod);
		// 2. stream over the MethodInvocations inside the received documentation method
		utilityCalls.entrySet().stream().forEach(e -> {
			// 3. find utility declarations using name search
			utilityMethods.stream().forEach(method -> {
				if (method.getName().toString().equals(e.getValue().getName().toString())) {
					utilityDeclarations.add(method);
				}
			});
		});

		return utilityDeclarations;
	}

	public boolean isUtilityMethod(String methodName) {
		return findUtilityMethodByName(methodName);
	}

	public boolean isDocummentionMethod(String methodName) {
		return findDocumentationMethodByName(methodName);
	}
	
	public MethodDeclaration getMethodByName (String methodName) {
		MethodDeclaration method = this.allMethodDeclarations.stream().filter(md -> md.getName().toString().equals(methodName)).findAny().orElse(null);
		return method;
	}

	private boolean findUtilityMethodByName(String methodName) {
		ArrayList<String> names = new ArrayList<String>();
		this.utilityMethods.stream().forEach(method -> {
			names.add(method.getName().toString());
		});

		return names.contains(methodName);
	}

	private boolean findDocumentationMethodByName(String methodName) {
		ArrayList<String> names = new ArrayList<String>();
		this.documentationMethods.stream().forEach(method -> {
			names.add(method.getName().toString());
		});

		return names.contains(methodName);
	}

}
