package create.volume.in.instance;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class CreateUserDriveRequest {
	private String username;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
