package nuriSolver;
/**
 * Exception raised when the state of cells in this puzzle lead to a
 * contradiction.
 */
public class ContradictionException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8081284492587462782L;

	public ContradictionException() {
		super();
	}

	public ContradictionException(String s) {
		super(s);
	}
}