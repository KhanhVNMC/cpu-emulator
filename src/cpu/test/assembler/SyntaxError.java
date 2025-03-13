package cpu.test.assembler;

public class SyntaxError extends Exception {
	public static final int eENTIRE_LINE = -1;
	public static final int eOPCODE      = 0;
	public static final int eFIRST_OPR   = 1;
	public static final int eSECND_OPR   = 2;
	
	private static final long serialVersionUID = -2651554753121392968L;
	public int errorLevel = eENTIRE_LINE;
	
	public SyntaxError(String reason) {
		this(reason, eENTIRE_LINE);
	}
	
	public SyntaxError(String reason, int errorLevel) {
		super(reason);
		this.errorLevel = errorLevel;
	}
}
