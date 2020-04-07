package create.volume.in.instance;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import io.micronaut.context.annotation.Value;
import io.micronaut.function.FunctionBean;
import io.micronaut.function.executor.FunctionInitializer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@FunctionBean("create-volume-in-instance")
public class CreateVolumeInInstanceFunction extends FunctionInitializer implements Function<LeastUsedInstance, LeastUsedInstance> {
	private Logger logger = LoggerFactory.getLogger(LeastUsedInstance.class);

	@Value("${loadBalancing.port}")
	public String port;
	@Value("${loadBalancing.path}")
	public String path;

	@Override
	public LeastUsedInstance apply(LeastUsedInstance instance) {
		List<String> ipAddresses = getIpAddresses();
		instance.setIp(findLeastUsedInstanceIp(getFreeDevicesCountInEndpoints(ipAddresses)));
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
		return endpoints.stream().collect(Collectors.toConcurrentMap(endpoint -> endpoint, o -> {
			try {
				Response execute = new OkHttpClient().newCall(new Request.Builder().url("http://" + o + ":" + port + path).get().build()).execute();
				return Long.parseUnsignedLong(Objects.requireNonNull(execute.body()).string());
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
		function.run(args, (context) -> function.apply(context.get(LeastUsedInstance.class)));
	}
}