package create.volume.in.instance;

import io.micronaut.function.FunctionBean;
import io.micronaut.function.executor.FunctionInitializer;

import java.io.IOException;
import java.util.function.Function;

@FunctionBean("create-volume-in-instance")
public class CreateVolumeInInstanceFunction extends FunctionInitializer implements Function<CreateVolumeInInstance, CreateVolumeInInstance> {
	@Override
	public CreateVolumeInInstance apply(CreateVolumeInInstance msg) {
		return msg;
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