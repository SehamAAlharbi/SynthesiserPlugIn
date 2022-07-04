package synthesiserplugin.deadcode.to.test;

import javax.swing.JFrame;

public class JFrameExample {

	public void docFrameWithoutTitle() {
		JFrame frame1 = new JFrame();
		if (false) {
			frame1.setTitle("App");
		}
		JFrame frame = frame1;
		show(frame);

	}

	public void docFrameWithTitle() {
		JFrame frame = createJFrame(true);
		show(frame);

	}

	public static JFrame createJFrame(boolean withTitle) {
		JFrame frame = new JFrame();
		if (withTitle) {
			frame.setTitle("App");
		}
		return frame;
		
	}

	public void show(JFrame frame) {
		frame.setBounds(100, 100, 200, 200);
		frame.setVisible(true);
	}

}