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
		CreateUserDriveRequest createUserDriveRequest = new CreateUserDriveRequest();
		createUserDriveRequest.setUsername("username2");
		assertEquals("create-volume-in-instance", client.apply(createUserDriveRequest).blockingGet().getIp());
	}
}
