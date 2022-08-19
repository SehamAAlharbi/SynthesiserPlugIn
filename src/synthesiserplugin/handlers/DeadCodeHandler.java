package synthesiserplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import synthesiserplugin.parser.Parser;

public class DeadCodeHandler extends AbstractHandler {
	

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
			System.out.println("Hi");
			
			Handler handler = new Handler();
			CompilationUnit cu = new Parser().parse(handler.getOriginal());
			System.out.println(cu.toString());
		
		return null;
	}

}
