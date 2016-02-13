/**
 * 
 */
package org.scienty.servlets.fileupload;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scienty.file.patch.AssemblyLog;
import org.scienty.file.patch.ContentRange;
import org.scienty.file.patch.TrackingFileAssembler;
import org.scienty.web.http.utils.HttpUtils;

/**
 * This is a consumer of multipart request. 
 * fileMgrMap should have FileUploadManager with the right key already initialized
 * 
 * Thread to consume multipart data, we still consume a thread for slow input streams
 * Can we use wait and notify on AsynReadListener
 * @author prakash
 *
 */
public class MultipartUploadRequestConsumer implements Runnable {
	private HttpServletRequest request;
	private HttpServletResponse response;
	private final static Logger logger = LogManager.getLogger();
	private Map<String, FileUploadContext> fileMgrMap;
	/**
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * 
	 */
	@SuppressWarnings("unchecked")
	public MultipartUploadRequestConsumer(HttpServletRequest request, HttpServletResponse response) throws FileNotFoundException, IOException {
		this.request = request;
		this.response = response;
		
		HttpSession session = request.getSession();
		if ( session == null ) {
			throw new IllegalStateException("Session not established");
		}

		fileMgrMap = (Map<String, FileUploadContext>)request.getServletContext().getAttribute(FileUploadContext.FILE_UPLOAD_CONTEXT_MAP);

	}

	@Override
	public void run() {
		try {
			process();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void process() throws IOException {
		logger.debug("Thread: " + Thread.currentThread().getName());
		int returnStatus = HttpStatus.SC_OK;
		String returnMessage = "";
		String contentType = request.getContentType();
		if ( HttpUtils.isMultipartContent(contentType) == false) {
			logger.error("Not a multipart request ");
			return;
		}

		byte[] boundry = HttpUtils.getBoundary(contentType);

		MultipartStream mStream = new MultipartStream(request.getInputStream(), boundry, 2048, null);

		boolean nextPart;
		try {
			//skip the preamble
			nextPart = mStream.skipPreamble();
			while(nextPart) {
				Map<String, String> headerLines = HttpUtils.parseHeadersLines(mStream.readHeaders());
				String fileName = HttpUtils.getFileName(headerLines);
				String etag = headerLines.get(HttpUtils.HEADER_ETAG);
				logger.debug("fileName: " + fileName);
				logger.debug("etag: " + etag);

				if ( fileName == null ) {
					logger.trace("This is not multipart data");
				} else {
					fileName.split("[.]");
					if ( etag == null ) etag = fileName;

					try {

						FileUploadContext upMgr = fileMgrMap.get(etag);

						TrackingFileAssembler assembler = upMgr.getAssembler();
						if ( assembler == null )  {
							throw new IllegalStateException("Request not in valid state");
						}

						ContentRange contentRange = HttpUtils.getContentRange(headerLines.get(HttpUtils.HEADER_CONTENT_RANGE));
						AssemblyLog asmLog = assembler.assemblyLog();
						if ( contentRange == null ) iaeRange();
						if ( contentRange.getSize().longValue() !=  asmLog.header().span().high + 1) iaeRange();
						if (  contentRange.getSpan().low < asmLog.header().span().low) iaeRange();
						if (  contentRange.getSpan().high > asmLog.header().span().high) iaeRange();

						OutputStream output = assembler.getOutputStream(contentRange.getSpan().low, contentRange.getSpan().high);
						mStream.readBodyData(output);
						output.close();
						
						if ( upMgr.checkAndClose() ) {
							fileMgrMap.remove(etag);
						}
					} catch (IllegalArgumentException ex) {
						logger.error(ex.getMessage(), ex);
						returnStatus = HttpStatus.SC_BAD_REQUEST;
						returnMessage = ex.getMessage();
						//allow other parts to continue, not to waste bandwidth
						mStream.readBodyData(new NullOutputStream());
					}
				}

				nextPart = mStream.readBoundary();
				logger.trace("has next boundary " + nextPart);
			}
		} catch (IOException ioe) {
			logger.error("Failed to process stream ", ioe);
			response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR, ioe.getMessage());
			throw ioe;
		}

		response.setStatus(returnStatus, returnMessage);
	}

	private boolean iaeRange() {
		throw new IllegalArgumentException("Invalid " + HttpUtils.HEADER_CONTENT_RANGE);
	}
}
