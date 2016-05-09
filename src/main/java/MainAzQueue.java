
import java.util.ArrayList;
import java.util.List;

import com.microsoft.windowsazure.exception.ServiceException;


public class MainAzQueue {
	private static int threadErrorCount = 0;
	private static AzClient client = new AzClient();

	private static void log(String msg) {
		System.out.println(msg);
	}
	
	private static String getTestJson() {
		return "{\"txt\":\"ThiS Is a TeST\",\"txt\":\"ThiS Is a TeST\",\"txt\":\"ThiS Is a TeST\"}";
	}

	public static void main(String[] args) {
		int nbMsg = 20;
		String eNbMsg = System.getenv("AC_NBMSG");
		if (eNbMsg != null) {
			nbMsg = Integer.parseInt(eNbMsg);
		}
		Thread.currentThread().setName(MainAzQueue.class.getSimpleName());
		log(String.format("main AzQueue [%d messages]", nbMsg));
		try  {
			test_client_sequence(nbMsg);
			//
			test_client_parallel(nbMsg);
		} catch (Throwable t) {
			String errMsg = String.format("%s | Errror: %s", Thread.currentThread().getName(), t.getMessage());
			log(errMsg);
		} finally {
	        client.shutdown();			
		}
	}



	private static void test_client_sequence(int nbMsg) throws InterruptedException, ServiceException {
		log("#### test_client_sequence");
    	threadErrorCount = 0;
    	
        // GIVEN
			final String sendMsgTxt = getTestJson();
	        log(String.format("msg size: %sb", sendMsgTxt.length()));
	        String qMsg = null; 
        	while ((qMsg = client.pollQueue()) != null) {
            	log(String.format("remove old msg : %s", qMsg));
	        };
	        // WHEN
	        List<Thread> threads = new ArrayList<Thread>();
	        for (int i=0;i<nbMsg;i++) {
	        	Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
	        	    public void uncaughtException(Thread th, Throwable ex) {
	        	    	threadErrorCount++;
	        	        System.out.println("Uncaught exception: " + ex);
	        	    }
	        	};
	        	Thread t = new Thread(String.format("Tr%d", i)) {
	        		  public void run() {
	      	        	try {
	      	        		client.sendMsg(sendMsgTxt);
		    	        	Thread.sleep(100);
		    		        String bMsg = client.pollQueue();
		    		        //THEN
		    		        String assertMsg = String.format("%s | timeout ! need to receive sent message", Thread.currentThread().getName());
		    		        if (bMsg == null) {
		    		        	throw new RuntimeException(assertMsg);
		    		        }
						} catch (Throwable e) {
							String errMsg = String.format("%s | Errror: %s", Thread.currentThread().getName(), e.getMessage());
							log(errMsg);
							throw new RuntimeException(errMsg);
						}

	        		  }
	        		 };
        		 threads.add(t);
        		 t.setUncaughtExceptionHandler(h);
	        	 t.start();
	        	 t.join();
	        }
	        /*
	        for (int j = 0 ; j < threads.size(); j++) {
	        	threads.get(j).join();
	        }
	        */

		log(String.format("ERROR COUNT=%d", threadErrorCount));
	}
	
	

	private static void test_client_parallel(int nbMsg) throws ServiceException, InterruptedException {
		log("#### test_client_parallel");
    	threadErrorCount = 0;
    	
        // GIVEN
		final String sendMsgTxt = getTestJson();
		 log(String.format("msg size: %sb", sendMsgTxt.length()));
        String qMsg = null; 
		while ((qMsg = client.pollQueue()) != null) {
			log(String.format("remove old msg : %s", qMsg));
        };
        // WHEN
        List<Thread> threads = new ArrayList<Thread>();
        for (int i=0;i<nbMsg;i++) {
        	Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
        	    public void uncaughtException(Thread th, Throwable ex) {
        	    	threadErrorCount++;
        	        System.out.println("Uncaught exception: " + ex);
        	    }
        	};
        	Thread t = new Thread(String.format("Tr%d", i)) {
        		  public void run() {
      	        	try {
      	        		client.sendMsg(sendMsgTxt);
	    	        	Thread.sleep(100);
	    		        String bMsg = client.pollQueue();
	    		        //THEN
	    		        String assertMsg = String.format("%s | timeout ! need to receive sent message", Thread.currentThread().getName());
	    		        if (bMsg == null) {
	    		        	throw new RuntimeException(assertMsg);
	    		        }
					} catch (Throwable e) {
						String errMsg = String.format("%s | Errror: %s", Thread.currentThread().getName(), e.getMessage());
						log(errMsg);
						throw new RuntimeException(errMsg);
					}

        		  }
        		 };
    		 threads.add(t);
    		 t.setUncaughtExceptionHandler(h);
        	 t.start();
        }
        for (int j = 0 ; j < threads.size(); j++) {
        	threads.get(j).join();
        }
		log(String.format("ERROR COUNT=%d", threadErrorCount));
	}
}
