# Linear Code Synthesiser Plug-in
Generates linear API code examples from java input classes

# Example
Let's say we have a class with a method annotated with ` @Docummention ` as given below:

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