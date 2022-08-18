package synthesiserplugin.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.jdt.core.ICompilationUnit;

import synthesiserplugin.models.JavaProject;
import synthesiserplugin.parser.Parser;

public class Listener implements IExecutionListener {
	
	private ICompilationUnit original;
	private JavaProject javaProject;
	
	public Listener(ICompilationUnit original, JavaProject javaProject) {
		this.original = original;
		this.javaProject = javaProject;
	}

	@Override
	public void preExecute(String commandId, ExecutionEvent event) {
		System.out.println("preExecute");
		
	}

	@Override
	public void postExecuteSuccess(String commandId, Object returnValue) {

		System.out.println("postExecuteSuccess");
		System.out.println(new Parser().parse(original).toString());

	}

	@Override
	public void postExecuteFailure(String commandId, ExecutionException exception) {
		// do nothing
	}

	@Override
	public void notHandled(String commandId, NotHandledException exception) {
		// do nothing
	}

}
