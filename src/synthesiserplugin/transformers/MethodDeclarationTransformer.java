package synthesiserplugin.transformers;

import java.util.ArrayList;	

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring.Mode;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import synthesiserplugin.handlers.Handler;
import synthesiserplugin.models.JavaProject;
import synthesiserplugin.visitors.MethodDeclarationVisitor;

@SuppressWarnings("restriction")
public class MethodDeclarationTransformer {

	private JavaProject javaProject;

	public MethodDeclarationTransformer(JavaProject javaProject) {
		this.javaProject = javaProject;
	}

	/**
	 * @param name of the doc method to in-line all utility calls within its body
	 */
	public void inlineDocMethod(String name) {

		this.javaProject.getiCompilationUnits().forEach(icu -> {
			MethodDeclarationVisitor visitor = new MethodDeclarationVisitor(icu);
			ArrayList<MethodDeclaration> docMethodsList = visitor.getDocumentationMethods();

			if (!docMethodsList.isEmpty()) {
				MethodDeclaration docMethod = docMethodsList.stream().filter(md -> md.getName().toString().equals(name))
						.findFirst().orElse(null);
				ArrayList<MethodInvocation> utilityInvocations = visitor.getUtilityInvocations(docMethod);

				if (!utilityInvocations.isEmpty()) {
					CompilationUnit cu = visitor.getParsedVersion(icu);
					MethodInvocation invocation = utilityInvocations.get(0);
					inlineMethodInvocation(icu, cu, invocation);
					
					// update CU after each in-line
					IJavaProject transformedJProject = Handler.getProject();
					
					try {
						this.javaProject = new JavaProject(transformedJProject);
						// recursive call
						inlineDocMethod(name);

					} catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	/**
	 * The method that does the actual in-lining
	 * @param icu  is where the utility invocation is found
	 * @param cu   the parsed version of icu
	 * @param utilityInvocation is the invocation to be in-lined
	 */
	private void inlineMethodInvocation(ICompilationUnit icu, CompilationUnit cu, MethodInvocation utilityInvocation) {

		int[] selection = getInvocationSelections(utilityInvocation);
		InlineMethodRefactoring refactoring = InlineMethodRefactoring.create(icu, cu, selection[0], selection[1]);
		refactoring.setDeleteSource(false);

		try {

			refactoring.setCurrentMode(Mode.INLINE_SINGLE);
			IProgressMonitor pm = new NullProgressMonitor();
			RefactoringStatus res = refactoring.checkInitialConditions(pm);
			res = refactoring.checkFinalConditions(pm);

			final PerformRefactoringOperation op = new PerformRefactoringOperation(refactoring,
					CheckConditionsOperation.ALL_CONDITIONS);
			op.run(new NullProgressMonitor());

		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * gets the selections of a MethodInvocation in source code
	 * @param node is the to-be-in-lined utility invocation
	 * @return array of selections, body of utility method
	 */
	private int[] getInvocationSelections(MethodInvocation node) {

		int start = node.getStartPosition();
		int end = node.getLength();
		int[] selections = { start, end };
		return selections;
	}

}