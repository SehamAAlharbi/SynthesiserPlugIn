package synthesiserplugin.transformers;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring.Mode;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

@SuppressWarnings("restriction")
public class MethodDeclarationTransformer {

	ICompilationUnit icu;
	CompilationUnit cu;

	public MethodDeclarationTransformer(ICompilationUnit icu, CompilationUnit cu) {
		this.icu = icu;
		this.cu = cu;
	}

	public ICompilationUnit getIcu() {
		return this.icu;
	}

	public CompilationUnit getCu() {
		return this.cu;
	}

	@SuppressWarnings("restriction")
	public void inlineMethodInvocations(MethodDeclaration documentationMethod, MethodDeclaration utilityMethod)
			throws CoreException {

		int[] selection = getSelections(utilityMethod);
		@SuppressWarnings("restriction")
		InlineMethodRefactoring refactoring = InlineMethodRefactoring.create(icu,
				new RefactoringASTParser(ASTProvider.SHARED_AST_LEVEL).parse(icu, true), selection[0], selection[1]);

		refactoring.setDeleteSource(true);
		refactoring.setCurrentMode(Mode.INLINE_ALL);

		IProgressMonitor pm = new NullProgressMonitor();
		RefactoringStatus res = refactoring.checkInitialConditions(pm);
		res = refactoring.checkFinalConditions(pm);

		final PerformRefactoringOperation op = new PerformRefactoringOperation(refactoring,
				CheckConditionsOperation.ALL_CONDITIONS);
		op.run(new NullProgressMonitor());
	}

	/**
	 * gets the selections of a MethodDeclaration in source code
	 * 
	 * @param node is the to-be-in-lined utility
	 * @return array of selections
	 */
	public int[] getSelections(MethodDeclaration node) {

		int start = node.getStartPosition();
		int end = node.getLength();
		int[] selections = { start, end };
		return selections;
	}

	/**
	 * rename the icu to a new chosen name
	 */
	public void renameClass() {

		RefactoringContribution contribution = RefactoringCore
				.getRefactoringContribution(IJavaRefactorings.RENAME_COMPILATION_UNIT);
		RenameJavaElementDescriptor descriptor = (RenameJavaElementDescriptor) contribution.createDescriptor();
		descriptor.setProject(icu.getResource().getProject().getName());
		 // new name for a Class
		descriptor.setNewName("NewClass");
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
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}