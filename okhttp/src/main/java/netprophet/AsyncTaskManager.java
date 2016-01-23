package netprophet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.google.gson.Gson;

//TODO: an asynchronous manager should be running to handle these tasks
public class AsyncTaskManager {
  //Singleton pattern
  private static AsyncTaskManager instance = null;
  public static AsyncTaskManager getInstance() {
    if(instance == null) {
      //TODO: pool size should be from configure file
      instance = new AsyncTaskManager(5);
    }
    return instance;
  }
  
  private ExecutorService executor;
  private AsyncTaskManager(int size){
    executor = Executors.newFixedThreadPool(size);
  };
  
  public void postTask(Runnable task){
	executor.execute(task);
  }
  
  public void waitForAllTask(long timeout){
	executor.shutdown();
	try {
		executor.awaitTermination(timeout, TimeUnit.MILLISECONDS);
	} catch (InterruptedException e) {
		// TODO deal with the try-catch
		e.printStackTrace();
	}
  }
  
  public class postCallInfoToServerTask implements Runnable{
    
	public postCallInfoToServerTask(){
    	
    }
	  
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	  
  }
  
}
