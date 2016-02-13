package org.scienty.servlets.fileupload;

//IOStream
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
//Servlet3.0 specific annotations
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpStatus;
import org.scienty.file.patch.AssemblyLog;
import org.scienty.file.patch.ContentRange;
import org.scienty.file.patch.Range;
import org.scienty.file.patch.TrackingFileAssembler;
import org.scienty.web.http.utils.HttpUtils;
import org.scienty.web.http.utils.UploadUtils;

@WebServlet(asyncSupported = true, description = "FileUploadServlet Description", urlPatterns = { "/MultipartFileUploadServlet" })
public class FileUploadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public FileUploadServlet() {
		super();
		System.out.println("FileUploadServlet Instantiated.");
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String orderId = request.getHeader(UploadUtils.HEADER_TICKET);
		if ( orderId == null  || orderId.length() <= 0 || FileUploadContext.isValidOrderId(orderId) == false)  {
			System.out.println("invalid headers");
			response.sendError(400, "Invalid headers");
			return;
		}
		
		File orderFolder = new File(FileUploadContext.orderPath(orderId));
		if (orderFolder.exists() && orderFolder.isDirectory()) {
			
			File [] allFiles = orderFolder.listFiles();
			Pattern exPattern = Pattern.compile(".+\\.(binlog|part)$");
			for (File diskFile : allFiles) {
				String diskFileName = diskFile.getName();
				if (exPattern.matcher(diskFileName).matches() == false) {
					response.getWriter().println(diskFileName);
				}
			}
		}
		
		
	}

	@Override
	public void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String fileName = request.getHeader("filename"); //TODO: externalize to httputils
		String etag = request.getHeader(HttpUtils.HEADER_ETAG);
		if ( etag == null || etag.length() <= 0 ) etag = fileName;
		fileName = HttpUtils.secureFileName(fileName);
		String orderId = request.getHeader(UploadUtils.HEADER_TICKET);
		String contentRangeStr = request.getHeader(HttpUtils.HEADER_CONTENT_RANGE);
		ContentRange contentRange = null;
		try{
			/** validate outside **/
			if (fileName == null || fileName.length() <= 0 || FileUploadContext.isValidFileName(fileName) == false ||
					orderId == null || orderId.length() <= 0 || FileUploadContext.isValidOrderId(orderId) == false) {
				System.out.println("invalid headers");
				response.sendError(HttpStatus.SC_BAD_REQUEST, "Invalid headers");
				return;
			}

			contentRange = HttpUtils.getContentRange(contentRangeStr);
			
			//check if order is for this user and is in valid state etc. upload file if state is not in session
			File finalFile = new File(FileUploadContext.finalPath(orderId, fileName));
			File partFile = new File(TrackingFileAssembler.toPartFile(finalFile.getPath()));
			File logFile = new File(TrackingFileAssembler.toLogFile(finalFile.getPath()));

			response.setContentLength(0);
			response.setHeader(HttpUtils.HEADER_ETAG, etag);
			response.setHeader(UploadUtils.HEADER_TICKET, orderId);//new request 0-0 is new, 0-fileSize-1 is complete
			response.setHeader("filename", fileName);
			
			if (finalFile.exists() && finalFile.isFile()) {
				System.out.println("File already uploaded");
				response.sendError(HttpStatus.SC_NOT_MODIFIED, "File already exist ");
				return;
			}
					
			if ( logFile.exists() && logFile.isFile() ) {
				//in progress
				AssemblyLog assemblyLog = FileUploadContext.newAssemblyLog(logFile);
				Range targetRange = assemblyLog.header().span();
				String targetTag = assemblyLog.header().tag();
				//response.setHeader(HttpUtils.HEADER_X_CONTENT_LENGTH, targetRange.high+1+"");
				response.setHeader(HttpUtils.HEADER_RANGE, targetRange.toString());
				response.setHeader("etag", targetTag);
				if (targetRange.low != contentRange.getSpan().low ||
						targetRange.high != contentRange.getSpan().high || 
						targetTag.equals(etag) == false) {
					System.out.println("Invalid Range/Tag");
					response.sendError(403, "Range/Tag Conflicts");
				}
			} else {
				//assemblyLog.initFile(0L, targetLimit, etag); //should never throw SignatureException as this is the first time
				response.setHeader(HttpUtils.HEADER_RANGE,contentRange.getSpan().toString());
				response.setHeader("etag", etag);
			}

			if ( request.getHeader(UploadUtils.HEADER_RESUME_UPLOAD) != null) {
				ServletContext scontext = request.getServletContext();
				Map<String, FileUploadContext> fileMgrMap = (Map<String, FileUploadContext>)scontext.getAttribute(FileUploadContext.FILE_UPLOAD_CONTEXT_MAP);
				FileUploadContext upMgr = fileMgrMap.get(finalFile);
				if ( upMgr == null ) {
					upMgr = new FileUploadContext(finalFile);
					upMgr.getAssembler().init(contentRange.getSpan().high + 1, etag);
					fileMgrMap.put(etag, upMgr);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			response.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("In Post");
		//TODO remove true
		HttpSession session = request.getSession();
		if ( session == null ) {
			response.setStatus(HttpStatus.SC_UNAUTHORIZED);
			return;
		}

		String contentType = request.getContentType();
		if ( HttpUtils.isMultipartContent(contentType) ) {
			MultipartUploadRequestConsumer consumer = new MultipartUploadRequestConsumer(request, response);
			consumer.process();
		} else {
			System.out.println("Not a multipart request");
		}
	}

}
