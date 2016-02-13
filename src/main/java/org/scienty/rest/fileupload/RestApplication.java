package org.scienty.rest.fileupload;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ApplicationPath( "rest" )
public class RestApplication extends Application {
	private final static Logger logger = LogManager.getLogger();
	public RestApplication( ) {}
 
	@Override
	public Set<Class<?>> getClasses() {
		logger.info("Loading rest application");
		final Set<Class<?>> returnValue = new HashSet<Class<?>>( );
		returnValue.add( OrderServiceImpl.class );
		return returnValue;
	}
	
	
}