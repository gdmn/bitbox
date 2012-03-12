package pl.devsite.bitbox.server;

/**
 *
 * @author dmn
 */
public interface Processor<T> {
	public static final Processor LAST = new Processor<Processor>() {

		@Override
		public void initialize(RequestContext context) {
		}

		@Override
		public Processor execute() throws Exception {
			return this;
		}		
	};
	void initialize(RequestContext context);
	T execute() throws Exception;
}
