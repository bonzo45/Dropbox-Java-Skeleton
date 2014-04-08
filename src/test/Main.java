package test;

import com.dropbox.core.*;

import java.io.*;
import java.util.Locale;

public class Main {

  private static final String APP_KEY = "YOUR_KEY_HERE";
  private static final String APP_SECRET = "YOUR_SECRET_HERE";
  private static final String APP_NAME = "YOUR_APPLICATION_NAME_HERE";
  private static final String ACCESS_TOKEN = "YOUR PERSISTENT ACCESS TOKEN HERE";

  public static void main(String[] args) throws IOException, DbxException {
    // Initialise
    DbxClient client = initialiseClient();
    
    // Print name and directory information
    printAccountDetails(client);
    printDirectory(client, "/", true);
    
    // Upload a file
    System.out.println(upload(client, "/home/sam/walle.wav", "/walle.wav"));
    
    // Download a file
    System.out.println(download(client, "/walle.wav", "/home/sam/walle2.wav"));
  }

  /**
   * Displays the account name.
   * @param client
   */
  private static void printAccountDetails(DbxClient client) {
    try {
      System.out.println("Account: " + client.getAccountInfo().displayName);
    } catch (DbxException e) {
      System.err.println("Error Printing Account Details: Dropbox reported an error.");
    }
  }
  
  /**
   * Displays contents of a directory.
   * @param client
   * @param directory
   */
  private static void printDirectory(DbxClient client, String directory, Boolean detailed) {
    // Try to read directory
    DbxEntry.WithChildren listing;
    try {
      listing = client.getMetadataWithChildren(directory);
    } catch (DbxException e) {
      System.err.println("Could not read directory: " + directory);
      return;
    }
    
    // Display results
    System.out.println("Directory: " + directory);
    for (DbxEntry child : listing.children) {
        System.out.print("  " + child.name);
        if (detailed)
          System.out.println(": " + child.toString());
        else
          System.out.println("");
    }
  }
  
  /**
   * Downloads a file from Dropbox
   * @param client
   * @param source - source file path e.g. "/images/image.jpg"
   * @param dest - destination file path e.g. "/home/user/image.jpg"
   * @return
   */
  public static DbxEntry.File download(DbxClient client, String source, String dest) {
    System.out.println("Downloading " + source + " to " + dest);
    
    // Try to open destination for writing
    FileOutputStream outputStream;
    try {
      outputStream = new FileOutputStream(dest);
    } catch (FileNotFoundException e) {
      System.err.println("File not found: " + dest);
      return null;
    }
    
    // Try to download the file
    DbxEntry.File downloadedFile = null;
    try {
      downloadedFile = client.getFile(source, null, outputStream);
      System.out.println("Download Successful");
    } catch (DbxException e) {
      System.err.println("Download Error: Dropbox reported an error.");
      return null;
    } catch  (IOException e) {
      System.err.println("Download Error: Could not read file.");
      return null;
    } finally {
        try {
          outputStream.close();
        } catch (IOException e) {
          System.err.println("Warning: Could not close output stream for " + source);
        }
    }
    
    return downloadedFile;
  }
  
  /**
   * Uploads a file to Dropbox.
   * @param client
   * @param source - source file path e.g. "/home/user/image.jpg"
   * @param dest - destination file path e.g. "/images/image.jpg"
   * @return 
   */
  public static DbxEntry.File upload(DbxClient client, String source, String dest) {
    System.out.println("Uploading " + source + " to " + dest);
    
    // Attempt to open file
    File inputFile = new File(source);
    try (FileInputStream inputStream = new FileInputStream(inputFile);){
      DbxEntry.File uploadedFile = null;
      // Attempt to upload file
      uploadedFile = client.uploadFile(
          dest,
          DbxWriteMode.add(), //                          <-- Renames the file to (1) if it exists...
          //DbxWriteMode.force()                          <-- Blasts the file and starts again...
          //DbxWriteMode.update(String revisionToReplace) <-- Updates the file, produces conflicted copy if changed since last pull...
          inputFile.length(),
          inputStream
      );
      System.out.println("Upload Successful");
      return uploadedFile;
    
    } catch (FileNotFoundException e) {
      System.err.println("File not found: " + source);
    } catch (SecurityException e) {
      System.err.println("Permission denied: " + source);
    } catch (DbxException e) {
      System.err.println("Upload Error: Dropbox returned an error.");
    } catch (IOException e) {
      System.err.println("Upload Error: Could not read file.");
    }
    
    return null;
  }

  /**
   * Sets up the Dropbox client.
   * @return Dropbox client. Returns null if it could not be created.
   */
  private static DbxClient initialiseClient() {
    //Set Up
    DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);
    DbxRequestConfig config = new DbxRequestConfig(
        APP_NAME,
        Locale.getDefault().toString()
    );

    // Authorise
    Boolean firstTimeActivation = false;
    String accessToken;
    if (firstTimeActivation) { 
      accessToken = authorise(appInfo, config);
    }
    else {
      accessToken = ACCESS_TOKEN;
   }
    
    // If valid then create the client
    if (accessToken == null) {
      return null;
    }
    else {
      return new DbxClient(config, accessToken);  
    }
  }

  /**
   * Generates an access token for an application.
   * @param appInfo
   * @param config
   * @return Generated access token. Returns null if one could not be generated.
   */
  private static String authorise(DbxAppInfo appInfo, DbxRequestConfig config) {
    // Create authoriser
    DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
    
    // Generate authorisation URL
    String authorizeUrl = webAuth.start();
    System.out.println("Go to: " + authorizeUrl);
    
    // Read in response
    String authorisationCode;
    try {
      authorisationCode = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
    } catch (IOException e) {
      System.err.println("Authorisation Failed: Could not read authorisation code from input.");
      return null;
    }
    
    // Generate persistent access token
    DbxAuthFinish authFinish;
    try {
      authFinish = webAuth.finish(authorisationCode);
    } catch (DbxException e) {
      System.err.println("Authorisation Failure: Dropbox reported an error.");
      return null;
    }
    
    return authFinish.accessToken;
  }
}
