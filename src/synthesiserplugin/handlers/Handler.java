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
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import synthesiserplugin.deadcode.detector.DeadCodeDetector;
import synthesiserplugin.models.JavaProject;
import synthesiserplugin.transformers.MethodDeclarationTransformer;

public class Handler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IJavaProject jProject = getProject();
		
		//---------------------
		// Check the dead code detection
		IPackageFragmentRoot packageFragment = null;
		try {
			for (IPackageFragmentRoot packageFragmentRoot : jProject.getPackageFragmentRoots()) {
					if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
						packageFragment = packageFragmentRoot;
					}
					
			}
			
		} catch (JavaModelException e1) {
			e1.printStackTrace();
		}
	        
		DeadCodeDetector detector = new DeadCodeDetector(jProject, packageFragment );
		System.out.println(jProject.getElementName().toString() + packageFragment.getElementName().toString());
		
		try {
			detector.DetectIfThenDeadCode();
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		
		//---------------------
		
		
		
		try {
			// create a model
			JavaProject javaProject  = new JavaProject(jProject);
			// transform
			MethodDeclarationTransformer transformer = new MethodDeclarationTransformer(javaProject);
			// in-line from a doc method perspective
//			transformer.inlineDocMethod("FlockDocMethod");
			
			// in-line all doc methods in a particular CU
//			transformer.inlineAllDocIn("JFrameExample");
	
			
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public static IJavaProject getProject() {
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IJavaProject jProject = null;
		IProject[] projects = root.getProjects();
		for (IProject project : projects) {
			// get the project you need to work with
			try {
				if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")
						&& project.getName().toString().equals("SampleProject")) {
					jProject = JavaCore.create(project);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		
		return jProject;
	}
}