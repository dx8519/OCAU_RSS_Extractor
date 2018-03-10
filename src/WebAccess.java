import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Calendar;

import org.eclipse.jgit.api.Git;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;


public class WebAccess {
	// prevent syncing before this 24HR time, set to 0 to remove
	static final int NOSYNCTIME = 7;
	// synchronisation interval in minutes
	static final int SYNCTIME = 30;
	// script for uploading
	static final String LOGINFILE = "login.txt";
	static final String GITPATH = ".";
	static final String TOKENFILE = "token.txt";

	int currentHour;
	String username;
	String password;
	String token;

	WebClient webClient;
	HtmlPage webPage;
	HtmlForm loginForm;
	HtmlTextInput usernameField;
	HtmlPasswordInput passwordField;
	HtmlSubmitInput loginButton;

	public void login() throws Exception {
		webPage = webClient.getPage("https://forums.overclockers.com.au/login");

		loginForm = webPage.getForms().get(0);

		usernameField = loginForm.getInputByName("login");
		usernameField.setValueAttribute(username);

		passwordField = loginForm.getInputByName("password");
        passwordField.setValueAttribute(password);

        loginButton = loginForm.getInputByValue("Log in");

        webPage = loginButton.click();
	}

	public void readLogin(String filename) {
		try {
			BufferedReader login = new BufferedReader(new FileReader(filename));
			username = login.readLine();
			password = login.readLine();
			login.close();
			BufferedReader tokenReader = new BufferedReader(new FileReader(TOKENFILE));
			token = tokenReader.readLine();
			tokenReader.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void push () {
		try {
			File file = new File(".git");
			Git git = Git.open(file);
			DirCache index = git.add().addFilepattern("index.rss").call();
			RevCommit commit = git.commit().setMessage("rss update").call();
			Iterable<PushResult> iterable = git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, "")).call();
			PushResult pushResult = iterable.iterator().next();
		}

		catch (Exception e) {
			e.printStackTrace();
		}

	}

 	public void homePage() throws Exception {

 		readLogin(LOGINFILE);

 		webClient = new WebClient();
		login();
    	while (true) {
    		try {
		        Page xmlFile = webClient.getPage("https://forums.overclockers.com.au/forums/for-sale-pc-related.15/index.rss");
		        WebResponse response = xmlFile.getWebResponse();
		        File rss = new File("index.rss");
		        BufferedWriter writer = new BufferedWriter(new FileWriter(rss));
		        writer.write(response.getContentAsString());
		        writer.close();
				push();
		        currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		        if(currentHour<NOSYNCTIME) {
		        	//sleep for 6.5 hours
		        	Thread.sleep(1000*60*SYNCTIME*13);
		        }
		        Thread.sleep(1000*60*SYNCTIME);
    		}
    		catch (Exception e) {
    			e.printStackTrace();
    			login();
    		}
    	}

	}
}
