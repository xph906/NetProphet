package netprophet;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.gson.Gson;
import static okhttp3.internal.Internal.logger;

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
  
  
  
}
