package create.volume.in.instance;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class CreateUserDriveRequest {
	private String username;
	private String token;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
