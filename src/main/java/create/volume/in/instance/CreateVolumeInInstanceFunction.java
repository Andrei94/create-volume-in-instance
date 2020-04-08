package create.volume.in.instance;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import io.micronaut.context.annotation.Value;
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
public class CreateVolumeInInstanceFunction extends FunctionInitializer implements Function<CreateUserDriveRequest, LeastUsedInstance> {
	private Logger logger = LoggerFactory.getLogger(LeastUsedInstance.class);
	private OkHttpClient httpClient = new OkHttpClient.Builder()
			.connectTimeout(20, TimeUnit.SECONDS)
			.readTimeout(40, TimeUnit.SECONDS)
			.build();

	@Value("${loadBalancing.port}")
	public String port;
	@Value("${loadBalancing.path}")
	public String path;
	@Value("${subscription-plan.createVolumePath}")
	public String createVolumePath;

	@Override
	public LeastUsedInstance apply(CreateUserDriveRequest request) {
		List<String> ipAddresses = getIpAddresses();
		String leastUsedInstanceIp = findLeastUsedInstanceIp(getFreeDevicesCountInEndpoints(ipAddresses));
		LeastUsedInstance instance = new LeastUsedInstance();
		try {
			String url = "http://" + leastUsedInstanceIp + ":" + port + createVolumePath + "/" + request.getUsername();
			Response response = httpClient.newCall(new Request.Builder().url(url)
					.put(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "")).build())
					.execute();
			String createVolumeResponse = Objects.requireNonNull(response.body()).string();
			instance.setUser(createVolumeResponse.split(" ")[0]);
			instance.setToken(createVolumeResponse.split(" ")[1]);
			instance.setIp(leastUsedInstanceIp);
		} catch(IOException e) {
			logger.error("An error occurred", e);
		}
		return instance;
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
				Response response = httpClient.newCall(new Request.Builder().url("http://" + o + ":" + port + path).get().build()).execute();
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