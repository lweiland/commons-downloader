package commonsdl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import commonsdl.Download.Mode;

public final class CommonsDownloader {
	
	private static final Logger LOGGER = LogManager.getLogger(CommonsDownloader.class);
	
	private final Download download;
	
	private final CloseableHttpAsyncClient client;
	
	private final URI commons;
	
	private CountDownLatch latch; 
	
	public CommonsDownloader(final Download download) throws URISyntaxException {
		this.download = download;
		this.client = HttpAsyncClients.createDefault();
		this.commons = new URIBuilder("https://commons.wikimedia.org/w/index.php").addParameter("title", "Special:FilePath").build();
	}

	public void download() throws IOException {
		this.client.start();
		try {
			
			final int lines = (int) Files.lines(download.getFile(), download.getCharset()).count();
			//LOGGER.info("Will download {} files", lines);
			this.latch = new CountDownLatch(lines);
			Files.lines(download.getFile(), download.getCharset())
				.parallel()
				.map(s -> s.substring(0, s.lastIndexOf(',')))
				.forEach(s -> this.download(s, download.getDestination()));
			try {
				LOGGER.info("Waiting for downloads to complete");
				latch.await();
			} catch (final InterruptedException e) {
				Thread.interrupted();
			}
		} catch (final Throwable t) {
			LOGGER.error("Oups", t);
		} finally {
			LOGGER.info("All done");
			this.client.close();
		}
	}
	
	
	public void download(String file, String destination) throws IOException {
		this.client.start();
		try {
					
			final int lines = (int) Files.lines(Paths.get(file), download.getCharset()).count();
			//LOGGER.info("Will download {} files", lines);
			this.latch = new CountDownLatch(lines);
			Files.lines(Paths.get(file), download.getCharset())
				.parallel()
				.map(s -> s.substring(0, s.lastIndexOf(',')))
				.forEach(s -> this.download(s, Paths.get(destination)));
			try {
				LOGGER.info("Waiting for downloads to complete");
				latch.await();
			} catch (final InterruptedException e) {
				Thread.interrupted();
			}
		} catch (final Throwable t) {
			LOGGER.error("Oups", t);
		} finally {
			LOGGER.info("All done");
			this.client.close();
		}
	}
	
	
	private void download(final String fileName, final Path destination) {
		final StructuredDataMessage event = new StructuredDataMessage(Integer.toHexString(fileName.hashCode()) , null, "download");
		final Path destinationFile = Paths.get(destination.toString(), fileName.replaceAll("[:*?\"<>|/\\\\]", "_"));
		event.put("destinationFile", destinationFile.toAbsolutePath().toString());
		if (download.getMode() == Mode.RESTART || !destinationFile.toFile().exists()) {
			try {
				final URI uri = new URIBuilder(commons).addParameter("file", fileName).build();
				//event.put("uri", uri.toString());
				final HttpGet request = new HttpGet(uri);
				client.execute(request, new DownloaderCallback(fileName, destination, event));
			} catch (final URISyntaxException e) {
				event.put("status", "error");
				EventLogger.logEvent(event, Level.WARN);
				LOGGER.warn("Could not build URI for {}.", fileName, e);
				latch.countDown();
			}
		} else {
			event.put("status", "skipped");
			EventLogger.logEvent(event, Level.DEBUG);
			latch.countDown();
		}
	}
	
	//read folder with txt 
	//for each txt make threads (until no of threads?)
	//	make folder to save files
	

