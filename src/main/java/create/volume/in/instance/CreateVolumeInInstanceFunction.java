package create.volume.in.instance;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import io.micronaut.function.FunctionBean;
import io.micronaut.function.executor.FunctionInitializer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@FunctionBean("create-volume-in-instance")
public class CreateVolumeInInstanceFunction extends FunctionInitializer implements Function<CreateVolumeInInstance, CreateVolumeInInstance> {
	@Override
	public CreateVolumeInInstance apply(CreateVolumeInInstance msg) {
		List<String> privateIpAddress = getIpAddresses();
		msg.setName(findLeastUsed(getFreeDevicesCount(privateIpAddress)).toString());
		return msg;
	}

	private List<String> getIpAddresses() {
		AmazonEC2 client = AmazonEC2ClientBuilder.defaultClient();
		DescribeInstancesResult backedupInstances = client.describeInstances(new DescribeInstancesRequest().withFilters(new Filter("tag:project", Collections.singletonList("backedup"))));
		client.shutdown();
		return backedupInstances.getReservations().get(0).getInstances().stream().map(Instance::getPublicIpAddress).collect(Collectors.toList());
	}

	private ConcurrentMap<String, Long> getFreeDevicesCount(List<String> endpoints) {
		OkHttpClient httpClient = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();
		return endpoints.parallelStream().collect(Collectors.toConcurrentMap(endpoint -> endpoint, o -> {
			try {
				Response execute = httpClient.newCall(new Request.Builder().url("http://" + o + ":8080" + "/freeDevicesCount").get().build()).execute();
				return Long.parseUnsignedLong(Objects.requireNonNull(execute.body()).string());
			} catch(IOException e) {
				e.printStackTrace();
			}
			return 0L;
		}));
	}

	private Long findLeastUsed(ConcurrentMap<String, Long> freeDevicesCount) {
		Optional<Map.Entry<String, Long>> max = freeDevicesCount.entrySet().parallelStream().max(Comparator.comparingLong(Map.Entry::getValue));
		if(max.isPresent())
			return max.get().getValue();
		return 0L;
	}

	/**
	 * This main method allows running the function as a CLI application using: echo '{}' | java -jar function.jar
	 * where the argument to echo is the JSON to be parsed.
	 */
	public static void main(String... args) throws IOException {
		CreateVolumeInInstanceFunction function = new CreateVolumeInInstanceFunction();
		function.run(args, (context) -> function.apply(context.get(CreateVolumeInInstance.class)));
	}
}