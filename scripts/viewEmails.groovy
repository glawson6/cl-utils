import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.*
import groovy.servlet.*
import org.subethamail.smtp.AuthenticationHandler
import org.subethamail.smtp.AuthenticationHandlerFactory
import org.subethamail.smtp.TooMuchDataException
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter
import org.subethamail.smtp.server.SMTPServer
import org.apache.commons.io.*
import java.io.IOException;
import java.io.InputStream;
import org.subethamail.smtp.helper.SimpleMessageListener;
import javax.mail.internet.MimeMessage;

@Grapes([
        @Grab('org.eclipse.jetty:jetty-server:8.1.8.v20121106'),
        @Grab('org.eclipse.jetty:jetty-servlet:8.1.8.v20121106'),
        @Grab('javax.servlet:javax.servlet-api:3.0.1'),
        @Grab('org.subethamail:subethasmtp:3.1.7'),
        @Grab('commons-io:commons-io:2.5'),
        @GrabExclude('org.eclipse.jetty.orbit:javax.servlet')
])

def startJetty() {
    def jetty = new Server(8080)

    def context = new ServletContextHandler(jetty, '/', ServletContextHandler.SESSIONS)  // Allow sessions.
    //context.resourceBase = '.'  // Look in current dir for Groovy scripts.
    //context.addServlet(GroovyServlet, '*.groovy')  // All files ending with .groovy will be served.
    context.setAttribute('version', '1.0')  // Set an context attribute.
    String homePath = "target/test";
    ServletHolder holderHome = new ServletHolder("static-home", DefaultServlet.class);
    holderHome.setInitParameter("resourceBase",homePath);
    holderHome.setInitParameter("dirAllowed","true");
    holderHome.setInitParameter("pathInfoOnly","true");
    context.addServlet(holderHome,"/email/*");
    jetty.start()
}

class SMTPAuthHandler implements AuthenticationHandler {
    private static final String USER_IDENTITY = "User";
    private static final String PROMPT_USERNAME = "334 VXNlcm5hbWU6"; // VXNlcm5hbWU6 is base64 for "Username:"
    private static final String PROMPT_PASSWORD = "334 UGFzc3dvcmQ6"; // UGFzc3dvcmQ6 is base64 for "Password:"
    private int pass = 0;
    @Override
    public String auth(String clientInput) {
        String prompt;

        if (++pass == 1) {
            prompt = SMTPAuthHandler.PROMPT_USERNAME;
        } else if (pass == 2) {
            prompt = SMTPAuthHandler.PROMPT_PASSWORD;
        } else {
            pass = 0;
            prompt = null;
        }
        return prompt;
    }
    @Override
    public Object getIdentity() {
        return SMTPAuthHandler.USER_IDENTITY;
    }
}

class SMTPAuthHandlerFactory implements AuthenticationHandlerFactory {
    private static final String LOGIN_MECHANISM = "LOGIN";

    @Override
    public AuthenticationHandler create() {
        return new SMTPAuthHandler();
    }

    @Override
    public List<String> getAuthenticationMechanisms() {
        List<String> result = new ArrayList<String>();
        result.add(SMTPAuthHandlerFactory.LOGIN_MECHANISM);
        return result;
    }
}

def createDirectory(recipient){
    def fileName = "target/test/${recipient}"
// Defining a file handler/pointer to handle the file.
    def inputFile = new File(fileName).mkdir()
// Check if a file with same name exisits in the folder.
    fileName
}

class Lock{
    public static Object object = new Object();
}

def writeEmailToDirectory(directory, from, recipient, data){
    MimeMessage message = new MimeMessage(null, data)
    String content = message.getContent().toString()
    String subject = message.getSubject() == null ? "na":message.getSubject()
    InputStream contentStream = IOUtils.toInputStream(content,"utf-8");
    String dateString = String.valueOf(new Date())
    String fileName = "${directory}/${from}-${subject}-${dateString}.html"
    synchronized (Lock.object) {
        def fileOut = new FileOutputStream(fileName)
        int bytes = IOUtils.copy(contentStream, fileOut)
    }

}

def simpleMailListener(){
    return new SimpleMessageListener(){

        @Override
        boolean accept(String from, String recipient) {
            return true
        }

        @Override
        void deliver(String from, String recipient, InputStream data) throws TooMuchDataException, IOException {
            def directory = createDirectory(recipient);
            def hasWritten = writeEmailToDirectory(directory, from, recipient, data)
        }
    }
}

def bindAddress(){
      InetAddress.getByName("localhost")
}

def port(){
    2525
}

def startSmtpServer(){
    SMTPServer smtpServer = new SMTPServer(new SimpleMessageListenerAdapter( simpleMailListener()), new SMTPAuthHandlerFactory());
    smtpServer.setBindAddress(bindAddress());
    smtpServer.setPort(port());
    smtpServer.start();
}


println "Starting Jetty, press Ctrl+C to stop."
startJetty()
println "Starting SMTP Server"
startSmtpServer()
