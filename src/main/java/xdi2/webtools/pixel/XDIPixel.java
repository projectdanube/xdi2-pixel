package xdi2.webtools.pixel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.core.Graph;
import xdi2.core.features.linkcontracts.LinkContract;
import xdi2.core.impl.memory.MemoryGraphFactory;
import xdi2.core.io.XDIWriter;
import xdi2.core.io.XDIWriterRegistry;
import xdi2.core.io.writers.XDIDisplayWriter;
import xdi2.pixel.PixelParser;
import xdi2.pixel.PixelPolicy;

/**
 * Servlet implementation class for Servlet: XDIPixel
 *
 */
public class XDIPixel extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

	private static final long serialVersionUID = 3749646618800425812L;

	private static Logger log = LoggerFactory.getLogger(XDIPixel.class);

	private static MemoryGraphFactory graphFactory;
	private static List<String> sampleInputs;

	static {

		graphFactory = MemoryGraphFactory.getInstance();
		graphFactory.setSortmode(MemoryGraphFactory.SORTMODE_ORDER);

		sampleInputs = new ArrayList<String> ();

		while (true) {

			InputStream inputStream = XDIPixel.class.getResourceAsStream("graph" + (sampleInputs.size() + 1) + ".xdi");
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			int i;

			try {

				while ((i = inputStream.read()) != -1) outputStream.write(i);
				sampleInputs.add(new String(outputStream.toByteArray()));
			} catch (Exception ex) {

				break;
			} finally {

				try {

					inputStream.close();
					outputStream.close();
				} catch (Exception ex) {

				}
			}
		}
	}

	public XDIPixel() {

		super();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String sample = request.getParameter("sample");
		if (sample == null) sample = "1";

		request.setAttribute("resultFormat", XDIDisplayWriter.FORMAT_NAME);
		request.setAttribute("writeImplied", null);
		request.setAttribute("writeOrdered", "on");
		request.setAttribute("writeInner", "on");
		request.setAttribute("writePretty", null);
		request.setAttribute("sampleInputs", Integer.valueOf(sampleInputs.size()));
		request.setAttribute("input", sampleInputs.get(Integer.parseInt(sample) - 1));
		request.getRequestDispatcher("/XDIPixel.jsp").forward(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String resultFormat = request.getParameter("resultFormat");
		String writeImplied = request.getParameter("writeImplied");
		String writeOrdered = request.getParameter("writeOrdered");
		String writeInner = request.getParameter("writeInner");
		String writePretty = request.getParameter("writePretty");
		String input = request.getParameter("input");
		String output = "";
		String stats = "-1";
		String error = null;

		Properties xdiWriterParameters = new Properties();

		if ("on".equals(writeImplied)) xdiWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_IMPLIED, "1");
		if ("on".equals(writeOrdered)) xdiWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_ORDERED, "1");
		if ("on".equals(writeInner)) xdiWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_INNER, "1");
		if ("on".equals(writePretty)) xdiWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_PRETTY, "1");

		XDIWriter xdiResultWriter = XDIWriterRegistry.forFormat(resultFormat, xdiWriterParameters);
		Graph graph = null;

		try {

			StringWriter writer = new StringWriter();

			PixelPolicy pixelPolicy = PixelParser.pixel(input);
			LinkContract linkContract = pixelPolicy.getLinkContract();

			graph = linkContract.getContextNode().getGraph();

			xdiResultWriter.write(graph, writer);

			output = StringEscapeUtils.escapeHtml(writer.getBuffer().toString());
		} catch (Exception ex) {

			log.error(ex.getMessage(), ex);
			error = ex.getMessage();
			if (error == null) error = ex.getClass().getName();
		}

		stats = "";
		stats += Integer.toString(graph.getRootContextNode().getAllContextNodeCount()) + " context nodes. ";
		stats += Integer.toString(graph.getRootContextNode().getAllRelationCount()) + " relations. ";
		stats += Integer.toString(graph.getRootContextNode().getAllLiteralCount()) + " literals. ";
		stats += Integer.toString(graph.getRootContextNode().getAllStatementCount()) + " statements. ";

		graph.close();

		// display results

		request.setAttribute("sampleInputs", Integer.valueOf(sampleInputs.size()));
		request.setAttribute("resultFormat", resultFormat);
		request.setAttribute("writeImplied", writeImplied);
		request.setAttribute("writeOrdered", writeOrdered);
		request.setAttribute("writeInner", writeInner);
		request.setAttribute("writePretty", writePretty);
		request.setAttribute("input", input);
		request.setAttribute("output", output);
		request.setAttribute("stats", stats);
		request.setAttribute("error", error);

		request.getRequestDispatcher("/XDIPixel.jsp").forward(request, response);
	}   	  	    
}
