package pl.devsite.bitbox.server;

/**
 *
 * @author dmn
 */
public class Operation {

	private Type type = Type.UNKNOWN;
	private String argument;

	public static enum Type {

		UNKNOWN,
		ERROR,
	}

	public Operation(String u) {
		parse(u);
	}

	public Operation() {
	}

	public Type getType() {
		return type;
	}

	public String getArgument() {
		return argument;
	}

	protected void parse(String u) {
		type = null;
		argument = null;
		if (u == null) {
			return;
		}
		int colonPos = u.indexOf(':');
		String res;
		Type op = Type.UNKNOWN;
		if (colonPos < 0) {
			res = u;
		} else {
			String opS = u.substring(0, colonPos);
			res = u.substring(colonPos + 1);
			if (res.startsWith("//")) {
				res = u;
			}
			try {
				op = Type.valueOf(opS.toUpperCase());
			} catch (IllegalArgumentException e) {
				op = Type.UNKNOWN;
			}
		}

		//System.out.println("in=" + u + ",op=" + op + ",res=" + res);

		this.argument = res;
		this.type = op;
	}
}
