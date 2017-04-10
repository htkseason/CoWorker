package priv.season.coworker.utils;

import java.io.FileDescriptor;
import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.net.InetAddress;
import java.security.Permission;
import java.security.SecurityPermission;
import java.util.PropertyPermission;

import priv.season.coworker.CoWorkerService;

public class CwSecurityManager extends SecurityManager {
	protected CoWorkerService service;

	public CwSecurityManager(CoWorkerService service) {
		this.service = service;
	}


	@Override
	public void checkPermission(Permission perm, Object context) {
		if (!service.isTaskThread())
			return;
		
		//super.checkPermission(perm, context);

	}

	@Override
	public void checkPermission(Permission perm) {
		if (!service.isTaskThread())
			return;

		//super.checkPermission(perm);

	}


}
