package org.scienty.servlets.fileupload;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class FileUploadServletContextListener implements ServletContextListener {

	public FileUploadServletContextListener() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void contextDestroyed(ServletContextEvent evt) {
		//TODO: user weakmap and track the distructor to close();
		Map<String, FileUploadContext> upCtxMap = (Map<String, FileUploadContext>)evt.getServletContext().getAttribute(FileUploadContext.FILE_UPLOAD_CONTEXT_MAP);
		for (FileUploadContext upCtx : upCtxMap.values() ) {
			if (upCtx.getAssembler() != null && upCtx.getAssembler() instanceof Closeable) {
				try {
					((Closeable) upCtx.getAssembler()).close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent evt) {
		Map<String, FileUploadContext> uploadCtxMap = new WeakHashMap<String, FileUploadContext>();
		evt.getServletContext().setAttribute(FileUploadContext.FILE_UPLOAD_CONTEXT_MAP, uploadCtxMap);
	}

}
