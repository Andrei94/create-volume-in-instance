package create.volume.in.instance;

import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class CreateVolumeInInstanceFunctionTest {
	@Inject
	CreateVolumeInInstanceClient client;

	@Test
	public void testFunction() {
		CreateVolumeInInstance body = new CreateVolumeInInstance();
		body.setName("create-volume-in-instance");
		assertEquals("create-volume-in-instance", client.apply(body).blockingGet().getName());
	}
}
