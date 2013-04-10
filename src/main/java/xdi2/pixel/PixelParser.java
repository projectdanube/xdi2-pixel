package xdi2.pixel;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

public class PixelParser {

	private PixelParser() { }

	public static PixelPolicy pixel(String pixel) throws PixelParserException {

		org.antlr.runtime.ANTLRStringStream input = new org.antlr.runtime.ANTLRStringStream(pixel);
		com.kynetx.PersonalChannelPolicyLexer lexer = new com.kynetx.PersonalChannelPolicyLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		com.kynetx.PersonalChannelPolicyParser parser = new com.kynetx.PersonalChannelPolicyParser(tokens);

		PrintStream out = System.out;
		System.setOut(new PrintStream(new ByteArrayOutputStream()));

		try {

			parser.policy();
		} catch (RecognitionException ex) {

			throw new PixelParserException("Cannot parse policy: " + ex.getMessage(), ex);
		} finally {

			System.setOut(out);
		}

		if (parser.parse_errors.size() > 0) throw PixelParserException.fromParseErrors(parser.parse_errors);

		return PixelPolicy.fromMap(parser.policy);
	}
}
