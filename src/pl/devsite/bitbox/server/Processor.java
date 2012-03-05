package pl.devsite.bitbox.server;

/**
 *
 * @author dmn
 */
public interface Processor {
	void initialize(RequestContext context);
	void execute() throws Exception;
}
