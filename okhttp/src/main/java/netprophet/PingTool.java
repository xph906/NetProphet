package netprophet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.internal.Internal.NetProphetLogger;

public class PingTool
{
	public class MeasureResult{
		public float avgDelay;
		public float lossRate;
		public float stdDev;
		public String ip;
		public MeasureResult(String ip, float avg, float stddev, float loss){
			this.avgDelay = avg;
			this.lossRate = loss;
			this.stdDev = stddev;
			this.ip = ip;
		}
		public MeasureResult(String ip){
			this.ip = ip;
			this.lossRate = (float) 100.0;
			this.avgDelay = (float) 1000.0;
			this.stdDev = (float) 0.0;
		}
		public String toString(){
			return "IP:"+ip+" LossRate:"+ lossRate + "% AvgDelay:"+avgDelay+" ms StdDev"+stdDev;
		}
	}
	
    List<String> commands;
    Pattern summaryPattern, dataPattern, icmpTimeoutPattern;
    
    public PingTool(){
        commands = new ArrayList<String>();
        commands.add("ping");
        commands.add("-c");
        commands.add("5");
        summaryPattern = Pattern.compile(
        		"[0-9]+ packets transmitted, [0-9]+ (packets )?received, ([0-9]+.[0-9]+)% packet loss");
        dataPattern = Pattern.compile(
        		"min/avg/max/[a-z]+dev = ([0-9]+.[0-9]+)/([0-9]+.[0-9]+)/([0-9]+.[0-9]+)/([0-9]+.[0-9]+) ms");
        icmpTimeoutPattern = Pattern.compile("Request timeout for icmp_seq");
    }
    public MeasureResult getFailedMeasureResult(String host){
    	return new MeasureResult(host, (float)1000.0, (float)0.0, (float)100.0);
    }
    
    private MeasureResult doTCPPing(String ip){
    	//NetProphetLogger.logDebugging("doTCPPing", "TCP Ping for "+ip);
    	MeasureResult result = new MeasureResult(ip);
    	long[] delays = new long[5];
    	int errorCount = 0;
		try{
			for(int i=0; i<5; i++){
				long t1 = System.currentTimeMillis();
				Socket socket = new Socket();
				try{
					socket.connect(new InetSocketAddress(ip, 80), 1000);
				}
				catch(Exception e){
					errorCount++;
				}
				long t2 = System.currentTimeMillis();
				socket.close();;
				delays[i] = t2 - t1;
				//NetProphetLogger.logDebugging("doTCPPing", "rs1: "+delays[i]);
			}
		}
		catch(Exception e){
			NetProphetLogger.logError("doTCPPing",e.toString());
			e.printStackTrace();
			errorCount = 5;
		}
		
		result.lossRate = (float) (((float)errorCount/5.0) * 100.0);
		int tmp = 0;
		for(int i=0; i<5; i++)
			tmp += delays[0];
		result.avgDelay = tmp / 5;
		return result;
	
    }

    public MeasureResult doPing(String ip){
    	MeasureResult result = new MeasureResult(ip);
    	commands.add(ip);
    	boolean doTCPPing = true;
    	
        try{
            ProcessBuilder pb = new ProcessBuilder(commands);
            Process process = pb.start();
            BufferedReader stdInput =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError =
                    new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String s;
            List<String> stdoutRS = new LinkedList<String>();    
            while ((s = stdInput.readLine()) != null)
            {
            	//NetProphetLogger.logDebugging("doPing","raw results:"+s);
            	Matcher match = summaryPattern.matcher(s);
            	if(match.find()){
            		Float f = Float.valueOf(match.group(2));
            		//NetProphetLogger.logDebugging("PingTool", "IP:"+ip+" lossrate:"+f+"%");
            		result.lossRate = f;
            		continue;
            	}
            	match = dataPattern.matcher(s);
            	if(match.find()){
            		Float avg = Float.valueOf(match.group(2));
            		Float stddev = Float.valueOf(match.group(4));
            		//NetProphetLogger.logDebugging("PingTool", "IP:"+ip+" avg:"+avg+"ms "+"stddev:"+stddev);
            		result.avgDelay = avg;
            		result.stdDev = stddev;
            		doTCPPing = false;
            		break;
            	}    	
            }
            
            /*while (!doTCPPing && (s = stdError.readLine()) != null)
            {
            	NetProphetLogger.logError("PingTool", s);
            }*/
            
        }
        catch(Exception e){
        	NetProphetLogger.logError("PingTool", e.toString());
        }
        finally{
            commands.remove(commands.size()-1);
        }
        
        if(doTCPPing){
        	return doTCPPing(ip);
        }
        
        return result;
    }
    
    private boolean parseResults(List<String> stdoutRS, List<String> stderrRS){
    	for(String s : stdoutRS){
    		NetProphetLogger.logDebugging("parseResults", s);
    	}
    	return true;
    }

}