	public static void main(String[] args) throws InterruptedException {
		
		
		
		
		Download download = new Download();
		final CmdLineParser parser = new CmdLineParser(download);
		try {
			parser.parseArgument(args);
			
			boolean folderOption = download.folderSet();
	
			if(folderOption){
				Path path2Fold = download.getFolder();
				File files = new File(path2Fold.toString());
				File[] listOfFiles = files.listFiles();
				
				//overkill - I do not use an unbalanced chuck size as we have a controlled set of files to download
				//ForkJoinPool forkJoinPool = new ForkJoinPool(24);
				
				int poolSize = 15;//Runtime.getRuntime().availableProcessors();
				int jobCount = 30;
				ExecutorService pool = Executors.newFixedThreadPool(poolSize);
				System.out.println("poolSize"+poolSize);
				Thread.sleep(20000);
				//loop over each file in folder 
				
				for(int i = 0; i< listOfFiles.length;i++){
					
				
					//mkdir
					// get folder name
					String pathTxt = listOfFiles[i].toString();
					
					Path dest = download.getDestination();
					
					// add os 
					String delim = "/";
					if(dest.toString().contains("C:") || dest.toString().contains("D:")){
						delim="\\";
					}
					
					String folderName = pathTxt.substring(pathTxt.lastIndexOf(delim)+1, pathTxt.lastIndexOf(".txt"));
				
					
					
					
					String destPath = dest.toString()+"/"+folderName+"/";
					File d = new File(destPath);
					if(!d.exists()){
						d.mkdir();
					}
					
					
					
					final CommonsDownloader downloader = new CommonsDownloader(download);
					
					
					//TODO add max number of threads
					
					DownloadThread dt = new DownloadThread("thread"+i,downloader, download, 
							listOfFiles[i].toString(), destPath);
					
					dt.start();
					
					
					/*
					ForkJoinPool.commonPool().invoke(new DownloadTask(files, download));
					forkJoinPool.shutdown();
					*/
				
					
					
				}
				pool.shutdown();
				while(!pool.isTerminated());

			}else{
				
			
				final CommonsDownloader downloader = new CommonsDownloader(download);
				downloader.download();
			}
		} catch (final CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.out);
		} catch (final URISyntaxException | IOException e) {
			System.err.printf("Could not open file: %1$s.", e.getMessage());
		}
	}
	
	private final class DownloaderCallback implements FutureCallback<HttpResponse> {
		
		private final String fileName;
		
		private final Path destination;
		
		private final StructuredDataMessage event;
		
		
		public DownloaderCallback(final String fileName, final Path destination, final StructuredDataMessage event) {
			this.fileName = fileName;
			this.destination = destination;
			this.event = event;
			
		}
		
		@Override
		public void failed(final Exception e) {
			latch.countDown();
			event.put("status", "error");
			EventLogger.logEvent(event, Level.WARN);
			LOGGER.warn("Donwload of {} failed.", fileName, e);
		}

		@Override
		public void completed(final HttpResponse response) {
			final Path destinationFile = getDestinationFile();
			try (final BufferedOutputStream os = new BufferedOutputStream(Files.newOutputStream(destinationFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
				response.getEntity().writeTo(os);
			//	event.put("status", "success");
			//	EventLogger.logEvent(event, Level.INFO);
			} catch (final IOException e) {
				event.put("status", "error");
				EventLogger.logEvent(event, Level.WARN);
				LOGGER.warn("Failed to save {}.", fileName, e);
			} finally {
				latch.countDown();
			}
		}

		private Path getDestinationFile() {
			return Paths.get(destination.toString(), fileName.replaceAll("[:*?\"<>|/\\\\]", "_"));
		}

		@Override
		public void cancelled() {
			event.put("status", "cancelled");
			EventLogger.logEvent(event, Level.DEBUG);
			latch.countDown();
		}
	}
	


	
	
	
	static class DownloadThread extends Thread {
		   private Thread t;
		   private String threadName;
		   CommonsDownloader commonsDow;
		   Download download;
		   String fitxt;
		   String dest;
		   
		   DownloadThread(String name, CommonsDownloader commonsDown, Download down, 
				   String fitxt, String dest) throws URISyntaxException {
		      threadName = name;
		      System.out.println("Creating " +  threadName );
		      commonsDow = commonsDown;
		      download = down;
		      this.fitxt = fitxt;
		      this.dest = dest;
		      
		   }
		   
		   public void run() {
		      System.out.println("Running " +  threadName );
		      try {
		       
		    	 commonsDow = new CommonsDownloader(download);
		    	 commonsDow.download(fitxt, dest);
		    	  
		      }catch (Exception e) {
		         System.out.println("Thread " +  threadName + " interrupted.");
		      }
		      System.out.println("Thread " +  threadName + " exiting.");
		   }
		   
		   public void start () {
		      System.out.println("Starting " +  threadName );
		      if (t == null) {
		         t = new Thread (this, threadName);
		         t.start (); //calls the run method
		      }
		   }
		}

}
