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
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import synthesiser.models.JavaProject;
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
				if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
					IJavaProject jProject = JavaCore.create(project);
					// create a model
					JavaProject javaProject = new JavaProject(jProject);
					javaProject.getiCompilationUnits().forEach(icu -> {

					CompilationUnit cu = javaProject.getParsedVersion(icu);
					MethodDeclarationVisitor visitor = new MethodDeclarationVisitor(cu);
					
					if (visitor.getAllMethodDeclarations().size() != 0) {
						// This is should not be hard-coded
						MethodDeclaration utilityMethod = visitor.getMethodByName("show");
							// Transform
							MethodDeclarationTransformer transformer = new MethodDeclarationTransformer(icu, cu);
							//	transformer.inlineMethodInvocations(utilityMethod);
							transformer.inlineMethod(utilityMethod);
						
						}
					});
				}

			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	/**
	 * Reads a ICompilationUnit and creates the AST DOM for manipulating the Java source file
	 * @param icu
	 * @return
	 */
	private static CompilationUnit parse(ICompilationUnit icu) {
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		// parse
		return (CompilationUnit) parser.createAST(null);
	}
}