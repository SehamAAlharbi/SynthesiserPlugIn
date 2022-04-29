package synthesiserplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import synthesiserplugin.models.JavaProject;
import synthesiserplugin.transformers.MethodDeclarationTransformer;

public class Handler extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();
		for (IProject project : projects) {
			try {
				// get the project you need to work with
				if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")
						&& project.getName().toString().equals("EpsilonProject")) {
					IJavaProject jProject = JavaCore.create(project);
					// create a model
					JavaProject javaProject = new JavaProject(jProject);

					// transform
					MethodDeclarationTransformer transformer = new MethodDeclarationTransformer(javaProject);
					transformer.inlineMethodByName("execute");
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
}
