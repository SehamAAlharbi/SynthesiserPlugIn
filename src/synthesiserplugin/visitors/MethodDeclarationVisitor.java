package synthesiserplugin.visitors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

import synthesiserplugin.parser.Parser;

public class MethodDeclarationVisitor {

	ICompilationUnit icu;
	private CompilationUnit cu;
	private ArrayList<MethodDeclaration> allMethodDeclarations;
	private ArrayList<MethodDeclaration> utilityMethods;
	private Map<MethodDeclaration, ICompilationUnit> utilityMap;
	private ArrayList<MethodDeclaration> documentationMethods;

	public MethodDeclarationVisitor(ICompilationUnit icu) {
		this.icu = icu;
		this.cu = getParsedVersion(icu);
		this.allMethodDeclarations = new ArrayList<MethodDeclaration>();
		this.utilityMethods = new ArrayList<MethodDeclaration>();
		this.utilityMap = new HashMap<>();
		this.documentationMethods = new ArrayList<MethodDeclaration>();

		visitCU();
	}

	public ICompilationUnit getIcu() {
		return icu;
	}

	public void setIcu(ICompilationUnit icu) {
		this.icu = icu;
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

	public Map<MethodDeclaration, ICompilationUnit> getUtilityMap() {
		return utilityMap;
	}

	public void setUtilityMap(Map<MethodDeclaration, ICompilationUnit> utilityMap) {
		this.utilityMap = utilityMap;
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
				allMethodDeclarations.add(node);

				@SuppressWarnings("unchecked")
				List<ASTNode> modifiers = (List<ASTNode>) node.getStructuralProperty(node.getModifiersProperty());
				modifiers.stream().forEach(modifier -> {
					if (modifier instanceof Annotation) {
						Annotation annotation = (Annotation) modifier;
						String typeName = annotation.getTypeName().toString();
						if (typeName.equals("Documentation")) {
							documentationMethods.add(node);
						}

						else if (typeName.equals("Utility")) {
							utilityMethods.add(node);
							utilityMap.put(node, icu);
						}
					}
				});

				return true;
			}
		});
	}

	public CompilationUnit getParsedVersion(ICompilationUnit icu) {
		Parser parser = new Parser();
		return parser.parse(icu);
	}

	/**
	 * 
	 * @return MethodInvocation nodes of Utility methods called inside @param
	 *         documentationMethod
	 */
	private Map<Integer, MethodInvocation> locateUtilityCalls(MethodDeclaration documentationMethod) {

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
	 * @return list of MethodDeclaration nodes of Utility methods called
	 *         inside @param documentationMethod
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

	/**
	 * 
	 * @param documentationMethod is the doc method to find utility calls in it
	 * @return a list of MethodInvocation nodes in it
	 */
	public ArrayList<MethodInvocation> getUtilityInvocations(MethodDeclaration documentationMethod) {

		ArrayList<MethodInvocation> utilityInvocations = new ArrayList<MethodInvocation>();

		documentationMethod.accept(new ASTVisitor() {
			public boolean visit(MethodInvocation node) {

				// check whether this invocation is ( of / binding to ) a utility method
				IMethodBinding iMethod = (IMethodBinding) node.resolveMethodBinding();
				if (iMethod != null && isUtilityBinding(iMethod)) {
					utilityInvocations.add(node);
				}

				return true;
			}
		});

		return utilityInvocations;

	}

	private boolean isUtilityBinding(IMethodBinding iMethod) {

		boolean isUtility = false;
		if (iMethod.getAnnotations().length != 0) {
			String annotations = Arrays.toString(iMethod.getAnnotations());
			if (annotations.contains("@Utility()")) {
				isUtility = true;
			}
		}

		return isUtility;

	}

	public boolean isUtilityMethod(String methodName) {
		return findUtilityMethodByName(methodName);
	}

	public boolean isDocummentionMethod(String methodName) {
		return findDocumentationMethodByName(methodName);
	}

	public MethodDeclaration getMethodByName(String methodName) {
		MethodDeclaration method = this.allMethodDeclarations.stream()
				.filter(md -> md.getName().toString().equals(methodName)).findAny().orElse(null);
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
