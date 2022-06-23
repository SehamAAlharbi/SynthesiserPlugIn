package synthesiserplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;			
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;


import synthesiserplugin.models.JavaProject;
import synthesiserplugin.transformers.MethodDeclarationTransformer;

public class Handler extends AbstractHandler {
	
	static IFile file;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		// get the name of the selected .java source file
		IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
		IResource resource = Adapters.adapt(selection.getFirstElement(), IResource.class);
		if (resource instanceof IFile) {
		  IFile file = (IFile)resource;
		  Handler.file = file;

		}
		
		// get the Java project you want to work with
		IJavaProject jProject = getProject();
		
		try {
			// create a model
			JavaProject javaProject  = new JavaProject(jProject);
			
			// transform
			MethodDeclarationTransformer transformer = new MethodDeclarationTransformer(javaProject);
			
			// in-line all doc methods in the selected CU i.e. (.java) file by the user
			transformer.inlineAllDocIn(file.getName());
			// work on the dead code
			transformer.detectAndGenerate();
				
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	/**
	 * once the user select a .java file, the project where that file is located will be used
	 * @return the java project where the .java file is being selected
	 */
	public static IJavaProject getProject() {
		
		IJavaProject jProject = null;
		IProject project = file.getProject();
			try {
				// only open projects that are of Java nature will be returned
				if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")
						&& project.isOpen()) {
					jProject = JavaCore.create(project);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		
		return jProject;
	}
}