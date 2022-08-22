package synthesiserplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import synthesiserplugin.deadcode.DeadCodeDetector;
import synthesiserplugin.models.JavaProject;
import synthesiserplugin.parser.Parser;
import synthesiserplugin.transformers.MethodDeclarationTransformer;

public class DeadCodeHandler extends AbstractHandler {
	
	private static IFile file;
	private static ICompilationUnit currentICU;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		

		try {
			// get the name of the selected .java source file
			IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
			IResource resource = Adapters.adapt(selection.getFirstElement(), IResource.class);
			if (resource instanceof IFile) {
				IFile file = (IFile) resource;
				DeadCodeHandler.file = file;
			}

			// get the Java project you want to work with
			IJavaProject jProject = getProject();

			// create a model
			JavaProject javaProject = new JavaProject(jProject);
			currentICU = javaProject.getICUByName(file.getName());
			
			// detect dead/unreachable code problems
			DeadCodeDetector detector = new DeadCodeDetector(currentICU);
			detector.detectProblems();
			// generate clean code
			detector.generateCode(javaProject.getDocumentationPackage());
			// reset content to original
			resetContent();
			
			
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
			
		return null;
	}
	
	
	/**
	 * once the user select a .java file, the project where that file is located
	 * will be used
	 * 
	 * @return the java project where the .java file is being selected
	 */
	public static IJavaProject getProject() {

		IJavaProject jProject = null;
		IProject project = file.getProject();
		try {
			// only open projects that are of Java nature will be returned
			if (project.isNatureEnabled("org.eclipse.jdt.core.javanature") && project.isOpen()) {
				jProject = JavaCore.create(project);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return jProject;
	}
	
	public void resetContent () {
		Handler handler = new Handler();
		ICompilationUnit originalICU = handler.getOriginal();
		try {
			currentICU.getBuffer().setContents(originalICU.getBuffer().getContents());
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

}
