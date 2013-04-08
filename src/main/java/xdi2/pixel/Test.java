package xdi2.pixel;

public class Test {

	public static void main(String[] args) throws Exception {

		String policy = "allow cloudos:{subscribe, unsubscribe} events on channel =!345;";

		PixelPolicy pixelPolicy = PixelParser.pixel(policy);

		System.err.println(pixelPolicy.getLinkContract().getContextNode().getGraph().toString());
	}
}
