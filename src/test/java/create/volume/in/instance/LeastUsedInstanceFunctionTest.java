package create.volume.in.instance;

import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class LeastUsedInstanceFunctionTest {
	@Inject
	CreateVolumeInInstanceClient client;

	@Test
	void testFunction() {
		LeastUsedInstance body = new LeastUsedInstance();
		body.setIp("create-volume-in-instance");
		assertEquals("create-volume-in-instance", client.apply(body).blockingGet().getIp());
	}
}
