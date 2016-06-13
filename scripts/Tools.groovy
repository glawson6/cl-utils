import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest
import org.bson.Document
import org.bson.json.JsonWriterSettings
import org.joda.time.Interval

abstract class Tools extends Script {

    def executeOnShell(String command) {
        return executeOnShell(command, new File(System.properties.'user.dir'))
    }

    def executeOnShell(String command, File workingDir) {
        println command
        def process = new ProcessBuilder(addShellPrefix(command))
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()
        process.inputStream.eachLine { println it }
        process.waitFor();
        return process.exitValue()
    }

    def addShellPrefix(String command) {
        commandArray = new String[3]
        commandArray[0] = "sh"
        commandArray[1] = "-c"
        commandArray[2] = command
        return commandArray
    }

    def resolveBasicAuth(String fileName) {
        String username = null;
        String password = null;
        if (null != fileName) {
            System.out.println("Using fileName ${fileName} for authorization credentials");
            if (null != fileName) {
                String jsonFileText = new File(fileName).getText('UTF-8')
                Document jsonFileDoc = Document.parse(jsonFileText);
                username = jsonFileDoc.get("username");
                password = jsonFileDoc.get("password");
            }
        } else if (null != System.getProperty("username") && null != System.getProperty("password")) {
            username = System.getProperty("username");
            password = System.getProperty("password");
        } else if  (null != System.getenv("username") && null != System.getenv("password")){
            username = System.getenv("username");
            password = System.getenv("password");
        } else {
            String message = "username and password fields not found in System properties, environment, or in json file " +
                    "with username and password as top level fields...."
            throw new IllegalArgumentException(message);
        }
        String encoded = "${username}:${password}".bytes.encodeBase64().toString();
        return encoded;
    }

    def getAccessToken(String authorization){
        String apiHost = (null != System.getProperty("api.host")) ? System.getProperty("api.host") : "sandbox.api.manheim.com";
        String tokenURL = "https://${apiHost}:443/oauth2/token";
        System.out.println("Sending api requests to host => ${apiHost}");
        System.out.println("Sending token to request url => ${tokenURL}");
        HttpResponse<String> tokenResponse = Unirest.post(tokenURL)
                .header("authorization", "Basic ${authorization}")
                .body("{\n    \"grant_type\":\"client_credentials\"\n}")
                .asString();
        Document tokenDoc = Document.parse(tokenResponse.getBody());
        System.out.println("Token Response Body => " + tokenResponse.getBody());
        System.out.println("Token Response Headers => " + tokenResponse.headers);
        System.out.println("Access token => " + tokenDoc.get("accessToken"));
        String accessToken = tokenDoc.get("accessToken");
        return accessToken;
    }

    def getAPIPort(){ return (null != System.getProperty("api.port")) ? System.getProperty("api.port") : "8080"; }

    def getAPIHost(){
        return (null != System.getProperty("api.host")) ? System.getProperty("api.host") : "0.0.0.0";
    }
    def createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        JodaModule jodaModule = new JodaModule();

        objectMapper.registerModule(jodaModule);
        return objectMapper;
    }

    /*
    def setupUniRest(){
        def jacksonObjectMapper = createObjectMapper()
        Unirest.setObjectMapper(new ObjectMapper() {

            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return jacksonObjectMapper.readValue(value, valueType);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public String writeValue(Object value) {
                try {
                    return jacksonObjectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    */

}
