package create.volume.in.instance;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.google.gson.Gson;
import io.micronaut.function.FunctionBean;
import io.micronaut.function.executor.FunctionInitializer;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@FunctionBean("create-volume-in-instance")
public class CreateVolumeInInstanceFunction extends FunctionInitializer implements Function<CreateUserDriveRequest, CreateUserDriveResponse> {
	private final Logger logger = LoggerFactory.getLogger(CreateUserDriveResponse.class);
	private final OkHttpClient httpClient = new OkHttpClient.Builder()
			.connectTimeout(20, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.build();

	@Override
	public CreateUserDriveResponse apply(CreateUserDriveRequest request) {
		List<String> ipAddresses = getIpAddresses();
		String leastUsedInstanceIp = findLeastUsedInstanceIp(getFreeDevicesCountInEndpoints(ipAddresses));
		CreateUserDriveResponse createUserDriveResponse = new CreateUserDriveResponse();
		try {
			String url = "http://" + leastUsedInstanceIp + ":8080/volume/createVolume/";
			Response response = httpClient.newCall(new Request.Builder().url(url)
					.put(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new Gson().toJson(request))).build())
					.execute();
			String createVolumeResponse = Objects.requireNonNull(response.body()).string();
			if(createVolumeResponse.equals("invalid token"))
				return createUserDriveResponse;
			createUserDriveResponse.setVolumeId(createVolumeResponse.split(" ")[0]);
			createUserDriveResponse.setToken(createVolumeResponse.split(" ")[1]);
			createUserDriveResponse.setIp(leastUsedInstanceIp);
		} catch(IOException e) {
			logger.error("An error occurred", e);
		}
		return createUserDriveResponse;
	}

	private List<String> getIpAddresses() {
		AmazonEC2 client = AmazonEC2ClientBuilder.defaultClient();
		DescribeInstancesResult backedupInstances = client.describeInstances(new DescribeInstancesRequest().
				withFilters(new Filter("tag:project", Collections.singletonList("backedup")),
						new Filter("instance-state-name", Collections.singletonList("running"))));
		client.shutdown();
		return backedupInstances.getReservations().stream()
				.flatMap(reservation -> reservation.getInstances().stream()).collect(Collectors.toList())
				.stream().map(Instance::getPublicIpAddress).collect(Collectors.toList());
	}

	private ConcurrentMap<String, Long> getFreeDevicesCountInEndpoints(List<String> endpoints) {
		return endpoints.parallelStream().collect(Collectors.toConcurrentMap(endpoint -> endpoint, o -> {
			try {
				Response response = httpClient.newCall(new Request.Builder().url("http://" + o + ":8080/freeDevicesCount").get().build()).execute();
				return Long.parseUnsignedLong(Objects.requireNonNull(response.body()).string());
			} catch(IOException e) {
				logger.error("An error occurred", e);
			}
			return 0L;
		}));
	}

	private String findLeastUsedInstanceIp(ConcurrentMap<String, Long> freeDevicesCount) {
		Optional<Map.Entry<String, Long>> max = freeDevicesCount.entrySet().parallelStream().max(Comparator.comparingLong(Map.Entry::getValue));
		if(max.isPresent())
			return max.get().getKey();
		return "";
	}

	/**
	 * This main method allows running the function as a CLI application using: echo '{}' | java -jar function.jar
	 * where the argument to echo is the JSON to be parsed.
	 */
	public static void main(String... args) throws IOException {
		CreateVolumeInInstanceFunction function = new CreateVolumeInInstanceFunction();
		function.run(args, (context) -> function.apply(context.get(CreateUserDriveRequest.class)));
	}
}