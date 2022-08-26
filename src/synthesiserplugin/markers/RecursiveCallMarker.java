package synthesiserplugin.markers;

import java.util.ArrayList;

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
	
	/**
	 * only returns the marker that were created using RecursiveCallMarker.createMarker()
	 * @param target the IFile 
	 * @return
	 */
	public ArrayList<IMarker> findMarkers(IResource target) {
		String type = "org.eclipse.core.resources.problemmarker";
		IMarker[] allMarkers = null;
		ArrayList<IMarker> markers = new ArrayList<IMarker>();
		try {
			allMarkers = target.findMarkers(type, false, IResource.DEPTH_ZERO);
			// to return an ArrayList of markers
			for (IMarker marker : allMarkers) {
				if (marker.getType().equalsIgnoreCase("org.eclipse.core.resources.problemmarker")) {
					markers.add(marker);
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return markers;
	}
	
	/**
	 * deletes a particular marker to clean the ICU from previous markers
	 * @param marker
	 */
	public void deleteMarker(IMarker marker) {
		try {
			marker.delete();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}
