import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;

import com.sun.mail.imap.protocol.MessageSet;


public class ThreadDownloader {
	public static void main(String[] args) {
		Properties props = System.getProperties();
		props.setProperty("mail.store.protocol", Creds.protocol);
		try {
		  Session session = Session.getDefaultInstance(props, null);
		  Store store = session.getStore(Creds.protocol);
		  store.connect(Creds.server, Creds.user, Creds.password);
		  
		  Folder threads = store.getFolder("official threads");
		  
		  
		  Set<String>subjects = new HashSet<String>();
		  threads.open(Folder.READ_ONLY);

		  
		  System.out.println("Message count: " + threads.getMessageCount());
		  
		  /*
		  System.out.println("Fetching messages...");
		   Message[] messages = threads.getMessages();
		  FetchProfile fp = new FetchProfile();
		  fp.add(FetchProfile.Item.ENVELOPE);
		  
		  //fp.add("X-mailer");
		  threads.fetch(messages, fp);

		  System.out.println("Done fetching.");
		  */
		  
		  Message messages[] = threads.getMessages();
		  
		  File dir = new File(Creds.directory);
		  int count = messages.length;
		  for (int i = Creds.offset; i < count; i++) {
			  Message msg = messages[i];

			  File msgFile = new File(dir, i + ".txt");
			  try {
				  System.out.println("Fetching " + i + " of " + count);
				  OutputStream outstream = new FileOutputStream(msgFile);
				  msg.writeTo(outstream);
				  outstream.close();
			  } catch (Exception e) {
				  e.printStackTrace();
			  }
			  
		  }
		  
		  
		  
		  for (String subject : subjects) {
			  System.out.println(subject);
		  }
		  
		} catch (NoSuchProviderException e) {
		  e.printStackTrace();
		  System.exit(1);
		} catch (MessagingException e) {
		  e.printStackTrace();
		  System.exit(2);
		}
	}
}
