import com.fasterxml.jackson.databind.ObjectMapper
import com.mashape.unirest.http.HttpMethod
@Grab('com.mashape.unirest:unirest-java:1.4.7')
@Grab('org.mongodb:bson:3.0.4')
@Grab('joda-time:joda-time:2.9.1')
@Grab('com.fasterxml.jackson.datatype:jackson-datatype-joda:2.4.4')
@GrabConfig(systemClassLoader=true)

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest
import groovy.transform.BaseScript
import org.bson.BsonArray;
import org.bson.Document
import org.bson.BsonDocument
import org.bson.json.JsonWriterSettings

@BaseScript Tools tools


if (args.length > 0) {

    args.eachWithIndex{
        it, index ->
            String fileName = args[index];
            System.out.println("")
            System.out.println("Using file => ${fileName}");
            String jsonFileText = new File(fileName).getText('UTF-8')
            Document jsonFileDoc = Document.parse(jsonFileText);

            def uri = jsonFileDoc.get("uri")
            def host = jsonFileDoc.get("host")
            def port = jsonFileDoc.get("port")
            def username = jsonFileDoc.get("username")
            def password = jsonFileDoc.get("password")
            def httpMethod = jsonFileDoc.get("httpMethod")
            String encoded = "${username}:${password}".bytes.encodeBase64().toString();
            String vhostsURL = "http://${host}:${port}/api/vhosts";
            HttpResponse<String> vhostResponse = Unirest.get(vhostsURL)
            // .header("authorization", "Basic Z3Vlc3Q6Z3Vlc3Q=")
                    .header("content-type", "application/json")
                    .header("authorization", "Basic ${encoded}")
                    .header("cache-control", "no-cache")
                    .header("postman-token", "6873faf9-41ce-ec09-5c9d-57b7e01f4c54")
                    .asString();
            System.out.println("Response Body => " + vhostResponse.getBody());
            System.out.println("Response Headers => " + vhostResponse.headers);
            System.out.println("Response Status => " + vhostResponse.status);

            String operationURL = "http://${host}:${port}${uri}";
            System.out.println("Sending operation to url => ${operationURL}")
            System.out.println("httpMethod => ${httpMethod.toString()}")
            HttpResponse<String> response = null;
            if (HttpMethod.GET.valueOf(httpMethod.toString())){
                response = Unirest.get(operationURL)
                // .header("authorization", "Basic Z3Vlc3Q6Z3Vlc3Q=")
                        .header("authorization", "Basic ${encoded}")
                        .header("content-type", "application/json")
                        .header("cache-control", "no-cache")
                        .header("postman-token", "6873faf9-41ce-ec09-5c9d-57b7e01f4c54")
                        .asString();
            } else {

                Document jsonToSend = jsonFileDoc.get("jsonToSend")
                System.out.println("json => ${jsonToSend.toJson()}")
                response = Unirest.post(operationURL)
                // .header("authorization", "Basic Z3Vlc3Q6Z3Vlc3Q=")
                        .header("content-type", "application/json")
                        .header("authorization", "Basic ${encoded}")
                        .header("cache-control", "no-cache")
                        .header("postman-token", "6873faf9-41ce-ec09-5c9d-57b7e01f4c54")
                        .body(jsonToSend.toJson())
                        .asString();
            }

            System.out.println("Response Body => " + response.getBody());
            System.out.println("Response Headers => " + response.headers);
            System.out.println("Response Status => " + response.status);
            System.out.println("Location => " + response.headers.get("location"))
            def slurper = new groovy.json.JsonSlurper ()
            def results =  slurper.parseText(response.getBody())

            results.each { result ->
                def name = result.name
                def vhost = result.vhost
                def deleteURL = "http://${host}:${port}/api/queues/dev/${name}"
                System.out.println("")
                System.out.println("name => ${name} vhost => ${vhost}")
                System.out.println("deleteURL => ${deleteURL}")
                HttpResponse<String> deleteResponse = Unirest.delete(deleteURL)
                // .header("authorization", "Basic Z3Vlc3Q6Z3Vlc3Q=")
                        .header("content-type", "application/json")
                        .header("authorization", "Basic ${encoded}")
                        .header("cache-control", "no-cache")
                        .header("postman-token", "6873faf9-41ce-ec09-5c9d-57b7e01f4c54")
                        .asString();
                System.out.println("Response Body => " + deleteResponse.getBody());
                System.out.println("Response Headers => " + deleteResponse.headers);
                System.out.println("Response Status => " + deleteResponse.status);

            }


    }

}
