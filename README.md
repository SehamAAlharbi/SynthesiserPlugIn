# Linear Code Synthesiser Plug-in
Generates linear API code examples from .java files

# Example
Let's say we have the following class that has a method annotated with ` @Docummention ` which contains invocations of other methods annotated with `@Utility` :

	public class JFrameExample extends Printer{

		@Documentation
		public void docFrameWithoutTitle() {
			JFrame frame = createJFrame(false);
			show(frame);
			printInfo();
		}
	}


Then calling the following code will generate the linear version of the above code:

```
transformer.inlineDocMethod("docFrameWithoutTitle");
```
