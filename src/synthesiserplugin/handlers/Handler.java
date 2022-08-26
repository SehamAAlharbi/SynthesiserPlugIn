package synthesiserplugin.handlers;

import java.util.ArrayList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;

import synthesiserplugin.markers.RecursiveCallMarker;
import synthesiserplugin.models.JavaProject;
import synthesiserplugin.transformers.MethodDeclarationTransformer;
import synthesiserplugin.visitors.MethodDeclarationVisitor;

public class Handler extends AbstractHandler {

	public static IFile file;
	public static ICompilationUnit original;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		try {
			// get the name of the selected .java source file
			IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
			IResource resource = Adapters.adapt(selection.getFirstElement(), IResource.class);
			if (resource instanceof IFile) {
				IFile ifile = (IFile) resource;
				file = ifile;
			}

			
			// get the Java project you want to work with
			IJavaProject jProject = getProject();

			// create a model
			JavaProject javaProject = new JavaProject(jProject);

			// listener to pre-execution and post-execution
			Listener listener = new Listener(javaProject.getICUByName(file.getName()), javaProject);
			addListener(listener);

			// set original ICU - only use a working copy so you do not get the synthesised
			// version - to be used later to set content back to original
			Handler.original = javaProject.getICUByName(file.getName()).getWorkingCopy(null);
			
			// since markers are permanent, delete all previous markers made on the recursive method calls
			file.deleteMarkers("org.eclipse.core.resources.problemmarker", false, IResource.DEPTH_ZERO );
			
			// mark new recursive method calls if any exists
			ICompilationUnit icu = javaProject.getICUByName(file.getName());
			MethodDeclarationVisitor visitor = new MethodDeclarationVisitor(icu);
			visitor.markRecursiveInvocations();

			// transform
			MethodDeclarationTransformer transformer = new MethodDeclarationTransformer(javaProject);
			
			// the transformation is only done when there is no recursive method calls
			if (new RecursiveCallMarker().findMarkers(file).isEmpty()) {
			// in-line all doc methods in the selected CU i.e. (.java) file by the user
			transformer.inlineAllDocIn(file.getName());
			}

		} catch (CoreException e) {
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

	public ICompilationUnit getOriginal() {
		return Handler.original;
	}

	private void addListener(Listener listener) {
		ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
		Command command = commandService.getCommand("SynthesiserPlugIn.commands.sampleCommand");
		command.addExecutionListener(listener);
	}

}