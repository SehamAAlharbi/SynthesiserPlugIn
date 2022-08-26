package synthesiserplugin.visitors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ThisExpression;

import synthesiserplugin.handlers.Handler;
import synthesiserplugin.markers.RecursiveCallMarker;
import synthesiserplugin.parser.Parser;

public class MethodDeclarationVisitor {

	private ICompilationUnit icu;
	private CompilationUnit cu;
	private ArrayList<MethodDeclaration> allMethodDeclarations;
	private ArrayList<MethodDeclaration> utilityMethods;
	private Map<MethodDeclaration, ICompilationUnit> utilityMap;
	private ArrayList<MethodDeclaration> documentationMethods;
	private boolean isRecursive ;

	public MethodDeclarationVisitor(ICompilationUnit icu) {
		this.icu = icu;
		this.cu = getParsedVersion(icu);
		this.allMethodDeclarations = new ArrayList<MethodDeclaration>();
		this.utilityMethods = new ArrayList<MethodDeclaration>();
		this.utilityMap = new HashMap<>();
		this.documentationMethods = new ArrayList<MethodDeclaration>();
		this.isRecursive = false;

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

		if (documentationMethod == null) {
			return utilityInvocations;
		} else {
			documentationMethod.accept(new ASTVisitor() {
				public boolean visit(MethodInvocation node) {

					// check whether this invocation is ( of / binding to ) a utility method
					IMethodBinding iMethod = (IMethodBinding) node.resolveMethodBinding();
					MethodDeclaration declaration = getMethodDeclarationNode(node);
				
					// if the call is of a recursive method, it will not be in-lined - not added to the ArrayList
					if (iMethod != null && isUtilityBinding(iMethod) && !isRecursive(declaration)) {
						utilityInvocations.add(node);
						//reset its class member value to false for future use by other invocations
						isRecursive = false;
					}

					return true;
				}
			});
		}

		return utilityInvocations;

	}

	public boolean isUtilityBinding(IMethodBinding iMethod) {

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

	/**
	 * finds whether a method declaration contains recursive calls
	 * @param declaration
	 * @param node
	 * @return
	 */
	public boolean isRecursive(MethodDeclaration declaration) {
		
		if (declaration != null) {
			declaration.accept(new ASTVisitor() {
				public boolean visit(MethodInvocation node) {

					IMethodBinding fBinding = declaration.resolveBinding();
					Expression expression = node.getExpression();
					IMethodBinding binding = node.resolveMethodBinding();
					if (binding == null || !Modifier.isStatic(binding.getModifiers()) && binding.isEqualTo(fBinding)
							&& (expression == null || expression instanceof ThisExpression)) {
						isRecursive = true;
					}

					return true;
				}
			});
		}

		return isRecursive;
		
	}
	
	/**
	 * to be used to identify whether the invocation is of a recursive method
	 * @return the MethodDeclaration node of a MethodInvocation
	 */
	public MethodDeclaration getMethodDeclarationNode(MethodInvocation node) {
		
		IMethodBinding binding = (IMethodBinding) node.getName().resolveBinding();
		ICompilationUnit icu = (ICompilationUnit) binding.getJavaElement().getAncestor( IJavaElement.COMPILATION_UNIT );
		// if its taken from an external JAR not a java project
		
		if ( icu == null ) {
		   return null;
		}
		
		// if the compilation unit is found, parse it and return the required MethodDeclaration
		CompilationUnit cu =  new Parser().parse(icu);
		MethodDeclaration declaration = (MethodDeclaration)cu.findDeclaringNode(binding.getKey());
		
		return declaration;
		
	}
	
	/**
	 * marks all method invocations within this CU if they are of [1] recursive methods [2] annotated with @Utility
	 */
	public void markRecursiveInvocations() {
		this.cu.accept(new ASTVisitor() {
			public boolean visit(MethodDeclaration node) {
				if (isDocumentationMethod(node)) {
					node.accept(new ASTVisitor() {
						public boolean visit(MethodInvocation Invonode) {
							MethodDeclaration declaration = getMethodDeclarationNode(Invonode);
							if (declaration != null && isUtilityMethod(declaration) && isRecursive(declaration)) {
								// create a marker for this method invocation node so the user knows its
								// location
								IFile file = Handler.file;
								int lineNumber = getCu().getLineNumber(Invonode.getStartPosition());
								new RecursiveCallMarker().createMarker(file, lineNumber);
								// reset its class member value to false for future use by other invocations
								isRecursive = false;
							}
							return true;
						}
					});
				}
				return true;
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	public boolean isDocumentationMethod(MethodDeclaration node) {
		if (node == null ) {
			return false;
		}
		List<ASTNode> modifiers = (List<ASTNode>) node.getStructuralProperty(node.getModifiersProperty());
		boolean isDocumentationMethod = modifiers.stream().anyMatch(modifier -> modifier instanceof Annotation
				&& ((Annotation) modifier).getTypeName().toString().equals("Documentation"));
		return isDocumentationMethod;
	}
	
	@SuppressWarnings("unchecked")
	public boolean isUtilityMethod(MethodDeclaration node) {
		if (node == null ) {
			return false;
		}
		List<ASTNode> modifiers = (List<ASTNode>) node.getStructuralProperty(node.getModifiersProperty());
		boolean isUtilityMethod = modifiers.stream().anyMatch(modifier -> modifier instanceof Annotation
				&& ((Annotation) modifier).getTypeName().toString().equals("Utility"));
		return isUtilityMethod;
	}	

}
