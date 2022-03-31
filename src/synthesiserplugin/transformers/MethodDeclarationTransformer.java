package synthesiserplugin.transformers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.InlineMethodDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring.Mode;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import synthesiserplugin.visitors.MethodDeclarationVisitor;

import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;



public class MethodDeclarationTransformer {
	
	ICompilationUnit icu ;
	CompilationUnit cu;
	
	public MethodDeclarationTransformer (ICompilationUnit icu, CompilationUnit cu) {
		this.icu = icu;
		this.cu = cu;
	}
	

	public ICompilationUnit getIcu() {
		return this.icu;
	}


	public CompilationUnit getCu() {
		return this.cu;
	}

	public void inlineMethodInvocation(MethodDeclaration documentationMethod, MethodDeclaration utilityMethod) throws CoreException {

		// get the start and end position of the utility method invocation to in-line
		// its body
		MethodDeclarationVisitor visitor = new MethodDeclarationVisitor (cu);
		Map<Integer, MethodInvocation> invoctions = visitor.locateUtilityCalls(documentationMethod);

		// this is should not be hard-coded
		MethodInvocation node = invoctions.get(13);

		int[] selection = getSelections(utilityMethod);
			InlineMethodRefactoring refactoring = InlineMethodRefactoring.create(icu,
					new RefactoringASTParser(ASTProvider.SHARED_AST_LEVEL).parse(icu, true), selection[0],
					selection[1]);

//		InlineMethodRefactoring refactoring = InlineMethodRefactoring.create(icu, cu, selection[0], selection[1]);

		refactoring.setDeleteSource(true);
		refactoring.setCurrentMode(Mode.INLINE_ALL); // or INLINE SINGLE based on the user's intervention

		IProgressMonitor pm = new NullProgressMonitor();
		RefactoringStatus res = refactoring.checkInitialConditions(pm);
		res = refactoring.checkFinalConditions(pm);

		final PerformRefactoringOperation op = new PerformRefactoringOperation(refactoring,
				CheckConditionsOperation.ALL_CONDITIONS);
		op.run(new NullProgressMonitor());
	}

	public int[] getSelections(MethodDeclaration node) {

		int start = node.getStartPosition();
		int end = node.getLength();
		int[] selections = { start, end };
		return selections;
	}
	

	public void inlineMethodInvocation2() throws CoreException {

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject("Hello");
		project.open(null /* IProgressMonitor */);

		IJavaProject javaProject = JavaCore.create(project);
		IType itype = javaProject.findType("SimpleTest");
		org.eclipse.jdt.core.ICompilationUnit icu = itype.getCompilationUnit();

		String projectName = icu.getResource().getProject().getName();
		String description = "Inline method programmatically";
		String comments = "";
		Map arguments = new HashMap();
		// arguments.put("", ""); <-- ???
		int flags = 0;

		// used when we do not want to use UI classes
		InlineMethodDescriptor descriptor = new InlineMethodDescriptor(projectName, description, comments, arguments,
				flags);

		RefactoringStatus status = new RefactoringStatus();
		try {
			Refactoring refactoring = descriptor.createRefactoring(status);

			IProgressMonitor monitor = new NullProgressMonitor();
			refactoring.checkInitialConditions(monitor);
			refactoring.checkFinalConditions(monitor);
			Change change = refactoring.createChange(monitor);
			change.perform(monitor);

		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * A working method that rename the icu to "New Class"
	 */
	public void renameClass() {


		RefactoringContribution contribution = RefactoringCore
				.getRefactoringContribution(IJavaRefactorings.RENAME_COMPILATION_UNIT);
		RenameJavaElementDescriptor descriptor = (RenameJavaElementDescriptor) contribution.createDescriptor();
		descriptor.setProject(icu.getResource().getProject().getName());
		descriptor.setNewName("NewClass"); // new name for a Class
		descriptor.setJavaElement(icu);

		RefactoringStatus status = new RefactoringStatus();
		try {
			Refactoring refactoring = descriptor.createRefactoring(status);

			IProgressMonitor monitor = new NullProgressMonitor();
			refactoring.checkInitialConditions(monitor);
			refactoring.checkFinalConditions(monitor);
			Change change = refactoring.createChange(monitor);
			change.perform(monitor);

		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
