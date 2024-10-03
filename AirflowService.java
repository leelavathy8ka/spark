import com.example.springbootrestapi.service.S3Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Service
public class AirflowService {

    private final RestTemplate restTemplate;
    private final S3Service s3Service;

    public AirflowService(RestTemplate restTemplate, S3Service s3Service) {
        this.restTemplate = restTemplate;
        this.s3Service = s3Service;
    }

    public String getDagDetails(String bucketName, String key) {
        // Fetch the token from S3
        String token = s3Service.getTokenFromS3(bucketName, key);

        // Define the Airflow API endpoint
        String url = "http://your-airflow-url/api/v1/dags/famdag";

        // Set up HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        // Create the request entity
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Make the REST call to fetch DAG details
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        // Return the response body (DAG details)
        return response.getBody();
    }
}
