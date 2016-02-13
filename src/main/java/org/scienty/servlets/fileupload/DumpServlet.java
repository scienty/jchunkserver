package org.scienty.servlets.fileupload;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.util.Streams;

/**
 * Servlet implementation class DumpServlet
 */
@WebServlet("/DumpServlet")
public class DumpServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DumpServlet() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    @Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("===Headers===");
		Enumeration<String> headers = request.getHeaderNames();
		while ( headers.hasMoreElements() ) {
			String hName = headers.nextElement();
			System.out.println(hName +":"+request.getHeader(hName));
		}
			
		Streams.copy(request.getInputStream(), System.out, true);
		System.out.println("Dumped");
	}


}
