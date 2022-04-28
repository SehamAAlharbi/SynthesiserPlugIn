package synthesiserplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import synthesiserplugin.models.JavaProject;
import synthesiserplugin.transformers.MethodDeclarationTransformer;
import synthesiserplugin.visitors.MethodDeclarationVisitor;

public class Handler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		// get all projects in the workspace
		IProject[] projects = root.getProjects();
		// loop over all projects
		for (IProject project : projects) {

			try {

				// get the project you need to work with
				if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")
						&& project.getName().toString().equals("EpsilonProject")) {
					IJavaProject jProject = JavaCore.create(project);
					// create a model
					JavaProject javaProject = new JavaProject(jProject);

					System.out.println(javaProject.getCompilationUnits().size());
					System.out.println(javaProject.getPackages().size());

//					javaProject.getiCompilationUnits().forEach(icu -> {

					ICompilationUnit icu = javaProject.getiCompilationUnits().get(0);
//					CompilationUnit cu = javaProject.getParsedVersion(icu);

					MethodDeclarationVisitor visitor = new MethodDeclarationVisitor(icu);

//					visitor.getAllMethodDeclarations().stream().forEach(md -> {
//						System.out.println(md.getName().toString());
//					});

					System.out.println(visitor.getDocumentationMethods().size());
					System.out.println(visitor.getUtilityMethods().size());

					// Transform
					MethodDeclarationTransformer transformer = new MethodDeclarationTransformer();
//						transformer.inlineMethod(utilityMethod);
					transformer.inlineMethodByName("execute");

				}

			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		return null;

	}
}
