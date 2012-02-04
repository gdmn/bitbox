package pl.devsite.bigbitbox.system;

/**
 *
 * @author dmn
 */
public interface SystemProcessCallback<T> {
	void results(T value);
}
