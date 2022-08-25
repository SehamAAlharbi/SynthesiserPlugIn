package synthesiserplugin.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class RecursiveCallMarker {

	public  IMarker createMarker(IResource resource, int lineNumber) {

		IMarker marker = null;
		try {
			marker = resource.createMarker("org.eclipse.core.resources.problemmarker");
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
			marker.setAttribute(IMarker.MESSAGE, "Code Synthesis Cannot Be Completed. Method Declaration Contains Recursive Call.");
	        marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
	        marker.setAttribute(IMarker.SOURCE_ID, "RecursiveCallMarker");

		} catch (CoreException e) {
			// You need to handle the cases where attribute value is rejected
		}
		return marker;
	}
	
	
	public IMarker[] findMarkers(IResource target) {
		String type = "org.eclipse.core.resources.problemmarker";
		IMarker[] markers = null;
		
//		file.findMarkers(MARKER_ID, true, IResource.DEPTH_INFINITE);
		try {
			markers = target.findMarkers(type, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return markers;
	}
	
	public void deleteMarker(IMarker marker) {
		try {
			marker.delete();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}
