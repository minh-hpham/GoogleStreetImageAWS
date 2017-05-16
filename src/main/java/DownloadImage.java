import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.S3ResponseMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class DownloadImage {

	private final static String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/streetview?size=640x440&location=%s&heading=%s&pitch=-0.7&key=%s";
	private final static int LIMIT = 25000; // one key can download up to 25000
	// images/day

	private final static String[] API_KEYS = new String[] { "EMAIL ME TO HAVE ALL THE KEYS" };

	private final static String file_addr = "/home/minhp/Downloads";

	private final static String METADATA_REQUEST = "https://maps.googleapis.com/maps/api/streetview/metadata?size=640x440&location=%s&heading=%s&pitch=-0.7&key=%s";
	private final static HttpClient client = HttpClientBuilder.create().build();

	public static void main(String[] args) throws ParseException, IOException {

		File[] files = listOfFilesType(file_addr, ".txt"); // replace with
															// something
		// similar to
		// C://mydirectory
		FileOutputStream fos = null;
		ZipOutputStream zos = null;
		String zipFile = null;

		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		String loc = null;
		int countURLrequest = 0;
		int countLine = 0;
		LocalDateTime timeFirstKeyReEnable = LocalDateTime.now();

		for (File textFile : files) {
			System.out.println("Current file: " + textFile.getName());

			// Open input stream for reading the text file MyTextFile.txt
			InputStream is = new FileInputStream(textFile);

			// create new input stream reader
			InputStreamReader instrm = new InputStreamReader(is);

			// Create the object of BufferedReader object
			br = new BufferedReader(instrm);

			String currentLine;
			int key_index = 0;
			// Read the name of columns
			br.readLine();

			// Read one line at a time
			while ((currentLine = br.readLine()) != null) {

				if (key_index == 0) {
					timeFirstKeyReEnable = LocalDateTime.now().plusDays(1);// 24
																			// //
																			// re-enable
				} else if (countURLrequest >= LIMIT && key_index >= API_KEYS.length - 1) {
					int diffInMilli = (int) java.time.Duration.between(LocalDateTime.now(), timeFirstKeyReEnable)
							.toMillis();
					// wait until first key is valid
					if (diffInMilli > 0) {
						try {
							Thread.sleep(diffInMilli);
						} catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
						}
					}
					key_index = 0;
					timeFirstKeyReEnable = LocalDateTime.now().plusDays(1);
					countURLrequest = 0;
				}
				// Change key if necessary
				else if (countURLrequest >= LIMIT && key_index < API_KEYS.length - 1) {
					countURLrequest = 0;
					key_index++;
				}

				// Get location
				String[] content = currentLine.split(",");
				sb.append(content[2]);
				sb.append(",");
				sb.append(content[3]);
				loc = sb.toString();
				sb.delete(0, sb.length());
				countLine++;

				File f = new File(loc + "a.jpg");
				if (f.exists() == false) {
					InputStream inputStream = null;
					String json = "";

					try {
						HttpGet get = new HttpGet(String.format(METADATA_REQUEST, loc, 0, API_KEYS[key_index]));
						HttpResponse response = client.execute(get);
						HttpEntity entity = response.getEntity();
						inputStream = entity.getContent();
					} catch (Exception e) {
					}

					try {
						BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"), 8);
						StringBuilder sbuild = new StringBuilder();
						String line = null;
						while ((line = reader.readLine()) != null) {
							sbuild.append(line);
						}
						inputStream.close();
						json = sbuild.toString();
					} catch (Exception e) {
					}

					// now parse
					JSONParser parser = new JSONParser();
					Object obj = parser.parse(json);
					JSONObject jb = (JSONObject) obj;
					// check if image at this location is available
					if (jb.get("status").equals("OK")) {
						// Download images
						saveImage(loc, 0, API_KEYS[key_index], loc + "a.jpg");
						saveImage(loc, 90, API_KEYS[key_index], loc + "b.jpg");
						saveImage(loc, 180, API_KEYS[key_index], loc + "c.jpg");
						saveImage(loc, 270, API_KEYS[key_index], loc + "d.jpg");
						countURLrequest += 4;
					}

				}
			}
			br.close();
			// add files to zip
			zipFile = nameZipFile(textFile);
			try {
				fos = new FileOutputStream(zipFile);
				zos = new ZipOutputStream(fos);

				for (File file : listOfFilesType(System.getProperty("user.dir"), ".jpg")) {

					String name = file.getName();

					ZipEntry entry = new ZipEntry(name);
					zos.putNextEntry(entry);

					FileInputStream fis = null;
					try {
						fis = new FileInputStream(file);
						byte[] byteBuffer = new byte[1024];
						int bytesRead = -1;
						while ((bytesRead = fis.read(byteBuffer)) != -1) {
							zos.write(byteBuffer, 0, bytesRead);
						}
						zos.flush();
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("Image at " + name);
					} finally {
						try {
							fis.close();
						} catch (Exception e) {
						}
					}
					zos.closeEntry();

					zos.flush();
				}
			} finally {
				try {
					zos.close();
					fos.close();
				} catch (Exception e) {

				}
			}
			// Add zip to AWS S3
			// Bucket's name: googlestreetimages
			// Upload type: multiple upload for each zip file
			// Delete zip file after done
			// EMAIL ME TO GET IAM ID'S PERMISSION 
			try {
				uploadToAWS(zipFile);
				new File(zipFile).delete();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// DELETE ALL IMAGES TO SAVE DISK SPACE
			for (File file : listOfFilesType(System.getProperty("user.dir"), ".jpg")) {
				file.delete();
			}

		}
		System.out.println("DOWNLOADING COMPLETE");
		System.exit(0);
	}

	private static void saveImage(String location, int heading, String API_key, String filename) {
		try {
			// Download image
			String loc_url = URIUtil.encodeQuery(String.format(PLACES_API_BASE, location, heading, API_key));
			URL url = new URL(loc_url);
			BufferedImage image = ImageIO.read(url);
			File output = new File(filename);
			ImageIO.write(image, "jpg", output);
		} catch (URIException e) {
			e.printStackTrace();
			System.out.println("Image at " + location);
			System.out.println("With key " + API_key);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.out.println("Image at " + location);
			System.out.println("With key " + API_key);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Image at " + location);
			System.out.println("With key " + API_key);
		}

	}

	private static void uploadToAWS(String zipFile) throws InterruptedException {
	String existingBucketName = "googlestreetimages";
        String keyName            =  zipFile;
        File file = new File(zipFile);
       
        AmazonS3Client s3Client =  (AmazonS3Client) AmazonS3ClientBuilder.standard().withRegion("us-west-2").withCredentials(new ProfileCredentialsProvider()).build();
       
        TransferManager tm = TransferManagerBuilder.standard().withS3Client(s3Client).build();
        
        // TransferManager processes all transfers asynchronously, 
        // so this call will return immediately.
        Upload upload = tm.upload(existingBucketName, keyName, file);      

        try { 
        	// Or you can block and wait for the upload to finish
        	upload.waitForCompletion();
        	System.out.println("Upload complete.");
        } catch (AmazonClientException amazonClientException) {
        	System.out.println("Unable to upload file, upload was aborted.");
        	amazonClientException.printStackTrace();
        }
		
	}

	private static String nameZipFile(File textFile) {
		String filename = textFile.getName();
		int pos = filename.lastIndexOf(".");

		StringBuilder sb = new StringBuilder();
		if (pos > 0) {
			sb.append(filename.substring(0, pos));
			sb.append(".zip");
		} else {
			sb.append(filename);
		}
		return sb.toString();
	}

	private static File[] listOfFilesType(String DIRECTORY, String fileFormat) {
		File f = new File(DIRECTORY);

		FilenameFilter textFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(fileFormat);// e.g ".txt"
			}
		};

		File[] files = f.listFiles(textFilter);
		return files;
	}

}
