package priv.season.coworker.test;

import java.io.File;
import priv.season.coworker.CoWorkerCenter;
import priv.season.coworker.CoWorkerService;
import priv.season.coworker.utils.CwSecurityManager;

public class Entrance {

	public final static void main(String[] args) throws Exception {
		
		CoWorkerService service = new CoWorkerService();
		System.setSecurityManager(new CwSecurityManager(service));
		
		System.out.println(System.getProperties());
		
		new Thread(new Runnable() {

			@Override
			public void run() {

				service.waitForRequest(233);
			}

		}).start();



	}

}
