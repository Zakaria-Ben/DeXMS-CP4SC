package org.zefxis.dexms.mediator.manager;

import java.io.Serializable;

public class MediatorOutput implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public byte[] war;
    public byte[] jar;
	public byte[] wsdl;
	public String service_port;
	public String setinvaddr_service_port;
	public String service_bc_port;
	public String bc_manager_servlet_port;
	public String response_code;
	public String response_message;
	public String generatedCodePath;
	public String service_name;
	public String printer_service_port;
	public String subcomponent_port;
	public String JKSPathBus;
	public String JKSPathService;
}
