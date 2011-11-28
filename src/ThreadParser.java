import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class ThreadParser {
	
	// Labels
	public static final String JOSH = "Josh Gordon";
	public static final String TEDDY = "Ted Ross";
	public static final String GARRETT = "Garrett Young";
	public static final String BELLOS = "Alex Bellos";
	public static final String CRAIG = "Craig Cohen";
	public static final String LEE = "Eric Lee";
	public static final String BEN = "Ben Dodson";
	public static final String GUESTS = "Guests";
	
	public static final String[] SENDERS = {JOSH,TEDDY,GARRETT,BELLOS,CRAIG,LEE,BEN,GUESTS};
	
	public static Map<String,String>matches = new HashMap<String,String>();
	
	static {
		// Matching on email addresses would have been the obvious thing to do.
		// Oh well.
		matches.put("gordon",JOSH);
		matches.put("ross",TEDDY);
		matches.put("young",GARRETT);
		matches.put("bellos",BELLOS);
		matches.put("cohen",CRAIG);
		matches.put("eric", LEE);
		matches.put("dodson",BEN);
		
		matches.put("josh",JOSH);
		matches.put("ted",TEDDY);
		matches.put("garrett",GARRETT);
		matches.put("alex",BELLOS);
		matches.put("craig",CRAIG);
		matches.put("lee", LEE);
		matches.put("ben",BEN);
		
		// others are guests
	}
	
	public static Set<String>skipTitles = new TreeSet<String>();
	static {
		skipTitles.add("Official SF Hike Club, meeting I");
	}
	
	public static SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM d, yyyy");
	
	////////////////////////////////////////////////////
	
	
	public static void main(String[] argv) {

		Properties props = System.getProperties();
		props.setProperty("mail.store.protocol", Creds.protocol);
		try {
			Session session = Session.getDefaultInstance(props, null);
			File directory = new File(Creds.directory);

			ThreadStats stats = new ThreadStats();
			File outFile = new File(Creds.output_file);
			File outHTML = new File(Creds.output_html);
			
			OutputStream outStream = new FileOutputStream(outFile);
			PrintStream p = new PrintStream(outStream);

			OutputStream htmlOutStream = new FileOutputStream(outHTML);
			PrintStream phtml = new PrintStream(htmlOutStream);
			
			int count = Creds.max;
			Message lastMsg = null;

			boolean isContinuation = false;

			System.out.println("Parsing threads...");
			
			
			// header 
			p.print("thread,count,author,date");
			ThreadStats.writeHeaderTo(p);
			p.println("");
			
			
			// html
			phtml.println("<html>");
			phtml.println("  <head>");
			phtml.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\"/>");
			phtml.println("  </head>");
			phtml.println("  <body>");
			phtml.println("    <div class=\"container\">");
			phtml.println("    <table class=\"threads\">");
			for (int i = 1; i <= count; i++) {
				if (i % 50 == 0) {
					System.out.println(i + " of " + count);
				}
				File msgFile = new File(directory, i + ".txt");
				FileInputStream instream = new FileInputStream(msgFile);
				MimeMessage msg = new MimeMessage(session, instream);
				instream.close();
				
				if (ThreadStats.skipPost(msg)) {
					continue;
				}
				
				String subj = ThreadStats.getThreadName(msg.getSubject());
				
				// should work as long as last thread is longer than 1 message.
				// otherwise, last thread is skipped.
				isContinuation = (subj.equalsIgnoreCase(stats.subject) && i < (count));

				
				if (!isContinuation) {
					if (stats.subject.length() > 0) {
						
						// Print to file
						p.print("\"" + stats.subject.replace("\"", "\"\"")
										+ "\"");
						p.print(",");
						p.print(stats.numPosts);
						p.print(",");
						p.print("\"" + lastMsg.getSentDate() + "\"");
						p.print(",");
						
						p.print("\"" + stats.author.replace("\"", "\"\"") + "\"");
						
						
						stats.writeTextTo(p);
						p.println("");
						
						// Print HTML
						String parody = ((ThreadStats.threadCount)%2 == 0) ? "even" : "odd";
						phtml.println("  <tr><td class=\"thread " + parody + "\">");
						phtml.print("    <div class=\"title\">" + stats.subject + "</div>");
						phtml.print("    <div class=\"details\">");
						phtml.print(stats.author+". ");
						phtml.print(dateFormat.format(stats.authorDate)+". " + stats.numPosts + " posts.");
						phtml.println("    </div>");
						
						phtml.println("  </td></tr>");
					}
					
					// reset 
					stats.reset(msg);
				}

				stats.add(msg);
				lastMsg = msg; // reverse order always
			}
			phtml.println("    </table>");
			phtml.println("    </div>");
			phtml.println("  </body>");
			phtml.println("</html>");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}



class ThreadStats {
	public static int threadCount=0;
	public int numPosts = 0;
	Map<String,Integer>counts = new HashMap<String, Integer>();
	Set<String>guests = new TreeSet<String>();
	
	public String subject = "";
	public String author;
	public Date authorDate;
	
	public void reset(Message msg) throws MessagingException {
		//counts.clear();
		guests.clear();
		numPosts=0;
		for (String s : ThreadParser.SENDERS) {
			counts.put(s, 0);
		}
		// gross place but whatever
		threadCount++;
		
		// current thread
		subject = getThreadName(msg.getSubject());
		author = ThreadStats.getAuthor(msg);
		authorDate = msg.getSentDate();
	}
	
	public static boolean skipPost(Message msg) throws MessagingException {
		if (msg == null || msg.getSubject() == null) {
			return true;
		}
		String subj = getThreadName(msg.getSubject());
		if (ThreadParser.skipTitles.contains(subj)) {
			return true;
		}
		if (msg.getFrom() == null || msg.getFrom()[0] == null || msg.getFrom()[0].toString().equals("")) {
			return true;
		}
		
		String f = msg.getFrom()[0].toString().toLowerCase(); 
		if (f.contains("mail delivery subsystem")) {
			return true;
		}
		
		int num = 0;
		if (null != msg.getRecipients(Message.RecipientType.TO))
			num += msg.getRecipients(Message.RecipientType.TO).length;
		if (null != msg.getRecipients(Message.RecipientType.CC))
			num += msg.getRecipients(Message.RecipientType.CC).length;
		
		if (num < 6) return true;
		
		return false;
	}
	
	public void add(Message msg) throws MessagingException {
		numPosts++;
		String from = getAuthor(msg);
		
		if (authorDate != null && msg.getSentDate().before(authorDate)) {
			authorDate = msg.getSentDate();
			author = getAuthor(msg);
		}
		
		if (counts.containsKey(from)) {
			counts.put(from,1+counts.get(from));
			return;
		} else {
			guests.add(from);
		}
		
		//System.out.println("No match found for " + from);
	}
	
	public void writeTextTo(PrintStream p) {
		for (String s : ThreadParser.SENDERS) {
			p.print(",");
			p.print(counts.get(s));
		}
		
		if (guests.size() > 0) {
			StringBuffer buff = new StringBuffer();
			for (String s : guests) {
				buff.append(","+s);
			}
			p.print(",\""+buff.substring(1)+"\"");
		} else {
			p.print(",\"\"");
		}
	}
	
	public static void writeHeaderTo(PrintStream p) {
		for (String s : ThreadParser.SENDERS) {
			p.print(",");
			p.print(s);
		}
		p.print(",guests");
	}
	
	public static String getAuthor(Message msg) throws MessagingException {
		String from = ((InternetAddress)msg.getFrom()[0]).getPersonal();
		if (from == null) from = msg.getFrom()[0].toString();
		
		from = from.toLowerCase();
		for (String hit : ThreadParser.matches.keySet()) {
			if (from.contains(hit)) {
				return ThreadParser.matches.get(hit);
			}
		}
		
		return from;
	}
	
	static String getThreadName(String subject) {
		String s;
		do {
			s = subject;
			while (subject.length() >= 3 && subject.toLowerCase().startsWith("re:")) {
				subject = subject.substring(3);
				subject = subject.trim();
			}
			
			while (subject.length() >= 3 && subject.toLowerCase().startsWith("fw:")) {
				subject = subject.substring(3);
				subject = subject.trim();
			}
			
			while (subject.length() >= 3 && subject.toLowerCase().startsWith("fwd:")) {
				subject = subject.substring(4);
				subject = subject.trim();
			}
		} while (!subject.equals(s));
		
		// hacky bugfix
		subject = subject.replaceAll("\\s+", " ");
		
		return subject;
	}
}