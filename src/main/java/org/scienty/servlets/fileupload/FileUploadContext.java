package org.scienty.servlets.fileupload;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.scienty.file.patch.AssemblyLog;
import org.scienty.file.patch.TrackingFileAssembler;
import org.scienty.web.http.utils.UploadUtils;

public class FileUploadContext {
	public static final String FILE_UPLOAD_CONTEXT_MAP = FileUploadContext.class.getName() + "_map";
	private static Pattern imageFileNamePattern = Pattern.compile(UploadUtils.IMG_FILE_PATTERN);
	private static Pattern orderIdPattern = Pattern.compile(UploadUtils.TICKET_PATTERN);
	private static String uploadFolder = "c:\\temp1";
	private File finalFile;
	private File logFile;
	private TrackingFileAssembler assembler;
	//private ReentrantLock conLock = new ReentrantLock();

	public FileUploadContext(File uploadFile) {
		this.finalFile = uploadFile;
		this.logFile = new File(TrackingFileAssembler.toLogFile(uploadFile.getPath()));
		assembler = new TrackingFileAssembler(uploadFile, logFile, false);
		
	}

	public TrackingFileAssembler getAssembler() {
		return assembler;
	}
	
	public static AssemblyLog newAssemblyLog(File logFile) throws IOException {
		AssemblyLog log = new AssemblyLog(logFile);
		log.init(true);
		return log;
	}
	
	public static boolean isValidFileName(String fileName) {
		return imageFileNamePattern.matcher(fileName.toLowerCase()).matches();
	}
	
	public static boolean isValidOrderId(String orderId) {
		return orderIdPattern.matcher(orderId).matches();
	}
	
	public static String finalPath(String orderId, String fileName) {
		return orderPath(orderId) + "\\" + fileName;
	}
	
	public static String orderPath(String orderId) {
		return uploadFolder + "\\" + orderId;
	}

	public File getFinalFile() {
		return finalFile;
	}

	public void setFinalFile(File uploadFile) {
		this.finalFile = uploadFile;
	}
	
	public boolean isUploadCompleted() throws IOException {
		return (assembler.assemblyLog().read().size() <= 0 );
	}
	
	public boolean checkAndClose() throws IOException {
		if ( isUploadCompleted() ) {
			assembler.close();
			return true;
		}
		return false;
	}
}
