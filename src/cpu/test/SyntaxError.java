package cpu.test;

public class SyntaxError extends Exception {
	private static final long serialVersionUID = -2651554753121392968L;
	
	public SyntaxError(String reason) {
		super(reason);
	}

}
