package jedi;

/**
 * User: zhaoyao
 * Date: 11-12-31
 */
public class JediException extends RuntimeException {

	public JediException(String message) {
		super(message);    //To change body of overridden methods use File | Settings | File Templates.
	}

	public JediException(String message, Throwable cause) {
		super(message, cause);    //To change body of overridden methods use File | Settings | File Templates.
	}

	public JediException(Throwable cause) {
		super(cause);    //To change body of overridden methods use File | Settings | File Templates.
	}
}
