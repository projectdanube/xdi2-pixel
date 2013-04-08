package xdi2.pixel;

import java.util.ArrayList;

public class PixelParserException extends Exception {

	private static final long serialVersionUID = -8657342463311806980L;

	public PixelParserException() {

		super();
	}

	public PixelParserException(String message, Throwable cause) {

		super(message, cause);
	}

	public PixelParserException(String message) {

		super(message);
	}

	public PixelParserException(Throwable cause) {

		super(cause);
	}

	static PixelParserException fromParseErrors(ArrayList<?> parse_errors) {

		StringBuffer buffer = new StringBuffer();
		buffer.append("Parser error: ");

		for (Object parse_error : parse_errors) {

			buffer.append(parse_error.toString());
			buffer.append(". ");
		}

		return new PixelParserException(buffer.toString());
	}
}
