/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gcfv2;

import java.io.BufferedWriter;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

// [START healthcare_fhir_execute_bundle]
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.healthcare.v1.CloudHealthcare;
import com.google.api.services.healthcare.v1.CloudHealthcareScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import org.apache.http.HttpEntity;
//import org.apache.http.HttpResponse as HttpResponse2;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

public class HelloHttpFunction implements HttpFunction {
  private static final String FHIR_NAME = "projects/<<your-project-id>>/locations/us-central1/datasets/dataset-ohs-gcp/fhirStores/fhirstore-ohs-gcp";
  private static final JsonFactory JSON_FACTORY = new JacksonFactory();
  private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();

  public void service(final HttpRequest request_arg, final HttpResponse response_arg) throws Exception {
    final BufferedWriter writer = response_arg.getWriter();
    writer.write("Hello world!");
    
    String fhir_data = "";
    
    try{
     // api_url = request_arg.getQueryParameters().get("api_url").get(0);
      fhir_data = request_arg.getQueryParameters().get("fhir_data").get(0);
      }catch(Exception e){
        writer.write(fhir_data);
          fhir_data = "";
          writer.write("Not Applicable");
          return;
      }
      response_arg.appendHeader("Access-Control-Allow-Origin", "*");

    fhirStoreExecuteBundle(fhir_data);

  }

  public static void fhirStoreExecuteBundle(String data)
      throws IOException, URISyntaxException {
   String fhirStoreName = FHIR_NAME;
   String dataFhir = "{\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":[{\"resource\":{\"resourceType\":\"Patient\",\"active\":true,\"name\":[{\"family\":\"Test6March18\",\"given\":[\"Abirami\"]}],\"telecom\":[{\"system\":\"phone\",\"value\":\"9008601605\"}],\"gender\":\"female\",\"birthDate\":\"2000-01-01\",\"address\":[{\"city\":\"Bangalore\",\"country\":\"India\"}]}}]}";
   String dataR4 = data.replace("}]}}]}","}]},\"request\": {\"method\": \"POST\",\"url\": \"Patient\"}}]}");
   data = dataR4;
    // Initialize the client, which will be used to interact with the service.
    CloudHealthcare client = createClient();
    HttpClient httpClient = HttpClients.createDefault();
    String baseUri = String.format("%sv1/%s/fhir", client.getRootUrl(), fhirStoreName);
    URIBuilder uriBuilder = new URIBuilder(baseUri).setParameter("access_token", getAccessToken());
    StringEntity requestEntity = new StringEntity(data);

    HttpUriRequest request =
        RequestBuilder.post()
            .setUri(uriBuilder.build())
            .setEntity(requestEntity)
            .addHeader("Content-Type", "application/fhir+json")
            .addHeader("Accept-Charset", "utf-8")
            .addHeader("Accept", "application/fhir+json; charset=utf-8")
            .build();

    // Execute the request and process the results.
    org.apache.http.HttpResponse response = httpClient.execute(request);
    HttpEntity responseEntity = response.getEntity();
    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
      System.err.print(
          String.format(
              "Exception executing FHIR bundle: %s\n", response.getStatusLine().toString()));
      responseEntity.writeTo(System.err);
      throw new RuntimeException();
    }
    System.out.print("FHIR bundle executed: ");
    responseEntity.writeTo(System.out);
  }

  private static CloudHealthcare createClient() throws IOException {
    // Use Application Default Credentials (ADC) to authenticate the requests
    // For more information see https://cloud.google.com/docs/authentication/production
    GoogleCredentials credential =
        GoogleCredentials.getApplicationDefault()
            .createScoped(Collections.singleton(CloudHealthcareScopes.CLOUD_PLATFORM));

    // Create a HttpRequestInitializer, which will provide a baseline configuration to all requests.
    HttpRequestInitializer requestInitializer =
        request -> {
          new HttpCredentialsAdapter(credential).initialize(request);
          request.setConnectTimeout(60000); // 1 minute connect timeout
          request.setReadTimeout(60000); // 1 minute read timeout
        };

    // Build the client for interacting with the service.
    return new CloudHealthcare.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
        .setApplicationName("your-application-name")
        .build();
  }

  private static String getAccessToken() throws IOException {
    GoogleCredentials credential =
        GoogleCredentials.getApplicationDefault()
            .createScoped(Collections.singleton(CloudHealthcareScopes.CLOUD_PLATFORM));
    
    return credential.refreshAccessToken().getTokenValue();
  }

}
