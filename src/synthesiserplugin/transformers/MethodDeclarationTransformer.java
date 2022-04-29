package synthesiserplugin.transformers;

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
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
// you need to reuse this!
import org.eclipse.jdt.core.refactoring.descriptors.InlineMethodDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring.Mode;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import synthesiserplugin.models.JavaProject;
import synthesiserplugin.visitors.MethodDeclarationVisitor;

@SuppressWarnings("restriction")
public class MethodDeclarationTransformer {
	
	private JavaProject javaProject;

	public MethodDeclarationTransformer(JavaProject javaProject) {
		this.javaProject = javaProject;
	}
	
	/**
	 * 
	 * @param name of utility method to be in-lined where it is called
	 * @throws CoreException 
	 */
	public void inlineMethodByName(String name) {
		
		this.javaProject.getiCompilationUnits().forEach(icu -> {
		ICompilationUnit iCompilationUnit = new MethodDeclarationVisitor(icu).getUtilityMap().entrySet().stream()
					.filter(e -> e.getKey().getName().toString().equals(name))
					.map(Map.Entry::getValue)
					.findFirst()
					.orElse(null);
		if (iCompilationUnit !=null) {
			MethodDeclarationVisitor visitor = new MethodDeclarationVisitor(iCompilationUnit);
			CompilationUnit compilationUnit = visitor.getParsedVersion(iCompilationUnit);
			MethodDeclaration method = visitor.getMethodByName(name);
			inlineMethod(iCompilationUnit, compilationUnit, method);
		}
		});
		
	}

	@SuppressWarnings("restriction")
	public void inlineMethod(ICompilationUnit icu, CompilationUnit cu , MethodDeclaration utilityMethod) {

		if (utilityMethod == null) {
			throw new IllegalArgumentException("Utility Method is Null!");
		}

		else {

			int[] selection = getSelections(utilityMethod);
			@SuppressWarnings("restriction")
			InlineMethodRefactoring refactoring = InlineMethodRefactoring.create(icu, cu, selection[0], selection[1]);

			refactoring.setDeleteSource(true);
			try {
				refactoring.setCurrentMode(Mode.INLINE_ALL);
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
	}

	/**
	 * gets the selections of a MethodDeclaration in source code
	 * 
	 * @param node is the to-be-in-lined utility
	 * @return array of selections, body of utility method
	 */
	public int[] getSelections(MethodDeclaration node) {

		int start = node.getStartPosition();
		int end = node.getLength();
		int[] selections = { start, end };
		return selections;
	}
	
	/**
	 * try in-lining using non-internal classes
	 */
	public void inlineUsingContribution () throws CoreException {
		// 1. Get ICompiationUnit for type "smcho.Hello"
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject("Hello");
		project.open(null /* IProgressMonitor */);

		IJavaProject javaProject = JavaCore.create(project);
		IType itype = javaProject.findType("smcho.Hello");
		ICompilationUnit icu = itype.getCompilationUnit();

		// 2. Contribution and Description creation
		RefactoringContribution contribution = RefactoringCore.getRefactoringContribution(IJavaRefactorings.INLINE_METHOD);
		InlineMethodDescriptor descriptor = (InlineMethodDescriptor) contribution.createDescriptor();

		descriptor.setProject(icu.getResource().getProject().getName( ));

		// 3. executing the refactoring
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
	 * rename the icu to a new chosen name, does not use internal packages, you can use it to in-line methods as well, try IJavaRefactorings.INLINE_METHO
	 */
	public void renameClass() {

		RefactoringContribution contribution = RefactoringCore
				.getRefactoringContribution(IJavaRefactorings.RENAME_COMPILATION_UNIT);
		RenameJavaElementDescriptor descriptor = (RenameJavaElementDescriptor) contribution.createDescriptor();
//		descriptor.setProject(icu.getResource().getProject().getName());
		
		 // new name for a Class
		descriptor.setNewName("NewClass");
//		descriptor.setJavaElement(icu);
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